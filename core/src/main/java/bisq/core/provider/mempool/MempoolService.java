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
import bisq.core.dao.state.DaoStateService;
import bisq.core.filter.FilterManager;
import bisq.core.offer.OfferPayload;
import bisq.core.trade.Trade;
import bisq.core.user.Preferences;

import bisq.network.Socks5ProxyProvider;

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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MempoolService {
    private final Socks5ProxyProvider socks5ProxyProvider;
    private final Config config;
    private final Preferences preferences;
    private final FilterManager filterManager;
    private final DaoFacade daoFacade;
    private final DaoStateService daoStateService;
    private List<String> btcFeeReceivers;
    @Getter
    private int outstandingRequests = 0;

    @Inject
    public MempoolService(Socks5ProxyProvider socks5ProxyProvider,
                          Config config,
                          Preferences preferences,
                          FilterManager filterManager,
                          DaoFacade daoFacade,
                          DaoStateService daoStateService) {
        this.socks5ProxyProvider = socks5ProxyProvider;
        this.config = config;
        this.preferences = preferences;
        this.filterManager = filterManager;
        this.daoFacade = daoFacade;
        this.daoStateService = daoStateService;
        this.btcFeeReceivers = getAllBtcFeeReceivers();
    }

    public boolean canRequestBeMade() {
        return outstandingRequests < 5; // limit max simultaneous lookups
    }

    public void validateOfferMakerTx(OfferPayload offerPayload, Consumer<TxValidator> resultHandler) {
        validateOfferMakerTx(new TxValidator(daoStateService, offerPayload.getOfferFeePaymentTxId(), Coin.valueOf(offerPayload.getAmount()),
                        offerPayload.isCurrencyForMakerFeeBtc()), resultHandler);
    }

    public void validateOfferMakerTx(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!isServiceSupported()) {
            UserThread.runAfter(() -> resultHandler.accept(txValidator.endResult("mempool request not supported, bypassing", true)), 1);
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, socks5ProxyProvider);
        retryValidateOfferMakerTx(mempoolRequest, txValidator, resultHandler);
    }

    public void validateOfferTakerTx(Trade trade, Consumer<TxValidator> resultHandler) {
        validateOfferTakerTx(new TxValidator(daoStateService, trade.getTakerFeeTxId(), trade.getTradeAmount(),
                        trade.isCurrencyForTakerFeeBtc()), resultHandler);
    }

    public void validateOfferTakerTx(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!isServiceSupported()) {
            UserThread.runAfter(() -> resultHandler.accept(txValidator.endResult("mempool request not supported, bypassing", true)), 1);
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, socks5ProxyProvider);
        retryValidateOfferTakerTx(mempoolRequest, txValidator, resultHandler);
    }

    public void checkTxIsConfirmed(String txId, Consumer<TxValidator> resultHandler) {
        TxValidator txValidator = new TxValidator(daoStateService, txId, daoFacade.getChainHeight());
        if (!isServiceSupported()) {
            UserThread.runAfter(() -> resultHandler.accept(txValidator.endResult("mempool request not supported, bypassing", true)), 1);
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, socks5ProxyProvider);
        SettableFuture<String> future = SettableFuture.create();
        Futures.addCallback(future, callbackForTxRequest(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txId, null);
    }

    public void getMakerOutspends(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!isServiceSupported()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, socks5ProxyProvider);
        SettableFuture<String> future = SettableFuture.create();
        Futures.addCallback(future, callbackForTxRequest(mempoolRequest, txValidator, resultHandler), MoreExecutors.directExecutor());
        mempoolRequest.getTxStatus(future, txValidator.getTxId(), "/outspends");
    }

    public void validateDepositTx(TxValidator txValidator, Consumer<TxValidator> resultHandler) {
        if (!isServiceSupported()) {
            return;
        }
        MempoolRequest mempoolRequest = new MempoolRequest(preferences, socks5ProxyProvider);
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
        outstandingRequests++;
        FutureCallback<String> myCallback = new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String jsonTxt) {
                outstandingRequests--;
                UserThread.execute(() -> {
                    resultHandler.accept(txValidator.parseJsonValidateMakerFeeTx(jsonTxt, btcFeeReceivers));
                });
            }
            @Override
            public void onFailure(Throwable throwable) {
                outstandingRequests--;
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
        outstandingRequests++;
        FutureCallback<String> myCallback = new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String jsonTxt) {
                outstandingRequests--;
                UserThread.execute(() -> {
                    resultHandler.accept(txValidator.parseJsonValidateTakerFeeTx(jsonTxt, btcFeeReceivers));
                });
            }
            @Override
            public void onFailure(Throwable throwable) {
                outstandingRequests--;
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
        outstandingRequests++;
        FutureCallback<String> myCallback = new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String jsonTxt) {
                outstandingRequests--;
                UserThread.execute(() -> {
                    txValidator.setJsonTxt(jsonTxt);
                    resultHandler.accept(txValidator);
                });
            }
            @Override
            public void onFailure(Throwable throwable) {
                outstandingRequests--;
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

    private boolean isServiceSupported() {
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
        return true;
    }
}
