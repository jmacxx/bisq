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

import bisq.core.dao.governance.param.Param;
import bisq.core.dao.state.DaoStateService;
import bisq.core.util.ParsingUtils;
import bisq.core.util.coin.BsqFormatter;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import org.bitcoinj.core.Coin;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.junit.Test;
import org.junit.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TxValidatorTest {
    private static final Logger log = LoggerFactory.getLogger(TxValidatorTest.class);

    private List<String> btcFeeReceivers = new ArrayList<>();

    public TxValidatorTest() {
        btcFeeReceivers.add("1EKXx73oUhHaUh8JBimtiPGgHfwNmxYKAj");
        btcFeeReceivers.add("1HpvvMHcoXQsX85CjTsco5ZAAMoGu2Mze9");
        btcFeeReceivers.add("3EfRGckBQQuk7cpU7SwatPv8kFD1vALkTU");
        btcFeeReceivers.add("13sxMq8mTw7CTSqgGiMPfwo6ZDsVYrHLmR");
        btcFeeReceivers.add("19qA2BVPoyXDfHKVMovKG7SoxGY7xrBV8c");
        btcFeeReceivers.add("19BNi5EpZhgBBWAt5ka7xWpJpX2ZWJEYyq");
        btcFeeReceivers.add("38bZBj5peYS3Husdz7AH3gEUiUbYRD951t");
        btcFeeReceivers.add("3EtUWqsGThPtjwUczw27YCo6EWvQdaPUyp");
        btcFeeReceivers.add("1BVxNn3T12veSK6DgqwU4Hdn7QHcDDRag7");
        btcFeeReceivers.add("3A8Zc1XioE2HRzYfbb5P8iemCS72M6vRJV");
        btcFeeReceivers.add("34VLFgtFKAtwTdZ5rengTT2g2zC99sWQLC");
        log.warn("Known BTC fee receivers: {}", btcFeeReceivers.toString());
    }

    @Test
    public void testMakerTx()  throws InterruptedException {
        String mempoolData, offerData;

        // 48067552 10000000 SELL BTC/DCR klakxqyfew3zhb3u.onion:9999  height=667656  expected 1.01 BSQ, actual fee paid 0.80 BSQ
        // 21499153 5000000 BUY  BTC/BSQ  rqf5kfmh              height=668729  expected 0.50 BSQ, actual fee paid 0.33 BSQ

        // all the following by 1 user:
        // am7DzIv  6100000 SELL BTC/DAI              height=668195  [Expected fee: 0.61 BSQ, actual fee paid: 0.35 BSQ, Maker tx validation]
        // F1dzaFNQ 1440000 SELL BTC/USD CASHBYMAIL   height=670822  expected 0.11 BSQ, actual fee paid 0.08 BSQ
        // 8207250  2330000 SELL BTC/USD CASHBYMAIL   height=670822  expected 0.18 BSQ, actual fee paid 0.13 BSQ
        // BKHFU    2330000 SELL BTC/USD CASHBYMAIL   height=673808  expected 0.18 BSQ, actual fee paid 0.13 BSQ
        // 7999295  2330000 SELL BTC/USD CASHBYMAIL   height=673808  expected 0.18 BSQ, actual fee paid 0.13 BSQ
        // 9336748  2330000 SELL BTC/USD CASHBYMAIL   height=673809  expected 0.18 BSQ, actual fee paid 0.13 BSQ
        // mmabyczt 2330000 BUY  BTC/USD CASHBYMAIL   height=673809  expected 0.18 BSQ, actual fee paid 0.13 BSQ
        // JTZUWI   2330000 BUY  BTC/USD CASHBYMAIL   height=673809  expected 0.18 BSQ, actual fee paid 0.13 BSQ

/*
xnsxw : [Expected fee: 0.49 BSQ, actual fee paid: 0.28 BSQ, Maker tx validation]
FQ0A7G : [Expected fee: 1.05 BSQ, actual fee paid: 0.92 BSQ, Maker tx validation]
GTYSQAY : [Expected fee: 0.04 BSQ, actual fee paid: 0.03 BSQ, Maker tx validation]

 */
        // paid the correct amount of BSQ fees
        offerData = "msimscqb,0636bafb14890edfb95465e66e2b1e15915f7fb595f9b653b9129c15ef4c1c4b,1000000,10,0,662390";
        mempoolData = "{\"txid\":\"0636bafb14890edfb95465e66e2b1e15915f7fb595f9b653b9129c15ef4c1c4b\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":7899}},{\"vout\":2,\"prevout\":{\"value\":54877439}}],\"vout\":[{\"scriptpubkey_address\":\"1FCUu7hqKCSsGhVJaLbGEoCWdZRJRNqq8w\",\"value\":7889},{\"scriptpubkey_address\":\"bc1qkj5l4wxl00ufdx6ygcnrck9fz5u927gkwqcgey\",\"value\":1600000},{\"scriptpubkey_address\":\"bc1qkw4a8u9l5w9fhdh3ue9v7e7celk4jyudzg5gk5\",\"value\":53276799}],\"size\":405,\"weight\":1287,\"fee\":650,\"status\":{\"confirmed\":true,\"block_height\":663140}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID expected 1.01 BSQ, actual fee paid 0.80 BSQ (USED 8.00 RATE INSTEAD OF 10.06 RATE)
        offerData = "48067552,3b6009da764b71d79a4df8e2d8960b6919cae2e9bdccd5ef281e261fa9cd31b3,10000000,80,0,667656";
        mempoolData = "{\"txid\":\"3b6009da764b71d79a4df8e2d8960b6919cae2e9bdccd5ef281e261fa9cd31b3\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":9717}},{\"vout\":0,\"prevout\":{\"value\":4434912}},{\"vout\":2,\"prevout\":{\"value\":12809932}}],\"vout\":[{\"scriptpubkey_address\":\"1Nzqa4J7ck5bgz7QNXKtcjZExAvReozFo4\",\"value\":9637},{\"scriptpubkey_address\":\"bc1qhmmulf5prreqhccqy2wqpxxn6dcye7ame9dd57\",\"value\":11500000},{\"scriptpubkey_address\":\"bc1qx6hg8km2jdjc5ukhuedmkseu9wlsjtd8zeajpj\",\"value\":5721894}],\"size\":553,\"weight\":1879,\"fee\":23030,\"status\":{\"confirmed\":true,\"block_height\":667660}}";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID Expected fee: 0.61 BSQ, actual fee paid: 0.35 BSQ (USED 5.75 RATE INSTEAD OF 10.06 RATE)
        offerData = "am7DzIv,4cdea8872a7d96210f378e0221dc1aae8ee9abb282582afa7546890fb39b7189,6100000,35,0,668195";
        mempoolData = "{\"txid\":\"4cdea8872a7d96210f378e0221dc1aae8ee9abb282582afa7546890fb39b7189\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":23893}},{\"vout\":1,\"prevout\":{\"value\":1440000}},{\"vout\":2,\"prevout\":{\"value\":16390881}}],\"vout\":[{\"scriptpubkey_address\":\"1Kmrzq3WGCQsZw5kroEphuk1KgsEr65yB7\",\"value\":23858},{\"scriptpubkey_address\":\"bc1qyw5qql9m7rkse9mhcun225nrjpwycszsa5dpjg\",\"value\":7015000},{\"scriptpubkey_address\":\"bc1q90y3p6mg0pe3rvvzfeudq4mfxafgpc9rulruff\",\"value\":10774186}],\"size\":554,\"weight\":1559,\"fee\":41730,\"status\":{\"confirmed\":true,\"block_height\":668198}}";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID expected 0.11 BSQ, actual fee paid 0.08 BSQ (USED 5.75 RATE INSTEAD OF 7.53)
        offerData = "F1dzaFNQ,f72e263947c9dee6fbe7093fc85be34a149ef5bcfdd49b59b9cc3322fea8967b,1440000,8,0,670822, bsq paid too little";
        mempoolData = "{\"txid\":\"f72e263947c9dee6fbe7093fc85be34a149ef5bcfdd49b59b9cc3322fea8967b\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":15163}},{\"vout\":2,\"prevout\":{\"value\":6100000}}],\"vout\":[{\"scriptpubkey_address\":\"1MEsc2m4MSomNJWSr1p6fhnUQMyA3DRGrN\",\"value\":15155},{\"scriptpubkey_address\":\"bc1qztgwe9ry9a9puchjuscqdnv4v9lsm2ut0jtfec\",\"value\":2040000},{\"scriptpubkey_address\":\"bc1q0nstwxc0vqkj4x000xt328mfjapvlsd56nn70h\",\"value\":4048308}],\"size\":406,\"weight\":1291,\"fee\":11700,\"status\":{\"confirmed\":true,\"block_height\":670823}}";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());
    }

    @Test
    public void testTakerTx()  throws InterruptedException {
        String mempoolData, offerData;

        // The fee was more than what we expected: Expected BTC fee: 5000 sats , actual fee paid: 6000 sats
        offerData = "00072328,3524364062c96ba0280621309e8b539d152154422294c2cf263a965dcde9a8ca,1000000,6000,1,614672";
        mempoolData = "{\"txid\":\"3524364062c96ba0280621309e8b539d152154422294c2cf263a965dcde9a8ca\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":1,\"prevout\":{\"value\":2971000}}],\"vout\":[{\"scriptpubkey_address\":\"3A8Zc1XioE2HRzYfbb5P8iemCS72M6vRJV\",\"value\":6000},{\"scriptpubkey_address\":\"1Hxu2X9Nr2fT3qEk9yjhiF54TJEz1Cxjoa\",\"value\":1607600},{\"scriptpubkey_address\":\"16VP6nHDDkmCMwaJj4PeyVHB88heDdVu9e\",\"value\":1353600}],\"size\":257,\"weight\":1028,\"fee\":3800,\"status\":{\"confirmed\":true,\"block_height\":614672}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // The fee matched what we expected
        offerData = "00072328,12f658954890d38ce698355be0b27fdd68d092c7b1b7475381918db060f46166,6250000,188,0,615955";
        mempoolData = "{\"txid\":\"12f658954890d38ce698355be0b27fdd68d092c7b1b7475381918db060f46166\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":19980}},{\"vout\":2,\"prevout\":{\"value\":2086015}},{\"vout\":0,\"prevout\":{\"value\":1100000}},{\"vout\":2,\"prevout\":{\"value\":938200}}],\"vout\":[{\"scriptpubkey_address\":\"17qiF1TYgT1YvsCPJyXQoKMtBZ7YJBW9GH\",\"value\":19792},{\"scriptpubkey_address\":\"16aFKD5hvEjJgPme5yRNJT2rAPdTXzdQc2\",\"value\":3768432},{\"scriptpubkey_address\":\"1D5V3QW8f5n4PhwfPgNkW9eWZwNJFyVU8n\",\"value\":346755}],\"size\":701,\"weight\":2804,\"fee\":9216,\"status\":{\"confirmed\":true,\"block_height\":615955}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // The fee was more than what we expected: Expected BTC fee: 5000 sats , actual fee paid: 7000 sats
        offerData = "bsqtrade,dfa4555ab78c657cad073e3f29c38c563d9dafc53afaa8c6af28510c734305c4,1000000,10,1,662390";
        mempoolData = "{\"txid\":\"dfa4555ab78c657cad073e3f29c38c563d9dafc53afaa8c6af28510c734305c4\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":678997}}],\"vout\":[{\"scriptpubkey_address\":\"3EfRGckBQQuk7cpU7SwatPv8kFD1vALkTU\",\"value\":7000},{\"scriptpubkey_address\":\"bc1qu6vey3e7flzg8gmhun05m43uc2vz0ay33kuu6r\",\"value\":647998}],\"size\":224,\"weight\":566,\"fee\":23999,\"status\":{\"confirmed\":true,\"block_height\":669720}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // The fee matched what we expected
        offerData = "89284,e1269aad63b3d894f5133ad658960971ef5c0fce6a13ad10544dc50fa3360588,900000,9,0,666473";
        mempoolData = "{\"txid\":\"e1269aad63b3d894f5133ad658960971ef5c0fce6a13ad10544dc50fa3360588\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":72738}},{\"vout\":0,\"prevout\":{\"value\":1600000}}],\"vout\":[{\"scriptpubkey_address\":\"17Kh5Ype9yNomqRrqu2k1mdV5c6FcKfGwQ\",\"value\":72691},{\"scriptpubkey_address\":\"bc1qdr9zcw7gf2sehxkux4fmqujm5uguhaqz7l9lca\",\"value\":629016},{\"scriptpubkey_address\":\"bc1qgqrrqv8q6l5d3t52fe28ghuhz4xqrsyxlwn03z\",\"value\":956523}],\"size\":404,\"weight\":1286,\"fee\":14508,\"status\":{\"confirmed\":true,\"block_height\":672388}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID: Expected fee: 7.04 BSQ, actual fee paid: 1.01 BSQ
        offerData = "VOxRS,e99ea06aefc824fd45031447f7a0b56efb8117a09f9b8982e2c4da480a3a0e91,10000000,101,0,669129";
        mempoolData = "{\"txid\":\"e99ea06aefc824fd45031447f7a0b56efb8117a09f9b8982e2c4da480a3a0e91\",\"version\":1,\"locktime\":0,\"vin\":[{\"vout\":0,\"prevout\":{\"value\":16739}},{\"vout\":2,\"prevout\":{\"value\":113293809}}],\"vout\":[{\"scriptpubkey_address\":\"1F14nF6zoUfJkqZrFgdmK5VX5QVwEpAnKW\",\"value\":16638},{\"scriptpubkey_address\":\"bc1q80y688ev7u43vqy964yf7feqddvt2mkm8977cm\",\"value\":11500000},{\"scriptpubkey_address\":\"bc1q9whgyc2du9mrgnxz0nl0shwpw8ugrcae0j0w8p\",\"value\":101784485}],\"size\":406,\"weight\":1291,\"fee\":9425,\"status\":{\"confirmed\":true,\"block_height\":669134}}";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());
    }

    @Test
    public void testGoodOffers()  throws InterruptedException {
        Map<String, String> goodOffers = loadJsonTestData("offerTestData.json");
        Map<String, String> mempoolData = loadJsonTestData("txInfo.json");
        Assert.assertTrue(goodOffers.size() > 0);
        Assert.assertTrue(mempoolData.size() > 0);
        log.warn("TESTING GOOD OFFERS");
        testOfferSet(goodOffers, mempoolData, true);
    }

    @Test
    public void testBadOffers()  throws InterruptedException {
        Map<String, String> badOffers = loadJsonTestData("badOfferTestData.json");
        Map<String, String> mempoolData = loadJsonTestData("txInfo.json");
        Assert.assertTrue(badOffers.size() > 0);
        Assert.assertTrue(mempoolData.size() > 0);
        log.warn("TESTING BAD OFFERS");
        testOfferSet(badOffers, mempoolData, false);
    }

    private void testOfferSet(Map<String, String> offers, Map<String, String> mempoolData, boolean expectedResult) {
        Set<String> knownValuesList = new HashSet<>(offers.values());
        knownValuesList.forEach(offerData -> {
            TxValidator txValidator = createTxValidator(offerData);
            log.warn("TESTING {}", txValidator.getTxId());
            String jsonTxt = mempoolData.get(txValidator.getTxId());
            if (jsonTxt == null || jsonTxt.isEmpty()) {
                log.warn("{} was not found in the mempool", txValidator.getTxId());
                Assert.assertFalse(expectedResult);  // tx was not found in explorer
            } else {
                txValidator.parseJsonValidateMakerFeeTx(jsonTxt, btcFeeReceivers);
                Assert.assertTrue(expectedResult == txValidator.getResult());
            }
        });
    }

    private Map<String, String> loadJsonTestData(String fileName) {
        String json = "";
        try {
            json = IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
        } catch (IOException e) {
            log.error(e.toString());
        }
        Map<String, String> map = new Gson().fromJson(json, Map.class);
        return map;
    }

    // initialize the TxValidator with offerData to be validated
    // and mock the used DaoStateService
    private TxValidator createTxValidator(String offerData) {
        try {
            String[] y = offerData.split(",");
            String txId = y[1];
            long amount = Long.parseLong(y[2]);
            boolean isCurrencyForMakerFeeBtc = Long.parseLong(y[4]) > 0;
            DaoStateService mockedDaoStateService = mock(DaoStateService.class);

            Answer<Coin> mockGetMakerFeeBsq = invocation -> {
                return mockedGetMakerFeeBsq(invocation.getArgument(1));
            };
            Answer<Coin> mockGetTakerFeeBsq = invocation -> {
                return mockedGetTakerFeeBsq(invocation.getArgument(1));
            };
            Answer<Coin> mockGetMakerFeeBtc = invocation -> {
                return mockedGetMakerFeeBtc(invocation.getArgument(1));
            };
            Answer<Coin> mockGetTakerFeeBtc = invocation -> {
                return mockedGetTakerFeeBtc(invocation.getArgument(1));
            };
            when(mockedDaoStateService.getParamValueAsCoin(Mockito.same(Param.DEFAULT_MAKER_FEE_BSQ), Mockito.anyInt())).thenAnswer(mockGetMakerFeeBsq);
            when(mockedDaoStateService.getParamValueAsCoin(Mockito.same(Param.DEFAULT_TAKER_FEE_BSQ), Mockito.anyInt())).thenAnswer(mockGetTakerFeeBsq);
            when(mockedDaoStateService.getParamValueAsCoin(Mockito.same(Param.DEFAULT_MAKER_FEE_BTC), Mockito.anyInt())).thenAnswer(mockGetMakerFeeBtc);
            when(mockedDaoStateService.getParamValueAsCoin(Mockito.same(Param.DEFAULT_TAKER_FEE_BTC), Mockito.anyInt())).thenAnswer(mockGetTakerFeeBtc);
            TxValidator txValidator = new TxValidator(mockedDaoStateService, txId, Coin.valueOf(amount), isCurrencyForMakerFeeBtc);
            return txValidator;
        } catch (RuntimeException ignore) {
            // If input format is not as expected we ignore entry
        }
        return null;
    }

    // for testing purposes, we have a hardcoded list of needed DAO param values
    // since we cannot start the P2P network / DAO in order to run tests
    Coin mockedGetMakerFeeBsq(int blockHeight) {
        BsqFormatter bsqFormatter = new BsqFormatter();
        LinkedHashMap<Long, String> feeMap = new LinkedHashMap<>();
        feeMap.put(670027L, "7.53");
        feeMap.put(660667L, "10.06");
        feeMap.put(655987L, "8.74");
        feeMap.put(641947L, "7.6");
        feeMap.put(632587L, "6.6");
        feeMap.put(623227L, "5.75");
        feeMap.put(599827L, "10.0");
        feeMap.put(590467L, "13.0");
        feeMap.put(585787L, "8.0");
        feeMap.put(581107L, "1.6");
        for (Map.Entry<Long, String> entry : feeMap.entrySet()) {
            if (blockHeight >= entry.getKey()) {
                return ParsingUtils.parseToCoin(entry.getValue(), bsqFormatter);
            }
        }
        return ParsingUtils.parseToCoin("0.5", bsqFormatter); // DEFAULT_MAKER_FEE_BSQ("0.50", ParamType.BSQ, 5, 5),     // ~ 0.01% of trade amount
    }

    Coin mockedGetTakerFeeBsq(int blockHeight) {
        BsqFormatter bsqFormatter = new BsqFormatter();
        LinkedHashMap<Long, String> feeMap = new LinkedHashMap<>();
        feeMap.put(670027L, "52.68");
        feeMap.put(660667L, "70.39");
        feeMap.put(655987L, "61.21");
        feeMap.put(641947L, "53.23");
        feeMap.put(632587L, "46.30");
        feeMap.put(623227L, "40.25");
        feeMap.put(599827L, "30.00");
        feeMap.put(590467L, "38.00");
        feeMap.put(585787L, "24.00");
        feeMap.put(581107L, "4.80");
        for (Map.Entry<Long, String> entry : feeMap.entrySet()) {
            if (blockHeight >= entry.getKey()) {
                return ParsingUtils.parseToCoin(entry.getValue(), bsqFormatter);
            }
        }
        return ParsingUtils.parseToCoin("1.5", bsqFormatter);
    }

    Coin mockedGetMakerFeeBtc(int blockHeight) {
        BsqFormatter bsqFormatter = new BsqFormatter();
        LinkedHashMap<Long, String> feeMap = new LinkedHashMap<>();
        feeMap.put(623227L, "0.0010");
        feeMap.put(585787L, "0.0020");
        for (Map.Entry<Long, String> entry : feeMap.entrySet()) {
            if (blockHeight >= entry.getKey()) {
                return ParsingUtils.parseToCoin(entry.getValue(), bsqFormatter);
            }
        }
        return ParsingUtils.parseToCoin("0.001", bsqFormatter);
    }

    Coin mockedGetTakerFeeBtc(int blockHeight) {
        BsqFormatter bsqFormatter = new BsqFormatter();
        LinkedHashMap<Long, String> feeMap = new LinkedHashMap<>();
        feeMap.put(623227L, "0.0070");
        feeMap.put(585787L, "0.0060");
        for (Map.Entry<Long, String> entry : feeMap.entrySet()) {
            if (blockHeight >= entry.getKey()) {
                return ParsingUtils.parseToCoin(entry.getValue(), bsqFormatter);
            }
        }
        return ParsingUtils.parseToCoin("0.003", bsqFormatter);
    }

}
