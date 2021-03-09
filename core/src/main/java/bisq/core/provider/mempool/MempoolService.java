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

import bisq.core.dao.DaoFacade;
import bisq.core.filter.FilterManager;
import bisq.core.offer.OfferPayload;
import bisq.core.provider.MempoolHttpClient;
import bisq.core.trade.Trade;
import bisq.core.user.Preferences;

import org.bitcoinj.core.Coin;

import bisq.common.UserThread;
import bisq.common.config.Config;

import com.google.inject.Inject;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MempoolService {
    private final MempoolHttpClient mempoolHttpClient;
    private final Config config;
    private static Preferences preferences;
    private final FilterManager filterManager;
    private final DaoFacade daoFacade;
    private List<String> btcFeeReceivers;

    @Inject
    public MempoolService(MempoolHttpClient mempoolHttpClient,
                          Config config,
                          Preferences preferences,
                          FilterManager filterManager,
                          DaoFacade daoFacade) {
        this.mempoolHttpClient = mempoolHttpClient;
        this.config = config;
        this.preferences = preferences;
        this.filterManager = filterManager;
        this.daoFacade = daoFacade;
        this.btcFeeReceivers = getAllBtcFeeReceivers();
    }

    public void validateOfferMakerTx(OfferPayload offerPayload, Consumer<TxValidator> resultHandler) {
        validateOfferMakerTx2(new TxValidator(offerPayload.getOfferFeePaymentTxId(), Coin.valueOf(offerPayload.getAmount()), offerPayload.isCurrencyForMakerFeeBtc(), offerPayload.getBlockHeightAtOfferCreation())
                , resultHandler);
    }

    public void validateOfferMakerTx2(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!canRequestBeMade()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, mempoolHttpClient);
        retryValidateOfferMakerTx(mempoolRequest, txValidator, resultHandler);
    }

    public void validateOfferTakerTx(Trade trade, Consumer<TxValidator> resultHandler) {
        validateOfferTakerTx(new TxValidator(trade.getTakerFeeTxId(), trade.getTradeAmount(), trade.isCurrencyForTakerFeeBtc(), null)
                , resultHandler);
    }

    public void validateOfferTakerTx(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!canRequestBeMade()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, mempoolHttpClient);
        retryValidateOfferTakerTx(mempoolRequest, txValidator, resultHandler);
    }

    public void checkTxIsConfirmed(String txId, Consumer<TxValidator> resultHandler) {
        if (!canRequestBeMade()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, mempoolHttpClient);
        SettableFuture<String> future = SettableFuture.create();
        TxValidator txValidator = new TxValidator(txId, daoFacade.getChainHeight());
        Futures.addCallback(future, callbackForTxRequest(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txId, null);
    }

    public void getMakerOutspends(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!canRequestBeMade()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, mempoolHttpClient);
        SettableFuture<String> future = SettableFuture.create();
        Futures.addCallback(future, callbackForTxRequest(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txValidator.getTxId(), "/outspends");
    }

    public void validateDepositTx(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!canRequestBeMade()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, mempoolHttpClient);
        SettableFuture<String> future = SettableFuture.create();
        Futures.addCallback(future, callbackForTxRequest(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txValidator.getTxId(), null);
    }

    // ///////////////////////////

    private void retryValidateOfferMakerTx(MempoolRequest mempoolRequest, TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        SettableFuture<String> future = SettableFuture.create();
        Futures.addCallback(future, callbackForMakerTxValidation(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txValidator.getTxId(), null);
    }

    private void retryValidateOfferTakerTx(MempoolRequest mempoolRequest, TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        SettableFuture<String> future = SettableFuture.create();
        Futures.addCallback(future, callbackForTakerTxValidation(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txValidator.getTxId(), null);
    }

    private FutureCallback<String> callbackForMakerTxValidation(MempoolRequest theRequest, TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        FutureCallback<String> myCallback = new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String jsonTxt) {
                UserThread.execute(() -> {
                    resultHandler.accept(txValidator.parseJsonValidateMakerFeeTx(jsonTxt, btcFeeReceivers));
                });
            }
            @Override
            public void onFailure(Throwable throwable) {
                log.warn("onFailure - {}", throwable.toString());
                if (theRequest.switchToAnotherProvider()) {
                    retryValidateOfferMakerTx(theRequest, txValidator, resultHandler);
                } else {
                    UserThread.execute(() -> {
                        // exhausted all providers, let user know of failure
                        resultHandler.accept(txValidator.endResult("Maker Tx not found", false));
                    });
                }
            }
        };
        return myCallback;
    }

    private FutureCallback<String> callbackForTakerTxValidation(MempoolRequest theRequest, TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        FutureCallback<String> myCallback = new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String jsonTxt) {
                UserThread.execute(() -> {
                    resultHandler.accept(txValidator.parseJsonValidateTakerFeeTx(jsonTxt, btcFeeReceivers));
                });
            }
            @Override
            public void onFailure(Throwable throwable) {
                log.warn("onFailure - {}", throwable.toString());
                if (theRequest.switchToAnotherProvider()) {
                    retryValidateOfferTakerTx(theRequest, txValidator, resultHandler);
                } else {
                    UserThread.execute(() -> {
                        // exhausted all providers, let user know of failure
                        resultHandler.accept(txValidator.endResult("Taker Tx not found", false));
                    });
                }
            }
        };
        return myCallback;
    }

    private FutureCallback<String> callbackForTxRequest(MempoolRequest theRequest, TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        FutureCallback<String> myCallback = new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String jsonTxt) {
                UserThread.execute(() -> {
                    txValidator.setJsonTxt(jsonTxt);
                    resultHandler.accept(txValidator);
                });
            }
            @Override
            public void onFailure(Throwable throwable) {
                log.warn("onFailure - {}", throwable.toString());
                UserThread.execute(() -> {
                    resultHandler.accept(txValidator.endResult("Tx not found", false));
                });
            }
        };
        return myCallback;
    }

    // /////////////////////////////

    private List<String> getAllBtcFeeReceivers() {
        List<String> btcFeeReceivers = new ArrayList<> ();
        // fee receivers from filter ref: bisq-network/bisq/pull/4294
        List<String> feeReceivers = Optional.ofNullable(filterManager.getFilter())
                .flatMap(f -> Optional.ofNullable(f.getBtcFeeReceiverAddresses()))
                .orElse(List.of());
        feeReceivers.forEach(e -> {
            try {
                btcFeeReceivers.add(e.split("#")[0]); // victim's receiver address
            } catch (RuntimeException ignore) {
                // If input format is not as expected we ignore entry
            }
        });
        btcFeeReceivers.addAll(daoFacade.getAllDonationAddresses());
        log.info("Known BTC fee receivers: {}", btcFeeReceivers.toString());

        return btcFeeReceivers;
    }

    private boolean canRequestBeMade() {
        return true; // JMC HACK
        /*
        if (filterManager.getFilter() != null && filterManager.getFilter().isDisableMempoolValidation()) {
            log.info("MempoolService bypassed by filter setting disableMempoolValidation=true");
            return false;
        }
        if (config.bypassMempoolValidation) {
            log.info("MempoolService bypassed by config setting bypassMempoolValidation=true");
            return false;
        }
        if (!Config.baseCurrencyNetwork().isMainnet()) {
            log.info("MempoolService only supports mainnet");
            return false;
        }
        return true;*/
    }
}
