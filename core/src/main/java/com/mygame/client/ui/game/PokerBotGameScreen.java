package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.RoyalFlushG;
import com.mygame.client.ai.PokerBot;
import com.mygame.client.controller.LobbyController;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.poker.PokerGameState;
import com.mygame.shared.game.poker.PokerHandEvaluator;
import com.mygame.shared.game.poker.PokerHandResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PokerBotGameScreen - Offline Poker game vs 4 AI bots.
 * Player acts as host managing the turn cycle.
 * Bot actions delayed by 2.5 seconds for easy player notice.
 *
 * @author Royal FlushG Team
 */
@View(id = "pokerBotGame", value = "ui/templates/login.lml")
public class PokerBotGameScreen implements ViewRenderer {

    private static final String TAG = "PokerBotGameScreen";
    private static final String BACKGROUND_PATH = "ui/Background_poker.png";
    private static final float BOT_DELAY_SECONDS = 2.5f;
    private static final long STARTING_CHIPS = 20000L;

    // Track current chips for Play Again (persist across hands)
    private Map<Integer, Long> currentPlayerChips;
    private static final int SMALL_BLIND = 500;
    private static final int BIG_BLIND = 1000;

    @Inject
    private InterfaceService interfaceService;
    @Inject
    private SessionManager sessionManager;

    // Stage and UI
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private boolean uiInitialized = false;

    // Layout
    private Stack rootStack;
    private PokerTableLayout tableLayout;

    // Game state
    private PokerGameState gameState;
    private Deck deck;
    private List<Integer> playerOrder;
    private Map<Integer, PokerBot> bots;
    private int localPlayerId;
    private String localUsername;

    // Turn management
    private int currentTurnIndex;
    private boolean isProcessingBotTurn = false;
    private boolean gameEnded = false;
    private int roundStartIndex; // For tracking betting round completion
    private int lastRaisePlayerIndex = -1;

    // Player mapping
    private Map<Integer, Integer> playerVisualMap;
    // Static flag to force reset when returning to this screen
    private static boolean needsReset = true;

    @Override
    public void render(Stage stage, float delta) {
        // Check if we need to reinitialize (coming back from lobby)
        if (needsReset) {
            // Only reset if we're actually being shown again (uiInitialized was true
            // before)
            if (uiInitialized) {
                resetState();
            }
            needsReset = false;
        }

        if (!uiInitialized) {
            this.stage = stage;
            initializeGame();
            uiInitialized = true;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    /**
     * Reset all state for fresh game when returning to screen.
     */
    private void resetState() {
        uiInitialized = false;
        gameEnded = true; // Stop any running timers
        isProcessingBotTurn = false;
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        tableLayout = null;
        gameState = null;
        bots = null;
        currentPlayerChips = null;
    }

    /**
     * Call this when leaving the screen to force reset next time.
     */
    public static void markForReset() {
        needsReset = true;
    }

    private void initializeGame() {
        skin = UISkinManager.getInstance().getSkin();

        // Get local player info
        if (sessionManager != null && sessionManager.getPlayerProfile() != null) {
            localPlayerId = sessionManager.getPlayerProfile().id;
            localUsername = sessionManager.getPlayerProfile().username;
        } else {
            localPlayerId = 1;
            localUsername = "Player";
        }

        // Create player order: Player + 4 bots
        playerOrder = new ArrayList<>();
        playerOrder.add(localPlayerId);
        playerOrder.add(-1); // Bot 1
        playerOrder.add(-2); // Bot 2
        playerOrder.add(-3); // Bot 3
        playerOrder.add(-4); // Bot 4

        // Create bots
        bots = new HashMap<>();
        bots.put(-1, new PokerBot(-1));
        bots.put(-2, new PokerBot(-2));
        bots.put(-3, new PokerBot(-3));
        bots.put(-4, new PokerBot(-4));

        // Initialize game state with fixed starting chips (not real credits)
        currentPlayerChips = new HashMap<>();
        for (int playerId : playerOrder) {
            currentPlayerChips.put(playerId, STARTING_CHIPS);
        }
        gameState = new PokerGameState(currentPlayerChips, SMALL_BLIND, BIG_BLIND);

        // Initialize visual mapping
        playerVisualMap = new HashMap<>();
        for (int i = 0; i < playerOrder.size(); i++) {
            playerVisualMap.put(playerOrder.get(i), i);
        }

        // Build UI
        buildUI();

        // Initialize seats
        initializeSeats();

        // Start game
        startNewHand();

        Gdx.app.log(TAG, "Poker Bot Game initialized");
    }

    private void buildUI() {
        stage.clear();

        rootStack = new Stack();
        rootStack.setFillParent(true);

        // Load background
        try {
            backgroundTexture = new Texture(Gdx.files.internal(BACKGROUND_PATH));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Image backgroundImage = new Image(backgroundTexture);
            backgroundImage.setFillParent(true);
            rootStack.add(backgroundImage);
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to load background", e);
        }

        // Poker table layout
        tableLayout = new PokerTableLayout();
        rootStack.add(tableLayout);

        // Setup control callbacks
        setupControlCallbacks();

        stage.addActor(rootStack);
        Gdx.input.setInputProcessor(stage);
    }

    private void setupControlCallbacks() {
        tableLayout.setOnCheckAction(() -> handlePlayerAction("CHECK", 0));
        tableLayout.setOnCallAction(() -> handlePlayerAction("CALL", 0));
        tableLayout.setOnFoldAction(() -> handlePlayerAction("FOLD", 0));
        tableLayout.setOnRaiseAction(amount -> handlePlayerAction("RAISE", amount));
    }

    private void initializeSeats() {
        Map<Integer, String> names = new HashMap<>();

        names.put(localPlayerId, localUsername);

        for (int i = 1; i <= 4; i++) {
            int botId = -i;
            names.put(botId, "Bot " + i);
        }

        for (int i = 0; i < playerOrder.size(); i++) {
            int playerId = playerOrder.get(i);
            boolean isLocal = (playerId == localPlayerId);
            long balance = currentPlayerChips.getOrDefault(playerId, STARTING_CHIPS);
            tableLayout.updateSeat(i, playerId, names.get(playerId), balance, isLocal);

            if (!isLocal) {
                PlayerSeatWidget seat = tableLayout.getSeat(i);
                if (seat != null) {
                    seat.showBackCards();
                }
            }
        }
    }

    private void startNewHand() {
        gameEnded = false;
        deck = new Deck();
        deck.shuffle();

        // Reset game state for new hand - use current chips (persist from previous
        // hand)
        for (int playerId : playerOrder) {
            currentPlayerChips.put(playerId, gameState.getPlayerChips(playerId));
        }
        gameState = new PokerGameState(currentPlayerChips, SMALL_BLIND, BIG_BLIND);

        // Reset all seats - clear fold state and prepare for new hand
        for (int i = 0; i < playerOrder.size(); i++) {
            PlayerSeatWidget seat = tableLayout.getSeat(i);
            if (seat != null) {
                seat.setFolded(false); // Clear folded state
                seat.setCurrentTurn(false); // Clear turn highlight
                seat.updateCurrentBet(0); // Clear bet display
            }
        }

        // Deal 2 cards to each player
        for (int playerId : playerOrder) {
            List<Card> holeCards = new ArrayList<>();
            holeCards.add(deck.deal());
            holeCards.add(deck.deal());
            gameState.dealHoleCards(playerId, holeCards);
        }

        // Show player's cards
        List<Card> myCards = gameState.getPlayerHole(localPlayerId);
        PlayerSeatWidget mainSeat = tableLayout.getMainUserSeat();
        if (mainSeat != null && myCards != null) {
            mainSeat.updateCards(myCards);
        }

        // Show back cards for bots/opponents
        for (int i = 0; i < playerOrder.size(); i++) {
            int playerId = playerOrder.get(i);
            if (playerId != localPlayerId) {
                PlayerSeatWidget seat = tableLayout.getSeat(i);
                if (seat != null) {
                    seat.showBackCards();
                }
            }
        }

        // Post blinds (simplified: player 1 = SB, player 2 = BB)
        int sbIndex = 1 % playerOrder.size();
        int bbIndex = 2 % playerOrder.size();
        gameState.bet(playerOrder.get(sbIndex), SMALL_BLIND);
        gameState.bet(playerOrder.get(bbIndex), BIG_BLIND);

        // Update pot display
        tableLayout.updatePot(gameState.getPot());
        tableLayout.clearCommunityCards();

        // Start betting from player after BB
        currentTurnIndex = (bbIndex + 1) % playerOrder.size();
        roundStartIndex = currentTurnIndex;
        lastRaisePlayerIndex = bbIndex;

        syncUIWithState();
        startTurn();

        Gdx.app.log(TAG, "New hand started. Turn: " + playerOrder.get(currentTurnIndex));
    }

    private void startTurn() {
        if (gameEnded)
            return;

        // Check if only one player left
        if (countActivePlayers() <= 1) {
            endHand();
            return;
        }

        int currentPlayerId = playerOrder.get(currentTurnIndex);

        // Skip folded players
        if (gameState.isPlayerFolded(currentPlayerId)) {
            nextTurn();
            return;
        }

        // Highlight current turn
        updateTurnHighlight(currentPlayerId);

        if (currentPlayerId == localPlayerId) {
            // Player's turn
            tableLayout.setControlsVisible(true);
            updateControlPanel();
        } else {
            // Bot's turn
            tableLayout.setControlsVisible(false);
            processBotTurn(currentPlayerId);
        }
    }

    private void processBotTurn(int botId) {
        if (isProcessingBotTurn || gameEnded)
            return;
        isProcessingBotTurn = true;

        PokerBot bot = bots.get(botId);
        if (bot == null) {
            isProcessingBotTurn = false;
            nextTurn();
            return;
        }

        // Delay bot action for 2.5 seconds
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                Gdx.app.postRunnable(() -> executeBotAction(botId, bot));
            }
        }, BOT_DELAY_SECONDS);
    }

    private void executeBotAction(int botId, PokerBot bot) {
        if (gameEnded) {
            isProcessingBotTurn = false;
            return;
        }

        List<Card> holeCards = gameState.getPlayerHole(botId);
        PokerBot.BotAction action = bot.decideAction(gameState, holeCards);

        Gdx.app.log(TAG, "Bot " + botId + " action: " + action);

        switch (action) {
            case FOLD:
                gameState.fold(botId);
                markPlayerFolded(botId);
                break;
            case CHECK:
                // Do nothing, just pass turn
                break;
            case CALL:
                long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(botId);
                if (toCall > 0) {
                    gameState.bet(botId, toCall);
                }
                break;
            case RAISE:
                long raiseAmount = bot.calculateRaiseAmount(gameState);
                gameState.bet(botId, raiseAmount);
                lastRaisePlayerIndex = currentTurnIndex;
                break;
            case ALL_IN:
                long allIn = gameState.getPlayerChips(botId);
                gameState.bet(botId, allIn);
                lastRaisePlayerIndex = currentTurnIndex;
                break;
        }

        syncUIWithState();
        isProcessingBotTurn = false;
        nextTurn();
    }

    private void handlePlayerAction(String actionType, long amount) {
        if (gameEnded)
            return;

        int currentPlayerId = playerOrder.get(currentTurnIndex);
        if (currentPlayerId != localPlayerId) {
            Gdx.app.log(TAG, "Not player's turn!");
            return;
        }

        Gdx.app.log(TAG, "Player action: " + actionType + " amount: " + amount);

        switch (actionType) {
            case "FOLD":
                gameState.fold(localPlayerId);
                markPlayerFolded(localPlayerId);
                break;
            case "CHECK":
                // Do nothing
                break;
            case "CALL":
                long toCall = gameState.getCurrentBet() - gameState.getPlayerBet(localPlayerId);
                if (toCall > 0) {
                    gameState.bet(localPlayerId, toCall);
                }
                break;
            case "RAISE":
                gameState.bet(localPlayerId, amount);
                lastRaisePlayerIndex = currentTurnIndex;
                break;
        }

        tableLayout.setControlsVisible(false);
        syncUIWithState();
        nextTurn();
    }

    private void nextTurn() {
        if (gameEnded)
            return;

        // Check if betting round complete
        int nextIndex = (currentTurnIndex + 1) % playerOrder.size();

        // Skip folded players when checking round completion
        int checksNeeded = 0;
        int activeCount = countActivePlayers();

        // If only one player left, end hand
        if (activeCount <= 1) {
            endHand();
            return;
        }

        // Check if we've gone full circle with everyone matched
        boolean roundComplete = isBettingRoundComplete();

        if (roundComplete) {
            advanceToNextStage();
        } else {
            currentTurnIndex = nextIndex;
            // Skip folded players
            while (gameState.isPlayerFolded(playerOrder.get(currentTurnIndex))) {
                currentTurnIndex = (currentTurnIndex + 1) % playerOrder.size();
            }
            startTurn();
        }
    }

    private boolean isBettingRoundComplete() {
        long currentBet = gameState.getCurrentBet();

        for (int playerId : playerOrder) {
            if (gameState.isPlayerFolded(playerId))
                continue;
            if (gameState.getPlayerChips(playerId) == 0)
                continue; // All-in

            long playerBet = gameState.getPlayerBet(playerId);
            if (playerBet < currentBet) {
                return false; // Someone hasn't matched
            }
        }

        // Everyone has matched the current bet
        return true;
    }

    private void advanceToNextStage() {
        gameState.resetForNewRound();

        switch (gameState.getCurrentStage()) {
            case PREFLOP:
                // Deal Flop (3 cards)
                List<Card> flop = new ArrayList<>();
                flop.add(deck.deal());
                flop.add(deck.deal());
                flop.add(deck.deal());
                gameState.dealFlop(flop);

                for (int i = 0; i < 3; i++) {
                    tableLayout.setCommunityCard(i, flop.get(i));
                }
                break;

            case FLOP:
                // Deal Turn (1 card)
                Card turn = deck.deal();
                gameState.dealTurn(turn);
                tableLayout.setCommunityCard(3, turn);
                break;

            case TURN:
                // Deal River (1 card)
                Card river = deck.deal();
                gameState.dealRiver(river);
                tableLayout.setCommunityCard(4, river);
                break;

            case RIVER:
                // Showdown
                endHand();
                return;

            default:
                endHand();
                return;
        }

        syncUIWithState();

        // Reset to first active player
        currentTurnIndex = 0;
        while (gameState.isPlayerFolded(playerOrder.get(currentTurnIndex))) {
            currentTurnIndex = (currentTurnIndex + 1) % playerOrder.size();
        }
        roundStartIndex = currentTurnIndex;
        lastRaisePlayerIndex = -1;

        startTurn();
    }

    private void endHand() {
        gameEnded = true;
        tableLayout.setControlsVisible(false);

        // Find winner(s)
        List<Integer> activePlayers = new ArrayList<>();
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFolded(playerId)) {
                activePlayers.add(playerId);
            }
        }

        int winnerId;
        if (activePlayers.size() == 1) {
            winnerId = activePlayers.get(0);
        } else {
            // Evaluate hands
            winnerId = findBestHand(activePlayers);
        }

        // Award pot
        gameState.awardPot(winnerId);

        boolean playerWon = (winnerId == localPlayerId);
        String winnerName = (winnerId == localPlayerId) ? localUsername : "Bot " + Math.abs(winnerId);

        Gdx.app.log(TAG, "Hand ended! Winner: " + winnerName);

        // Show end dialog
        showEndGameDialog(playerWon, winnerName);
    }

    private int findBestHand(List<Integer> players) {
        int bestPlayer = players.get(0);
        PokerHandResult bestResult = null;

        for (int playerId : players) {
            List<Card> allCards = new ArrayList<>(gameState.getPlayerHole(playerId));
            allCards.addAll(gameState.getCommunityCards());

            if (allCards.size() >= 5) {
                PokerHandResult result = PokerHandEvaluator.evaluate(allCards);
                if (bestResult == null || result.compareTo(bestResult) > 0) {
                    bestResult = result;
                    bestPlayer = playerId;
                }
            }
        }

        return bestPlayer;
    }

    private int countActivePlayers() {
        int count = 0;
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFolded(playerId)) {
                count++;
            }
        }
        return count;
    }

    private void syncUIWithState() {
        // Update pot
        tableLayout.updatePot(gameState.getPot());

        // Update all seats
        for (int i = 0; i < playerOrder.size(); i++) {
            int playerId = playerOrder.get(i);
            PlayerSeatWidget seat = tableLayout.getSeat(i);
            if (seat != null) {
                seat.updateCredits(gameState.getPlayerChips(playerId));
                seat.updateCurrentBet(gameState.getPlayerBet(playerId));
            }
        }
    }

    private void updateTurnHighlight(int currentPlayerId) {
        for (int i = 0; i < playerOrder.size(); i++) {
            int playerId = playerOrder.get(i);
            PlayerSeatWidget seat = tableLayout.getSeat(i);
            if (seat != null) {
                seat.setCurrentTurn(playerId == currentPlayerId);
            }
        }
    }

    private void markPlayerFolded(int playerId) {
        Integer visualIndex = playerVisualMap.get(playerId);
        if (visualIndex != null) {
            PlayerSeatWidget seat = tableLayout.getSeat(visualIndex);
            if (seat != null) {
                seat.setFolded(true);
            }
        }
    }

    private void updateControlPanel() {
        long myBet = gameState.getPlayerBet(localPlayerId);
        long currentTableBet = gameState.getCurrentBet();
        long toCall = currentTableBet - myBet;
        long myChips = gameState.getPlayerChips(localPlayerId);

        if (toCall <= 0) {
            tableLayout.setCheckButtonVisible(true);
            tableLayout.setCallButtonVisible(false);
        } else {
            tableLayout.setCheckButtonVisible(false);
            tableLayout.setCallButtonVisible(true, toCall);
        }

        long minRaise = BIG_BLIND;
        long maxRaise = myChips;
        tableLayout.setRaiseLimits(Math.min(minRaise, maxRaise), maxRaise);
    }

    private void showEndGameDialog(boolean playerWon, String winnerName) {
        // Dark overlay
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.7f);
        pixmap.fill();
        Texture overlayTex = new Texture(pixmap);
        pixmap.dispose();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(overlayTex));

        Table popup = new Table();
        popup.setBackground(skin.getDrawable("panel1"));
        popup.pad(40);

        // Title
        String title = playerWon ? "YOU WIN!" : "BOT WINS!";
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("Big_blue_font");
        titleStyle.fontColor = playerWon ? Color.GOLD : Color.WHITE;
        Label titleLabel = new Label(title, titleStyle);
        titleLabel.setAlignment(Align.center);
        popup.add(titleLabel).padBottom(20).row();

        // Winner message
        Label.LabelStyle msgStyle = new Label.LabelStyle();
        msgStyle.font = skin.getFont("Blue_font");
        msgStyle.fontColor = Color.WHITE;
        Label messageLabel = new Label(winnerName + " wins the pot!", msgStyle);
        messageLabel.setAlignment(Align.center);
        popup.add(messageLabel).padBottom(30).row();

        // Buttons
        Table buttonTable = new Table();

        TextButton playAgainBtn = new TextButton("PLAY AGAIN", skin, "blue_text_button");
        playAgainBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                overlay.remove();
                startNewHand();
            }
        });

        TextButton lobbyBtn = new TextButton("RETURN TO LOBBY", skin, "blue_text_button");
        lobbyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                overlay.remove();
                returnToLobby();
            }
        });

        buttonTable.add(playAgainBtn).width(200).height(60).padRight(20);
        buttonTable.add(lobbyBtn).width(250).height(60);

        popup.add(buttonTable);

        overlay.add(popup).center();
        stage.addActor(overlay);
    }

    private void returnToLobby() {
        Gdx.app.log(TAG, "Returning to lobby...");
        markForReset(); // Force reset next time this screen is opened
        if (interfaceService != null) {
            interfaceService.show(LobbyController.class);
        }
    }

    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }
}
