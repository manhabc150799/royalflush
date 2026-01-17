package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.client.RoyalFlushG;
import com.mygame.client.service.NetworkService;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.poker.PokerGameState;
import com.mygame.shared.game.tienlen.TienLenGameState;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;
import com.mygame.shared.network.packets.game.PlayerTurnPacket;
import com.mygame.shared.network.packets.game.GameEndPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameScreen - Main gameplay screen for Poker and Tien Len.
 * 
 * Layout (Stack-based):
 * - Layer 0: Background (Green for Poker, Blue for Tien Len)
 * - Layer 1: Game Area (Table center, player seats, community cards)
 * - Layer 2: HUD/Controls (Action buttons, pot display, turn indicator)
 * 
 * Features:
 * - Oval seat positioning using Math.cos/sin
 * - Control panel slides up/down based on turn
 * - Card deal animations
 * - Context-sensitive center area (community cards vs tricks)
 */
public class GameScreen implements Screen {

    private static final String TAG = "GameScreen";

    // Background paths
    private static final String BACKGROUND_POKER = "ui/Background_poker.png";
    private static final String BACKGROUND_TIENLEN = "ui/Background_tienlen.png";

    // Layout constants
    private static final float HERO_HAND_Y = 20f;
    private static final float CONTROL_PANEL_HEIGHT = 100f;
    private static final float SEAT_WIDTH = 140f;
    private static final float SEAT_HEIGHT = 70f;

    // Stage and UI
    private Stage stage;
    private Skin skin;
    private TextureAtlas atlas;
    private Texture backgroundTexture;

    // Game state
    private GameType gameType;
    private int localPlayerId;
    private int roomId;
    private int currentTurnPlayerId;
    private boolean isMyTurn;

    // Network
    private NetworkService networkService;

    // Warning toast for validation
    private Label warningToast;

    // UI Layers
    private Stack rootStack;
    private Image backgroundImage;
    private Table gameAreaTable;
    private Table hudTable;
    private Table controlPanel;

    // Player seats (mapped by player ID)
    private Map<Integer, PlayerSeatActor> playerSeats;
    private List<Integer> playerOrder;

    // Hero hand (local player's cards)
    private Table heroHandContainer;
    private List<CardActor> heroCards;
    private List<Card> selectedCards; // For Tien Len

    // Center area
    private Table centerArea;
    private List<CardActor> communityCards; // Poker
    private List<CardActor> trickCards; // Tien Len
    private Label potLabel; // Poker

    // Control buttons
    private Table pokerControls;
    private Table tienLenControls;
    private Slider betSlider;
    private Label betAmountLabel;

    // Animation source (deck position)
    private float deckX, deckY;

    /**
     * Create GameScreen for the specified game type.
     * 
     * @param gameType       Game mode (POKER or TIENLEN)
     * @param localPlayerId  Local player's user ID
     * @param roomId         Room ID for packet construction
     * @param networkService Network service for sending packets
     */
    public GameScreen(GameType gameType, int localPlayerId, int roomId, NetworkService networkService) {
        this.gameType = gameType;
        this.localPlayerId = localPlayerId;
        this.roomId = roomId;
        this.networkService = networkService;
        this.playerSeats = new HashMap<>();
        this.playerOrder = new ArrayList<>();
        this.heroCards = new ArrayList<>();
        this.selectedCards = new ArrayList<>();
        this.communityCards = new ArrayList<>();
        this.trickCards = new ArrayList<>();
        this.isMyTurn = false;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(RoyalFlushG.VIRTUAL_WIDTH, RoyalFlushG.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        skin = UISkinManager.getInstance().getSkin();
        atlas = skin.getAtlas();

        buildUI();
    }

    // Game area group for absolute positioning
    private com.badlogic.gdx.scenes.scene2d.Group gameAreaGroup;

    /**
     * Build the complete UI structure.
     * 
     * Architecture:
     * - Layer 0: Background Image
     * - Layer 1: GameAreaGroup (Group) - seats and pot with absolute coords
     * - Layer 2: HUD Table (Table) - deck, hero hand, controls
     */
    private void buildUI() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Root Stack layout
        rootStack = new Stack();
        rootStack.setFillParent(true);

        // === LAYER 0: Background ===
        loadBackground();
        if (backgroundTexture != null) {
            backgroundImage = new Image(backgroundTexture);
            backgroundImage.setFillParent(true);
            rootStack.add(backgroundImage);
        }

        // === LAYER 1: Game Area (Group for absolute positioning) ===
        gameAreaGroup = new com.badlogic.gdx.scenes.scene2d.Group();
        gameAreaGroup.setSize(screenW, screenH);
        buildGameArea(screenW, screenH);

        // Wrap Group in a Container for Stack
        Container<com.badlogic.gdx.scenes.scene2d.Group> gameAreaContainer = new Container<>(gameAreaGroup);
        gameAreaContainer.setFillParent(true);
        gameAreaContainer.fill();
        rootStack.add(gameAreaContainer);

        // === LAYER 2: HUD (Table for alignment) ===
        hudTable = new Table();
        hudTable.setFillParent(true);
        buildHUD();
        rootStack.add(hudTable);

        stage.addActor(rootStack);

        // Deck position for animations
        deckX = screenW / 2f;
        deckY = screenH / 2f;

        Gdx.app.log(TAG, "buildUI complete. Screen: " + screenW + "x" + screenH);
    }

    /**
     * Load background texture based on game type.
     */
    private void loadBackground() {
        String bgPath = (gameType == GameType.POKER) ? BACKGROUND_POKER : BACKGROUND_TIENLEN;
        try {
            backgroundTexture = new Texture(Gdx.files.internal(bgPath));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to load background: " + bgPath, e);
        }
    }

    /**
     * Build the game area with community cards and pot.
     * Uses absolute positioning (1280x720 base, scaled to screen).
     */
    private void buildGameArea(float screenW, float screenH) {
        float scaleX = screenW / 1280f;
        float scaleY = screenH / 720f;

        if (gameType == GameType.POKER) {
            buildPokerBoard(screenW, screenH, scaleX, scaleY);
        } else {
            buildTienLenBoard(screenW, screenH, scaleX, scaleY);
        }
    }

    // Store community card slot references
    private Table[] communityCardSlots = new Table[5];

    /**
     * Build Poker game board with 5 visible community card slots.
     * 
     * Layout at 1280x720:
     * - Community Cards: Y=400, centered horizontally
     * - Pot Label: Y=340, centered
     */
    private void buildPokerBoard(float screenW, float screenH, float scaleX, float scaleY) {
        float cardW = CardActor.CARD_WIDTH * 0.75f;
        float cardH = CardActor.CARD_HEIGHT * 0.75f;
        float cardGap = 10f;
        float totalWidth = cardW * 5 + cardGap * 4;
        float startX = (screenW - totalWidth) / 2f;
        float cardY = 400 * scaleY;

        // Create 5 visible card slots with semi-transparent background
        for (int i = 0; i < 5; i++) {
            Table slot = new Table();
            slot.setBackground(skin.getDrawable("panel2")); // Visible placeholder
            slot.setSize(cardW, cardH);
            slot.setPosition(startX + i * (cardW + cardGap), cardY);

            communityCardSlots[i] = slot;
            gameAreaGroup.addActor(slot);
        }

        // Pot Label - positioned below cards, centered, NO BACKGROUND
        Label.LabelStyle potStyle = new Label.LabelStyle();
        potStyle.font = skin.getFont("Big_blue_font");
        potStyle.fontColor = Color.GOLD;

        potLabel = new Label("POT: $0", potStyle);
        potLabel.setFontScale(1.1f);
        potLabel.setPosition(screenW / 2f - 60, 340 * scaleY);
        gameAreaGroup.addActor(potLabel);

        // Add coin icon next to pot
        TextureAtlas.AtlasRegion creditIcon = atlas.findRegion("icon_credit1");
        if (creditIcon != null) {
            Image coinImage = new Image(creditIcon);
            coinImage.setSize(24, 24);
            coinImage.setPosition(screenW / 2f - 90, 340 * scaleY);
            gameAreaGroup.addActor(coinImage);
        }

        Gdx.app.log(TAG, "Poker board built: 5 card slots at Y=" + cardY);
    }

    /**
     * Build Tien Len game board (trick area).
     */
    private void buildTienLenBoard(float screenW, float screenH, float scaleX, float scaleY) {
        // Trick display area - center of screen
        centerArea = new Table();
        centerArea.setSize(400, 200);
        centerArea.setPosition(screenW / 2f - 200, screenH / 2f - 100);

        Label.LabelStyle style = new Label.LabelStyle();
        style.font = skin.getFont("Blue_font");
        style.fontColor = Color.WHITE;

        Label instructionLabel = new Label("Play cards here", style);
        instructionLabel.setFontScale(1f);
        centerArea.add(instructionLabel);

        gameAreaGroup.addActor(centerArea);
    }

    /**
     * Build HUD layer with controls.
     * 
     * Layout:
     * - Bottom-Left: Dealer deck image
     * - Bottom-Center: Hero hand cards
     * - Bottom-Right: Action buttons (Fold/Check/Raise)
     */
    private void buildHUD() {
        hudTable.bottom();

        // === Dealer Deck (bottom left) - NO BACKGROUND ===
        Table dealerArea = new Table();
        // Removed: dealerArea.setBackground(skin.getDrawable("hub1"));
        dealerArea.pad(8);

        TextureAtlas.AtlasRegion deckBack = atlas.findRegion("BACK");
        if (deckBack != null) {
            Image deckImage = new Image(deckBack);
            deckImage.setSize(CardActor.CARD_WIDTH * 0.6f, CardActor.CARD_HEIGHT * 0.6f);
            dealerArea.add(deckImage).size(CardActor.CARD_WIDTH * 0.6f, CardActor.CARD_HEIGHT * 0.6f);
        }
        Label dealerLabel = new Label("DECK", skin);
        dealerLabel.setFontScale(0.6f);
        dealerArea.row();
        dealerArea.add(dealerLabel).padTop(4);

        // === Hero Hand Container (bottom center) - FIXED MIN SIZE ===
        heroHandContainer = new Table();
        // Light background for visibility
        heroHandContainer.pad(10);

        // === Control Panel (bottom right) - FIXED WIDTH ===
        controlPanel = new Table();
        controlPanel.pad(15);

        if (gameType == GameType.POKER) {
            buildPokerControls();
        } else {
            buildTienLenControls();
        }

        // Layout: [Dealer] [Hero Cards] [Controls]
        Table bottomSection = new Table();
        bottomSection.defaults().bottom();

        // Left: Dealer deck
        bottomSection.add(dealerArea).padLeft(20).padBottom(10).left();

        // Center: Hero hand - FIXED MIN SIZE to always reserve space
        bottomSection.add(heroHandContainer).minSize(500, 140).expandX().center().padBottom(10);

        // Right: Control panel - FIXED WIDTH
        bottomSection.add(controlPanel).width(380).padRight(20).padBottom(10).right();

        hudTable.add(bottomSection).expandX().fillX().bottom();

        // Initially hide control panel (slide in when it's player's turn)
        controlPanel.setTransform(true);
        controlPanel.setOrigin(Align.right);
        hideControlPanel();
    }

    /**
     * Build Poker control buttons.
     */
    private void buildPokerControls() {
        pokerControls = new Table();

        // Bet slider row
        Table sliderRow = new Table();

        ImageButton minusBtn = createIconButton("btn_minus1", "btn_minus2");
        minusBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                adjustBet(-100);
            }
        });

        betSlider = new Slider(0, 10000, 100, false, skin);
        betSlider.setValue(0);
        betSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateBetLabel();
            }
        });

        ImageButton plusBtn = createIconButton("btn_plus1", "btn_plus2");
        plusBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                adjustBet(100);
            }
        });

        // Button size increased to 64x64 for better visibility
        sliderRow.add(minusBtn).size(50, 50).padRight(10);
        sliderRow.add(betSlider).width(180).padRight(10);
        sliderRow.add(plusBtn).size(50, 50);

        // Bet amount label
        Label.LabelStyle amountStyle = new Label.LabelStyle();
        amountStyle.font = skin.getFont("Blue_font");
        amountStyle.fontColor = new Color(1f, 0.84f, 0f, 1f); // Gold

        betAmountLabel = new Label("$0", amountStyle);
        betAmountLabel.setFontScale(1.1f);
        betAmountLabel.setAlignment(Align.center);

        // Action buttons row
        Table actionRow = new Table();

        ImageButton foldBtn = createIconButton("btn_cancel1", "btn_cancel2");
        foldBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onPokerAction("FOLD", 0);
            }
        });

        ImageButton checkBtn = createIconButton("btn_check1", "btn_check2");
        checkBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onPokerAction("CHECK", 0);
            }
        });

        ImageButton betBtn = createIconButton("btn_bet1", "btn_bet2");
        betBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                long amount = (long) betSlider.getValue();
                onPokerAction("RAISE", amount);
            }
        });

        // Labels under buttons
        Table foldSection = createLabeledButton(foldBtn, "FOLD");
        Table checkSection = createLabeledButton(checkBtn, "CHECK");
        Table betSection = createLabeledButton(betBtn, "BET");

        actionRow.add(foldSection).padRight(20);
        actionRow.add(checkSection).padRight(20);
        actionRow.add(betSection);

        // Combine
        pokerControls.add(sliderRow).row();
        pokerControls.add(betAmountLabel).padTop(5).padBottom(10).row();
        pokerControls.add(actionRow);

        controlPanel.add(pokerControls);
    }

    /**
     * Build Tien Len control buttons.
     */
    private void buildTienLenControls() {
        tienLenControls = new Table();

        ImageButton playBtn = createIconButton("btn_play1", "btn_play2");
        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onTienLenPlay();
            }
        });

        ImageButton skipBtn = createIconButton("btn_cancel1", "btn_cancel2");
        skipBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onTienLenSkip();
            }
        });

        Table playSection = createLabeledButton(playBtn, "PLAY");
        Table skipSection = createLabeledButton(skipBtn, "SKIP");

        tienLenControls.add(playSection).padRight(40);
        tienLenControls.add(skipSection);

        controlPanel.add(tienLenControls);
    }

    /**
     * Create an ImageButton from atlas regions.
     */
    private ImageButton createIconButton(String upRegion, String downRegion) {
        TextureAtlas.AtlasRegion up = atlas.findRegion(upRegion);
        TextureAtlas.AtlasRegion down = atlas.findRegion(downRegion);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        if (up != null) {
            style.imageUp = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(up);
        }
        if (down != null) {
            style.imageDown = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(down);
        }

        return new ImageButton(style);
    }

    /**
     * Create a button with label underneath.
     * Button size: 64x64 for better visibility.
     */
    private Table createLabeledButton(ImageButton button, String labelText) {
        Table container = new Table();
        // Removed background for clean look
        container.pad(8);

        // Larger button size (64x64)
        container.add(button).size(64, 64).row();

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("Blue_font");
        labelStyle.fontColor = Color.WHITE;

        Label label = new Label(labelText, labelStyle);
        label.setFontScale(0.8f);
        container.add(label).padTop(4);

        return container;
    }

    // ==================== PUBLIC API ====================

    /**
     * Initialize player seats using fixed positions from SeatConfig.
     * Local player (visualIndex=0) is NOT rendered as a seat - uses HUD instead.
     * 
     * @param playerIds   List of player IDs in order
     * @param playerNames Map of player ID to display name
     * @param balances    Map of player ID to balance
     */
    public void initializeSeats(List<Integer> playerIds, Map<Integer, String> playerNames,
            Map<Integer, Long> balances) {
        this.playerOrder = new ArrayList<>(playerIds);
        playerSeats.clear();

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Find local player index in server order
        int localIndex = playerIds.indexOf(localPlayerId);
        int playerCount = playerIds.size();

        Gdx.app.log(TAG, "initializeSeats: " + playerCount + " players, localIndex=" + localIndex);

        for (int i = 0; i < playerCount; i++) {
            int playerId = playerIds.get(i);
            boolean isLocal = (playerId == localPlayerId);

            String name = playerNames.getOrDefault(playerId, "Player " + playerId);
            long balance = balances.getOrDefault(playerId, 0L);

            PlayerSeatActor seat = new PlayerSeatActor(playerId, name, balance, isLocal);
            playerSeats.put(playerId, seat);

            // Calculate visual index: local player = 0, others = 1,2,3,4
            int visualIndex = SeatConfig.getVisualIndex(i, localIndex, playerCount);

            // Skip rendering seat 0 (local player uses HUD)
            if (SeatConfig.isUserSeat(visualIndex)) {
                Gdx.app.log(TAG, "Skipping seat render for local player " + playerId);
                continue;
            }

            // Get position from SeatConfig (scaled coordinates)
            float x = SeatConfig.getX(visualIndex, screenWidth) - PlayerSeatActor.WIDTH / 2f;
            float y = SeatConfig.getY(visualIndex, screenHeight) - PlayerSeatActor.HEIGHT / 2f;

            seat.setPosition(x, y);
            seat.setSize(PlayerSeatActor.WIDTH, PlayerSeatActor.HEIGHT);

            // Add to gameAreaGroup (NOT Table)
            gameAreaGroup.addActor(seat);

            // Show back cards for opponents
            seat.showBackCards(2);

            Gdx.app.log(TAG, "Added seat " + visualIndex + " for player " + playerId + " at (" + x + ", " + y + ")");
        }
    }

    /**
     * Deal cards to the hero (local player).
     */
    public void dealHeroCards(List<Card> cards) {
        heroCards.clear();
        heroHandContainer.clearChildren();

        float startDelay = 0f;
        float cardSpacing = CardActor.CARD_WIDTH * 0.6f;
        float totalWidth = cards.size() * cardSpacing;
        float startX = -totalWidth / 2f + CardActor.CARD_WIDTH / 2f;

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            CardActor cardActor = new CardActor(card, true);
            cardActor.setSelectable(gameType == GameType.TIENLEN);

            heroCards.add(cardActor);
            heroHandContainer.addActor(cardActor);

            // Animate deal
            float endX = startX + i * cardSpacing;
            float endY = HERO_HAND_Y;
            cardActor.animateDeal(deckX, deckY, endX, endY, startDelay);

            startDelay += 0.1f;
        }
    }

    /**
     * Add community card (Poker).
     */
    public void addCommunityCard(Card card, int position) {
        if (gameType != GameType.POKER || position < 0 || position >= 5)
            return;

        CardActor cardActor = new CardActor(card, true);
        communityCards.add(cardActor);

        // Add to center area with animation
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;
        float slotX = centerX - (2 - position) * (CardActor.CARD_WIDTH * 0.8f + 10);

        centerArea.addActor(cardActor);
        cardActor.animateDeal(deckX, deckY, slotX, centerY, 0);
    }

    /**
     * Show trick cards (Tien Len).
     */
    public void showTrickCards(List<Card> cards) {
        if (gameType != GameType.TIENLEN)
            return;

        // Clear previous trick
        for (CardActor actor : trickCards) {
            actor.remove();
        }
        trickCards.clear();

        // Display new trick
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;
        float spacing = CardActor.CARD_WIDTH * 0.4f;
        float startX = centerX - (cards.size() - 1) * spacing / 2f;

        for (int i = 0; i < cards.size(); i++) {
            CardActor cardActor = new CardActor(cards.get(i), true);
            trickCards.add(cardActor);
            centerArea.addActor(cardActor);
            cardActor.animateDeal(deckX, deckY, startX + i * spacing, centerY, i * 0.05f);
        }
    }

    /**
     * Update pot display (Poker).
     */
    public void updatePot(long amount) {
        if (potLabel != null) {
            potLabel.setText("POT: " + formatAmount(amount));
        }
    }

    /**
     * Set current turn.
     */
    public void setCurrentTurn(int playerId) {
        this.currentTurnPlayerId = playerId;
        this.isMyTurn = (playerId == localPlayerId);

        // DEBUG: Log turn state for sync debugging
        Gdx.app.log(TAG, "setCurrentTurn: playerId=" + playerId +
                " | localPlayerId=" + localPlayerId +
                " | isMyTurn=" + isMyTurn);

        // Update seat highlights
        for (Map.Entry<Integer, PlayerSeatActor> entry : playerSeats.entrySet()) {
            boolean isActive = (entry.getKey() == playerId);
            entry.getValue().setCurrentTurn(isActive);
            Gdx.app.log(TAG, "Seat " + entry.getKey() + " active: " + isActive);
        }

        // Show/hide control panel
        if (isMyTurn) {
            Gdx.app.log(TAG, "Showing control panel - MY TURN");
            showControlPanel();
        } else {
            hideControlPanel();
        }
    }

    /**
     * Mark player as folded (Poker).
     */
    public void setPlayerFolded(int playerId) {
        PlayerSeatActor seat = playerSeats.get(playerId);
        if (seat != null) {
            seat.setFolded(true);
        }
    }

    /**
     * Update player card count (for opponents).
     */
    public void updatePlayerCardCount(int playerId, int count) {
        PlayerSeatActor seat = playerSeats.get(playerId);
        if (seat != null) {
            seat.setCardCount(count);
        }
    }

    /**
     * Set dealer position.
     */
    public void setDealer(int playerId) {
        for (Map.Entry<Integer, PlayerSeatActor> entry : playerSeats.entrySet()) {
            entry.getValue().setDealer(entry.getKey() == playerId);
        }
    }

    // ==================== CONTROL PANEL ====================

    private void showControlPanel() {
        controlPanel.clearActions();
        controlPanel.addAction(Actions.sequence(
                Actions.moveTo(controlPanel.getX(), 0, 0.3f, Interpolation.swingOut)));
    }

    private void hideControlPanel() {
        controlPanel.clearActions();
        controlPanel.addAction(Actions.sequence(
                Actions.moveTo(controlPanel.getX(), -CONTROL_PANEL_HEIGHT, 0.3f, Interpolation.smooth)));
    }

    private void adjustBet(int delta) {
        float current = betSlider.getValue();
        betSlider.setValue(Math.max(0, Math.min(betSlider.getMaxValue(), current + delta)));
        updateBetLabel();
    }

    private void updateBetLabel() {
        if (betAmountLabel != null) {
            betAmountLabel.setText("$" + formatAmount((long) betSlider.getValue()));
        }
    }

    // ==================== ACTION CALLBACKS ====================

    /**
     * Called when a Poker action button is clicked.
     * Constructs and sends PlayerActionPacket to server.
     */
    protected void onPokerAction(String actionType, long amount) {
        // DEBUG: Log full turn state
        Gdx.app.log(TAG, "onPokerAction called: action=" + actionType +
                " | isMyTurn=" + isMyTurn +
                " | currentTurnPlayerId=" + currentTurnPlayerId +
                " | localPlayerId=" + localPlayerId);

        if (!isMyTurn) {
            Gdx.app.log(TAG, "Not my turn, ignoring action. Expected turn: " + currentTurnPlayerId);
            return;
        }

        Gdx.app.log(TAG, "Poker action: " + actionType + ", amount: " + amount);

        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setRoomId(roomId);
        packet.setPlayerId(localPlayerId);
        packet.setGameType(gameType);
        packet.setActionType(actionType);
        packet.setAmount(amount);

        if (networkService != null) {
            networkService.sendPacket(packet);
        }

        // Hide control panel after action
        hideControlPanel();
    }

    /**
     * Called when Tien Len PLAY is clicked.
     * Validates selection, constructs and sends PlayerActionPacket.
     */
    protected void onTienLenPlay() {
        if (!isMyTurn) {
            Gdx.app.log(TAG, "Not my turn, ignoring action");
            return;
        }

        // Collect selected cards
        selectedCards.clear();
        for (CardActor cardActor : heroCards) {
            if (cardActor.isSelected()) {
                selectedCards.add(cardActor.getCard());
            }
        }

        // Validation: check if cards selected
        if (selectedCards.isEmpty()) {
            showWarningToast("Select cards first!");
            return;
        }

        Gdx.app.log(TAG, "Playing " + selectedCards.size() + " cards");

        // Build packet with selected cards
        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setRoomId(roomId);
        packet.setPlayerId(localPlayerId);
        packet.setGameType(gameType);
        packet.setActionType("PLAY");
        packet.setCards(new ArrayList<>(selectedCards));

        if (networkService != null) {
            networkService.sendPacket(packet);
        }

        // Remove selected cards from hero hand (will be synced by server state)
        removePlayedCards();

        // Reset selection
        for (CardActor cardActor : heroCards) {
            cardActor.resetSelection();
        }

        // Hide control panel after action
        hideControlPanel();
    }

    /**
     * Called when Tien Len SKIP is clicked.
     * Sends SKIP action to server.
     */
    protected void onTienLenSkip() {
        if (!isMyTurn) {
            Gdx.app.log(TAG, "Not my turn, ignoring action");
            return;
        }

        Gdx.app.log(TAG, "Skipping turn");

        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setRoomId(roomId);
        packet.setPlayerId(localPlayerId);
        packet.setGameType(gameType);
        packet.setActionType("SKIP");

        if (networkService != null) {
            networkService.sendPacket(packet);
        }

        // Hide control panel after action
        hideControlPanel();
    }

    /**
     * Remove cards that were played from the hero hand.
     */
    private void removePlayedCards() {
        List<CardActor> toRemove = new ArrayList<>();
        for (CardActor cardActor : heroCards) {
            if (cardActor.isSelected()) {
                toRemove.add(cardActor);
            }
        }

        for (CardActor cardActor : toRemove) {
            heroCards.remove(cardActor);
            cardActor.remove();
        }

        // Rearrange remaining cards
        rearrangeHeroCards();
    }

    /**
     * Rearrange hero cards after some are removed.
     */
    private void rearrangeHeroCards() {
        if (heroCards.isEmpty())
            return;

        float cardSpacing = CardActor.CARD_WIDTH * 0.6f;
        float totalWidth = heroCards.size() * cardSpacing;
        float startX = -totalWidth / 2f + CardActor.CARD_WIDTH / 2f;

        for (int i = 0; i < heroCards.size(); i++) {
            CardActor card = heroCards.get(i);
            float targetX = startX + i * cardSpacing;
            card.addAction(Actions.moveTo(targetX, card.getY(), 0.2f, Interpolation.smooth));
        }
    }

    /**
     * Show a warning toast message.
     */
    private void showWarningToast(String message) {
        if (warningToast == null) {
            Label.LabelStyle style = new Label.LabelStyle();
            style.font = skin.getFont("Pink_font");
            style.fontColor = Color.WHITE;

            warningToast = new Label("", style);
            warningToast.setFontScale(0.9f);
            warningToast.setAlignment(Align.center);
            stage.addActor(warningToast);
        }

        // Position at center top
        warningToast.setText(message);
        warningToast.pack();
        warningToast.setPosition(
                (Gdx.graphics.getWidth() - warningToast.getWidth()) / 2f,
                Gdx.graphics.getHeight() - 100);

        // Fade in, wait, fade out
        warningToast.clearActions();
        warningToast.getColor().a = 0f;
        warningToast.addAction(Actions.sequence(
                Actions.fadeIn(0.2f),
                Actions.delay(1.5f),
                Actions.fadeOut(0.3f)));
    }

    // ==================== NETWORK RESPONSE HANDLING ====================

    /**
     * Handle incoming packet and update UI accordingly.
     * MUST be called from network listener - wraps all UI updates in postRunnable.
     * 
     * @param packet The received packet (GameStatePacket, PlayerTurnPacket, or
     *               GameEndPacket)
     */
    public void updateUIFromPacket(Object packet) {
        Gdx.app.postRunnable(() -> {
            if (packet instanceof GameStatePacket) {
                handleGameStatePacket((GameStatePacket) packet);
            } else if (packet instanceof PlayerTurnPacket) {
                handlePlayerTurnPacket((PlayerTurnPacket) packet);
            } else if (packet instanceof GameEndPacket) {
                handleGameEndPacket((GameEndPacket) packet);
            }
        });
    }

    /**
     * Handle GameStatePacket - sync all UI elements with server state.
     */
    private void handleGameStatePacket(GameStatePacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        Object state = packet.getGameState();

        if (gameType == GameType.POKER && state instanceof PokerGameState) {
            syncPokerState((PokerGameState) state);
        } else if (gameType == GameType.TIENLEN && state instanceof TienLenGameState) {
            syncTienLenState((TienLenGameState) state);
        }
    }

    /**
     * Sync UI with Poker game state.
     * STRICT SERVER-AUTHORITY: Clear all existing state before rebuilding from
     * packet.
     */
    private void syncPokerState(PokerGameState state) {
        if (state == null) {
            Gdx.app.log(TAG, "syncPokerState: received null state, ignoring");
            return;
        }

        // Update pot
        updatePot(state.getPot());

        // ==================== CLEAR-BEFORE-UPDATE: Community Cards
        // ====================
        // CRITICAL: Clear existing community cards to prevent duplicates
        for (CardActor existingCard : communityCards) {
            existingCard.remove();
        }
        communityCards.clear();

        // Rebuild community cards from server state
        List<Card> community = state.getCommunityCards();
        if (community != null) {
            for (int i = 0; i < community.size(); i++) {
                addCommunityCard(community.get(i), i);
            }
        }

        // ==================== Update Player Seats with Defensive Null Checks
        // ====================
        for (Map.Entry<Integer, PlayerSeatActor> entry : playerSeats.entrySet()) {
            int playerId = entry.getKey();
            PlayerSeatActor seat = entry.getValue();

            // Defensive check - player may have disconnected
            if (seat == null) {
                Gdx.app.log(TAG, "syncPokerState: seat for player " + playerId + " is null, skipping");
                continue;
            }

            // Update balance
            long chips = state.getPlayerChips(playerId);
            seat.updateBalance(chips);

            // Update folded status
            boolean folded = state.isPlayerFolded(playerId);
            seat.setFolded(folded);

            // Update card count (hole cards = 2)
            if (!seat.isLocalPlayer()) {
                seat.setCardCount(folded ? 0 : 2);
            }
        }

        // Update current turn
        setCurrentTurn(state.getCurrentPlayerTurn());
    }

    /**
     * Sync UI with Tien Len game state.
     * STRICT SERVER-AUTHORITY: Clear and rebuild trick display from server state.
     */
    private void syncTienLenState(TienLenGameState state) {
        if (state == null) {
            Gdx.app.log(TAG, "syncTienLenState: received null state, ignoring");
            return;
        }

        // ==================== CLEAR-BEFORE-UPDATE: Trick Cards ====================
        // Always sync trick display - showTrickCardsWithRotation handles the clearing
        // internally
        List<Card> trick = state.getCurrentTrick();
        if (trick != null && !trick.isEmpty()) {
            showTrickCardsWithRotation(trick);
        } else {
            // Clear trick cards when trick is empty (new round)
            for (CardActor actor : trickCards) {
                actor.remove();
            }
            trickCards.clear();
        }

        // ==================== Update Player Card Counts with Defensive Null Checks
        // ====================
        for (Map.Entry<Integer, PlayerSeatActor> entry : playerSeats.entrySet()) {
            int playerId = entry.getKey();
            PlayerSeatActor seat = entry.getValue();

            // Defensive check - player may have disconnected
            if (seat == null) {
                Gdx.app.log(TAG, "syncTienLenState: seat for player " + playerId + " is null, skipping");
                continue;
            }

            if (!seat.isLocalPlayer()) {
                List<Card> hand = state.getPlayerHand(playerId);
                seat.setCardCount(hand != null ? hand.size() : 0);
            }

            // Check if player has finished (won)
            boolean finished = state.isPlayerFinished(playerId);
            if (finished && !seat.isLocalPlayer()) {
                seat.setCardCount(0); // Show no cards for finished player
            }
        }

        // Update current turn
        setCurrentTurn(state.getCurrentPlayerId());
    }

    /**
     * Handle PlayerTurnPacket - highlight active player and show/hide controls.
     */
    private void handlePlayerTurnPacket(PlayerTurnPacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        setCurrentTurn(packet.getCurrentPlayerId());
    }

    /**
     * Handle GameEndPacket - show winner and results.
     */
    private void handleGameEndPacket(GameEndPacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        int winnerId = packet.getWinnerId();
        Gdx.app.log(TAG, "Game ended! Winner: " + winnerId);

        // Highlight winner seat
        for (Map.Entry<Integer, PlayerSeatActor> entry : playerSeats.entrySet()) {
            if (entry.getKey() == winnerId) {
                entry.getValue().setCurrentTurn(true);
            } else {
                entry.getValue().setCurrentTurn(false);
            }
        }

        // Show winner message
        String winMsg = (winnerId == localPlayerId) ? "YOU WIN!" : "Player " + winnerId + " wins!";
        showWarningToast(winMsg);
    }

    // ==================== CARD ANIMATION ====================

    /**
     * Animate cards being played from a player to the center table.
     * 
     * @param playerId The player who played the cards
     * @param cards    The cards being played
     */
    public void playCardAnimation(int playerId, List<Card> cards) {
        if (cards == null || cards.isEmpty())
            return;

        // Find source position
        float sourceX, sourceY;
        PlayerSeatActor seat = playerSeats.get(playerId);

        if (playerId == localPlayerId) {
            // From hero hand
            sourceX = Gdx.graphics.getWidth() / 2f;
            sourceY = HERO_HAND_Y + 50;
        } else if (seat != null) {
            // From opponent seat
            sourceX = seat.getX() + seat.getWidth() / 2f;
            sourceY = seat.getY() + seat.getHeight() / 2f;
        } else {
            // Fallback to center
            sourceX = Gdx.graphics.getWidth() / 2f;
            sourceY = Gdx.graphics.getHeight() / 2f;
        }

        // Destination: center of table
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        // Calculate card positions
        float spacing = CardActor.CARD_WIDTH * 0.4f;
        float startX = centerX - (cards.size() - 1) * spacing / 2f;

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            CardActor cardActor = new CardActor(card, true);
            cardActor.setPosition(sourceX, sourceY);
            cardActor.setScale(0.8f);

            // Random rotation for natural look (-15 to +15 degrees)
            float randomAngle = (float) (Math.random() * 30 - 15);

            float destX = startX + i * spacing;
            float destY = centerY;

            // Animation sequence
            cardActor.addAction(Actions.sequence(
                    Actions.delay(i * 0.05f),
                    Actions.parallel(
                            Actions.moveTo(destX, destY, 0.4f, Interpolation.pow2Out),
                            Actions.scaleTo(0.9f, 0.9f, 0.3f),
                            Actions.rotateTo(randomAngle, 0.4f))));

            // Add to center area (leave on table)
            centerArea.addActor(cardActor);

            if (gameType == GameType.TIENLEN) {
                trickCards.add(cardActor);
            }
        }
    }

    /**
     * Show trick cards with random rotation (Tien Len).
     */
    private void showTrickCardsWithRotation(List<Card> cards) {
        // Clear previous trick
        for (CardActor actor : trickCards) {
            actor.remove();
        }
        trickCards.clear();

        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;
        float spacing = CardActor.CARD_WIDTH * 0.4f;
        float startX = centerX - (cards.size() - 1) * spacing / 2f;

        for (int i = 0; i < cards.size(); i++) {
            CardActor cardActor = new CardActor(cards.get(i), true);

            // Random rotation (-15 to +15 degrees)
            float randomAngle = (float) (Math.random() * 30 - 15);
            cardActor.setRotation(randomAngle);

            float x = startX + i * spacing;
            cardActor.setPosition(x - CardActor.CARD_WIDTH / 2, centerY - CardActor.CARD_HEIGHT / 2);

            trickCards.add(cardActor);
            centerArea.addActor(cardActor);
        }
    }

    /**
     * Get currently selected cards (for Tien Len).
     */
    public List<Card> getSelectedCards() {
        selectedCards.clear();
        for (CardActor cardActor : heroCards) {
            if (cardActor.isSelected()) {
                selectedCards.add(cardActor.getCard());
            }
        }
        return new ArrayList<>(selectedCards);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Animate dealing a card from deck to destination.
     */
    public void animateCardDeal(float startX, float startY, float endX, float endY,
            Card card, boolean faceUp, float delay) {
        CardActor cardActor = new CardActor(card, faceUp);
        stage.addActor(cardActor);
        cardActor.animateDeal(startX, startY, endX, endY, delay);
    }

    private String formatAmount(long amount) {
        if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }

    // ==================== SCREEN LIFECYCLE ====================

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }

    // ==================== GETTERS ====================

    public Stage getStage() {
        return stage;
    }

    public GameType getGameType() {
        return gameType;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }
}
