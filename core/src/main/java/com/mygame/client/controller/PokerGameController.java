package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.asset.AssetService;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.github.czyzby.autumn.mvc.stereotype.ViewActionContainer;
import com.github.czyzby.lml.annotation.LmlAction;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.github.czyzby.lml.annotation.LmlActor;
import com.github.czyzby.lml.annotation.LmlAfter;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.mygame.client.service.SessionManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.poker.PokerGameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Poker game
 */
@View(id = "pokerGame", value = "ui/templates/poker_game.lml")
@ViewActionContainer("poker")
public class PokerGameController implements ViewRenderer, ActionContainer {
    private static final Logger logger = LoggerFactory.getLogger(PokerGameController.class);
    
    @Inject private SessionManager sessionManager;
    @Inject private InterfaceService interfaceService;
    @Inject private AssetService assetService;
    
    @LmlActor("playerCardsTable") private VisTable playerCardsTable;
    @LmlActor("communityCardsTable") private VisTable communityCardsTable;
    @LmlActor("potLabel") private VisLabel potLabel;
    @LmlActor("betSlider") private VisSlider betSlider;
    @LmlActor("betAmountLabel") private VisLabel betAmountLabel;
    @LmlActor("foldButton") private VisTextButton foldButton;
    @LmlActor("checkButton") private VisTextButton checkButton;
    @LmlActor("callButton") private VisTextButton callButton;
    @LmlActor("raiseButton") private VisTextButton raiseButton;
    @LmlActor("allinButton") private VisTextButton allinButton;
    
    private PokerGameState gameState;
    private Deck deck;
    private Map<String, Texture> textureCache = new HashMap<>();
    private List<Card> playerHoleCards;
    private long playerChips = 1000;
    private long currentBet = 0;
    
    @LmlAfter
    public void initialize() {
        logger.info("PokerGameController đã khởi tạo");
        
        // Setup bet slider listener
        if (betSlider != null && betAmountLabel != null) {
            betSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updateBetAmount();
                }
            });
        }
        
        // Start singleplayer game
        startSingleplayerGame();
    }
    
    /**
     * Bắt đầu singleplayer game
     */
    private void startSingleplayerGame() {
        logger.info("Bắt đầu singleplayer poker game");
        
        // Tạo deck và xáo bài
        deck = new Deck();
        deck.shuffle();
        
        // Tạo game state với 5 players (1 player + 4 bots)
        List<Integer> playerIds = java.util.Arrays.asList(
            sessionManager.getCurrentUserId(),
            1001, 1002, 1003, 1004 // Bot IDs
        );
        gameState = new PokerGameState(playerIds, 1000, 10, 20);
        
        // Deal hole cards cho player
        playerHoleCards = deck.deal(2);
        gameState.dealHoleCards(sessionManager.getCurrentUserId(), playerHoleCards);
        
        // Deal hole cards cho bots (ẩn)
        for (int i = 1; i < 5; i++) {
            List<Card> botCards = deck.deal(2);
            gameState.dealHoleCards(playerIds.get(i), botCards);
        }
        
        // Hiển thị player cards
        displayPlayerCards();
        
        // Update UI
        updateUI();
        
        logger.info("Đã deal cards cho tất cả players");
    }
    
    /**
     * Hiển thị player hole cards
     */
    private void displayPlayerCards() {
        if (playerCardsTable == null || playerHoleCards == null) {
            return;
        }
        
        playerCardsTable.clearChildren();
        
        for (Card card : playerHoleCards) {
            Image cardImage = createCardImage(card);
            playerCardsTable.add(cardImage).width(100).height(140).pad(5);
        }
    }
    
    /**
     * Tạo Image từ Card
     */
    private Image createCardImage(Card card) {
        String assetPath = card.getAssetPath();
        Texture texture = getTexture(assetPath);
        Image image = new Image(texture);
        image.setSize(100, 140);
        return image;
    }
    
    /**
     * Tạo Image từ card back
     */
    private Image createCardBackImage() {
        String assetPath = Card.getBackAssetPath();
        Texture texture = getTexture(assetPath);
        Image image = new Image(texture);
        image.setSize(100, 140);
        return image;
    }
    
    /**
     * Lấy texture từ cache hoặc load mới
     */
    private Texture getTexture(String assetPath) {
        if (textureCache.containsKey(assetPath)) {
            return textureCache.get(assetPath);
        }
        
        Texture texture = new Texture(Gdx.files.internal(assetPath));
        textureCache.put(assetPath, texture);
        return texture;
    }
    
    /**
     * Hiển thị community cards
     */
    private void displayCommunityCards() {
        if (communityCardsTable == null || gameState == null) {
            return;
        }
        
        communityCardsTable.clearChildren();
        
        List<Card> communityCards = gameState.getCommunityCards();
        for (Card card : communityCards) {
            Image cardImage = createCardImage(card);
            // Animation: card bay từ deck
            cardImage.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.moveBy(-200, 0),
                Actions.parallel(
                    Actions.fadeIn(0.3f),
                    Actions.moveBy(200, 0, 0.3f)
                )
            ));
            communityCardsTable.add(cardImage).width(80).height(120).pad(5);
        }
    }
    
    /**
     * Update UI
     */
    private void updateUI() {
        if (gameState != null) {
            if (potLabel != null) {
                potLabel.setText("Pot: " + gameState.getPot());
            }
            
            // Update bet slider max
            if (betSlider != null) {
                betSlider.setRange(0, playerChips);
            }
            
            displayCommunityCards();
        }
    }
    
    /**
     * Update bet amount label
     */
    private void updateBetAmount() {
        if (betSlider != null && betAmountLabel != null) {
            long betAmount = (long) betSlider.getValue();
            betAmountLabel.setText(String.valueOf(betAmount));
        }
    }
    
    /**
     * Action: Fold
     */
    @LmlAction("fold")
    public void fold() {
        logger.info("Player folded");
        gameState.fold(sessionManager.getCurrentUserId());
        // TODO: Bot turn logic
    }
    
    /**
     * Action: Check
     */
    @LmlAction("check")
    public void check() {
        logger.info("Player checked");
        // Check = bet 0
        // TODO: Implement
    }
    
    /**
     * Action: Call
     */
    @LmlAction("call")
    public void call() {
        logger.info("Player called");
        long callAmount = currentBet - gameState.getPlayerBet(sessionManager.getCurrentUserId());
        if (callAmount > 0) {
            gameState.bet(sessionManager.getCurrentUserId(), callAmount);
            playerChips -= callAmount;
            updateUI();
        }
    }
    
    /**
     * Action: Raise
     */
    @LmlAction("raise")
    public void raise() {
        if (betSlider == null) return;
        
        long raiseAmount = (long) betSlider.getValue();
        logger.info("Player raised: {}", raiseAmount);
        
        if (raiseAmount > 0) {
            gameState.bet(sessionManager.getCurrentUserId(), raiseAmount);
            playerChips -= raiseAmount;
            currentBet = gameState.getPlayerBet(sessionManager.getCurrentUserId());
            updateUI();
        }
    }
    
    /**
     * Action: All-in
     */
    @LmlAction("allin")
    public void allin() {
        logger.info("Player all-in");
        long allinAmount = playerChips;
        gameState.bet(sessionManager.getCurrentUserId(), allinAmount);
        playerChips = 0;
        updateUI();
    }
    
    /**
     * Action: Show menu
     */
    @LmlAction("showMenu")
    public void showMenu() {
        // TODO: Show pause menu
        logger.info("Show menu");
    }
    
    /**
     * Deal flop
     */
    public void dealFlop() {
        if (deck != null && gameState != null) {
            List<Card> flop = deck.deal(3);
            gameState.dealFlop(flop);
            displayCommunityCards();
            logger.info("Dealt flop");
        }
    }
    
    /**
     * Deal turn
     */
    public void dealTurn() {
        if (deck != null && gameState != null) {
            Card turn = deck.deal();
            gameState.dealTurn(turn);
            displayCommunityCards();
            logger.info("Dealt turn");
        }
    }
    
    /**
     * Deal river
     */
    public void dealRiver() {
        if (deck != null && gameState != null) {
            Card river = deck.deal();
            gameState.dealRiver(river);
            displayCommunityCards();
            logger.info("Dealt river");
        }
    }
    
    @Override
    public void render(Stage stage, float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.act(delta);
        stage.draw();
    }
    
    /**
     * Cleanup textures
     */
    public void dispose() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
    }
}
