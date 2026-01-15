package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.client.service.NetworkService;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.tienlen.TienLenGameState;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.GameStartPacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;
import com.mygame.shared.network.packets.game.PlayerTurnPacket;
import com.mygame.shared.network.packets.game.GameEndPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mygame.shared.model.RoomInfo;

/**
 * TienLenGameScreen - Tien Len (Vietnamese Poker) game screen.
 * 
 * Uses TienLenGameLayout for the main UI structure matching the ASCII
 * blueprint.
 */
public class TienLenGameScreen implements Screen {

    private static final String TAG = "TienLenGameScreen";
    private static final String BACKGROUND_PATH = "ui/Background_poker.png";

    // Stage and UI
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private Image backgroundImage;

    // Main layout
    private Stack rootStack;
    private TienLenGameLayout gameLayout;

    // Game state
    private int localPlayerId;
    private String localUsername;
    private int roomId;
    private int currentTurnPlayerId;
    private boolean isMyTurn;

    // Player mapping: playerId -> visualIndex (1=left, 2=top, 3=right)
    private Map<Integer, Integer> playerVisualMap;
    private List<Integer> playerOrder;

    // Network
    private NetworkService networkService;

    // RoomInfo for player usernames
    private RoomInfo roomInfo;

    // Warning toast
    private Label warningToast;

    /**
     * Create TienLenGameScreen.
     *
     * @param localPlayerId  Local player's user ID
     * @param localUsername  Local player's username
     * @param roomId         Room ID for packet construction
     * @param roomInfo       Room info containing player data
     * @param networkService Network service for sending packets
     */
    public TienLenGameScreen(int localPlayerId, String localUsername, int roomId, RoomInfo roomInfo,
            NetworkService networkService) {
        this.localPlayerId = localPlayerId;
        this.localUsername = localUsername != null ? localUsername : "Player";
        this.roomId = roomId;
        this.roomInfo = roomInfo;
        this.networkService = networkService;
        this.playerVisualMap = new HashMap<>();
        this.playerOrder = new ArrayList<>();
        this.isMyTurn = false;
    }

    /**
     * Get username for a player from RoomInfo.
     */
    private String getUsernameForPlayer(int playerId) {
        if (playerId == localPlayerId) {
            return localUsername;
        }
        if (roomInfo != null && roomInfo.getPlayers() != null) {
            for (RoomInfo.RoomPlayerInfo player : roomInfo.getPlayers()) {
                if (player.getUserId() == playerId) {
                    return player.getUsername();
                }
            }
        }
        return "Player " + playerId;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage); // CRITICAL: Enable input handling

        skin = UISkinManager.getInstance().getSkin();

        buildUI();

        // Initialize seats immediately from RoomInfo
        initializeSeatsFromRoomInfo();

        Gdx.app.log(TAG, "TienLenGameScreen shown. InputProcessor set.");
    }

    /**
     * Initialize seats from RoomInfo data immediately on show.
     */
    private void initializeSeatsFromRoomInfo() {
        if (roomInfo == null || roomInfo.getPlayers() == null) {
            Gdx.app.log(TAG, "No RoomInfo available for initial seat setup");
            return;
        }

        List<Integer> playerIds = new ArrayList<>();
        Map<Integer, String> names = new HashMap<>();
        Map<Integer, Long> balances = new HashMap<>();

        for (RoomInfo.RoomPlayerInfo player : roomInfo.getPlayers()) {
            int pid = player.getUserId();
            playerIds.add(pid);
            names.put(pid, player.getUsername());
            balances.put(pid, player.getBalance());
        }

        if (!playerIds.isEmpty()) {
            initializeSeats(playerIds, names, balances);
            Gdx.app.log(TAG, "Initialized " + playerIds.size() + " seats from RoomInfo");
        }
    }

    /**
     * Build the complete UI structure.
     */
    private void buildUI() {
        // Root stack for layering
        rootStack = new Stack();
        rootStack.setFillParent(true);

        // Layer 0: Background
        loadBackground();
        if (backgroundTexture != null) {
            backgroundImage = new Image(backgroundTexture);
            backgroundImage.setFillParent(true);
            rootStack.add(backgroundImage);
        }

        // Layer 1: TienLenGameLayout (main UI)
        gameLayout = new TienLenGameLayout();
        rootStack.add(gameLayout);

        // Setup button callbacks
        setupControlCallbacks();

        stage.addActor(rootStack);

        Gdx.app.log(TAG, "UI built with TienLenGameLayout");
    }

    /**
     * Load background texture.
     */
    private void loadBackground() {
        try {
            backgroundTexture = new Texture(Gdx.files.internal(BACKGROUND_PATH));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to load background: " + BACKGROUND_PATH, e);
        }
    }

    /**
     * Setup control panel button callbacks.
     */
    private void setupControlCallbacks() {
        gameLayout.setOnPlayAction(this::onPlayCards);
        gameLayout.setOnSkipAction(this::onSkip);
        // Sort and Unselect have default implementations in layout
    }

    // ==================== PUBLIC API ====================

    /**
     * Initialize player seats.
     *
     * @param playerIds   List of player IDs in server order
     * @param playerNames Map of player ID to display name
     * @param balances    Map of player ID to balance
     */
    public void initializeSeats(List<Integer> playerIds, Map<Integer, String> playerNames,
            Map<Integer, Long> balances) {
        this.playerOrder = new ArrayList<>(playerIds);
        playerVisualMap.clear();

        int localIndex = playerIds.indexOf(localPlayerId);
        int playerCount = playerIds.size();

        Gdx.app.log(TAG, "Initializing " + playerCount + " seats, localIndex=" + localIndex);

        // Tien Len: 4 players
        // Local player = bottom (0), others = 1 (left), 2 (top), 3 (right)
        for (int i = 0; i < playerCount; i++) {
            int playerId = playerIds.get(i);

            // Calculate visual position relative to local player
            int visualIndex = (i - localIndex + playerCount) % playerCount;
            playerVisualMap.put(playerId, visualIndex);

            String name = getUsernameForPlayer(playerId);
            long balance = balances.getOrDefault(playerId, 0L);

            if (visualIndex == 0) {
                // Local player (me) - use actual username
                gameLayout.updateMyInfo(localUsername, balance);
            } else {
                // Rival - update widget
                gameLayout.updateRival(visualIndex, playerId, name, balance, 13);
            }

            Gdx.app.log(TAG, "Visual " + visualIndex + " -> Player " + playerId + " (" + name + ")");
        }
    }

    /**
     * Deal cards to the local player.
     */
    public void dealMyCards(List<Card> cards) {
        gameLayout.setMyCards(cards);
    }

    /**
     * Set current turn.
     */
    public void setCurrentTurn(int playerId) {
        this.currentTurnPlayerId = playerId;
        this.isMyTurn = (playerId == localPlayerId);

        Gdx.app.log(TAG, "setCurrentTurn: " + playerId + ", isMyTurn=" + isMyTurn);

        // Highlight active player
        gameLayout.setActivePlayer(playerId);

        // Highlight MY INFO if it is my turn
        gameLayout.setMyActive(isMyTurn);

        // Enable/disable buttons based on turn
        gameLayout.setButtonsEnabled(isMyTurn);

        if (isMyTurn) {
            showWarningToast("Your Turn!");
        }
    }

    /**
     * Update a rival's card count.
     */
    public void updateRivalCardCount(int playerId, int cardCount) {
        TienLenPlayerWidget widget = gameLayout.getRivalWidget(playerId);
        if (widget != null) {
            widget.updateCardCount(cardCount);
        }
    }

    /**
     * Show cards played by a player.
     */
    public void showPlayedCards(int playerId, List<Card> cards) {
        String playerName = "Player " + playerId;
        if (playerId == localPlayerId) {
            playerName = "You";
        }
        gameLayout.showPlayedCards(cards, playerName);
    }

    /**
     * Clear the center pile (new round).
     */
    public void clearPlayedCards() {
        gameLayout.clearCenterPile();
    }

    // ==================== ACTIONS ====================

    /**
     * Called when PLAY button is clicked.
     */
    private void onPlayCards() {
        Gdx.app.log(TAG, "onPlayCards clicked. isMyTurn=" + isMyTurn + ", currentTurn=" + currentTurnPlayerId);

        if (!isMyTurn) {
            showWarningToast("Not your turn!");
            return;
        }

        List<Card> selected = gameLayout.getSelectedCards();
        if (selected.isEmpty()) {
            showWarningToast("Select cards first!");
            return;
        }

        // Validate combination
        com.mygame.shared.game.tienlen.TienLenCombinationType type = com.mygame.shared.game.tienlen.CardCollection
                .detectCombination(selected);

        if (type == com.mygame.shared.game.tienlen.TienLenCombinationType.INVALID) {
            showWarningToast("Invalid Combination!");
            return;
        }

        Gdx.app.log(TAG, "Playing " + selected.size() + " cards");

        // Send packet
        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setRoomId(roomId);
        packet.setPlayerId(localPlayerId);
        packet.setGameType(GameType.TIENLEN);
        packet.setActionType("PLAY");
        packet.setCards(new ArrayList<>(selected));

        if (networkService != null) {
            networkService.sendPacket(packet);
        }

        // Remove cards from hand (will be confirmed by server)
        gameLayout.removeSelectedCards();
    }

    /**
     * Called when SKIP button is clicked.
     */
    private void onSkip() {
        if (!isMyTurn) {
            showWarningToast("Not your turn!");
            return;
        }

        Gdx.app.log(TAG, "Skipping turn");

        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setRoomId(roomId);
        packet.setPlayerId(localPlayerId);
        packet.setGameType(GameType.TIENLEN);
        packet.setActionType("SKIP");

        if (networkService != null) {
            networkService.sendPacket(packet);
        }
    }

    // ==================== NETWORK ====================

    /**
     * Handle incoming packet and update UI.
     */
    public void updateUIFromPacket(Object packet) {
        Gdx.app.postRunnable(() -> {
            if (packet instanceof GameStartPacket) {
                handleGameStartPacket((GameStartPacket) packet);
            } else if (packet instanceof GameStatePacket) {
                handleGameStatePacket((GameStatePacket) packet);
            } else if (packet instanceof PlayerTurnPacket) {
                handlePlayerTurnPacket((PlayerTurnPacket) packet);
            } else if (packet instanceof GameEndPacket) {
                handleGameEndPacket((GameEndPacket) packet);
            }
        });
    }

    private void handleGameStartPacket(GameStartPacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        Gdx.app.log(TAG, "Received GameStartPacket");

        java.util.List<Integer> playerOrder = packet.getPlayerOrder();
        if (playerOrder == null || playerOrder.isEmpty()) {
            Gdx.app.error(TAG, "GameStartPacket has no player order");
            return;
        }

        // Build names and balances from initial state or use defaults
        Map<Integer, String> names = new HashMap<>();
        Map<Integer, Long> balances = new HashMap<>();

        // Extract from TienLenGameState if available
        Object state = packet.getInitialState();
        if (state instanceof TienLenGameState) {
            TienLenGameState tienLenState = (TienLenGameState) state;
            for (int pid : playerOrder) {
                // Get name from RoomInfo if possible
                String name = "Player " + pid;
                long balance = tienLenState.getPlayerCredits(pid); // Read correct balance

                if (roomInfo != null && roomInfo.getPlayers() != null) {
                    for (com.mygame.shared.model.RoomInfo.RoomPlayerInfo p : roomInfo.getPlayers()) {
                        if (p.getUserId() == pid) {
                            name = p.getUsername();
                            // If GameState has 0 (not synced?), fallback to RoomInfo
                            if (balance == 0 && p.getBalance() > 0) {
                                balance = p.getBalance();
                            }
                            break;
                        }
                    }
                }

                names.put(pid, name);
                balances.put(pid, balance);
            }
            // Deal cards to local player
            List<Card> myCards = tienLenState.getPlayerHand(localPlayerId);
            if (myCards != null && !myCards.isEmpty()) {
                dealMyCards(myCards);
            }

            // CHECKPOINT: Sync initial state to set turn immediately
            syncTienLenState(tienLenState);

        } else {
            for (int pid : playerOrder) {
                names.put(pid, "Player " + pid);
                balances.put(pid, 1000L);
            }
        }

        initializeSeats(playerOrder, names, balances);
    }

    private void handleGameStatePacket(GameStatePacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        Object state = packet.getGameState();
        if (state instanceof TienLenGameState) {
            syncTienLenState((TienLenGameState) state);
        }
    }

    private void syncTienLenState(TienLenGameState state) {
        if (state == null)
            return;

        // Log current state
        Gdx.app.log(TAG, "Syncing state. Turn: " + state.getCurrentPlayerId());

        // Update current trick display
        List<Card> trick = state.getCurrentTrick();
        if (trick != null && !trick.isEmpty()) {
            showPlayedCards(state.getLastPlayedPlayer(), trick);
        } else {
            clearPlayedCards();
        }

        // CHECKPOINT: Sync my own hand (Fixes "disappearing cards" on rejected move)
        List<Card> myHand = state.getPlayerHand(localPlayerId);
        if (myHand != null) {
            gameLayout.setMyCards(myHand);
        }

        // Update rival card counts
        for (Map.Entry<Integer, Integer> entry : playerVisualMap.entrySet()) {
            int playerId = entry.getKey();
            int visualIndex = entry.getValue();

            if (visualIndex != 0) { // Not local player
                List<Card> hand = state.getPlayerHand(playerId);
                int cardCount = hand != null ? hand.size() : 0;
                updateRivalCardCount(playerId, cardCount);

                // Check if finished
                if (state.isPlayerFinished(playerId)) {
                    TienLenPlayerWidget widget = gameLayout.getRivalWidget(playerId);
                    if (widget != null) {
                        widget.setFinished();
                    }
                }
            }
        }

        // Update current turn
        setCurrentTurn(state.getCurrentPlayerId());
    }

    private void handlePlayerTurnPacket(PlayerTurnPacket packet) {
        if (packet.getRoomId() != roomId)
            return;
        setCurrentTurn(packet.getCurrentPlayerId());
    }

    private void handleGameEndPacket(GameEndPacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        int winnerId = packet.getWinnerId();
        Gdx.app.log(TAG, "Game ended! Winner: " + winnerId);

        String winMsg = (winnerId == localPlayerId) ? "YOU WIN!" : "Player " + winnerId + " wins!";
        showWarningToast(winMsg);
    }

    // ==================== TOAST ====================

    private void showWarningToast(String message) {
        if (warningToast == null) {
            Label.LabelStyle style = new Label.LabelStyle();
            style.font = skin.getFont("Pink_font");
            style.fontColor = Color.WHITE;

            warningToast = new Label("", style);
            warningToast.setFontScale(1.0f);
            warningToast.setAlignment(Align.center);
            stage.addActor(warningToast);
        }

        warningToast.setText(message);
        warningToast.pack();
        warningToast.setPosition(
                (Gdx.graphics.getWidth() - warningToast.getWidth()) / 2f,
                Gdx.graphics.getHeight() - 100);

        warningToast.clearActions();
        warningToast.getColor().a = 0f;
        warningToast.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.2f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(1.5f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.3f)));
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

        // Update hand width on resize
        if (gameLayout != null && gameLayout.getMyHandWidget() != null) {
            gameLayout.getMyHandWidget().setHandWidth(width * 0.95f);
        }
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

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    /**
     * Disable debug mode on the layout.
     */
    public void disableDebugMode() {
        if (gameLayout != null) {
            gameLayout.disableDebug();
        }
    }
}
