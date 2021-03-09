/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.provider.mempool;

import bisq.core.util.coin.CoinUtil;

import bisq.common.util.Tuple2;

import org.bitcoinj.core.Coin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

import org.jetbrains.annotations.Nullable;

@Slf4j
@Getter
public class TxValidator {
    private List<String> errorList;
    private String txId;
    private Coin amount;
    @Nullable
    private Boolean isFeeCurrencyBtc = null;
    @Nullable
    private Long blockHeight = null;
    @Setter
    private String jsonTxt;
    @Setter
    private boolean mockFeeLookups = false; // used by TxValidatorTest


    public TxValidator(String txId, Coin amount, @Nullable Boolean isFeeCurrencyBtc, @Nullable Long blockHeight) {
        this.txId = txId;
        this.amount = amount;
        this.isFeeCurrencyBtc = isFeeCurrencyBtc;
        this.blockHeight = blockHeight;
        errorList = new ArrayList<>();
        this.jsonTxt = "";
    }

    public TxValidator(String txId, long blockHeight) {
        this.txId = txId;
        this.blockHeight = blockHeight;
        errorList = new ArrayList<>();
        this.jsonTxt = "";
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public TxValidator parseJsonValidateMakerFeeTx(String jsonTxt, List<String> btcFeeReceivers) {
        this.jsonTxt = jsonTxt;
        boolean status = initialSanityChecks(txId, jsonTxt);
        try {
            if (status) {
                if (blockHeight == null)
                    blockHeight = getTxBlockHeight(jsonTxt);
                if (isFeeCurrencyBtc) {
                    status &= checkFeeAddressBTC(jsonTxt, btcFeeReceivers);
                    status &= checkFeeAmountBTC(jsonTxt, amount, true, blockHeight);
                } else {
                    status &= checkFeeAmountBSQ(jsonTxt, amount, true, blockHeight);
                }
            }
        } catch (JsonSyntaxException e) {
            String s = "The maker fee tx JSON validation failed with reason: " + e.toString();
            log.warn(s);
            errorList.add(s);
            status = false;
        }
        return endResult("Maker tx validation", status);
    }

    public TxValidator parseJsonValidateTakerFeeTx(String jsonTxt, List<String> btcFeeReceivers) {
        this.jsonTxt = jsonTxt;
        boolean status = initialSanityChecks(txId, jsonTxt);
        try {
            if (status) {
                if (isFeeCurrencyBtc == null) {
                    isFeeCurrencyBtc = checkFeeAddressBTC(jsonTxt, btcFeeReceivers);
                }
                if (isFeeCurrencyBtc) {
                    status &= checkFeeAddressBTC(jsonTxt, btcFeeReceivers);
                    status &= checkFeeAmountBTC(jsonTxt, amount, false, getTxBlockHeight(jsonTxt));
                } else {
                    status &= checkFeeAmountBSQ(jsonTxt, amount, false, getTxBlockHeight(jsonTxt));
                }
            }
        } catch (JsonSyntaxException e) {
            String s = "The taker fee tx JSON validation failed with reason: " + e.toString();
            log.warn(s);
            errorList.add(s);
            status = false;
        }
        return endResult("Taker tx validation", status);
    }

    public long parseJsonValidateTx() {
        if (!initialSanityChecks(txId, jsonTxt)) {
            return -1;
        }
        return getTxConfirms(jsonTxt, blockHeight.longValue());
    }

    public String extractDepositTxIdFromMakerOutspends() {
        JsonArray jsonTopArray = new Gson().fromJson(jsonTxt, JsonArray.class);
        if (jsonTopArray == null || jsonTopArray.size() < 2) {
            return "ERROR";
            //throw new JsonSyntaxException("not enough outspends");
        }
        // outspend 1 is the fee tx
        // outspend 2 is the deposit tx
        // outspend 3 is the change tx
        JsonObject jsonVout1 = jsonTopArray.get(1).getAsJsonObject();
        JsonElement jsonSpent = jsonVout1.get("spent");
        if (jsonSpent.getAsBoolean()) {
            JsonElement jsonTxId = jsonVout1.get("txid");
            return jsonTxId.getAsString();
        }
        return null;
    }

    public String extractTakerTxIdFromDepositTx(String knownMakerTxId) {
        // get vin 0 and vin 1
        // one of them will equal knownMakerTxId
        // the other will be the takerTxId
        try {
            JsonArray jsonVin = getVinAndVout(jsonTxt).first;
            if (jsonVin.size() != 2)
                throw new JsonSyntaxException("not a deposit tx, as it did not have 2 inputs");
            JsonObject jsonVin0 = jsonVin.get(0).getAsJsonObject();
            JsonObject jsonVin1 = jsonVin.get(1).getAsJsonObject();
            JsonElement jsonTxId0 = jsonVin0.get("txid");
            JsonElement jsonTxId1 = jsonVin1.get("txid");
            if (jsonTxId0.getAsString().equalsIgnoreCase(knownMakerTxId)) {
                return jsonTxId1.getAsString();
            }
            if (jsonTxId1.getAsString().equalsIgnoreCase(knownMakerTxId)) {
                return jsonTxId0.getAsString();
            }
        } catch (JsonSyntaxException e) {
            log.warn(e.toString());
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////

    private boolean checkFeeAddressBTC(String jsonTxt, List<String> btcFeeReceivers) {
        try {
            JsonArray jsonVout = getVinAndVout(jsonTxt).second;
            JsonObject jsonVout0 = jsonVout.get(0).getAsJsonObject();
            JsonElement jsonFeeAddress = jsonVout0.get("scriptpubkey_address");
            log.debug("fee address: {}", jsonFeeAddress.getAsString());
            if (btcFeeReceivers.contains(jsonFeeAddress.getAsString())) {
                return true;
            } else if (getTxBlockHeight(jsonTxt) < 600000L) {
                log.warn("Leniency rule, unrecognised fee receiver but its a really old offer so let it pass, {}", jsonFeeAddress.getAsString());
                return true;
            } else {
                String error = "fee address: " + jsonFeeAddress.getAsString() + " was not a known BTC fee receiver";
                errorList.add(error);
                log.info(error);
            }
        } catch (JsonSyntaxException e) {
            errorList.add(e.toString());
            log.warn(e.toString());
        }
        return false;
    }

    private boolean checkFeeAmountBTC(String jsonTxt, Coin tradeAmount, boolean isMaker, long blockHeight) {
        JsonArray jsonVin;
        JsonArray jsonVout;
        jsonVin = getVinAndVout(jsonTxt).first;
        jsonVout = getVinAndVout(jsonTxt).second;
        JsonObject jsonVin0 = jsonVin.get(0).getAsJsonObject();
        JsonObject jsonVout0 = jsonVout.get(0).getAsJsonObject();
        JsonElement jsonVIn0Value = jsonVin0.getAsJsonObject("prevout").get("value");
        JsonElement jsonFeeValue = jsonVout0.get("value");
        if (jsonVIn0Value == null || jsonFeeValue == null) {
            throw new JsonSyntaxException("vin/vout missing data");
        }
        long feeValue = jsonFeeValue.getAsLong();
        log.debug("BTC fee: {}", feeValue);
        Coin expectedFee = isMaker ?
                getMakerFeeHistorical(true, tradeAmount, blockHeight) :
                getTakerFeeHistorical(true, tradeAmount, blockHeight);
        Double leniencyCalc = feeValue / (double)expectedFee.getValue();
        String description = "Expected BTC fee: " + expectedFee.toString() + " sats , actual fee paid: " + Coin.valueOf(feeValue).toString() + " sats";
        if (expectedFee.getValue() == feeValue) {
            log.debug("The fee matched what we expected");
            return true;
        } else if (expectedFee.getValue() < feeValue) {
            log.warn("The fee was more than what we expected: " + description);
            return true;
        } else if (leniencyCalc > 0.85) {
            log.warn("Leniency rule: the fee was low, but above 85% of what was expected {} {}", leniencyCalc, description);
            return true;
        } else {
            String error = "UNDERPAID. " + description;
            errorList.add(error);
            log.warn(error);
        }
        return false;
    }

    private boolean checkFeeAmountBSQ(String jsonTxt, Coin tradeAmount, boolean isMaker, long blockHeight) {
        JsonArray jsonVin;
        JsonArray jsonVout;
        jsonVin = getVinAndVout(jsonTxt).first;
        jsonVout = getVinAndVout(jsonTxt).second;
        JsonObject jsonVin0 = jsonVin.get(0).getAsJsonObject();
        JsonObject jsonVout0 = jsonVout.get(0).getAsJsonObject();
        JsonElement jsonVIn0Value = jsonVin0.getAsJsonObject("prevout").get("value");
        JsonElement jsonFeeValue = jsonVout0.get("value");
        if (jsonVIn0Value == null || jsonFeeValue == null) {
            throw new JsonSyntaxException("vin/vout missing data");
        }
        Coin expectedFee = isMaker ?
                getMakerFeeHistorical(false, tradeAmount, blockHeight) :
                getTakerFeeHistorical(false, tradeAmount, blockHeight);
        long feeValue = jsonVIn0Value.getAsLong() - jsonFeeValue.getAsLong();
        // if the first output (BSQ) is greater than the first input (BSQ) include the second input (presumably BSQ)
        if (jsonFeeValue.getAsLong() > jsonVIn0Value.getAsLong()) {
            // in this case 2 or more UTXOs were spent to pay the fee:
            JsonObject jsonVin1 = jsonVin.get(1).getAsJsonObject();
            JsonElement jsonVIn1Value = jsonVin1.getAsJsonObject("prevout").get("value");
            feeValue += jsonVIn1Value.getAsLong();
        }
        log.debug("BURNT BSQ maker fee: {} BSQ ({} sats)", (double) feeValue / 100.0, feeValue);
        Double leniencyCalc = feeValue / (double) expectedFee.getValue();
        String description = String.format("Expected fee: %.2f BSQ, actual fee paid: %.2f BSQ",
                (double) expectedFee.getValue() / 100.0, (double) feeValue / 100.0);
        if (expectedFee.getValue() == feeValue) {
            log.debug("The fee matched what we expected");
            return true;
        } else if (expectedFee.getValue() < feeValue) {
            log.warn("The fee was more than what we expected. " + description);
            return true;
        } else if (leniencyCalc > 0.85) {
            log.warn("Leniency rule: the fee was low, but above 85% of what was expected {} {}", leniencyCalc, description);
            return true;
        } else {
            errorList.add(description);
            log.warn(description);
        }
        return false;
    }

    private static Tuple2<JsonArray, JsonArray> getVinAndVout(String jsonTxt) throws JsonSyntaxException {
        // there should always be "vout" at the top level
        // check that there are 2 or 3 vout elements: the fee, the reserved for trade, optional change
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        if (json.get("vin") == null || json.get("vout") == null) {
            throw new JsonSyntaxException("missing vin/vout");
        }
        JsonArray jsonVin = json.get("vin").getAsJsonArray();
        JsonArray jsonVout = json.get("vout").getAsJsonArray();
        if (jsonVin == null || jsonVout == null || jsonVin.size() < 1 || jsonVout.size() < 2) {
            throw new JsonSyntaxException("not enough vins/vouts");
        }
        return new Tuple2<>(jsonVin, jsonVout);
    }

    private static boolean initialSanityChecks(String txId, String jsonTxt) {
        // there should always be "status" container element at the top level
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        if (json.get("status") == null) {
            return false;
        }
        // there should always be "txid" string element at the top level
        if (json.get("txid") == null) {
            return false;
        }
        // txid should match what we requested
        if (!txId.equals(json.get("txid").getAsString())) {
            return false;
        }
        JsonObject jsonStatus = json.get("status").getAsJsonObject();
        JsonElement jsonConfirmed = jsonStatus.get("confirmed");
        return (jsonConfirmed != null);
        // the json is valid and it contains a "confirmed" field then tx is known to mempool.space
        // we don't care if it is confirmed or not, just that it exists.
    }

    // this would be useful for the arbitrator verifying that the delayed payout tx is confirmed
    private static long getTxBlockHeight(String jsonTxt) {
        // there should always be "status" container element at the top level
        JsonObject json = new Gson().fromJson(jsonTxt, JsonObject.class);
        if (json.get("status") == null) {
            return -1L;
        }
        JsonObject jsonStatus = json.get("status").getAsJsonObject();
        JsonElement jsonConfirmed = jsonStatus.get("confirmed");
        if (jsonConfirmed == null) {
            return -1L;
        }
        if (jsonConfirmed.getAsBoolean()) {
            // it is confirmed, lets get the block height
            JsonElement jsonBlockHeight = jsonStatus.get("block_height");
            if (jsonBlockHeight == null) {
                return -1L; // block height error
            }
            return (jsonBlockHeight.getAsLong());
        }
        return 0L;  // in mempool, not confirmed yet
    }

    private static long getTxConfirms(String jsonTxt, long chainHeight) {
        long blockHeight = getTxBlockHeight(jsonTxt);
        if (blockHeight > 0) {
            return (chainHeight - blockHeight) + 1; // if it is in the current block it has 1 conf
        }
        return 0;  // 0 indicates unconfirmed
    }

    private Coin getMakerFeeHistorical(boolean isFeeCurrencyBtc, Coin tradeAmount, long blockHeight) {
        if (mockFeeLookups) {
            return isFeeCurrencyBtc ? Coin.valueOf(5000) : Coin.valueOf(1);
        }
        return CoinUtil.getMakerFeeHistorical(true, tradeAmount, blockHeight);
    }

    private Coin getTakerFeeHistorical(boolean isFeeCurrencyBtc, Coin tradeAmount, long blockHeight) {
        if (mockFeeLookups) {
            return isFeeCurrencyBtc ? Coin.valueOf(7000) : Coin.valueOf(2);
        }
        return CoinUtil.getTakerFeeHistorical(true, tradeAmount, blockHeight);
    }

    public TxValidator endResult(String title, boolean status) {
        log.info("{} : {}", title, status ? "SUCCESS" : "FAIL");
        if (!status) {
            errorList.add(title);
        }
        return this;
    }

    public boolean isFail() {
        return errorList.size() > 0;
    }

    public boolean getResult() {
        return errorList.size() == 0;
    }

    public String errorSummary() {
        return errorList.toString().substring(0, Math.min(85, errorList.toString().length()));
    }

    public String toString() {
        return errorList.toString();
    }
}
