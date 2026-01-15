package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.RoyalFlushG;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.client.ui.UIHelper;
import com.mygame.shared.model.RoomInfo;
import com.mygame.shared.network.packets.LeaveRoomRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PokerGameController - Minimal implementation showing only background + pause
 * button
 * 
 * @author Royal FlushG Team
 */
@View(id = "pokerGameJava", value = "ui/templates/poker_game_java.lml")
public class PokerGameControllerJava implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PokerGameControllerJava.class);

    @Inject
    private SessionManager sessionManager;
    @Inject
    private InterfaceService interfaceService;
    @Inject
    private NetworkService networkService;

    private Stage stage;
    private Viewport viewport;
    private Skin skin;
    private Image backgroundImage;
    private Button pauseButton;
    private boolean initialized = false;
    private int currentRoomId = -1;

    @Override
    public void render(Stage stage, float delta) {
        if (!initialized) {
            this.stage = stage;
            logger.info("PokerGameControllerJava render() called - initializing...");
            Gdx.app.log("PokerGameControllerJava", "Initializing in render()");
            initialize();
            initialized = true;
            logger.info("PokerGameControllerJava initialization complete");
        }

        // Clear screen
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        // Update viewport if window resize (use stage's viewport)
        if (this.stage != null && this.stage.getViewport() != null) {
            this.stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        }

        // Update and render stage
        if (this.stage != null) {
            this.stage.act(delta);
            this.stage.draw();
        }
    }

    private void initialize() {
        logger.info("Initializing PokerGameControllerJava...");
        Gdx.app.log("PokerGameControllerJava", "initialize() called");

        // Get room ID from session manager
        RoomInfo roomInfo = sessionManager.getAndClearPendingRoomInfo();
        if (roomInfo != null) {
            currentRoomId = roomInfo.getRoomId();
            logger.info("Entered room: {}", currentRoomId);
            Gdx.app.log("PokerGameControllerJava", "Room ID: " + currentRoomId);
        } else {
            logger.warn("No room info found in session manager");
            Gdx.app.log("PokerGameControllerJava", "WARNING: No room info found");
        }

        // Use stage's viewport (don't create new one)
        if (stage != null) {
            viewport = stage.getViewport();
        } else {
            viewport = new FitViewport(RoyalFlushG.WIDTH, RoyalFlushG.HEIGHT);
        }

        // Set input processor
        Gdx.input.setInputProcessor(stage);

        // Get skin
        skin = UISkinManager.getInstance().getSkin();
        if (skin == null) {
            logger.error("Failed to load UI skin");
            Gdx.app.log("PokerGameControllerJava", "ERROR: Failed to load skin");
            return;
        }

        // Create background
        createBackground();

        // Create pause button (top-right)
        createPauseButton();

        logger.info("PokerGameControllerJava initialized successfully");
        Gdx.app.log("PokerGameControllerJava", "Initialization complete");
    }

    /**
     * Create background image
     */
    private void createBackground() {
        try {
            backgroundImage = UIHelper.createBackground("ui/Background_poker.png");
            if (backgroundImage != null) {
                backgroundImage.setFillParent(true);
                stage.addActor(backgroundImage);
                logger.info("Background loaded successfully");
            } else {
                logger.error("Failed to create background image");
            }
        } catch (Exception e) {
            logger.error("Failed to load background: {}", e.getMessage(), e);
        }
    }

    /**
     * Create pause button (top-right corner)
     */
    private void createPauseButton() {
        // Try to use pause_button style from skin
        try {
            pauseButton = new Button(skin, "pause_button");
        } catch (Exception e) {
            // Fallback: create simple text button
            logger.warn("pause_button style not found, creating text button: {}", e.getMessage());
            TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
            btnStyle.font = skin.getFont("Blue_font");
            btnStyle.fontColor = Color.WHITE;
            btnStyle.up = skin.getDrawable("panel1");
            pauseButton = new TextButton("||", btnStyle);
        }

        pauseButton.setSize(60, 60);
        pauseButton.setPosition(
                Gdx.graphics.getWidth() - pauseButton.getWidth() - 25,
                Gdx.graphics.getHeight() - pauseButton.getHeight() - 25);

        pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Pause button clicked");
                showPauseDialog();
            }
        });

        stage.addActor(pauseButton);
        logger.info("Pause button created");
    }

    /**
     * Show pause menu dialog
     */
    private void showPauseDialog() {
        Window.WindowStyle winStyle = new Window.WindowStyle(
                skin.getFont("Blue_font"),
                Color.WHITE,
                skin.getDrawable("panel1"));

        final Dialog dialog = new Dialog("", winStyle);
        dialog.pad(40);

        Table content = dialog.getContentTable();

        // Title
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("Blue_font");
        titleStyle.fontColor = new Color(0, 0.96f, 1, 1); // Cyan
        Label title = new Label("PAUSED", titleStyle);
        title.setFontScale(1.5f);
        content.add(title).padBottom(30).row();

        // Exit Lobby Button
        TextButton exitBtn = new TextButton("EXIT LOBBY", skin, "blue_text_button");
        exitBtn.getLabel().setFontScale(1.2f);
        exitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Exit Lobby clicked");
                exitLobby();
                dialog.hide();
            }
        });
        content.add(exitBtn).width(200).height(50).padBottom(15).row();

        // Resume Button
        TextButton resumeBtn = new TextButton("RESUME", skin, "blue_text_button");
        resumeBtn.getLabel().setFontScale(1.2f);
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        content.add(resumeBtn).width(200).height(50);

        dialog.show(stage);
    }

    /**
     * Exit lobby - Send LeaveRoomRequest and navigate back to LobbyController
     */
    private void exitLobby() {
        if (currentRoomId > 0) {
            LeaveRoomRequest request = new LeaveRoomRequest(currentRoomId);
            networkService.sendPacket(request);
            logger.info("Sent LeaveRoomRequest for room: {}", currentRoomId);
        } else {
            logger.warn("No room ID found, navigating to lobby anyway");
        }

        // Navigate back to lobby
        interfaceService.show(LobbyController.class);
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        logger.info("Disposing PokerGameControllerJava...");
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }
}
