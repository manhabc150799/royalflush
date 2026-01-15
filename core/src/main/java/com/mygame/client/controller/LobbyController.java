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
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.model.*;
import com.mygame.shared.network.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    @Inject
    private NetworkService networkService;
    @Inject
    private SessionManager sessionManager;
    @Inject
    private InterfaceService interfaceService;

    private static final String BACKGROUND_PATH = "ui/Background_lobby_mainscreen.png";

    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private boolean uiInitialized = false;

    // UI Components
    private Label creditLabel;
    private Table lobbyListContent;
    private Table leaderboardContent;
    private Table questListContent;
    private SelectBox<String> modeSelectBox;

    // Real Data from Server
    private List<LeaderboardEntry> leaderboardEntries = new java.util.ArrayList<>();
    private List<RoomInfo> roomList = new java.util.ArrayList<>();
    private List<Quest> quests = new java.util.ArrayList<>();
    private List<com.mygame.shared.model.MatchHistoryEntry> matchHistoryEntries = new java.util.ArrayList<>();
    private boolean sfxEnabled = true;

    // Auto-refresh timer for lobby list
    private float lobbyRefreshTimer = 0f;
    private static final float LOBBY_REFRESH_INTERVAL = 0.5f; // Refresh every 0.5 seconds

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

            // 1. Build UI first
            setupUI();

            // 2. Setup network listener
            setupNetworkListener();

            // 3. Request data from server
            requestServerData();

            uiInitialized = true;
            logger.info("LobbyController initialized");
        }

        // Clear screen
        ScreenUtils.clear(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Auto-refresh lobby list every 0.5 seconds
        lobbyRefreshTimer += delta;
        if (lobbyRefreshTimer >= LOBBY_REFRESH_INTERVAL) {
            lobbyRefreshTimer = 0f;
            requestRoomList();
        }

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

        questListContent = new Table();
        questListContent.top().left();

        refreshQuestTable();

        ScrollPane scroll = new ScrollPane(questListContent, skin);
        scroll.setScrollingDisabled(true, false);
        table.add(scroll).expand().fill().pad(15);

        return table;
    }

    private void refreshQuestTable() {
        if (questListContent == null)
            return;

        questListContent.clearChildren();

        // Display quests
        if (quests.isEmpty()) {
            Label emptyLabel = new Label("Loading quests...", transparentStyle);
            emptyLabel.setColor(Color.GRAY);
            emptyLabel.setFontScale(0.9f);
            questListContent.add(emptyLabel).pad(20);
        } else {
            for (final Quest q : quests) {
                Table row = new Table();

                // Progress text: "description (progress/target)"
                String progressText = q.description + " (" + q.currentProgress + "/" + q.targetCount + ")";
                Label descLabel = new Label(progressText, transparentStyle);
                descLabel.setWrap(true);
                descLabel.setAlignment(Align.left);
                descLabel.setFontScale(0.9f);

                // Color based on status
                if (q.isClaimed) {
                    descLabel.setColor(Color.GRAY);
                } else if (q.isCompleted()) {
                    descLabel.setColor(Color.GREEN);
                } else {
                    descLabel.setColor(Color.WHITE);
                }

                Button checkBtn = new Button(skin, "Check_button");

                if (q.isClaimed || !q.isCompleted()) {
                    checkBtn.setColor(1, 1, 1, 0.5f);
                    checkBtn.setDisabled(true);
                } else {
                    checkBtn.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            claimQuest(q.questId);
                        }
                    });
                }

                row.add(descLabel).width(200).padRight(5).left();
                row.add(checkBtn).size(30).right();

                questListContent.add(row).padBottom(8).row();
            }
        }
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

        modeSelectBox = new SelectBox<>(skin, "default");
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
        createBtn.getLabel().setFontScale(1.3f);
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCreateRoomDialog();
            }
        });

        TextButton historyBtn = new TextButton("HISTORY", skin, "blue_text_button");
        historyBtn.getLabel().setFontScale(1.3f);
        historyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showMatchHistoryDialog();
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

        buttonTable.add(createBtn).width(180).height(65).pad(130, 0, 20, 0);
        buttonTable.add(historyBtn).width(180).height(65).pad(130, 20, 20, 0);
        buttonTable.add(botBtn).width(180).height(65).pad(130, 20, 20, 0);

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
                skin.getDrawable("panel1"));

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
    // NETWORK DATA REQUESTS
    // =================================================================================
    private void requestServerData() {
        if (!networkService.isConnected()) {
            logger.warn("Not connected to server, cannot request data");
            return;
        }

        PlayerProfile profile = sessionManager.getPlayerProfile();
        int currentUserId = (profile != null) ? profile.id : -1;

        // Request leaderboard (top 50)
        LeaderboardRequest leaderboardReq = new LeaderboardRequest();
        leaderboardReq.limit = 50;
        networkService.sendPacket(leaderboardReq);

        // Request room list (all modes initially)
        requestRoomList();

        // Request match history (limit 5 most recent)
        if (currentUserId > 0) {
            MatchHistoryRequest historyReq = new MatchHistoryRequest(currentUserId, 5);
            networkService.sendPacket(historyReq);

            // Request daily quests
            if (networkService.isConnected()) {
                GetQuestsRequest questsReq = new GetQuestsRequest();
                networkService.sendPacket(questsReq);
                logger.info("Sent GetQuestsRequest for user {}", currentUserId);
            } else {
                logger.warn("Cannot request quests: not connected to server");
            }
        } else {
            logger.warn("Cannot request quests: invalid user ID");
        }

        logger.info("Requested lobby data from server");
    }

    /**
     * Request room list from server (used for auto-refresh)
     */
    private void requestRoomList() {
        if (!networkService.isConnected()) {
            return;
        }

        // Get selected mode from filter
        String selectedMode = modeSelectBox != null ? modeSelectBox.getSelected() : "ALL";
        com.mygame.shared.model.GameType gameType = null;
        if (!"ALL".equals(selectedMode)) {
            try {
                gameType = com.mygame.shared.model.GameType.valueOf(selectedMode);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid game type: {}", selectedMode);
            }
        }

        ListRoomsRequest roomsReq = new ListRoomsRequest(gameType);
        networkService.sendPacket(roomsReq);
    }

    private void refreshLobbyList() {
        if (lobbyListContent == null)
            return;

        lobbyListContent.clearChildren();

        String selectedMode = modeSelectBox != null ? modeSelectBox.getSelected() : "ALL";

        // Filter rooms based on selected mode
        for (RoomInfo room : roomList) {
            // Filter by mode
            if (!selectedMode.equals("ALL")) {
                if (room.getGameType() == null ||
                        !room.getGameType().toString().equals(selectedMode)) {
                    continue;
                }
            }

            // Only show waiting rooms
            if (!"WAITING".equals(room.getStatus())) {
                continue;
            }

            // Skip full rooms
            if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
                continue;
            }

            Table row = new Table();

            // Column 1: Room name
            String roomNameStr = room.getRoomName() != null ? room.getRoomName() : "Room " + room.getRoomId();
            Label roomLbl = new Label(roomNameStr, transparentStyle);
            roomLbl.setFontScale(1.1f);
            roomLbl.setColor(new Color(0, 0.55f, 0.55f, 1)); // Dark cyan

            // Column 2: Player count / Max
            String playerCountStr = room.getCurrentPlayers() + "/" + room.getMaxPlayers();
            Label playerCountLbl = new Label(playerCountStr, transparentStyle);
            playerCountLbl.setFontScale(1.1f);
            playerCountLbl.setColor(new Color(0, 0.55f, 0.55f, 1)); // Dark cyan

            // Column 3: Game mode (POKER or TIENLEN)
            String modeNameStr = (room.getGameType() != null) ? room.getGameType().toString() : "UNKNOWN";
            Label modeName = new Label(modeNameStr, transparentStyle);
            modeName.setFontScale(1.1f);
            modeName.setColor(new Color(0, 0.55f, 0.55f, 1)); // Dark cyan

            // Column 4: Join button
            TextButton joinBtn = new TextButton("JOIN", skin, "blue_text_button");

            final int roomId = room.getRoomId();
            joinBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    joinRoom(roomId);
                }
            });

            row.add(roomLbl).width(200).left();
            row.add(playerCountLbl).width(80).center();
            row.add(modeName).width(150).center();
            row.add(joinBtn).width(80).height(40).right();

            lobbyListContent.add(row).width(600).height(50).padBottom(5).row();
        }

        // Show message if no rooms
        if (lobbyListContent.getChildren().size == 0) {
            Label emptyLabel = new Label("No rooms available", transparentStyle);
            emptyLabel.setColor(Color.GRAY);
            emptyLabel.setFontScale(1.0f);
            lobbyListContent.add(emptyLabel).pad(20);
        }
    }

    private void refreshLeaderboard() {
        if (leaderboardContent == null)
            return;

        leaderboardContent.clearChildren();

        int rank = 1;
        for (LeaderboardEntry entry : leaderboardEntries) {
            Table row = new Table();

            Label rankLbl = new Label(rank + ".", transparentStyle);
            rankLbl.setFontScale(1.1f);

            Label nameLbl = new Label(entry.username != null ? entry.username : "Unknown", transparentStyle);
            nameLbl.setEllipsis(true);
            nameLbl.setFontScale(1.1f);

            Label creditLbl = new Label(formatNumber(entry.credits), goldStyle);
            creditLbl.setAlignment(Align.right);
            creditLbl.setFontScale(1.0f);

            row.add(rankLbl).width(40).left();
            row.add(nameLbl).width(180).left().padRight(5);
            row.add(creditLbl).width(90).right();

            leaderboardContent.add(row).width(300).height(20).row();
            rank++;
        }

        if (leaderboardEntries.isEmpty()) {
            Label emptyLabel = new Label("No data", transparentStyle);
            emptyLabel.setColor(Color.GRAY);
            leaderboardContent.add(emptyLabel).pad(20);
        }
    }

    private void setupNetworkListener() {
        logger.info("Setting up network listener in LobbyController");
        networkService.addPacketListener(packet -> {
            Gdx.app.postRunnable(() -> {
                // Log all received packets for debugging
                Gdx.app.log("LobbyController", "Packet received: " + packet.getClass().getSimpleName());

                if (packet instanceof LeaderboardResponse) {
                    handleLeaderboardResponse((LeaderboardResponse) packet);
                } else if (packet instanceof ListRoomsResponse) {
                    handleRoomsResponse((ListRoomsResponse) packet);
                } else if (packet instanceof MatchHistoryResponse) {
                    handleMatchHistoryResponse((MatchHistoryResponse) packet);
                } else if (packet instanceof DailyRewardResponse) {
                    handleDailyRewardResponse((DailyRewardResponse) packet);
                } else if (packet instanceof GetQuestsResponse) {
                    handleQuestsResponse((GetQuestsResponse) packet);
                } else if (packet instanceof ClaimQuestResponse) {
                    handleClaimQuestResponse((ClaimQuestResponse) packet);
                } else if (packet instanceof JoinRoomResponse) {
                    handleJoinRoomResponse((JoinRoomResponse) packet);
                } else if (packet instanceof CreateRoomResponse) {
                    Gdx.app.log("LobbyController", "CreateRoomResponse detected in listener, calling handler");
                    handleCreateRoomResponse((CreateRoomResponse) packet);
                }
            });
        });
        logger.info("Network listener setup complete");
    }

    private void handleLeaderboardResponse(LeaderboardResponse response) {
        if (response.entries != null) {
            leaderboardEntries.clear();
            leaderboardEntries.addAll(response.entries);
            refreshLeaderboard();
            logger.info("Received {} leaderboard entries", leaderboardEntries.size());
        }
    }

    private void handleRoomsResponse(ListRoomsResponse response) {
        if (response.getRooms() != null) {
            roomList.clear();
            roomList.addAll(response.getRooms());
            refreshLobbyList();
            logger.info("Received {} rooms", roomList.size());
        }
    }

    private void handleMatchHistoryResponse(MatchHistoryResponse response) {
        if (response.entries != null) {
            matchHistoryEntries.clear();
            matchHistoryEntries.addAll(response.entries);
            logger.info("Received {} match history entries", matchHistoryEntries.size());
        }
    }

    private void handleDailyRewardResponse(DailyRewardResponse response) {
        // Daily reward is now handled by quest system
        logger.debug("Daily reward response received: {}", response.success);
    }

    private void handleQuestsResponse(GetQuestsResponse response) {
        if (response.success && response.quests != null && !response.quests.isEmpty()) {
            quests.clear();
            quests.addAll(response.quests);
            // Refresh quest table UI
            refreshQuestTable();
            logger.info("Received {} quests", quests.size());
        } else {
            String errorMsg = response.errorMessage != null ? response.errorMessage : "Unknown error";
            logger.warn("Failed to get quests: {}", errorMsg);

            // Show error message in quest table
            if (questListContent != null) {
                questListContent.clearChildren();

                Label errorTitle = new Label("Failed to load quests", transparentStyle);
                errorTitle.setColor(Color.RED);
                errorTitle.setFontScale(1.0f);
                questListContent.add(errorTitle).padBottom(10).row();

                Label errorDetail = new Label(errorMsg, transparentStyle);
                errorDetail.setColor(Color.ORANGE);
                errorDetail.setFontScale(0.8f);
                errorDetail.setWrap(true);
                questListContent.add(errorDetail).width(250).pad(10);
            }
        }
    }

    private void handleClaimQuestResponse(ClaimQuestResponse response) {
        if (response.success) {
            logger.info("Quest claimed! Awarded {} credits", response.creditsAwarded);
            // Update credits display
            PlayerProfile profile = sessionManager.getPlayerProfile();
            if (profile != null && creditLabel != null) {
                profile.credits = response.newTotalCredits;
                creditLabel.setText(formatNumber(response.newTotalCredits));
            }
            // Refresh quests to update UI
            refreshQuestTable();
            GetQuestsRequest questsReq = new GetQuestsRequest();
            networkService.sendPacket(questsReq);
        } else {
            logger.warn("Failed to claim quest: {}", response.errorMessage);
        }
    }

    private void handleJoinRoomResponse(JoinRoomResponse response) {
        if (response.isSuccess() && response.getRoomInfo() != null) {
            logger.info("Joined room successfully: {}", response.getRoomInfo().getRoomId());
            sessionManager.setPendingRoomInfo(response.getRoomInfo());
            // Navigate to WaitingRoomScreen
            interfaceService.show(com.mygame.client.screen.WaitingRoomScreen.class);
            logger.info("Room joined: {}", response.getRoomInfo().getRoomName());
        } else {
            logger.warn("Failed to join room: {}", response.getErrorMessage());
            // Could show error dialog here
        }
    }

    private void handleCreateRoomResponse(CreateRoomResponse response) {
        Gdx.app.log("LobbyController", "Received CreateRoomResponse - success: " + response.isSuccess());

        if (response.isSuccess() && response.getRoomInfo() != null) {
            logger.info("Room created successfully: {}", response.getRoomInfo().getRoomId());
            Gdx.app.log("LobbyController", "Room created! ID: " + response.getRoomInfo().getRoomId());

            sessionManager.setPendingRoomInfo(response.getRoomInfo());

            // Navigate to WaitingRoomScreen instead of directly to game
            try {
                interfaceService.show(com.mygame.client.screen.WaitingRoomScreen.class);
                logger.info("Navigating to WaitingRoomScreen");
            } catch (Exception e) {
                logger.error("Error navigating to WaitingRoomScreen: {}", e.getMessage(), e);
            }
        } else {
            String errorMsg = response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error";
            logger.error("Failed to create room: {}", errorMsg);
            Gdx.app.log("LobbyController", "Create room failed: " + errorMsg);
        }
    }

    // =================================================================================
    // ACTION METHODS
    // =================================================================================
    private void joinRoom(int roomId) {
        JoinRoomRequest request = new JoinRoomRequest(roomId);
        networkService.sendPacket(request);
        logger.info("Joining room: {}", roomId);
    }

    private void claimQuest(int questId) {
        ClaimQuestRequest request = new ClaimQuestRequest(questId);
        networkService.sendPacket(request);
        logger.info("Claiming quest: {}", questId);
    }

    private void showCreateRoomDialog() {
        // Simple dialog to create room
        Window.WindowStyle winStyle = new Window.WindowStyle(
                skin.getFont("Blue_font"),
                Color.WHITE,
                skin.getDrawable("panel1"));

        final Dialog dialog = new Dialog("Create Room", winStyle);
        dialog.pad(40);

        Table content = dialog.getContentTable();

        // Room Name
        Label nameLabel = new Label("Room Name:", transparentStyle);
        nameLabel.setFontScale(1.0f);
        content.add(nameLabel).left().padBottom(10).row();

        final TextField nameField = new TextField("", skin, "text_field_login");
        nameField.setMessageText("Enter room name");
        content.add(nameField).width(300).height(40).padBottom(15).row();

        // Game Mode
        Label modeLabel = new Label("Mode:", transparentStyle);
        modeLabel.setFontScale(1.0f);
        content.add(modeLabel).left().padBottom(10).row();

        final SelectBox<String> modeBox = new SelectBox<>(skin, "default");
        modeBox.setItems("POKER", "TIENLEN");
        content.add(modeBox).width(300).height(40).padBottom(20).row();

        // Buttons
        Table buttonTable = new Table();

        TextButton createBtn = new TextButton("CREATE", skin, "blue_text_button");
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.log("LobbyController", "Create button clicked!");
                logger.info("Create button clicked in showCreateRoomDialog");

                String roomName = nameField.getText().trim();
                if (roomName.isEmpty()) {
                    roomName = "Room " + System.currentTimeMillis() % 10000;
                }

                String modeStr = modeBox.getSelected();
                GameType gameType = GameType.valueOf(modeStr);

                CreateRoomRequest request = new CreateRoomRequest();
                request.setRoomName(roomName);
                request.setGameType(gameType);
                request.setMaxPlayers(gameType == GameType.POKER ? 5 : 4);

                logger.info("Sending CreateRoomRequest: name={}, type={}, maxPlayers={}",
                        roomName, gameType, request.getMaxPlayers());

                networkService.sendPacket(request);
                dialog.hide();
                logger.info("CreateRoomRequest sent successfully");
            }
        });

        TextButton cancelBtn = new TextButton("CANCEL", skin, "blue_text_button");
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        buttonTable.add(createBtn).width(120).height(50).padRight(10);
        buttonTable.add(cancelBtn).width(120).height(50);

        content.add(buttonTable);

        dialog.show(stage);
    }

    private void showMatchHistoryDialog() {
        // Request fresh data
        PlayerProfile profile = sessionManager.getPlayerProfile();
        int currentUserId = (profile != null) ? profile.id : -1;
        if (currentUserId > 0) {
            MatchHistoryRequest historyReq = new MatchHistoryRequest(currentUserId, 5);
            networkService.sendPacket(historyReq);
        }

        // Create dialog
        Window.WindowStyle winStyle = new Window.WindowStyle(
                skin.getFont("Blue_font"),
                Color.WHITE,
                skin.getDrawable("panel1"));

        final Dialog dialog = new Dialog("Match History", winStyle);
        dialog.pad(40);

        Table content = dialog.getContentTable();

        // Title
        Label title = new Label("RECENT MATCHES", transparentStyle);
        title.setFontScale(1.5f);
        content.add(title).padBottom(20).row();

        // History table
        Table historyTable = new Table();
        historyTable.top().left();

        if (matchHistoryEntries.isEmpty()) {
            Label emptyLabel = new Label("No match history", transparentStyle);
            emptyLabel.setColor(Color.GRAY);
            emptyLabel.setFontScale(1.0f);
            historyTable.add(emptyLabel).pad(20);
        } else {
            // Header row
            Table headerRow = new Table();
            Label resultHeader = new Label("Result", transparentStyle);
            resultHeader.setFontScale(1.0f);
            resultHeader.setColor(Color.BLACK);
            Label modeHeader = new Label("Mode", transparentStyle);
            modeHeader.setFontScale(1.0f);
            modeHeader.setColor(Color.BLACK);
            Label creditsHeader = new Label("Credits", transparentStyle);
            creditsHeader.setFontScale(1.0f);
            creditsHeader.setColor(Color.BLACK);
            Label timeHeader = new Label("Time", transparentStyle);
            timeHeader.setFontScale(1.0f);
            timeHeader.setColor(Color.BLACK);

            headerRow.add(resultHeader).width(100).padRight(10);
            headerRow.add(modeHeader).width(120).padRight(10);
            headerRow.add(creditsHeader).width(100).padRight(10);
            headerRow.add(timeHeader).width(150);
            historyTable.add(headerRow).padBottom(10).row();

            // Data rows
            for (com.mygame.shared.model.MatchHistoryEntry entry : matchHistoryEntries) {
                Table row = new Table();

                // Result (Win/Lose) - Green for Win, Red for Lose
                String resultText = entry.result != null ? entry.result.toUpperCase() : "UNKNOWN";
                Label resultLabel = new Label(resultText, transparentStyle);
                resultLabel.setFontScale(0.9f);
                if ("WIN".equalsIgnoreCase(resultText) || entry.creditsChange > 0) {
                    resultLabel.setColor(Color.GREEN);
                } else if ("LOSE".equalsIgnoreCase(resultText) || entry.creditsChange < 0) {
                    resultLabel.setColor(Color.RED);
                } else {
                    resultLabel.setColor(Color.WHITE);
                }

                // Mode
                String modeText = (entry.gameType != null ? entry.gameType : "UNKNOWN") +
                        (entry.matchMode != null ? " - " + entry.matchMode : "");
                Label modeLabel = new Label(modeText, transparentStyle);
                modeLabel.setFontScale(0.9f);
                modeLabel.setColor(Color.WHITE);

                // Credits change - Green for positive, Red for negative
                String creditsText = (entry.creditsChange >= 0 ? "+" : "") + formatNumber(entry.creditsChange);
                Label creditsLabel = new Label(creditsText, transparentStyle);
                creditsLabel.setFontScale(0.9f);
                creditsLabel.setColor(entry.creditsChange >= 0 ? Color.GREEN : Color.RED);

                // Time
                String timeText = "N/A";
                if (entry.timestamp != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd HH:mm");
                    timeText = sdf.format(entry.timestamp);
                }
                Label timeLabel = new Label(timeText, transparentStyle);
                timeLabel.setFontScale(0.9f);
                timeLabel.setColor(Color.GRAY);

                row.add(resultLabel).width(100).padRight(10).left();
                row.add(modeLabel).width(120).padRight(10).left();
                row.add(creditsLabel).width(100).padRight(10).right();
                row.add(timeLabel).width(150).left();

                historyTable.add(row).padBottom(5).row();
            }
        }

        ScrollPane scroll = new ScrollPane(historyTable, skin);
        scroll.setScrollingDisabled(true, false);
        content.add(scroll).width(500).height(300).padBottom(20).row();

        // Close button
        TextButton closeBtn = new TextButton("CLOSE", skin, "blue_text_button");
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        content.add(closeBtn).width(150).height(40);

        dialog.show(stage);
    }

    // =================================================================================
    // UTILITIES
    // =================================================================================
    private String formatNumber(long number) {
        if (number < 100000) {
            return String.valueOf(number);
        }
        return String.format(Locale.US, "%.2e", (double) number);
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
