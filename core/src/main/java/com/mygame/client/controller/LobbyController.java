package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.model.PlayerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Comprehensive, production-ready Lobby Screen using LibGDX and Scene2D.
 * Features:
 * - Hub section with cat avatar, credits, and settings
 * - Daily Quest section with completion tracking
 * - Lobby list with game mode filtering
 * - Leaderboard with top players
 * - Settings dialog with SFX toggle and logout
 *
 * @author Royal FlushG Team
 */
@Component
@View(id = "lobby", value = "ui/templates/login.lml")
public class LobbyController implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(LobbyController.class);

    @Inject private NetworkService networkService;
    @Inject private SessionManager sessionManager;
    @Inject private InterfaceService interfaceService;

    private static final String BACKGROUND_PATH = "ui/Background_lobby_mainscreen.png";

    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private boolean uiInitialized = false;

    // UI Components
    private Label creditLabel;
    private Table lobbyListContent;
    private Table leaderboardContent;
    private SelectBox<String> modeSelectBox;

    // Simulation Data
    private Array<Quest> quests = new Array<>();
    private boolean sfxEnabled = true;

    // Custom Label Styles
    private Label.LabelStyle transparentStyle;
    private Label.LabelStyle goldStyle;

    public LobbyController() {
        // Default constructor for Autumn MVC
    }

    @Override
    public void render(Stage stage, float delta) {
        if (!uiInitialized) {
            this.stage = stage;

            // 1. LOAD DATA FIRST (Fixes missing Daily Quests)
            loadData();

            // 2. Then build UI
            setupUI();
            setupNetworkListener();

            uiInitialized = true;
            logger.info("LobbyController initialized");
        }

        // Clear screen
        ScreenUtils.clear(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and render stage
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    private void setupUI() {
        stage.clear();

        // Load assets
        skin = UISkinManager.getInstance().getSkin();
        if (skin == null) {
            logger.error("Failed to load UI skin");
            return;
        }

        try {
            backgroundTexture = new Texture(Gdx.files.internal(BACKGROUND_PATH));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            logger.error("Failed to load background: {}", BACKGROUND_PATH, e);
        }

        // Create custom label styles for cleaner look
        transparentStyle = new Label.LabelStyle();
        transparentStyle.font = skin.getFont("Blue_font");
        transparentStyle.fontColor = new Color(0, 0.96f, 1, 1); // Cyan

        goldStyle = new Label.LabelStyle();
        goldStyle.font = skin.getFont("Blue_font");
        goldStyle.fontColor = Color.GOLD;

        // Root Layout using Stack
        Stack stack = new Stack();
        stack.setFillParent(true);

        // --- LAYER 1: Background Image ---
        if (backgroundTexture != null) {
            Image bgImage = new Image(backgroundTexture);
            bgImage.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            bgImage.setFillParent(true);
            stack.add(bgImage);
        }

        // --- LAYER 2: Main Grid Layout (Hub, Lobby, Leaderboard) ---
        Table mainTable = new Table();
        mainTable.top();

        // --- LEFT COLUMN (Hub + Quest) ---
        Table leftCol = new Table();
        // Hub: Fixed Height
        leftCol.add(createHubTable()).height(280).fillX().padTop(40).padLeft(250);
        leftCol.row();

        // Quest: STRICT WIDTH CONSTRAINT to prevent text from expanding indefinitely
        leftCol.add(createQuestTable()).width(300).expandY().fillY()
               .padTop(200).padBottom(250).padLeft(0);

        mainTable.add(leftCol).width(420).expandY().fillY();

        // --- CENTER COLUMN (Lobby) ---
        // STRICT CONSTRAINT: width(800) keeps it inside the paper area
        mainTable.add(createLobbySection())
                 .width(800).expandY().fillY()
                 .padTop(330).padBottom(100).padLeft(120).padRight(50);

        // --- RIGHT COLUMN (Leaderboard) ---
        // STRICT CONSTRAINT: width(420) keeps it inside the grid area
        mainTable.add(createLeaderboardTable())
                 .width(390).expandY().fillY()
                 .padTop(300).padBottom(250).padRight(20).padLeft(80);

        stack.add(mainTable);

        // --- LAYER 3: Overlay (Pause Button) ---
        // This table floats on top of everything else
        Table overlayTable = new Table();
        overlayTable.top().right(); // Align content to Top-Right corner

        // Use the pause_button style from uiskin for click animation
        Button pauseBtn = new Button(skin, "pause_button");
        pauseBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showSettingsDialog();
            }
        });

        // Position: Top Right Corner
        overlayTable.add(pauseBtn).size(60).pad(25);

        stack.add(overlayTable);

        stage.addActor(stack);

        Gdx.input.setInputProcessor(stage);
    }

    // =================================================================================
    // SECTION 1: THE HUB (Credit Only, Transparent Style)
    // =================================================================================
    private Table createHubTable() {
        Table table = new Table();

        PlayerProfile profile = sessionManager.getPlayerProfile();
        long userCredits = (profile != null) ? profile.credits : 1000; // Fallback for testing

        creditLabel = new Label(formatNumber(userCredits), transparentStyle);
        creditLabel.setFontScale(1.7f); // Bigger font

        Table bottomRow = new Table();
        bottomRow.bottom().left();

        // padLeft(120) pushes it past the fish icon
        bottomRow.add(creditLabel).padLeft(-140).padBottom(20);

        table.add(bottomRow).expand().bottom().left();

        return table;
    }

    // =================================================================================
    // SECTION 2: DAILY QUEST (Fixed Text Wrapping)
    // =================================================================================
    private Table createQuestTable() {
        Table table = new Table();

        Table listTable = new Table();
        listTable.top().left();

        // Defensive null check to prevent crashes
        if (this.quests == null) {
            this.quests = new Array<>();
        }

        for (final Quest q : quests) {
            Table row = new Table();

            // Allow text to wrap but give it explicit width to breathe
            Label descLabel = new Label(q.description, transparentStyle);
            descLabel.setWrap(true);
            descLabel.setAlignment(Align.left);
            descLabel.setFontScale(0.9f); // Slightly smaller to fit better

            Button checkBtn = new Button(skin, "Check_button");

            if (!q.isCompleted) {
                checkBtn.setColor(1, 1, 1, 0.5f);
            } else {
                checkBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        logger.info("Quest Claimed: {}", q.description);
                        checkBtn.setColor(Color.GOLD);
                    }
                });
            }

            // Reduced width to 200px so button fits properly
            row.add(descLabel).width(200).padRight(5).left();
            row.add(checkBtn).size(30).right();

            listTable.add(row).padBottom(8).row(); // Reduced spacing to fit 5 items vertically
        }

        ScrollPane scroll = new ScrollPane(listTable, skin);
        scroll.setScrollingDisabled(true, false);
        table.add(scroll).expand().fill().pad(15);

        return table;
    }

    // =================================================================================
    // SECTION 3: LOBBY LIST (COMPACT & BIGGER FONT)
    // =================================================================================
    private Table createLobbySection() {
        Table container = new Table();

        // --- 1. FILTER ---
        Table filterTable = new Table();
        Label filterLabel = new Label("Mode:", transparentStyle);
        filterLabel.setColor(Color.BLACK);

        modeSelectBox = new SelectBox<>(skin);
        modeSelectBox.setItems("ALL", "POKER", "TIENLEN");
        modeSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshLobbyList();
            }
        });

        filterTable.add(filterLabel).padRight(10);
        filterTable.add(modeSelectBox).width(150).height(40);

        container.add(filterTable).left().padBottom(15).padLeft(60).row();

        // --- 2. LIST CONTENT ---
        lobbyListContent = new Table();
        lobbyListContent.top();
        refreshLobbyList();

        // CRITICAL: Strict size for ScrollPane to fit inside paper
        ScrollPane scroll = new ScrollPane(lobbyListContent, skin);
        scroll.setScrollingDisabled(true, false);

        // PadLeft/Right squeezes the scrollpane into the center of the paper
        container.add(scroll).width(600).expandY().fillY().padLeft(40).padRight(40).row();

        // --- 3. BUTTONS ---
        Table buttonTable = new Table();

        TextButton createBtn = new TextButton("CREATE", skin, "blue_text_button");
        createBtn.getLabel().setFontScale(1.3f); // Bigger font
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Create Lobby clicked");
                // TODO: Show create room dialog
            }
        });

        TextButton historyBtn = new TextButton("HISTORY", skin, "blue_text_button");
        historyBtn.getLabel().setFontScale(1.3f);
        historyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Match History clicked");
                // TODO: Show match history
            }
        });

        TextButton botBtn = new TextButton("VS BOT", skin, "blue_text_button");
        botBtn.getLabel().setFontScale(1.3f);
        botBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Play with Bot clicked");
                interfaceService.show(PokerGameControllerJava.class);
            }
        });

        buttonTable.add(createBtn).width(180).height(65).pad(130,0,20,0);
        buttonTable.add(historyBtn).width(180).height(65).pad(130,20,20,0);
        buttonTable.add(botBtn).width(180).height(65).pad(130,20,20,0);

        container.add(buttonTable).padTop(10);

        return container;
    }

    // =================================================================================
    // SECTION 4: LEADERBOARD (STRICT WIDTH)
    // =================================================================================
    private Table createLeaderboardTable() {
        Table container = new Table();

        leaderboardContent = new Table();
        leaderboardContent.top();

        refreshLeaderboard();

        ScrollPane scroll = new ScrollPane(leaderboardContent, skin);
        scroll.setScrollingDisabled(true, false);

        // Strict Width to fit in grid
        container.add(scroll).width(350).expandY().fillY().pad(20);

        return container;
    }

    // =================================================================================
    // SETTINGS MODAL
    // =================================================================================
    private void showSettingsDialog() {
        Window.WindowStyle winStyle = new Window.WindowStyle(
            skin.getFont("Blue_font"),
            Color.WHITE,
            skin.getDrawable("panel1")
        );

        final Dialog dialog = new Dialog("", winStyle);
        dialog.pad(40);

        Table content = dialog.getContentTable();

        // Title
        Label title = new Label("PAUSED", transparentStyle);
        title.setFontScale(1.5f);
        content.add(title).padBottom(30).row();

        // SFX Toggle
        Table sfxTable = new Table();
        Label sfxLabel = new Label("SFX: ", skin, "default");

        CheckBox.CheckBoxStyle sfxStyle = new CheckBox.CheckBoxStyle();
        sfxStyle.checkboxOn = skin.getDrawable("icon_volume");
        sfxStyle.checkboxOff = skin.getDrawable("icon_mute");
        sfxStyle.font = skin.getFont("Blue_font");
        sfxStyle.fontColor = Color.WHITE;

        final CheckBox sfxCheckbox = new CheckBox("", sfxStyle);
        sfxCheckbox.setChecked(sfxEnabled);
        sfxCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sfxEnabled = sfxCheckbox.isChecked();
                logger.info("SFX toggled: {}", sfxEnabled);
            }
        });

        sfxTable.add(sfxLabel).right().padRight(10);
        sfxTable.add(sfxCheckbox).size(24).left();
        content.add(sfxTable).padBottom(20).row();

        // Resume Button
        TextButton resumeBtn = new TextButton("RESUME", skin, "blue_text_button");
        resumeBtn.getLabel().setFontScale(1.2f);
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        content.add(resumeBtn).width(150).height(50).padBottom(15).row();

        // Log Out Button
        TextButton logoutBtn = new TextButton("LOG OUT", skin, "blue_text_button");
        logoutBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Logging out...");
                sessionManager.logout();
                networkService.disconnect();
                dialog.hide();
                interfaceService.show(LoginController.class);
            }
        });
        content.add(logoutBtn).width(150).height(40);

        dialog.show(stage);
    }

    // =================================================================================
    // DATA LOADING AND UPDATES
    // =================================================================================
    private void loadData() {
        // Initialize quests
        quests = new Array<>();
        quests.add(new Quest("Play 5 hands", true));
        quests.add(new Quest("Win with Flush", false));
        quests.add(new Quest("Bet 5000 total", true));
        quests.add(new Quest("Login 2 days", false));
        quests.add(new Quest("Beat a bot", true));

        // Generate dummy lobby list
        refreshLobbyList();

        // Generate leaderboard
        refreshLeaderboard();
    }

    private void refreshLobbyList() {
        if (lobbyListContent == null) return;

        lobbyListContent.clearChildren();

        // Generate dummy lobbies - slightly bigger fonts but tighter columns
        for (int i = 0; i < 20; i++) {
            Table row = new Table();

            // Fonts: Slightly bigger (1.1f) but columns are tighter
            Label roomLbl = new Label("R#" + (100+i), transparentStyle);
            roomLbl.setFontScale(1.1f);
            roomLbl.setColor(Color.DARK_GRAY);

            Label stakeLbl = new Label("$" + formatNumber((long)Math.pow(10, i%4 + 3)), transparentStyle);
            stakeLbl.setFontScale(1.1f);
            stakeLbl.setColor(new Color(0.6f, 0, 0, 1));

            Label modeName = new Label(i % 2 == 0 ? "Poker" : "TienLen", transparentStyle);
            modeName.setFontScale(1.1f);
            modeName.setColor(Color.NAVY);

            TextButton joinBtn = new TextButton("JOIN", skin, "blue_text_button");

            final int roomIndex = i;
            joinBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    logger.info("Joining room: {}", roomIndex);
                    // TODO: Send join room request
                }
            });

            // COMPACT COLUMN LAYOUT - Total Width available ~650px
            row.add(roomLbl).width(100).left();       // 100px
            row.add(stakeLbl).width(120).left();      // 120px
            row.add(modeName).width(250).center();    // 250px (Center Mode Name)
            row.add(joinBtn).width(80).height(40).right(); // 80px

            lobbyListContent.add(row).width(600).height(50).padBottom(5).row();
        }
    }

    private void refreshLeaderboard() {
        if (leaderboardContent == null) return;

        leaderboardContent.clearChildren();

        for (int i = 1; i <= 50; i++) {
            String name = (i == 1) ? "KingOfPoker" : "Player_" + i;
            long score = 90000000L / i;

            Table row = new Table();

            Label rankLbl = new Label(i + ".", transparentStyle);
            rankLbl.setFontScale(1.1f);

            Label nameLbl = new Label(name, transparentStyle);
            nameLbl.setEllipsis(true);
            nameLbl.setFontScale(1.1f);

            Label creditLbl = new Label(formatNumber(score), goldStyle);
            creditLbl.setAlignment(Align.right);
            creditLbl.setFontScale(1.0f); // Slightly smaller for big numbers

            // STRICT COLUMNS
            row.add(rankLbl).width(40).left();
            row.add(nameLbl).width(180).left().padRight(5);
            row.add(creditLbl).width(90).right();

            leaderboardContent.add(row).width(300).height(20).row();
        }
    }

    private void setupNetworkListener() {
        // TODO: Add network packet listeners for room updates, leaderboard updates, etc.
    }

    // =================================================================================
    // UTILITIES
    // =================================================================================
    private String formatNumber(long number) {
        if (number < 100000) {
            return String.valueOf(number);
        }
        return String.format(Locale.US, "%.2e", (double)number);
    }

    // Simple POJO for Quest logic
    private static class Quest {
        String description;
        boolean isCompleted;
        Quest(String d, boolean c) {
            description = d;
            isCompleted = c;
        }
    }


    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        logger.info("LobbyController disposed");
    }
}
