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

package bisq.desktop.main.overlays.windows;

import bisq.desktop.components.AutoTooltipButton;
import bisq.desktop.components.BisqTextArea;
import bisq.desktop.components.InputTextField;
import bisq.desktop.main.overlays.Overlay;
import bisq.core.offer.Offer;
import bisq.core.offer.OfferBookService;
import bisq.core.provider.mempool.MempoolService;
import bisq.core.provider.mempool.TxValidator;
import bisq.core.trade.Trade;
import bisq.core.trade.closed.ClosedTradableManager;

import bisq.common.UserThread;

import org.bitcoinj.core.Coin;

import com.google.gson.Gson;

import javax.inject.Inject;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static bisq.desktop.util.FormBuilder.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

// We don't translate here as it is for dev only purpose
public class MempoolValidationTool extends Overlay<MempoolValidationTool> {
    private static final Logger log = LoggerFactory.getLogger(MempoolValidationTool.class);
    private final OfferBookService offerBookService;
    private final ClosedTradableManager closedTradableManager;
    private final MempoolService mempoolService;
    GridPane makerValidationGridPane, takerValidationGridPane;
    private IntegerProperty inProgressCount = new SimpleIntegerProperty(0);
    int asciiSpinnerIdx = 0;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public MempoolValidationTool(OfferBookService offerBookService,
                                ClosedTradableManager closedTradableManager,
                                MempoolService mempoolService) {
        this.offerBookService = offerBookService;
        this.closedTradableManager = closedTradableManager;
        this.mempoolService = mempoolService;
        type = Type.Attention;
    }

    public void show() {
        if (headLine == null)
            headLine = "Mempool Validator tool"; // We dont translate here as it is for dev only purpose
        width = 1068;
        createGridPane();
        addHeadLine();
        addContent();
        addButtons();
        applyStyles();
        display();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void setupKeyHandler(Scene scene) {
        if (!hideCloseButton) {
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    e.consume();
                    doClose();
                }
            });
        }
    }

    @Override
    protected void doClose() {
        super.doClose();
        inProgressCount.set(0);
    }

    @Override
    protected void createGridPane() {
        gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(15);
        gridPane.setPadding(new Insets(64, 64, 64, 64));
        gridPane.setPrefWidth(width);
        ColumnConstraints columnConstraints1 = new ColumnConstraints();
        ColumnConstraints columnConstraints2 = new ColumnConstraints();
        columnConstraints1.setPercentWidth(18);
        columnConstraints2.setPercentWidth(82);
        gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
    }

    @Override
    protected void cleanup() {
        super.cleanup();
    }

    private void addContent() {
        rowIndex = 1;
        this.disableActionButton = true;
        addLeftPanelButtons();
        addMakerValidationPane();
        addTakerValidationPane();
        hideAllPanes();
        makerValidationGridPane.setVisible(true);
    }

    private void addLeftPanelButtons() {
        Button button1 = new AutoTooltipButton("Offer Book");
        Button button2 = new AutoTooltipButton("My Trades");
        VBox vBox = new VBox(12, button1, button2);
        vBox.getChildren().forEach(button -> ((Button) button).setPrefWidth(500));
        gridPane.add(vBox, 0, rowIndex);
        button1.getStyleClass().add("action-button");
        button1.setOnAction(e -> {
            hideAllPanes();
            vBox.getChildren().forEach(button -> button.getStyleClass().remove("action-button"));
            button1.getStyleClass().add("action-button");
            makerValidationGridPane.setVisible(true);
        });
        button2.setOnAction(e -> {
            hideAllPanes();
            vBox.getChildren().forEach(button -> button.getStyleClass().remove("action-button"));
            button2.getStyleClass().add("action-button");
            takerValidationGridPane.setVisible(true);
        });
    }

    private void addMakerValidationPane() {
        int rowIndexSub = 0;
        makerValidationGridPane = new GridPane();
        gridPane.add(makerValidationGridPane, 1, rowIndex);
        Label offersToProcessLbl = new Label("Results");
        Label spinnerLabel = new Label("");
        HBox hBox0 = new HBox(12, offersToProcessLbl, spinnerLabel);
        hBox0.setPrefWidth(800);
        makerValidationGridPane.add(hBox0, 0, ++rowIndexSub);
        TextArea resultsArea = new BisqTextArea();
        resultsArea.setEditable(true);
        resultsArea.setWrapText(true);
        resultsArea.setPrefSize(800, 150);
        makerValidationGridPane.add(resultsArea, 0, ++rowIndexSub);
        makerValidationGridPane.add(new Label(""), 0, ++rowIndexSub);  // spacer
        addLabel(makerValidationGridPane, ++rowIndexSub, "Filters");
        InputTextField txIdFilter = addInputTextField(makerValidationGridPane, ++rowIndexSub, "TxId");
        InputTextField onionFilter = addInputTextField(makerValidationGridPane, ++rowIndexSub, "Onion");
        makerValidationGridPane.add(new Label(""), 0, ++rowIndexSub);  // spacer
        CheckBox feePaidWithBtc = addCheckBox(makerValidationGridPane, rowIndexSub, "Fee paid with BTC");
        CheckBox feePaidWithBsq = addCheckBox(makerValidationGridPane, rowIndexSub, "Fee paid with BSQ");
        feePaidWithBtc.setSelected(true);
        feePaidWithBsq.setSelected(true);
        HBox hBox2 = new HBox(12, feePaidWithBtc, feePaidWithBsq);
        hBox2.setAlignment(Pos.BASELINE_CENTER);
        hBox2.setPrefWidth(800);
        makerValidationGridPane.add(hBox2, 0, ++rowIndexSub);
        makerValidationGridPane.add(new Label(""), 0, ++rowIndexSub);  // spacer
        Button buttonStart = new AutoTooltipButton("Start checking offers");
        Button buttonStop = new AutoTooltipButton("Stop checking offers");
        buttonStart.setOnAction(e -> {
            startOfferValidation(spinnerLabel, resultsArea, offersToProcessLbl, txIdFilter.getText(), onionFilter.getText(), feePaidWithBtc.isSelected(), feePaidWithBsq.isSelected());
        });
        buttonStop.setOnAction(e -> {
            stopMempoolValidation(resultsArea);
        });
        buttonStart.disableProperty().bind(inProgressCount.greaterThan(0));
        buttonStop.disableProperty().bind(inProgressCount.greaterThan(0).not());
        HBox hBox3 = new HBox(12, buttonStart, buttonStop);
        hBox3.setAlignment(Pos.BASELINE_CENTER);
        hBox3.setPrefWidth(800);
        makerValidationGridPane.add(hBox3, 0, ++rowIndexSub);
    }

    private void addTakerValidationPane() {
        int rowIndexSub = 0;
        takerValidationGridPane = new GridPane();
        gridPane.add(takerValidationGridPane, 1, rowIndex);
        Label tradesToProcessLbl = new Label("Results");
        Label spinnerLabel = new Label("");
        HBox hBox0 = new HBox(12, tradesToProcessLbl, spinnerLabel);
        hBox0.setPrefWidth(800);
        takerValidationGridPane.add(hBox0, 0, ++rowIndexSub);
        TextArea resultsArea = new BisqTextArea();
        resultsArea.setEditable(true);
        resultsArea.setWrapText(true);
        resultsArea.setPrefSize(800, 150);
        takerValidationGridPane.add(resultsArea, 0, ++rowIndexSub);
        addLabel(takerValidationGridPane, ++rowIndexSub, "Filters");
        InputTextField txIdFilter2 = addInputTextField(takerValidationGridPane, ++rowIndexSub, "TxId");
        takerValidationGridPane.add(new Label(""), 0, ++rowIndexSub);  // spacer
        Button buttonStart = new AutoTooltipButton("Start checking trades");
        Button buttonStop = new AutoTooltipButton("Stop checking trades");
        buttonStart.setOnAction(e -> {
            //startSpecialTest(tradesToProcessLbl, resultsArea.getText());
            startTradeValidation(spinnerLabel, resultsArea, tradesToProcessLbl, txIdFilter2.getText());
        });
        buttonStop.setOnAction(e -> {
            stopMempoolValidation(resultsArea);
        });
        buttonStart.disableProperty().bind(inProgressCount.greaterThan(0));
        buttonStop.disableProperty().bind(inProgressCount.greaterThan(0).not());
        HBox hBox3 = new HBox(12, buttonStart, buttonStop);
        hBox3.setAlignment(Pos.BASELINE_CENTER);
        hBox3.setPrefWidth(800);
        takerValidationGridPane.add(hBox3, 0, ++rowIndexSub);
    }

    private void hideAllPanes() {
        makerValidationGridPane.setVisible(false);
        takerValidationGridPane.setVisible(false);
    }

    private void startOfferValidation(Label spinnerLabel, TextArea resultsArea, Label offersToProcessLbl, String txIdFilter, String onionFilter, boolean feePaidWithBtc, boolean feePaidWithBsq) {
        if (inProgressCount.get() > 0)
            return;
        resultsArea.setText("");
        List<Offer> offerListRaw = offerBookService.getOffers();
        offerListRaw = offerListRaw.stream()
                .filter(o -> onionFilter.length() == 0 || o.getMakerNodeAddress().toString().equalsIgnoreCase(onionFilter))
                .filter(o -> txIdFilter.length() == 0 || o.getOfferFeePaymentTxId().equalsIgnoreCase(txIdFilter))
                .collect(Collectors.toList());
        if (!feePaidWithBtc) { // exclude btc offers
            offerListRaw = offerListRaw.stream().filter(o -> !o.isCurrencyForMakerFeeBtc()).collect(Collectors.toList());
        }
        if (!feePaidWithBsq) { // exclude bsq offers
            offerListRaw = offerListRaw.stream().filter(Offer::isCurrencyForMakerFeeBtc).collect(Collectors.toList());
        }
        List<Offer> offerList = offerListRaw;
        inProgressCount.set(offerList.size());
        displayCheckStatus(offersToProcessLbl, offerList.size());
        UserThread.runAfter(() -> {
            if (offerList.size() > 0) {
                checkOffer(resultsArea, offersToProcessLbl, offerList);
                updateSpinner(spinnerLabel);
            }
        }, 1, SECONDS);
    }

    private void checkOffer(TextArea resultsArea, Label offersToProcessLbl, List<Offer> offerList) {
        if (inProgressCount.get() < 1)
            return;
        if (mempoolService.canRequestBeMade()) {
            Offer offer = offerList.get(0);
            offerList.remove(0);
            mempoolService.validateOfferMakerTx(offer.getOfferPayload(), (txValidator -> {
                if (txValidator.isFail()) {
                    resultsArea.setText(offer.getShortId() + " : " + txValidator.errorSummary() + "\n" + resultsArea.getText());
                    log.warn(offer.toString());
                    log.warn(txValidator.toString());
                }
                inProgressCount.set(inProgressCount.get() - 1);
                displayCheckStatus(offersToProcessLbl, offerList.size());
            }));
        }
        // trigger the next check
        UserThread.runAfter(() -> {
            if (offerList.size() > 0) {
                checkOffer(resultsArea, offersToProcessLbl, offerList);
            }
        }, 500, MILLISECONDS);
    }

    private void displayCheckStatus(Label offersToProcessLbl, int listSize) {
        offersToProcessLbl.setText(Integer.toString(listSize) + "  remaining to check");
    }

    private void startTradeValidation(Label spinnerLabel, TextArea resultsArea, Label tradesToProcessLbl, String txIdFilter) {
        if (inProgressCount.get() > 0)
            return;
        resultsArea.setText("");
        List<Trade> tradeListRaw = new ArrayList<>(closedTradableManager.getClosedTrades());
        tradeListRaw = tradeListRaw.stream()
                .filter(o -> txIdFilter.length() == 0 ? true : o.getTakerFeeTxId().equalsIgnoreCase(txIdFilter))
                .collect(Collectors.toList());
        List<Trade> tradeList = tradeListRaw;
        inProgressCount.set(tradeList.size());
        displayCheckStatus(tradesToProcessLbl, tradeList.size());
        UserThread.runAfter(() -> {
            if (tradeList.size() > 0) {
                checkTrade(resultsArea, tradesToProcessLbl, tradeList);
                updateSpinner(spinnerLabel);
            }
        }, 1, SECONDS);
    }

    private void checkTrade(TextArea resultsArea, Label tradesToProcessLbl, List<Trade> tradeList) {
        if (inProgressCount.get() < 1)
            return;
        if (mempoolService.canRequestBeMade()) {
            Trade trade = tradeList.get(0);
            tradeList.remove(0);
            mempoolService.validateOfferTakerTx(trade, (txValidator -> {
                if (txValidator.isFail()) {
                    resultsArea.setText(trade.getShortId() + " : " + txValidator.errorSummary() + "\n" + resultsArea.getText());
                    log.warn(trade.toString());
                    log.warn(txValidator.toString());
                }
                displayCheckStatus(tradesToProcessLbl, tradeList.size());
                inProgressCount.set(inProgressCount.get() - 1);
            }));
        }
        // trigger the next check
        UserThread.runAfter(() -> {
            if (tradeList.size() > 0) {
                checkTrade(resultsArea, tradesToProcessLbl, tradeList);
            }
        }, 100, MILLISECONDS);
    }

    private void updateSpinner(Label spinnerLabel) {
        String asciiSpinner = "⠁⠂⠄⡀⢀⠠⠐⠈";
        asciiSpinnerIdx -= 1;
        if (asciiSpinnerIdx < 0)
            asciiSpinnerIdx = asciiSpinner.length() - 1;
        spinnerLabel.setText(asciiSpinner.substring(asciiSpinnerIdx, asciiSpinnerIdx + 1));
        // trigger the next update
        if (inProgressCount.get() > 0) {
            UserThread.runAfter(() -> {
                    updateSpinner(spinnerLabel);
            }, 100, MILLISECONDS);
        }
    }

    private void stopMempoolValidation(TextArea resultsArea) {
        if (inProgressCount.get() > 0) {
            inProgressCount.set(0);
            resultsArea.setText(resultsArea.getText() + "\nSTOPPED");
        }
    }






    private void startSpecialTest(Label label, String json) {
        if (inProgressCount.get() > 0)
            return;
        Map<String, String> offers = new Gson().fromJson(json, Map.class);
        startSpecialTest2(label, offers);
    }

    private void startSpecialTest2(Label label, Map<String, String> offers) {
        displayCheckStatus(label, offers.size());
        if (offers.size() == 0)
            return;
        String firstKey = offers.keySet().stream().findFirst().get();
        String offerData = offers.remove(firstKey);
        String[] y = offerData.split(",");
        String id = y[0];
        String makerTxId = y[1];
        long amount = Long.parseLong(y[2]);
        boolean isCurrencyForMakerFeeBtc = Long.parseLong(y[4]) > 0;
        log.warn("Start testing maker/taker/deposit of offer {}", id);
        mempoolService.getMakerOutspends(new TxValidator(null, makerTxId, Coin.valueOf(amount), isCurrencyForMakerFeeBtc), (result -> {
            String depositTxId = result.extractDepositTxIdFromMakerOutspends();
            UserThread.runAfter(() -> {
                if (depositTxId == null) {  // no deposit tx means the offer is still open
                    startSpecialTest2(label, offers);
                } else {
                    processDepositTx(label, makerTxId, depositTxId, Coin.valueOf(amount), offers);
                }
            }, 100, MILLISECONDS);
        }));
    }

    private void processDepositTx(Label label, String makerTxId, String depositTxId, Coin amount, Map<String, String> offers) {
        mempoolService.validateDepositTx(new TxValidator(null, depositTxId, amount, null), (result -> {
            String takerTxId = result.extractTakerTxIdFromDepositTx(makerTxId);
            UserThread.runAfter(() -> {
                if (takerTxId == null) {  // no taker tx means the offer was canceled
                    startSpecialTest2(label, offers);
                } else {
                    processTakerTx(label, takerTxId, amount, offers);
                }
            }, 100, MILLISECONDS);
        }));
    }

    private void processTakerTx(Label label, String takerTxId, Coin amount, Map<String, String> offers) {
        mempoolService.validateOfferTakerTx(new TxValidator(null, takerTxId, amount, null), (result -> {
            if (result.isFail()) {
                log.warn("{} : {}", takerTxId, result.toString());
            } else {
                log.warn("{} : VALIDATED SUCCESSFULLY", takerTxId);
            }
            // trigger processing of the next offer
            UserThread.runAfter(() -> {
                startSpecialTest2(label, offers);
            }, 100, MILLISECONDS);
        }));
    }


}
