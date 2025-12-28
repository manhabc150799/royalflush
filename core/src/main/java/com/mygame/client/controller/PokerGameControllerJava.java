package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.RoyalFlushG;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UIHelper;
import com.mygame.client.ui.UIResourceManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.poker.PokerGameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PokerGameController viết bằng Java thuần, không sử dụng LML templates
 * Sử dụng UIHelper và UIResourceManager để tạo UI components
 * 
 * @author Royal FlushG Team
 */
@View(id = "pokerGameJava", value = "ui/templates/poker_game_java.lml")
public class PokerGameControllerJava implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PokerGameControllerJava.class);
    
    @Inject private SessionManager sessionManager;
    @Inject private InterfaceService interfaceService;
    
    private Stage stage;
    private Viewport viewport;
    private UIHelper uiHelper;
    private UIResourceManager resourceManager;
    
    // Game state
    private PokerGameState gameState;
    private Deck deck;
    private Map<String, com.badlogic.gdx.graphics.Texture> textureCache = new HashMap<>();
    private List<Card> playerHoleCards;
    private long playerChips = 1000;
    private long currentBet = 0;
    
    // UI Components
    private Image backgroundImage;
    private Table rootTable;
    private Table playerCardsTable;
    private Table communityCardsTable;
    private Label potLabel;
    private Label betAmountLabel;
    private com.badlogic.gdx.scenes.scene2d.ui.Slider betSlider;
    private ImageButton foldButton;
    private ImageButton checkButton;
    private ImageButton callButton;
    private ImageButton raiseButton;
    private ImageButton allinButton;
    private ImageButton menuButton;
    
    /**
     * Khởi tạo UI khi view được show lần đầu
     */
    public void initialize() {
        logger.info("Khởi tạo PokerGameControllerJava...");
        
        // Khởi tạo viewport
        viewport = new FitViewport(RoyalFlushG.WIDTH, RoyalFlushG.HEIGHT);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);
        
        // Khởi tạo helpers
        uiHelper = new UIHelper();
        resourceManager = UIResourceManager.getInstance();
        
        // Tạo UI
        createUI();
        
        // Bắt đầu game
        startSingleplayerGame();
        
        logger.info("PokerGameControllerJava đã khởi tạo thành công");
    }
    
    /**
     * Tạo tất cả UI components
     */
    private void createUI() {
        // Tạo background
        createBackground();
        
        // Tạo root table
        createRootTable();
        
        // Tạo game info labels
        createGameInfo();
        
        // Tạo card tables
        createCardTables();
        
        // Tạo action buttons
        createActionButtons();
        
        // Layout
        layoutUI();
    }
    
    /**
     * Tạo background
     */
    private void createBackground() {
        try {
            backgroundImage = uiHelper.createBackground("Background_poker.png");
            stage.addActor(backgroundImage);
        } catch (Exception e) {
            logger.warn("Không thể load background: {}", e.getMessage());
        }
    }
    
    /**
     * Tạo root table
     */
    private void createRootTable() {
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.pad(20f);
        stage.addActor(rootTable);
    }
    
    /**
     * Tạo game info labels
     */
    private void createGameInfo() {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = com.kotcrab.vis.ui.VisUI.getSkin().getFont("default-font");
        labelStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        
        potLabel = new Label("Pot: 0", labelStyle);
        betAmountLabel = new Label("Bet: 0", labelStyle);
        
        // Tạo bet slider
        com.badlogic.gdx.scenes.scene2d.ui.Skin skin = com.kotcrab.vis.ui.VisUI.getSkin();
        betSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0, playerChips, 10, false, skin);
        betSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateBetAmount();
            }
        });
    }
    
    /**
     * Tạo card tables
     */
    private void createCardTables() {
        playerCardsTable = new Table();
        communityCardsTable = new Table();
    }
    
    /**
     * Tạo action buttons
     */
    private void createActionButtons() {
        // Fold button
        foldButton = createGameButton("btn_cancel", "Fold", () -> {
            logger.info("Player folded");
            if (gameState != null) {
                gameState.fold(sessionManager.getCurrentUserId());
                updateUI();
            }
        });
        
        // Check button
        checkButton = createGameButton("btn_check", "Check", () -> {
            logger.info("Player checked");
            // Check = bet 0
            updateUI();
        });
        
        // Call button
        callButton = createGameButton("btn_check", "Call", () -> {
            logger.info("Player called");
            if (gameState != null) {
                long callAmount = currentBet - gameState.getPlayerBet(sessionManager.getCurrentUserId());
                if (callAmount > 0 && callAmount <= playerChips) {
                    gameState.bet(sessionManager.getCurrentUserId(), callAmount);
                    playerChips -= callAmount;
                    updateUI();
                }
            }
        });
        
        // Raise button
        raiseButton = createGameButton("btn_bet", "Raise", () -> {
            if (betSlider != null) {
                long raiseAmount = (long) betSlider.getValue();
                logger.info("Player raised: {}", raiseAmount);
                
                if (raiseAmount > 0 && raiseAmount <= playerChips) {
                    if (gameState != null) {
                        gameState.bet(sessionManager.getCurrentUserId(), raiseAmount);
                        playerChips -= raiseAmount;
                        currentBet = gameState.getPlayerBet(sessionManager.getCurrentUserId());
                        updateUI();
                    }
                }
            }
        });
        
        // All-in button
        allinButton = createGameButton("btn_bet", "All-in", () -> {
            logger.info("Player all-in");
            if (gameState != null) {
                long allinAmount = playerChips;
                gameState.bet(sessionManager.getCurrentUserId(), allinAmount);
                playerChips = 0;
                updateUI();
            }
        });
        
        // Menu button
        menuButton = createGameButton("btn_pause", "Menu", () -> {
            logger.info("Show menu");
            if (interfaceService != null) {
                interfaceService.show(com.mygame.client.controller.MenuController.class);
            }
        });
    }
    
    /**
     * Tạo game button với label
     */
    private ImageButton createGameButton(String buttonName, String labelText, Runnable onClick) {
        ImageButton button;
        try {
            button = uiHelper.createCatUIButtonWithListener(buttonName, onClick);
        } catch (Exception e) {
            logger.warn("Không thể tạo CatUI button {}, tạo button đơn giản: {}", buttonName, e.getMessage());
            button = createSimpleButton("images/CatUI/" + buttonName + "1.png", 
                                       "images/CatUI/" + buttonName + "2.png", onClick);
        }
        
        if (button != null) {
            button.setSize(120, 50);
        }
        
        return button;
    }
    
    /**
     * Tạo button đơn giản từ raw textures
     */
    private ImageButton createSimpleButton(String upPath, String downPath, Runnable onClick) {
        try {
            return uiHelper.createImageButtonWithListener(upPath, downPath, onClick);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo button: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Layout UI
     */
    private void layoutUI() {
        rootTable.clear();
        
        // Top row: Pot, Bet amount, Menu button
        Table topRow = new Table();
        topRow.add(potLabel).padRight(20f);
        topRow.add(betAmountLabel).padRight(20f);
        topRow.add(menuButton).size(80, 40).right();
        rootTable.add(topRow).colspan(2).top().padBottom(20f).row();
        
        // Community cards (center top)
        rootTable.add(communityCardsTable).colspan(2).center().padBottom(30f).row();
        
        // Player cards (center bottom)
        rootTable.add(playerCardsTable).colspan(2).center().padBottom(30f).row();
        
        // Bet slider
        Table sliderRow = new Table();
        sliderRow.add(new Label("Bet Amount: ", com.kotcrab.vis.ui.VisUI.getSkin())).padRight(10f);
        sliderRow.add(betSlider).width(300f).padRight(10f);
        rootTable.add(sliderRow).colspan(2).center().padBottom(20f).row();
        
        // Action buttons
        Table buttonRow = new Table();
        buttonRow.add(foldButton).size(120, 50).pad(5f);
        buttonRow.add(checkButton).size(120, 50).pad(5f);
        buttonRow.add(callButton).size(120, 50).pad(5f);
        buttonRow.add(raiseButton).size(120, 50).pad(5f);
        buttonRow.add(allinButton).size(120, 50).pad(5f);
        rootTable.add(buttonRow).colspan(2).center();
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
        com.badlogic.gdx.graphics.Texture texture = getTexture(assetPath);
        Image image = new Image(texture);
        image.setSize(100, 140);
        return image;
    }
    
    /**
     * Lấy texture từ cache hoặc load mới
     */
    private com.badlogic.gdx.graphics.Texture getTexture(String assetPath) {
        if (textureCache.containsKey(assetPath)) {
            return textureCache.get(assetPath);
        }
        
        com.badlogic.gdx.graphics.Texture texture = resourceManager.getTexture(assetPath);
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
            betAmountLabel.setText("Bet: " + betAmount);
        }
    }
    
    @Override
    public void render(Stage stage, float delta) {
        // Clear screen
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        
        // Update viewport nếu window resize
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        
        // Update và render stage
        stage.act(delta);
        stage.draw();
    }
    
    /**
     * Dispose resources
     */
    public void dispose() {
        logger.info("Đang dispose PokerGameControllerJava...");
        
        // Dispose textures
        for (com.badlogic.gdx.graphics.Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
        
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        
        logger.info("Đã dispose PokerGameControllerJava");
    }
}

