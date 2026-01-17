package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.ai.TienLenBot;
import com.mygame.client.controller.LobbyController;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.card.Deck;
import com.mygame.shared.game.tienlen.CardCollection;
import com.mygame.shared.game.tienlen.TienLenCombinationType;
import com.mygame.shared.game.tienlen.TienLenGameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TienLenBotGameScreen - Offline Tien Len game vs 3 AI bots.
 * Bot actions delayed by 2.5 seconds for easy player notice.
 * Player who has 3 of Spades goes first in the first round.
 *
 * @author Royal FlushG Team
 */
@View(id = "tienLenBotGame", value = "ui/templates/login.lml")
public class TienLenBotGameScreen implements ViewRenderer {

    private static final String TAG = "TienLenBotGameScreen";
    private static final String BACKGROUND_PATH = "ui/Background_poker.png";
    private static final float BOT_DELAY_SECONDS = 2.5f;

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
    private TienLenGameLayout gameLayout;

    // Game state
    private TienLenGameState gameState;
    private List<Integer> playerOrder;
    private Map<Integer, TienLenBot> bots;
    private int localPlayerId;
    private String localUsername;

    // Turn management
    private int currentTurnIndex;
    private boolean isProcessingBotTurn = false;
    private boolean gameEnded = false;
    private boolean isFirstRound = true;

    // Player mapping
    private Map<Integer, Integer> playerVisualMap;

    // Warning toast
    private Label warningToast;
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
        isFirstRound = true;
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        gameLayout = null;
        gameState = null;
        bots = null;
        warningToast = null;
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

        // Create player order: Player + 3 bots
        playerOrder = new ArrayList<>();
        playerOrder.add(localPlayerId);
        playerOrder.add(-1); // Bot 1
        playerOrder.add(-2); // Bot 2
        playerOrder.add(-3); // Bot 3

        // Create bots
        bots = new HashMap<>();
        bots.put(-1, new TienLenBot(-1));
        bots.put(-2, new TienLenBot(-2));
        bots.put(-3, new TienLenBot(-3));

        // Initialize game state
        gameState = new TienLenGameState(playerOrder);

        // Initialize visual mapping
        playerVisualMap = new HashMap<>();
        for (int i = 0; i < playerOrder.size(); i++) {
            playerVisualMap.put(playerOrder.get(i), i);
        }

        // Build UI
        buildUI();

        // Initialize seats display
        initializeSeats();

        // Start game
        startNewGame();

        Gdx.app.log(TAG, "Tien Len Bot Game initialized");
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

        // Tien Len game layout
        gameLayout = new TienLenGameLayout();
        rootStack.add(gameLayout);

        // Setup control callbacks
        setupControlCallbacks();

        stage.addActor(rootStack);
        Gdx.input.setInputProcessor(stage);
    }

    private void setupControlCallbacks() {
        gameLayout.setOnPlayAction(this::onPlayCards);
        gameLayout.setOnSkipAction(this::onSkip);
    }

    private void initializeSeats() {
        Map<Integer, String> names = new HashMap<>();

        names.put(localPlayerId, localUsername);

        for (int i = 1; i <= 3; i++) {
            int botId = -i;
            names.put(botId, "Bot " + i);
        }

        // Update local player (no credits shown in Tien Len)
        gameLayout.updateMyInfo(localUsername, 0L);

        // Update rivals
        for (int i = 1; i <= 3; i++) {
            int botId = -i;
            gameLayout.updateRival(i, botId, names.get(botId), 0L, 13);
        }
    }

    private void startNewGame() {
        gameEnded = false;
        isFirstRound = true;

        // Create and shuffle deck
        Deck deck = new Deck();
        deck.shuffle();

        // Reset game state
        gameState = new TienLenGameState(playerOrder);

        // Deal 13 cards to each player
        for (int playerId : playerOrder) {
            List<Card> hand = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                hand.add(deck.deal());
            }
            gameState.dealHand(playerId, hand);
        }

        // Show player's cards
        List<Card> myCards = gameState.getPlayerHand(localPlayerId);
        if (myCards != null) {
            gameLayout.setMyCards(myCards);
        }

        // Update rival card counts
        updateAllRivalCardCounts();

        // Clear center pile
        gameLayout.clearCenterPile();

        // Find player with 3 of Spades to start
        int starterIndex = findPlayerWith3Spades();
        currentTurnIndex = starterIndex;
        gameState.setCurrentPlayerTurn(starterIndex);

        Gdx.app.log(TAG, "Game started. First turn: " + playerOrder.get(currentTurnIndex));

        // Start turn
        startTurn();
    }

    private int findPlayerWith3Spades() {
        for (int i = 0; i < playerOrder.size(); i++) {
            int playerId = playerOrder.get(i);
            List<Card> hand = gameState.getPlayerHand(playerId);
            if (hand != null) {
                for (Card card : hand) {
                    // 3 of Spades: rank = 3, suit = SPADES (ordinal 0)
                    if (card.getRank() == 3 && card.getSuit().ordinal() == 0) {
                        Gdx.app.log(TAG, "Player " + playerId + " has 3 of Spades");
                        return i;
                    }
                }
            }
        }
        // Default to player 0 if 3 of Spades not found
        return 0;
    }

    private void startTurn() {
        if (gameEnded)
            return;

        // Check win condition
        for (int playerId : playerOrder) {
            if (gameState.isPlayerFinished(playerId)) {
                continue; // Already finished, skip
            }
            List<Card> hand = gameState.getPlayerHand(playerId);
            if (hand == null || hand.isEmpty()) {
                // Player finished!
                gameState.getWinners().add(playerId);
            }
        }

        // If all but one finished, game over
        int activePlayers = countActivePlayers();
        if (activePlayers <= 1 || !gameState.getWinners().isEmpty()) {
            endGame();
            return;
        }

        int currentPlayerId = playerOrder.get(currentTurnIndex);

        // Skip finished players
        if (gameState.isPlayerFinished(currentPlayerId)) {
            nextTurn();
            return;
        }

        // Update UI highlight
        updateTurnHighlight(currentPlayerId);

        if (currentPlayerId == localPlayerId) {
            // Player's turn
            gameLayout.setButtonsEnabled(true);
            showWarningToast("Your Turn!");
        } else {
            // Bot's turn
            gameLayout.setButtonsEnabled(false);
            processBotTurn(currentPlayerId);
        }
    }

    private void processBotTurn(int botId) {
        if (isProcessingBotTurn || gameEnded)
            return;
        isProcessingBotTurn = true;

        TienLenBot bot = bots.get(botId);
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

    private void executeBotAction(int botId, TienLenBot bot) {
        if (gameEnded) {
            isProcessingBotTurn = false;
            return;
        }

        List<Card> cardsToPlay = bot.decidePlay(gameState);

        if (cardsToPlay == null || cardsToPlay.isEmpty()) {
            // Bot skips
            Gdx.app.log(TAG, "Bot " + botId + " SKIPS");
            gameState.passTurn(botId);
        } else {
            // Bot plays
            TienLenCombinationType type = CardCollection.detectCombination(cardsToPlay);
            Gdx.app.log(TAG, "Bot " + botId + " plays " + cardsToPlay.size() + " cards: " + type);

            gameState.playCards(botId, cardsToPlay, type);

            // Show played cards
            String botName = "Bot " + Math.abs(botId);
            gameLayout.showPlayedCards(cardsToPlay, botName);

            // Update card count
            updateRivalCardCount(botId);

            // Check if bot finished
            List<Card> botHand = gameState.getPlayerHand(botId);
            if (botHand == null || botHand.isEmpty()) {
                TienLenPlayerWidget widget = gameLayout.getRivalWidget(botId);
                if (widget != null) {
                    widget.setFinished();
                }
            }

            // Clear skip status for new round
            isFirstRound = false;
        }

        // Check if round should reset (everyone skipped except last player)
        checkRoundReset();

        isProcessingBotTurn = false;
        nextTurn();
    }

    private void onPlayCards() {
        if (gameEnded)
            return;

        int currentPlayerId = playerOrder.get(currentTurnIndex);
        if (currentPlayerId != localPlayerId) {
            showWarningToast("Not your turn!");
            return;
        }

        List<Card> selected = gameLayout.getSelectedCards();
        if (selected.isEmpty()) {
            showWarningToast("Select cards first!");
            return;
        }

        // Validate combination
        TienLenCombinationType type = CardCollection.detectCombination(selected);
        if (type == TienLenCombinationType.INVALID) {
            showWarningToast("Invalid Combination!");
            return;
        }

        // Check if can beat current trick
        List<Card> currentTrick = gameState.getCurrentTrick();
        TienLenCombinationType currentType = gameState.getCurrentTrickType();

        if (currentTrick != null && !currentTrick.isEmpty() && currentType != null) {
            if (!CardCollection.canBeat(currentType, currentTrick, type, selected)) {
                showWarningToast("Cannot beat current cards!");
                return;
            }
        }

        // First round: must include 3 of Spades if player has it
        if (isFirstRound) {
            List<Card> myHand = gameState.getPlayerHand(localPlayerId);
            boolean has3Spades = false;
            for (Card c : myHand) {
                if (c.getRank() == 3 && c.getSuit().ordinal() == 0) {
                    has3Spades = true;
                    break;
                }
            }
            if (has3Spades) {
                boolean selected3Spades = false;
                for (Card c : selected) {
                    if (c.getRank() == 3 && c.getSuit().ordinal() == 0) {
                        selected3Spades = true;
                        break;
                    }
                }
                if (!selected3Spades) {
                    showWarningToast("Must play 3 of Spades!");
                    return;
                }
            }
            isFirstRound = false;
        }

        Gdx.app.log(TAG, "Player plays " + selected.size() + " cards");

        // Play cards
        gameState.playCards(localPlayerId, selected, type);

        // Update UI
        gameLayout.showPlayedCards(selected, "You");
        gameLayout.removeSelectedCards();

        // Check if player finished
        List<Card> myHand = gameState.getPlayerHand(localPlayerId);
        if (myHand == null || myHand.isEmpty()) {
            endGame();
            return;
        }

        gameLayout.setButtonsEnabled(false);

        // Clear skip status for new round
        checkRoundReset();

        nextTurn();
    }

    private void onSkip() {
        if (gameEnded)
            return;

        int currentPlayerId = playerOrder.get(currentTurnIndex);
        if (currentPlayerId != localPlayerId) {
            showWarningToast("Not your turn!");
            return;
        }

        // Cannot skip if board is empty (must play)
        List<Card> currentTrick = gameState.getCurrentTrick();
        if (currentTrick == null || currentTrick.isEmpty()) {
            showWarningToast("Board is empty, must play!");
            return;
        }

        Gdx.app.log(TAG, "Player SKIPS");
        gameState.passTurn(localPlayerId);

        gameLayout.setButtonsEnabled(false);

        checkRoundReset();
        nextTurn();
    }

    private void checkRoundReset() {
        // If everyone except the last player who played has skipped, start new round
        int lastPlayer = gameState.getLastPlayedPlayer();
        boolean allOthersSkipped = true;

        for (int playerId : playerOrder) {
            if (playerId == lastPlayer)
                continue;
            if (gameState.isPlayerFinished(playerId))
                continue;
            if (!gameState.isSkipped(playerId)) {
                allOthersSkipped = false;
                break;
            }
        }

        if (allOthersSkipped && lastPlayer != -1) {
            Gdx.app.log(TAG, "All others skipped. Starting new round for player " + lastPlayer);
            gameState.startNewRound();
            gameLayout.clearCenterPile();

            // Set turn to last player who played
            for (int i = 0; i < playerOrder.size(); i++) {
                if (playerOrder.get(i) == lastPlayer) {
                    currentTurnIndex = i;
                    gameState.setCurrentPlayerTurn(i);
                    break;
                }
            }
        }
    }

    private void nextTurn() {
        if (gameEnded)
            return;

        // Check round reset FIRST before moving to next player
        checkRoundReset();

        // Move to next player
        int attempts = 0;
        int startIndex = currentTurnIndex;

        do {
            currentTurnIndex = (currentTurnIndex + 1) % playerOrder.size();
            attempts++;

            int pid = playerOrder.get(currentTurnIndex);

            // Skip finished players
            if (gameState.isPlayerFinished(pid)) {
                continue;
            }

            // If board is empty (new round), anyone can play
            if (gameState.getCurrentTrick() == null || gameState.getCurrentTrick().isEmpty()) {
                break;
            }

            // Skip players who have already skipped this round
            if (!gameState.isSkipped(pid)) {
                break;
            }
        } while (attempts < playerOrder.size());

        // Safety: If we've looped through all players without finding one,
        // force a new round (everyone has skipped)
        if (attempts >= playerOrder.size()) {
            Gdx.app.log(TAG, "All players skipped - forcing new round");
            gameState.startNewRound();
            gameLayout.clearCenterPile();

            // Find first non-finished player
            for (int i = 0; i < playerOrder.size(); i++) {
                if (!gameState.isPlayerFinished(playerOrder.get(i))) {
                    currentTurnIndex = i;
                    break;
                }
            }
        }

        gameState.setCurrentPlayerTurn(currentTurnIndex);
        startTurn();
    }

    private void endGame() {
        gameEnded = true;
        gameLayout.setButtonsEnabled(false);

        // Determine winner
        int winnerId;
        if (!gameState.getWinners().isEmpty()) {
            winnerId = gameState.getWinners().get(0);
        } else {
            // Find player with fewest cards
            winnerId = localPlayerId;
            int minCards = Integer.MAX_VALUE;
            for (int playerId : playerOrder) {
                List<Card> hand = gameState.getPlayerHand(playerId);
                int count = (hand != null) ? hand.size() : 0;
                if (count < minCards) {
                    minCards = count;
                    winnerId = playerId;
                }
            }
        }

        boolean playerWon = (winnerId == localPlayerId);
        String winnerName = (winnerId == localPlayerId) ? localUsername : "Bot " + Math.abs(winnerId);

        Gdx.app.log(TAG, "Game ended! Winner: " + winnerName);

        showEndGameDialog(playerWon, winnerName);
    }

    private int countActivePlayers() {
        int count = 0;
        for (int playerId : playerOrder) {
            if (!gameState.isPlayerFinished(playerId)) {
                List<Card> hand = gameState.getPlayerHand(playerId);
                if (hand != null && !hand.isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void updateAllRivalCardCounts() {
        for (int i = 1; i <= 3; i++) {
            int botId = -i;
            updateRivalCardCount(botId);
        }
    }

    private void updateRivalCardCount(int playerId) {
        TienLenPlayerWidget widget = gameLayout.getRivalWidget(playerId);
        if (widget != null) {
            List<Card> hand = gameState.getPlayerHand(playerId);
            int count = (hand != null) ? hand.size() : 0;
            widget.updateCardCount(count);
        }
    }

    private void updateTurnHighlight(int currentPlayerId) {
        gameLayout.setActivePlayer(currentPlayerId);
        gameLayout.setMyActive(currentPlayerId == localPlayerId);
    }

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
        Label messageLabel = new Label(winnerName + " finished first!", msgStyle);
        messageLabel.setAlignment(Align.center);
        popup.add(messageLabel).padBottom(30).row();

        // Buttons
        Table buttonTable = new Table();

        TextButton playAgainBtn = new TextButton("PLAY AGAIN", skin, "blue_text_button");
        playAgainBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                overlay.remove();
                startNewGame();
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
