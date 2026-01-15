package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.game.GameScreen;
import com.mygame.client.ui.game.PokerGameScreen;
import com.mygame.client.ui.game.TienLenGameScreen;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.RoomInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Autumn MVC Controller wrapper for game screens.
 * Routes to appropriate screen based on game type:
 * - POKER -> PokerGameScreen (new Table-based layout)
 * - TIENLEN -> TienLenGameScreen (new Table-based layout)
 */
@Component
@View(id = "gameScreen", value = "ui/templates/login.lml")
public class GameScreenController implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(GameScreenController.class);

    @Inject
    private NetworkService networkService;
    @Inject
    private SessionManager sessionManager;

    // Screen instances - use interface/base class for polymorphism
    private Screen currentScreen;
    private PokerGameScreen pokerScreen;
    private TienLenGameScreen tienLenScreen;

    private boolean initialized = false;

    // Game parameters
    private GameType gameType;
    private int roomId;
    private int localPlayerId;

    public GameScreenController() {
        // Default constructor for Autumn MVC
    }

    @Override
    public void render(Stage stage, float delta) {
        if (!initialized) {
            initializeGameScreen();
            initialized = true;
        }

        if (currentScreen != null) {
            // Clear screen
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // Delegate rendering to current screen
            currentScreen.render(delta);
        }
    }

    /**
     * Initialize the appropriate GameScreen based on game type.
     */
    private void initializeGameScreen() {
        try {
            // Get game parameters from session
            gameType = sessionManager.getAndClearPendingGameType();
            localPlayerId = sessionManager.getCurrentUserId();
            roomId = sessionManager.getPendingRoomId();

            // Get RoomInfo for player usernames
            RoomInfo roomInfo = sessionManager.getAndClearPendingRoomInfo();

            // Get local username from PlayerProfile
            String localUsername = "Player";
            if (sessionManager.getPlayerProfile() != null) {
                localUsername = sessionManager.getPlayerProfile().username;
            }

            if (gameType == null) {
                logger.error("No game type found in session, defaulting to TIENLEN");
                gameType = GameType.TIENLEN;
            }

            logger.info("Initializing GameScreen: type={}, playerId={}, username={}, roomId={}",
                    gameType, localPlayerId, localUsername, roomId);

            // Create appropriate screen based on game type
            if (gameType == GameType.POKER) {
                pokerScreen = new PokerGameScreen(localPlayerId, localUsername, roomId, roomInfo, networkService);
                pokerScreen.show();
                currentScreen = pokerScreen;

                // Register for network packets
                networkService.addPacketListener(packet -> {
                    if (pokerScreen != null) {
                        pokerScreen.updateUIFromPacket(packet);
                    }
                });

                logger.info("PokerGameScreen initialized (NEW UI)");
            } else {
                // TIENLEN
                tienLenScreen = new TienLenGameScreen(localPlayerId, localUsername, roomId, roomInfo, networkService);
                tienLenScreen.show();
                currentScreen = tienLenScreen;

                // Register for network packets
                networkService.addPacketListener(packet -> {
                    if (tienLenScreen != null) {
                        tienLenScreen.updateUIFromPacket(packet);
                    }
                });

                logger.info("TienLenGameScreen initialized (NEW UI)");
            }

        } catch (Exception e) {
            logger.error("Failed to initialize GameScreen: {}", e.getMessage(), e);
        }
    }

    /**
     * Called when this view is hidden.
     */
    public void dispose() {
        if (currentScreen != null) {
            currentScreen.dispose();
        }
        pokerScreen = null;
        tienLenScreen = null;
        currentScreen = null;
        initialized = false;
    }

    /**
     * Get the current screen (for external packet handling if needed).
     */
    public Screen getCurrentScreen() {
        return currentScreen;
    }
}
