package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.github.czyzby.autumn.mvc.stereotype.ViewActionContainer;
import com.github.czyzby.lml.annotation.LmlAction;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.github.czyzby.lml.annotation.LmlActor;
import com.github.czyzby.lml.annotation.LmlAfter;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.mygame.client.service.SessionManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.tienlen.CardCollection;
import com.mygame.shared.game.tienlen.TienLenCombinationType;
import com.mygame.shared.game.tienlen.TienLenGameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Controller cho Tiến Lên game
 */
@View(id = "tienlenGame", value = "ui/templates/tienlen_game.lml")
@ViewActionContainer("tienlen")
public class TienLenGameController implements ViewRenderer, ActionContainer {
    private static final Logger logger = LoggerFactory.getLogger(TienLenGameController.class);
    
    @Inject private SessionManager sessionManager;
    @Inject private InterfaceService interfaceService;
    
    @LmlActor("playerCardsTable") private VisTable playerCardsTable;
    @LmlActor("playedCardsTable") private VisTable playedCardsTable;
    @LmlActor("playButton") private VisTextButton playButton;
    @LmlActor("skipButton") private VisTextButton skipButton;
    @LmlActor("sortButton") private VisTextButton sortButton;
    
    private TienLenGameState gameState;
    private Deck deck;
    private Map<String, Texture> textureCache = new HashMap<>();
    private List<Card> playerHand;
    private Set<Card> selectedCards = new HashSet<>();
    private List<Card> currentTrick = new ArrayList<>();
    
    @LmlAfter
    public void initialize() {
        logger.info("TienLenGameController đã khởi tạo");
        
        // Start singleplayer game
        startSingleplayerGame();
    }
    
    /**
     * Bắt đầu singleplayer game
     */
    private void startSingleplayerGame() {
        logger.info("Bắt đầu singleplayer tien len game");
        
        // Tạo deck và xáo bài
        deck = new Deck();
        deck.shuffle();
        
        // Tạo game state với 4 players
        List<Integer> playerIds = Arrays.asList(
            sessionManager.getCurrentUserId(),
            1001, 1002, 1003 // Bot IDs
        );
        gameState = new TienLenGameState(playerIds);
        
        // Deal 13 cards cho mỗi player
        for (Integer playerId : playerIds) {
            List<Card> hand = deck.deal(13);
            gameState.dealHand(playerId, hand);
            
            if (playerId == sessionManager.getCurrentUserId()) {
                playerHand = new ArrayList<>(hand);
            }
        }
        
        // Hiển thị player hand
        displayPlayerHand();
        
        logger.info("Đã deal cards cho tất cả players");
    }
    
    /**
     * Hiển thị player hand với arc layout
     */
    private void displayPlayerHand() {
        if (playerCardsTable == null || playerHand == null) {
            return;
        }
        
        playerCardsTable.clearChildren();
        
        // Sort hand
        playerHand.sort((a, b) -> Integer.compare(
            a.getRankValueForTienLen(),
            b.getRankValueForTienLen()
        ));
        
        for (Card card : playerHand) {
            CardActor cardActor = createCardActor(card);
            playerCardsTable.add(cardActor).width(60).height(90).pad(2);
        }
    }
    
    /**
     * Tạo CardActor với hover và click effects
     */
    private CardActor createCardActor(Card card) {
        Texture texture = getTexture(card.getAssetPath());
        CardActor actor = new CardActor(texture, card);
        actor.setSize(60, 90);
        
        // Hover effect: nảy lên
        actor.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.addAction(Actions.moveBy(0, 20, 0.2f));
            }
            
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (!selectedCards.contains(card)) {
                    actor.addAction(Actions.moveBy(0, -20, 0.2f));
                }
            }
            
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleCardSelection(card, actor);
            }
        });
        
        return actor;
    }
    
    /**
     * Toggle card selection
     */
    private void toggleCardSelection(Card card, CardActor actor) {
        if (selectedCards.contains(card)) {
            selectedCards.remove(card);
            actor.addAction(Actions.moveBy(0, -20, 0.2f));
            actor.setSelected(false);
        } else {
            selectedCards.add(card);
            actor.addAction(Actions.moveBy(0, 20, 0.2f));
            actor.setSelected(true);
        }
        logger.debug("Selected cards: {}", selectedCards.size());
    }
    
    /**
     * Lấy texture từ cache
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
     * Action: Play selected cards
     */
    @LmlAction("playCards")
    public void playCards() {
        if (selectedCards.isEmpty()) {
            logger.warn("Không có card nào được chọn");
            return;
        }
        
        List<Card> cardsToPlay = new ArrayList<>(selectedCards);
        
        // Validate combination
        TienLenCombinationType type = CardCollection.detectCombination(cardsToPlay);
        if (type == TienLenCombinationType.INVALID) {
            logger.warn("Combination không hợp lệ");
            return;
        }
        
        // Check có thể chặt được không
        if (!currentTrick.isEmpty() && gameState.getCurrentTrickType() != null) {
            if (!CardCollection.canBeat(
                gameState.getCurrentTrickType(),
                currentTrick,
                type,
                cardsToPlay
            )) {
                logger.warn("Không thể chặt được bài trên bàn");
                return;
            }
        }
        
        // Play cards
        if (gameState.playCards(sessionManager.getCurrentUserId(), cardsToPlay)) {
            // Remove from hand
            playerHand.removeAll(cardsToPlay);
            selectedCards.clear();
            
            // Update current trick
            currentTrick = new ArrayList<>(cardsToPlay);
            
            // Display played cards
            displayPlayedCards(cardsToPlay);
            
            // Update player hand display
            displayPlayerHand();
            
            // Next turn
            gameState.nextTurn();
            
            logger.info("Đã đánh {} cards", cardsToPlay.size());
        }
    }
    
    /**
     * Action: Skip turn
     */
    @LmlAction("skipTurn")
    public void skipTurn() {
        gameState.skipTurn(sessionManager.getCurrentUserId());
        gameState.nextTurn();
        logger.info("Đã bỏ lượt");
    }
    
    /**
     * Action: Sort hand
     */
    @LmlAction("sortHand")
    public void sortHand() {
        gameState.sortHand(sessionManager.getCurrentUserId());
        playerHand = gameState.getPlayerHand(sessionManager.getCurrentUserId());
        displayPlayerHand();
        logger.info("Đã xếp bài");
    }
    
    /**
     * Hiển thị played cards
     */
    private void displayPlayedCards(List<Card> cards) {
        if (playedCardsTable == null) {
            return;
        }
        
        playedCardsTable.clearChildren();
        
        for (Card card : cards) {
            Image cardImage = new Image(getTexture(card.getAssetPath()));
            cardImage.setSize(60, 90);
            playedCardsTable.add(cardImage).pad(5);
        }
    }
    
    /**
     * Action: Show menu
     */
    @LmlAction("showMenu")
    public void showMenu() {
        // TODO: Show pause menu
        logger.info("Show menu");
    }
    
    @Override
    public void render(Stage stage, float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.act(delta);
        stage.draw();
    }
    
    /**
     * Cleanup
     */
    public void dispose() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
    }
    
    /**
     * Custom Actor cho Card với selection state
     */
    private static class CardActor extends Image {
        private final Card card;
        private boolean selected = false;
        
        public CardActor(Texture texture, Card card) {
            super(texture);
            this.card = card;
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                this.setColor(1f, 1f, 0.5f, 1f); // Highlight
            } else {
                this.setColor(1f, 1f, 1f, 1f);
            }
        }
        
        public Card getCard() {
            return card;
        }
    }
}
