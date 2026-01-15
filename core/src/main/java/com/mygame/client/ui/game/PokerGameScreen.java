package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygame.client.service.NetworkService;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.poker.PokerGameState;
import com.mygame.shared.model.GameType;
import com.mygame.shared.network.packets.game.GameStatePacket;
import com.mygame.shared.network.packets.game.GameStartPacket;
import com.mygame.shared.network.packets.game.PlayerActionPacket;
import com.mygame.shared.network.packets.game.PlayerTurnPacket;
import com.mygame.shared.network.packets.game.GameEndPacket;
import com.mygame.shared.model.RoomInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mygame.shared.model.RoomInfo;

/**
 * PokerGameScreen - Poker-specific game screen using Table-based layout.
 * 
 * Uses PokerTableLayout for the main UI structure matching the ASCII blueprint:
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ [ USER 2 ] [ USER 3 ] │
 * │ [ USER 1 ] [ USER 4 ] │
 * │ [ COMMUNITY CARDS ] │
 * │ ( POT ) │
 * │ [ DECK ] [ MAIN USER ] [ CONTROLS ] │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 */
public class PokerGameScreen implements Screen {

    private static final String TAG = "PokerGameScreen";
    private static final String BACKGROUND_PATH = "ui/Background_poker.png";

    // Stage and UI
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private Image backgroundImage;

    // Main layout
    private Stack rootStack;
    private PokerTableLayout tableLayout;

    // Game state
    private int localPlayerId;
    private String localUsername;
    private int roomId;
    private int currentTurnPlayerId;
    private boolean isMyTurn;

    // Player mapping: playerId -> visualIndex
    private Map<Integer, Integer> playerVisualMap;
    private List<Integer> playerOrder;

    // Network
    private NetworkService networkService;

    // RoomInfo for player usernames
    private RoomInfo roomInfo;

    // Hero cards
    private List<Card> heroCards;

    // Warning toast
    private Label warningToast;

    /**
     * Create PokerGameScreen.
     *
     * @param localPlayerId  Local player's user ID
     * @param localUsername  Local player's username
     * @param roomId         Room ID for packet construction
     * @param roomInfo       Room info containing player data
     * @param networkService Network service for sending packets
     */
    public PokerGameScreen(int localPlayerId, String localUsername, int roomId, RoomInfo roomInfo,
            NetworkService networkService) {
        this.localPlayerId = localPlayerId;
        this.localUsername = localUsername != null ? localUsername : "Player";
        this.roomId = roomId;
        this.roomInfo = roomInfo;
        this.networkService = networkService;
        this.playerVisualMap = new HashMap<>();
        this.playerOrder = new ArrayList<>();
        this.heroCards = new ArrayList<>();
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

    /**
     * Get balance for a player from RoomInfo.
     */
    private long getBalanceForPlayer(int playerId) {
        if (roomInfo != null && roomInfo.getPlayers() != null) {
            for (RoomInfo.RoomPlayerInfo player : roomInfo.getPlayers()) {
                if (player.getUserId() == playerId) {
                    return player.getBalance();
                }
            }
        }
        return 0L;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage); // CRITICAL: Enable input handling

        skin = UISkinManager.getInstance().getSkin();

        buildUI();

        // Initialize seats immediately from RoomInfo
        initializeSeatsFromRoomInfo();

        Gdx.app.log(TAG, "PokerGameScreen shown. InputProcessor set.");
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

        // Layer 1: PokerTableLayout (main UI)
        tableLayout = new PokerTableLayout();
        rootStack.add(tableLayout);

        // Setup button callbacks
        setupControlCallbacks();

        stage.addActor(rootStack);

        Gdx.app.log(TAG, "UI built with PokerTableLayout");
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
        tableLayout.setOnCheckAction(() -> sendPokerAction("CHECK", 0));
        tableLayout.setOnCallAction(() -> sendPokerAction("CALL", 0));
        tableLayout.setOnFoldAction(() -> sendPokerAction("FOLD", 0));
        tableLayout.setOnRaiseAction(amount -> sendPokerAction("RAISE", amount));
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

        for (int i = 0; i < playerCount; i++) {
            int playerId = playerIds.get(i);
            boolean isLocal = (playerId == localPlayerId);

            // Calculate visual index: local = 0, others = 1,2,3,4
            int visualIndex = (i - localIndex + playerCount) % playerCount;
            playerVisualMap.put(playerId, visualIndex);

            String name = getUsernameForPlayer(playerId);
            long balance = balances.getOrDefault(playerId, 0L);

            // Update seat in layout - use real username for local player
            String displayName = isLocal ? localUsername : name;
            tableLayout.updateSeat(visualIndex, playerId, displayName, balance, isLocal);

            // Show back cards for opponents
            if (!isLocal) {
                PlayerSeatWidget seat = tableLayout.getSeat(visualIndex);
                if (seat != null) {
                    seat.showBackCards();
                }
            }

            Gdx.app.log(TAG, "Seat " + visualIndex + " -> Player " + playerId + " (" + name + ")");
        }

        // Mark empty seats for Poker (supports 3-5 players)
        // Visual positions: 0=me, 1,2,3,4 = other seats
        for (int v = 1; v <= 4; v++) {
            boolean hasPlayer = false;
            for (int vIdx : playerVisualMap.values()) {
                if (vIdx == v) {
                    hasPlayer = true;
                    break;
                }
            }
            if (!hasPlayer) {
                PlayerSeatWidget seat = tableLayout.getSeat(v);
                if (seat != null) {
                    seat.setEmpty(true);
                    Gdx.app.log(TAG, "Seat " + v + " marked as empty");
                }
            }
        }
    }

    /**
     * Deal cards to the local player.
     */
    public void dealHeroCards(List<Card> cards) {
        this.heroCards = new ArrayList<>(cards);

        PlayerSeatWidget mainSeat = tableLayout.getMainUserSeat();
        if (mainSeat != null) {
            mainSeat.updateCards(cards);
        }
    }

    /**
     * Update pot display.
     */
    public void updatePot(long amount) {
        tableLayout.updatePot(amount);
    }

    /**
     * Add community card.
     */
    public void addCommunityCard(Card card, int position) {
        tableLayout.setCommunityCard(position, card);
    }

    /**
     * Set current turn.
     */
    public void setCurrentTurn(int playerId) {
        this.currentTurnPlayerId = playerId;
        this.isMyTurn = (playerId == localPlayerId);

        Gdx.app.log(TAG, "Turn Check: Me=" + localPlayerId + ", Current=" + playerId + " -> " + isMyTurn);

        // Update seat highlights
        for (Map.Entry<Integer, Integer> entry : playerVisualMap.entrySet()) {
            int pid = entry.getKey();
            int visualIndex = entry.getValue();
            PlayerSeatWidget seat = tableLayout.getSeat(visualIndex);
            if (seat != null) {
                seat.setCurrentTurn(pid == playerId);
            }
        }

        // Show/hide controls
        tableLayout.setControlsVisible(isMyTurn);
    }

    /**
     * Mark player as folded.
     */
    public void setPlayerFolded(int playerId) {
        Integer visualIndex = playerVisualMap.get(playerId);
        if (visualIndex != null) {
            PlayerSeatWidget seat = tableLayout.getSeat(visualIndex);
            if (seat != null) {
                seat.setFolded(true);
            }
        }
    }

    /**
     * Update player balance.
     */
    public void updatePlayerBalance(int playerId, long balance) {
        Integer visualIndex = playerVisualMap.get(playerId);
        if (visualIndex != null) {
            PlayerSeatWidget seat = tableLayout.getSeat(visualIndex);
            if (seat != null) {
                seat.updateCredits(balance);
            }
        }
    }

    // ==================== NETWORK ====================

    /**
     * Send a poker action to the server.
     */
    private void sendPokerAction(String actionType, long amount) {
        if (!isMyTurn) {
            Gdx.app.log(TAG, "Not my turn, ignoring action: " + actionType);
            showWarningToast("Not your turn!");
            return;
        }

        Gdx.app.log(TAG, "Sending action: " + actionType + ", amount: " + amount);

        PlayerActionPacket packet = new PlayerActionPacket();
        packet.setRoomId(roomId);
        packet.setPlayerId(localPlayerId);
        packet.setGameType(GameType.POKER);
        packet.setActionType(actionType);
        packet.setAmount(amount);

        if (networkService != null) {
            networkService.sendPacket(packet);
        }

        // Hide controls after action
        tableLayout.setControlsVisible(false);
    }

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

        // Extract from PokerGameState if available
        Object state = packet.getInitialState();
        if (state instanceof PokerGameState) {
            PokerGameState pokerState = (PokerGameState) state;
            for (int pid : playerOrder) {
                names.put(pid, getUsernameForPlayer(pid));
                balances.put(pid, pokerState.getPlayerChips(pid));
            }
        } else {
            for (int pid : playerOrder) {
                names.put(pid, getUsernameForPlayer(pid));
                balances.put(pid, getBalanceForPlayer(pid));
            }
        }

        initializeSeats(playerOrder, names, balances);

        // Deal cards to hero if available in state
        if (state instanceof PokerGameState) {
            PokerGameState pokerState = (PokerGameState) state;
            List<Card> myCards = pokerState.getPlayerHole(localPlayerId);
            if (myCards != null && !myCards.isEmpty()) {
                dealHeroCards(myCards);
                Gdx.app.log(TAG, "Dealt " + myCards.size() + " cards to hero");
            } else {
                Gdx.app.log(TAG, "No cards found for hero in GameStartPacket");
            }

            // Sync initial state (pot, dealer, turn)
            syncPokerState(pokerState);
        }
    }

    private void handleGameStatePacket(GameStatePacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        Object state = packet.getGameState();
        if (state instanceof PokerGameState) {
            syncPokerState((PokerGameState) state);
        }
    }

    private void syncPokerState(PokerGameState state) {
        if (state == null)
            return;

        // Update pot
        updatePot(state.getPot());

        // Clear and rebuild community cards
        tableLayout.clearCommunityCards();
        List<Card> community = state.getCommunityCards();
        if (community != null) {
            for (int i = 0; i < community.size(); i++) {
                addCommunityCard(community.get(i), i);
            }
        }

        // Update player seats
        for (Map.Entry<Integer, Integer> entry : playerVisualMap.entrySet()) {
            int playerId = entry.getKey();
            int visualIndex = entry.getValue();
            PlayerSeatWidget seat = tableLayout.getSeat(visualIndex);

            if (seat == null)
                continue;

            // Update balance
            long chips = state.getPlayerChips(playerId);
            seat.updateCredits(chips);

            // Update current round bet
            long currentBet = state.getPlayerBet(playerId);
            seat.updateCurrentBet(currentBet);

            // Update folded status
            boolean folded = state.isPlayerFolded(playerId);
            seat.setFolded(folded);
        }

        // Update control panel if it's my turn
        if (state.getCurrentPlayerTurn() == localPlayerId) {
            updateControlPanel(state);
        } else {
            // Ensure controls are hidden if not my turn (redundant safety)
            tableLayout.setControlsVisible(false);
        }

        // Update current turn
        setCurrentTurn(state.getCurrentPlayerTurn());
    }

    private void handlePlayerTurnPacket(PlayerTurnPacket packet) {
        if (packet.getRoomId() != roomId)
            return;
        setCurrentTurn(packet.getCurrentPlayerId());
    }

    private void updateControlPanel(PokerGameState state) {
        long myBet = state.getPlayerBet(localPlayerId);
        long currentTableBet = state.getCurrentBet();
        long toCall = currentTableBet - myBet;
        long myChips = state.getPlayerChips(localPlayerId);

        // Smart Check/Call Logic
        if (toCall <= 0) {
            // Can Check
            tableLayout.setCheckButtonVisible(true);
            tableLayout.setCallButtonVisible(false); // No need to call 0
        } else {
            // Must Call
            tableLayout.setCheckButtonVisible(false);
            tableLayout.setCallButtonVisible(true, toCall);
        }

        // Raise Slider Logic
        long minRaise = state.getLastRaiseAmount();
        long maxRaise = myChips - Math.max(0, toCall); // Remaining chips after call

        if (maxRaise < minRaise) {
            // Can only All-in (raise rest) or Fold/Call
            // For simplicity, disable raise or set min=max
            if (maxRaise > 0) {
                tableLayout.setSliderRange(maxRaise, maxRaise);
            } else {
                tableLayout.setSliderRange(0, 0); // Cannot raise
            }
        } else {
            tableLayout.setSliderRange(minRaise, maxRaise);
        }

        tableLayout.setControlsVisible(true);
    }

    private void handleGameEndPacket(GameEndPacket packet) {
        if (packet.getRoomId() != roomId)
            return;

        int winnerId = packet.getWinnerId();
        Gdx.app.log(TAG, "Game ended! Winner: " + winnerId);

        String winMsg = (winnerId == localPlayerId) ? "YOU WIN!" : getUsernameForPlayer(winnerId) + " wins!";
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
     * Disable debug mode on the layout (call after verifying layout works).
     */
    public void disableDebugMode() {
        if (tableLayout != null) {
            tableLayout.disableDebug();
        }
    }
}
