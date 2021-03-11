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
import org.mockito.invocation.InvocationOnMock;
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
        mempoolData = "{'txid':'0636bafb14890edfb95465e66e2b1e15915f7fb595f9b653b9129c15ef4c1c4b','version':1,'locktime':0,'vin':[{'txid':'b6fa3dd9ca5eeffc8a100f671752aed293c17f40c7e21e3bbfe1104a11783996','vout':0,'prevout':{'scriptpubkey':'76a91493b78047082a9ca5c0deb2627e8dbf8482475f4088ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 93b78047082a9ca5c0deb2627e8dbf8482475f40 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1EU4A5X3yboyYLnxMWhfsDFbrYfZzSqSeb','value':7899},'scriptsig':'47304402206cea532a2cb274c54b2e2cbbf3758a01cd9c94bf82bb9ffd96076ebf85a6918902201e7f127685f5d2392b2da65bbc356d03e1f6e3802543e84ba175456627efa1fb012102250ea6d4d1d6701f7d01ba9d6004bd50e88c2a37d2a5798177534663aaf5aa31','scriptsig_asm':'OP_PUSHBYTES_71 304402206cea532a2cb274c54b2e2cbbf3758a01cd9c94bf82bb9ffd96076ebf85a6918902201e7f127685f5d2392b2da65bbc356d03e1f6e3802543e84ba175456627efa1fb01 OP_PUSHBYTES_33 02250ea6d4d1d6701f7d01ba9d6004bd50e88c2a37d2a5798177534663aaf5aa31','is_coinbase':false,'sequence':4294967295},{'txid':'b6fa3dd9ca5eeffc8a100f671752aed293c17f40c7e21e3bbfe1104a11783996','vout':2,'prevout':{'scriptpubkey':'00147a0529f8d23efb1b23e09d787f8c2aaac81df395','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 7a0529f8d23efb1b23e09d787f8c2aaac81df395','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q0gzjn7xj8ma3kglqn4u8lrp24typmuu4egejrf','value':54877439},'scriptsig':'','scriptsig_asm':'','witness':['3045022100d548840ed9cadca94ac21bccdda021f9572cdb582d8012b1e8228e2d1d23c98b022003f81df59d7719faa24c2355677d2b7a06634380f715b0199548ae1123d03bdc01','033a341e585fdf7424873df24131626b68528b91517a6594fd703bdbfcd6c3079a'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a9149bbda0058e9c35e398d310e8d2b943e0d1b9ccf788ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 9bbda0058e9c35e398d310e8d2b943e0d1b9ccf7 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1FCUu7hqKCSsGhVJaLbGEoCWdZRJRNqq8w','value':7889},{'scriptpubkey':'0014b4a9fab8df7bf8969b4446263c58a91538557916','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 b4a9fab8df7bf8969b4446263c58a91538557916','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qkj5l4wxl00ufdx6ygcnrck9fz5u927gkwqcgey','value':1600000},{'scriptpubkey':'0014b3abd3f0bfa38a9bb6f1e64acf67d8cfed59138d','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 b3abd3f0bfa38a9bb6f1e64acf67d8cfed59138d','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qkw4a8u9l5w9fhdh3ue9v7e7celk4jyudzg5gk5','value':53276799}],'size':405,'weight':1287,'fee':650,'status':{'confirmed':true,'block_height':663140,'block_hash':'0000000000000000000ad84d34090adb2a8133c5008a150ac1adc74ebc5529c6','block_time':1609040109}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID expected 1.01 BSQ, actual fee paid 0.80 BSQ (USED 8.00 RATE INSTEAD OF 10.06 RATE)
        offerData = "48067552,3b6009da764b71d79a4df8e2d8960b6919cae2e9bdccd5ef281e261fa9cd31b3,10000000,80,0,667656";
        mempoolData = "{'txid':'3b6009da764b71d79a4df8e2d8960b6919cae2e9bdccd5ef281e261fa9cd31b3','version':1,'locktime':0,'vin':[{'txid':'c0ca0e06ceee91b76f81514e7857e7a6ea08bbc78c6ef4b0c50a2d5ae0a3a6d9','vout':0,'prevout':{'scriptpubkey':'76a91485773ef9041290df99d0e527956cac481ac7078a88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 85773ef9041290df99d0e527956cac481ac7078a OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1DAhisTJSE1jSJPEWzGBsH6wSjkSt7zYLF','value':9717},'scriptsig':'483045022100fe9fab0797b432d53d69dea44f8d0f8d41fda7bb49c9b117abffebbf6dad49da0220038d9a292e005592dba6522030877f801fbd082f124653368064f0468f5c024f0121039aee81372ddb8101e4ae001b10d1df7f0a64d37158a1897ad9b7a1c3f2da2a54','scriptsig_asm':'OP_PUSHBYTES_72 3045022100fe9fab0797b432d53d69dea44f8d0f8d41fda7bb49c9b117abffebbf6dad49da0220038d9a292e005592dba6522030877f801fbd082f124653368064f0468f5c024f01 OP_PUSHBYTES_33 039aee81372ddb8101e4ae001b10d1df7f0a64d37158a1897ad9b7a1c3f2da2a54','is_coinbase':false,'sequence':4294967295},{'txid':'7ead7cddc8b95da4dd0daf01c538296a6a44608bd11a5b34e3532341d729dfa9','vout':0,'prevout':{'scriptpubkey':'76a914adece7a57e6b7dc3d9be8a9841136ad17228d15288ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 adece7a57e6b7dc3d9be8a9841136ad17228d152 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1GrdhdHHXKMcsKfZJ5PVQ4Bs8EYwFmGkpF','value':4434912},'scriptsig':'47304402201ba2b894fa3b5518ffd16d55fa7f66bf538961ca76afc5b36805bf0a5f730f0402200a647ddf39992c35974929dded2b1534638fc07c416dfe1277d61b0a35df635901210382f7e9a1366d9e1127c2149fed9decb51e5490837fb3a80473e5e957df6d9720','scriptsig_asm':'OP_PUSHBYTES_71 304402201ba2b894fa3b5518ffd16d55fa7f66bf538961ca76afc5b36805bf0a5f730f0402200a647ddf39992c35974929dded2b1534638fc07c416dfe1277d61b0a35df635901 OP_PUSHBYTES_33 0382f7e9a1366d9e1127c2149fed9decb51e5490837fb3a80473e5e957df6d9720','is_coinbase':false,'sequence':4294967295},{'txid':'c0ca0e06ceee91b76f81514e7857e7a6ea08bbc78c6ef4b0c50a2d5ae0a3a6d9','vout':2,'prevout':{'scriptpubkey':'0014463fd77c524f4598e4274e6a41d24705475ba774','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 463fd77c524f4598e4274e6a41d24705475ba774','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qgclawlzjfaze3ep8fe4yr5j8q4r4hfm5j0ehg5','value':12809932},'scriptsig':'','scriptsig_asm':'','witness':['30440220266b80abae8f506e0f9484a04bce0c6b66ed29f78d5f27bec60061901398ac8302205cd79605b61f3f9d55c6ca5893978c8b5710e9e4e33b2e45e18419823522c3d301','03bf82e3cc50fcb0f61e584d56189f27e304a6268682a281d2c8641ae4bc212262'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a914f14ae53ae07204dca9645d036cde31653dc7530b88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 f14ae53ae07204dca9645d036cde31653dc7530b OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1Nzqa4J7ck5bgz7QNXKtcjZExAvReozFo4','value':9637},{'scriptpubkey':'0014bef7cfa68118f20be300229c0098d3d3704cfbbb','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 bef7cfa68118f20be300229c0098d3d3704cfbbb','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qhmmulf5prreqhccqy2wqpxxn6dcye7ame9dd57','value':11500000},{'scriptpubkey':'001436ae83db6a93658a72d7e65bbb433c2bbf092da7','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 36ae83db6a93658a72d7e65bbb433c2bbf092da7','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qx6hg8km2jdjc5ukhuedmkseu9wlsjtd8zeajpj','value':5721894}],'size':553,'weight':1879,'fee':23030,'status':{'confirmed':true,'block_height':667660,'block_hash':'0000000000000000000cd7318b5e3f61d5fe59c246981d21ceafde6884b2e357','block_time':1611615027}}\n";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID Expected fee: 0.61 BSQ, actual fee paid: 0.35 BSQ (USED 5.75 RATE INSTEAD OF 10.06 RATE)
        offerData = "am7DzIv,4cdea8872a7d96210f378e0221dc1aae8ee9abb282582afa7546890fb39b7189,6100000,35,0,668195";
        mempoolData = "{'txid':'4cdea8872a7d96210f378e0221dc1aae8ee9abb282582afa7546890fb39b7189','version':1,'locktime':0,'vin':[{'txid':'051770f8d7f43a9b6ca10fefa6cdf4cb124a81eed26dc8af2e40f57d2589107b','vout':0,'prevout':{'scriptpubkey':'76a9143fd2ff1958663942927b7adaa2043f62a6536e9588ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 3fd2ff1958663942927b7adaa2043f62a6536e95 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'16pULNutwpJ5E6EaxopQQDAVaFJXt8B18Z','value':23893},'scriptsig':'47304402206f2258c0e2d1afb5cdb0a30fab4a3a6e7a75c983b0b71ca731da647cdb32b8840220277b510969ad79a578777ab0b407ac92ab59e53dcfd6c9427390dd56f0ba24fb012102f2abcb9ee4a030b3772363f3c87ff5debc6bf995fe889cae1eae7a1f1e57f756','scriptsig_asm':'OP_PUSHBYTES_71 304402206f2258c0e2d1afb5cdb0a30fab4a3a6e7a75c983b0b71ca731da647cdb32b8840220277b510969ad79a578777ab0b407ac92ab59e53dcfd6c9427390dd56f0ba24fb01 OP_PUSHBYTES_33 02f2abcb9ee4a030b3772363f3c87ff5debc6bf995fe889cae1eae7a1f1e57f756','is_coinbase':false,'sequence':4294967295},{'txid':'4d69e8ddeaa26fb92aafa1c58d47444d50a77016d4c195bf6f2bbcb4604ed899','vout':1,'prevout':{'scriptpubkey':'001457f8cc40d47a6eca6a8ad81ea9d1e07485689639','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 57f8cc40d47a6eca6a8ad81ea9d1e07485689639','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q2luvcsx50fhv5652mq02n50qwjzk393e9vm9ez','value':1440000},'scriptsig':'','scriptsig_asm':'','witness':['30450221009c900703181e1cceaa265cb25b4f814eafdab149d66f04f7d273add43c5b0fda0220017b4f3e7c18c8952d01b561f6c48866215d71560f8dee7e053d8c2148661cfb01','026da6a417ba8915ba1e350f4a7bdf9d5c644f1f0ff1ff173ab0b40334c700ba4a'],'is_coinbase':false,'sequence':4294967295},{'txid':'04ccaff00ee6575cd67f58c30b8faefebd7e3a967f1cc85b9097f1c16c3447f5','vout':2,'prevout':{'scriptpubkey':'0014924be4010255fa10b299a7fe3fd32a33b89a4d87','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 924be4010255fa10b299a7fe3fd32a33b89a4d87','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qjf97gqgz2happv5e5llrl5e2xwuf5nv8fddn5t','value':16390881},'scriptsig':'','scriptsig_asm':'','witness':['3045022100f551e340b490d0b8c90eb571b3e4179e27c4d9598edbd790a2c87d764c7b9fdf02203c82727abf66161693737d1bd5a97c1c5fd50c694d03e33135e33f6a096fe5d801','024b9e58192e6d5d021d3ff87196a1b607c5090a91929fea61cb0eb7bca3e4351e'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a914cdee4d464f43dc2c8ebf3466b3b845db5d8d7a6e88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 cdee4d464f43dc2c8ebf3466b3b845db5d8d7a6e OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1Kmrzq3WGCQsZw5kroEphuk1KgsEr65yB7','value':23858},{'scriptpubkey':'001423a8007cbbf0ed0c9777c726a55263905c4c4050','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 23a8007cbbf0ed0c9777c726a55263905c4c4050','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qyw5qql9m7rkse9mhcun225nrjpwycszsa5dpjg','value':7015000},{'scriptpubkey':'00142bc910eb68787311b1824e78d05769375280e0a3','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 2bc910eb68787311b1824e78d05769375280e0a3','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q90y3p6mg0pe3rvvzfeudq4mfxafgpc9rulruff','value':10774186}],'size':554,'weight':1559,'fee':41730,'status':{'confirmed':true,'block_height':668198,'block_hash':'0000000000000000000352b9bdbd9fe6020158af312d0c308f9dd4b262d05c00','block_time':1611928140}}";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID expected 0.11 BSQ, actual fee paid 0.08 BSQ (USED 5.75 RATE INSTEAD OF 7.53)
        offerData = "F1dzaFNQ,f72e263947c9dee6fbe7093fc85be34a149ef5bcfdd49b59b9cc3322fea8967b,1440000,8,0,670822, bsq paid too little";
        mempoolData = "{'txid':'f72e263947c9dee6fbe7093fc85be34a149ef5bcfdd49b59b9cc3322fea8967b','version':1,'locktime':0,'vin':[{'txid':'4be51415d23be7240cca23346dd9e005526aa6c4260697060f39909cf85fa8bc','vout':0,'prevout':{'scriptpubkey':'76a914f7676abdf064b7914c67552c9979d0bb7d89599888ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 f7676abdf064b7914c67552c9979d0bb7d895998 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1PZ9nvGteEVbtAmaeWPYpHc9AAnb1VZhkm','value':15163},'scriptsig':'483045022100ddd38c173bfcaf78adbc13c07ca994d474af1e29e0f8e5a5ce906c1ad59656de02201f48f2ef0244bec4fa147052c06d282112778468523dccdf820a22a87c49bcac012102f3d41f8e0a2a1ca963f61bba4a701181bdab5bff645b891a4413df835a92cf04','scriptsig_asm':'OP_PUSHBYTES_72 3045022100ddd38c173bfcaf78adbc13c07ca994d474af1e29e0f8e5a5ce906c1ad59656de02201f48f2ef0244bec4fa147052c06d282112778468523dccdf820a22a87c49bcac01 OP_PUSHBYTES_33 02f3d41f8e0a2a1ca963f61bba4a701181bdab5bff645b891a4413df835a92cf04','is_coinbase':false,'sequence':4294967295},{'txid':'b0f97268a8e1c3856e46505933ce16d9712a9b487dcdf00df6c0108c0838e08d','vout':2,'prevout':{'scriptpubkey':'0014f6058503e3f150d4d6b7986262397f4cad929f8f','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 f6058503e3f150d4d6b7986262397f4cad929f8f','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q7czc2qlr79gdf44hnp3xywtlfjke98u0lzsx9z','value':6100000},'scriptsig':'','scriptsig_asm':'','witness':['3045022100d3ecb040eaebb813aaa392cc6e91b725a1baf5e17485ab525ad305510ec018260220034477722ddf5d1c340c26a53b22bc99a05f1fa2ae2fb2a421e5025008b7510201','03d47b722e66f3dd6deb83f9f4ebbd16ca0d6e79f60e632e6885481445af764159'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a914de022a4d9d3e658760261343c3a0d6ce798dee1c88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 de022a4d9d3e658760261343c3a0d6ce798dee1c OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1MEsc2m4MSomNJWSr1p6fhnUQMyA3DRGrN','value':15155},{'scriptpubkey':'001412d0ec94642f4a1e62f2e43006cd95617f0dab8b','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 12d0ec94642f4a1e62f2e43006cd95617f0dab8b','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qztgwe9ry9a9puchjuscqdnv4v9lsm2ut0jtfec','value':2040000},{'scriptpubkey':'00147ce0b71b0f602d2a99ef7997151f699742cfc1b4','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 7ce0b71b0f602d2a99ef7997151f699742cfc1b4','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q0nstwxc0vqkj4x000xt328mfjapvlsd56nn70h','value':4048308}],'size':406,'weight':1291,'fee':11700,'status':{'confirmed':true,'block_height':670823,'block_hash':'00000000000000000008418d2db421e37227a5a43f45bf99c3f84fc8796e7f55','block_time':1613461566}}";
        Assert.assertFalse(createTxValidator(offerData).parseJsonValidateMakerFeeTx(mempoolData, btcFeeReceivers).getResult());
    }

    @Test
    public void testTakerTx()  throws InterruptedException {
        String mempoolData, offerData;

        // The fee was more than what we expected: Expected BTC fee: 5000 sats , actual fee paid: 6000 sats
        offerData = "00072328,3524364062c96ba0280621309e8b539d152154422294c2cf263a965dcde9a8ca,1000000,6000,1,614672";
        mempoolData = "{'txid':'3524364062c96ba0280621309e8b539d152154422294c2cf263a965dcde9a8ca','version':1,'locktime':0,'vin':[{'txid':'00d04394b91d961ceff60d7fa2ad3e6ac38faf7e7743c6a1d2f8a6fc8e1b9da5','vout':1,'prevout':{'scriptpubkey':'76a914bfac870f346b1f2bd20d9908c3dc78fbd6b0d8cd88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 bfac870f346b1f2bd20d9908c3dc78fbd6b0d8cd OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1JUUk4p7Cjiq4yxJtjKL71NusjK9uQ6sCA','value':2971000},'scriptsig':'4730440220410dc9066148e6844aa2c78144a3796fd2439cf971f7ad0970c1822a0a0bd06002200fdc592033d12819b6d8a0b2f70d7ab13c6e59731090c0b310d4713bee6a9e930121030a715ab907d6d6d67ea79fe06779ead46f7eff401bbd80946c9271d231d72144','scriptsig_asm':'OP_PUSHBYTES_71 30440220410dc9066148e6844aa2c78144a3796fd2439cf971f7ad0970c1822a0a0bd06002200fdc592033d12819b6d8a0b2f70d7ab13c6e59731090c0b310d4713bee6a9e9301 OP_PUSHBYTES_33 030a715ab907d6d6d67ea79fe06779ead46f7eff401bbd80946c9271d231d72144','is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'a9145c95d9cf9d48683284dff53379acc296eb21488387','scriptpubkey_asm':'OP_HASH160 OP_PUSHBYTES_20 5c95d9cf9d48683284dff53379acc296eb214883 OP_EQUAL','scriptpubkey_type':'p2sh','scriptpubkey_address':'3A8Zc1XioE2HRzYfbb5P8iemCS72M6vRJV','value':6000},{'scriptpubkey':'76a914ba1451d986701f792a93e35cc91956558f35fa4a88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 ba1451d986701f792a93e35cc91956558f35fa4a OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1Hxu2X9Nr2fT3qEk9yjhiF54TJEz1Cxjoa','value':1607600},{'scriptpubkey':'76a9143c36b945d69b9a8736a92d2d75d7ece7b4ab867188ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 3c36b945d69b9a8736a92d2d75d7ece7b4ab8671 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'16VP6nHDDkmCMwaJj4PeyVHB88heDdVu9e','value':1353600}],'size':257,'weight':1028,'fee':3800,'status':{'confirmed':true,'block_height':614672,'block_hash':'00000000000000000004980432f71def5ac27de365b0c6497bf7db299762c724','block_time':1580071276}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // The fee matched what we expected
        offerData = "00072328,12f658954890d38ce698355be0b27fdd68d092c7b1b7475381918db060f46166,6250000,188,0,615955";
        mempoolData = "{'txid':'12f658954890d38ce698355be0b27fdd68d092c7b1b7475381918db060f46166','version':1,'locktime':0,'vin':[{'txid':'6a01b3cd20e03bee2380c526cbf4ed342afc3e8f990b6e535025e254345a63a6','vout':0,'prevout':{'scriptpubkey':'76a914958a9b8793fc60e0f45eac1357e26461fcd2a32f88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 958a9b8793fc60e0f45eac1357e26461fcd2a32f OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1EdhjCnpKgqBdaBTCThKbHTRVYrczgZDyn','value':19980},'scriptsig':'47304402203d751d3834c2a1d32c69e86711cada55872e6ef5e32421037fa8813ed20cb4e40220205e1dbe7dcc53509d94fa23bc7c34b64d68ea3847987eaafec443027c96acb801210202caef9af029b350b6dd297e3f4d24db1465f25e9f81da83c6acdb9b036d1075','scriptsig_asm':'OP_PUSHBYTES_71 304402203d751d3834c2a1d32c69e86711cada55872e6ef5e32421037fa8813ed20cb4e40220205e1dbe7dcc53509d94fa23bc7c34b64d68ea3847987eaafec443027c96acb801 OP_PUSHBYTES_33 0202caef9af029b350b6dd297e3f4d24db1465f25e9f81da83c6acdb9b036d1075','is_coinbase':false,'sequence':4294967295},{'txid':'6a01b3cd20e03bee2380c526cbf4ed342afc3e8f990b6e535025e254345a63a6','vout':2,'prevout':{'scriptpubkey':'76a914bb5933df70fbeb20446a7e1c607ece7c15ace77a88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 bb5933df70fbeb20446a7e1c607ece7c15ace77a OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1J5cDnDQxQGo6ooZ9RQBBV81BdW37jStAS','value':2086015},'scriptsig':'47304402206fb182c1e132dd74d06b76e1455ddcfc7ea973a93c5f992992d39aa78ba1735202203c5f6a9d21943371c7307da7e67cbeb76b20002af4594d2af447fd8e4da1b846012103ed6689b106876366f5f7aa51ca931a2c87fee556639dd7b75b64c6a99f1b5c4c','scriptsig_asm':'OP_PUSHBYTES_71 304402206fb182c1e132dd74d06b76e1455ddcfc7ea973a93c5f992992d39aa78ba1735202203c5f6a9d21943371c7307da7e67cbeb76b20002af4594d2af447fd8e4da1b84601 OP_PUSHBYTES_33 03ed6689b106876366f5f7aa51ca931a2c87fee556639dd7b75b64c6a99f1b5c4c','is_coinbase':false,'sequence':4294967295},{'txid':'5d911b776b14083e0406b88d51b53562d055f06429670ae01ff3adfd2a4a4f21','vout':0,'prevout':{'scriptpubkey':'76a9140cc1bacfcea8efb3bcd945b4c549e29fec80653088ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 0cc1bacfcea8efb3bcd945b4c549e29fec806530 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'12ATBt2iYsE4Hk6TnCnkFTgbAt6ZUjyJXW','value':1100000},'scriptsig':'483045022100e717e44822c79c2168f43253eab3ef15d06e71232eadd0274a8d8b4f8766058e02202099f4cf5cd6ebd229776cadd77b6a38a52b1e7362b69ae6a3bdd1f1cc4f75c6012102480844d01d4d058d456739124a90da59a262b372cdac83a22e04bbd2853bf15e','scriptsig_asm':'OP_PUSHBYTES_72 3045022100e717e44822c79c2168f43253eab3ef15d06e71232eadd0274a8d8b4f8766058e02202099f4cf5cd6ebd229776cadd77b6a38a52b1e7362b69ae6a3bdd1f1cc4f75c601 OP_PUSHBYTES_33 02480844d01d4d058d456739124a90da59a262b372cdac83a22e04bbd2853bf15e','is_coinbase':false,'sequence':4294967295},{'txid':'334bd516033a1f9259827c4005ec4d5dab5907daf2c22730b2e5ee6e6a398058','vout':2,'prevout':{'scriptpubkey':'76a9142faec5dd46cab63fb2e51c8439deb000153ee99b88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 2faec5dd46cab63fb2e51c8439deb000153ee99b OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'15M88T8dJZh1k1QRW2d7KhxLUHMQ25j7wT','value':938200},'scriptsig':'47304402207e4f19038e5cdc261a7abd2ea0d13b560c97e547f55a2f48f0e8f0c3c7d87ee302207c5c57cc4bfec93ce9bba5d8f1c125f935cafffe743dcd3015f4535372a7221a012102043f02a5957db961ff6493f8a0b3472d15a8b8fdd3207e4468f5f5bc7a620bbc','scriptsig_asm':'OP_PUSHBYTES_71 304402207e4f19038e5cdc261a7abd2ea0d13b560c97e547f55a2f48f0e8f0c3c7d87ee302207c5c57cc4bfec93ce9bba5d8f1c125f935cafffe743dcd3015f4535372a7221a01 OP_PUSHBYTES_33 02043f02a5957db961ff6493f8a0b3472d15a8b8fdd3207e4468f5f5bc7a620bbc','is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a9144b0725c5f57a5e0e1f39de5f7fca83ddc5bb8f4488ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 4b0725c5f57a5e0e1f39de5f7fca83ddc5bb8f44 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'17qiF1TYgT1YvsCPJyXQoKMtBZ7YJBW9GH','value':19792},{'scriptpubkey':'76a9143d224de8938284cd9e2ccb7fe8786295d5d3bf5288ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 3d224de8938284cd9e2ccb7fe8786295d5d3bf52 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'16aFKD5hvEjJgPme5yRNJT2rAPdTXzdQc2','value':3768432},{'scriptpubkey':'76a914847a94c274514daf1b3785b081a746a4392e463688ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 847a94c274514daf1b3785b081a746a4392e4636 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1D5V3QW8f5n4PhwfPgNkW9eWZwNJFyVU8n','value':346755}],'size':701,'weight':2804,'fee':9216,'status':{'confirmed':true,'block_height':615955,'block_hash':'0000000000000000000792847f27a77d1788302fc1a28a8355c725b69fa33523','block_time':1580831421}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // The fee was more than what we expected: Expected BTC fee: 5000 sats , actual fee paid: 7000 sats
        offerData = "bsqtrade,dfa4555ab78c657cad073e3f29c38c563d9dafc53afaa8c6af28510c734305c4,1000000,10,1,662390";
        mempoolData = "{'txid':'dfa4555ab78c657cad073e3f29c38c563d9dafc53afaa8c6af28510c734305c4','version':1,'locktime':0,'vin':[{'txid':'7bc53f219f94af7d71cd9d1b7fc7270b5aaf72aa798e9447d76c351b4f45bc85','vout':0,'prevout':{'scriptpubkey':'001452e44938f1160e943c3010151390240f8ffb76f3','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 52e44938f1160e943c3010151390240f8ffb76f3','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q2tjyjw83zc8fg0pszq238ypyp78lkahn4y2444','value':678997},'scriptsig':'','scriptsig_asm':'','witness':['30450221008f446e8d740665e0bfe0060dd6244568d503d92fb865c8f5d36b072c4f7cab71022072703e044a96bcb43e60ea03047e85b908b8043bd077609f45f5c69b5a3ba0bf01','022dd05c26eb7ccbaee023736cd1388f56d72c6742c5c661d66d860f256b1ad757'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'a9148e4c4aa66b700598b1e08a5706fcbeba6f26839987','scriptpubkey_asm':'OP_HASH160 OP_PUSHBYTES_20 8e4c4aa66b700598b1e08a5706fcbeba6f268399 OP_EQUAL','scriptpubkey_type':'p2sh','scriptpubkey_address':'3EfRGckBQQuk7cpU7SwatPv8kFD1vALkTU','value':7000},{'scriptpubkey':'0014e69992473e4fc483a377e4df4dd63cc29827f491','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 e69992473e4fc483a377e4df4dd63cc29827f491','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qu6vey3e7flzg8gmhun05m43uc2vz0ay33kuu6r','value':647998}],'size':224,'weight':566,'fee':23999,'status':{'confirmed':true,'block_height':669720,'block_hash':'00000000000000000005dab6d9ad630d162b9912d211bac7eff372da3ab9fb5d','block_time':1612798475}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // The fee matched what we expected
        offerData = "89284,e1269aad63b3d894f5133ad658960971ef5c0fce6a13ad10544dc50fa3360588,900000,9,0,666473";
        mempoolData = "{'txid':'e1269aad63b3d894f5133ad658960971ef5c0fce6a13ad10544dc50fa3360588','version':1,'locktime':0,'vin':[{'txid':'96178b7ae841accc21fbb72a892f55ba6bd62a663065ca02db5667b8fc8a59e4','vout':0,'prevout':{'scriptpubkey':'76a914157579601ba2cf59e75347b31bb2e63e1876acf488ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 157579601ba2cf59e75347b31bb2e63e1876acf4 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'12xTvGsKs4237mwDwhoUChV4iWK38CTRJg','value':72738},'scriptsig':'4730440220619142fd53a9b5deb11c6fc08a6cdf50b36b97b148b12d10fa344e2a4605d40b022038c407ad403ebe5f21208ea1e4c172f4c516366b266275f05445a988e89585a1012102d536e828b9744c8b6ed6b4cf1350dd9d1dfff3e44fcf8a09758ac98d83d951ad','scriptsig_asm':'OP_PUSHBYTES_71 30440220619142fd53a9b5deb11c6fc08a6cdf50b36b97b148b12d10fa344e2a4605d40b022038c407ad403ebe5f21208ea1e4c172f4c516366b266275f05445a988e89585a101 OP_PUSHBYTES_33 02d536e828b9744c8b6ed6b4cf1350dd9d1dfff3e44fcf8a09758ac98d83d951ad','is_coinbase':false,'sequence':4294967295},{'txid':'1524d81b8bba3a1fcd6ea2fd85642ae6568c227ec9eded9821c84a7159e4b8b7','vout':0,'prevout':{'scriptpubkey':'001428377cc960fb79462eed247c56891b9cd70e1c5e','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 28377cc960fb79462eed247c56891b9cd70e1c5e','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q9qmhejtqldu5vthdy379dzgmnntsu8z74q8ttv','value':1600000},'scriptsig':'','scriptsig_asm':'','witness':['304402201d01ae7d179cf372560bc61168df1bd006c890107e63df8c622560de2f13ae97022064a3178961dfd40e4335a555fb642799b76eadf8db6419fab6f02976e13d80d101','035f80f4812f2517c6591e689cf81fe0fba9ae2fc72fbd3aade27e2d36cc23d1d3'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a9144559b2a2f74cd1bf574094fbf22c1810b51a3d1a88ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 4559b2a2f74cd1bf574094fbf22c1810b51a3d1a OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'17Kh5Ype9yNomqRrqu2k1mdV5c6FcKfGwQ','value':72691},{'scriptpubkey':'001468ca2c3bc84aa19b9adc3553b0725ba711cbf402','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 68ca2c3bc84aa19b9adc3553b0725ba711cbf402','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qdr9zcw7gf2sehxkux4fmqujm5uguhaqz7l9lca','value':629016},{'scriptpubkey':'001440063030e0d7e8d8ae8a4e54745f97154c01c086','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 40063030e0d7e8d8ae8a4e54745f97154c01c086','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qgqrrqv8q6l5d3t52fe28ghuhz4xqrsyxlwn03z','value':956523}],'size':404,'weight':1286,'fee':14508,'status':{'confirmed':true,'block_height':672388,'block_hash':'0000000000000000000ae2562b7351210584deb566a4d8b28e57f11fff4703eb','block_time':1614409209}}";
        Assert.assertTrue(createTxValidator(offerData).parseJsonValidateTakerFeeTx(mempoolData, btcFeeReceivers).getResult());

        // UNDERPAID: Expected fee: 7.04 BSQ, actual fee paid: 1.01 BSQ
        offerData = "VOxRS,e99ea06aefc824fd45031447f7a0b56efb8117a09f9b8982e2c4da480a3a0e91,10000000,101,0,669129";
        mempoolData = "{'txid':'e99ea06aefc824fd45031447f7a0b56efb8117a09f9b8982e2c4da480a3a0e91','version':1,'locktime':0,'vin':[{'txid':'29bbbf343bbe22b6931a0ad1df6bbc89632682fa81e4d45af28a2c0f7edd94bf','vout':0,'prevout':{'scriptpubkey':'76a914ded49342ab7f4531d435047c7c005d632c51f7f088ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 ded49342ab7f4531d435047c7c005d632c51f7f0 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1MKDfaDXZKtgNhW6Dbdk2TbB56SuDCpJze','value':16739},'scriptsig':'483045022100cb40dc0b6b4951d798d574382ebb420031722a0b263979f3dd75d3db4f422e8d02200c327994a64ce6306809c22bbb7301f7bb103718f9640fa6c82ed5d7ae184ff4012103a17e3f1d86d996d0a3a0e5e44226b13397d3b72818b26385490c74df76da9223','scriptsig_asm':'OP_PUSHBYTES_72 3045022100cb40dc0b6b4951d798d574382ebb420031722a0b263979f3dd75d3db4f422e8d02200c327994a64ce6306809c22bbb7301f7bb103718f9640fa6c82ed5d7ae184ff401 OP_PUSHBYTES_33 03a17e3f1d86d996d0a3a0e5e44226b13397d3b72818b26385490c74df76da9223','is_coinbase':false,'sequence':4294967295},{'txid':'29bbbf343bbe22b6931a0ad1df6bbc89632682fa81e4d45af28a2c0f7edd94bf','vout':2,'prevout':{'scriptpubkey':'00141da70599668da91dbbb3fa45931ab95b0585e877','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 1da70599668da91dbbb3fa45931ab95b0585e877','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1qrknstxtx3k53mwanlfzexx4etvzct6rhwxh0uu','value':113293809},'scriptsig':'','scriptsig_asm':'','witness':['3045022100dcdc7ac2113618fdb21c69fee0fd5a64ad170706784ecca737c259c051f9415f0220305d8e0ea6ac3bc8fc56f91c782c7f6979b3171226f704f02f4734faf716267901','03ce3750141bafe38e0ea75f421e5a71eaf6498d2d98805416823141380866745a'],'is_coinbase':false,'sequence':4294967295}],'vout':[{'scriptpubkey':'76a9149994ea996b49cac5fcdeab9c043a0207c0b38b8988ac','scriptpubkey_asm':'OP_DUP OP_HASH160 OP_PUSHBYTES_20 9994ea996b49cac5fcdeab9c043a0207c0b38b89 OP_EQUALVERIFY OP_CHECKSIG','scriptpubkey_type':'p2pkh','scriptpubkey_address':'1F14nF6zoUfJkqZrFgdmK5VX5QVwEpAnKW','value':16638},{'scriptpubkey':'00143bc9a39f2cf72b160085d5489f27206b58b56edb','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 3bc9a39f2cf72b160085d5489f27206b58b56edb','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q80y688ev7u43vqy964yf7feqddvt2mkm8977cm','value':11500000},{'scriptpubkey':'00142bae82614de176344cc27cfef85dc171f881e3b9','scriptpubkey_asm':'OP_0 OP_PUSHBYTES_20 2bae82614de176344cc27cfef85dc171f881e3b9','scriptpubkey_type':'v0_p2wpkh','scriptpubkey_address':'bc1q9whgyc2du9mrgnxz0nl0shwpw8ugrcae0j0w8p','value':101784485}],'size':406,'weight':1291,'fee':9425,'status':{'confirmed':true,'block_height':669134,'block_hash':'0000000000000000000654ee8d9ad298053d628588f872b4aa860bedb472a735','block_time':1612486242}}";
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
            if (jsonTxt.isEmpty()) {
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

    private TxValidator createTxValidator(String offerData) {
        try {
            String[] y = offerData.split(",");
            String txId = y[1];
            long amount = Long.parseLong(y[2]);
            boolean isCurrencyForMakerFeeBtc = Long.parseLong(y[4]) > 0;
            long blockHeightAtOfferCreation = Long.parseLong(y[5]);
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
            TxValidator txValidator = new TxValidator(mockedDaoStateService, txId, Coin.valueOf(amount), isCurrencyForMakerFeeBtc, blockHeightAtOfferCreation);
            return txValidator;
        } catch (RuntimeException ignore) {
            // If input format is not as expected we ignore entry
        }
        return null;
    }

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
