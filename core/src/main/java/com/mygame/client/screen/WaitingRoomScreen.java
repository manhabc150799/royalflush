package com.mygame.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.controller.LobbyController;
import com.mygame.client.controller.PokerGameControllerJava;
import com.mygame.client.controller.TienLenGameController;
import com.mygame.client.controller.GameScreenController;
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.client.ui.game.GameScreen;
import com.mygame.shared.model.GameType;
import com.mygame.shared.model.RoomInfo;
import com.mygame.shared.network.packets.*;
import com.mygame.shared.network.packets.game.GameStartPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * WaitingRoomScreen - Professional Scene2D UI for the room waiting lobby.
 * Features:
 * - Room header with name, ID, and game mode
 * - Player list with headers: Avatar, Username, Balance, Status
 * - Start Game button (host only, styled distinctively)
 * - Leave Room button
 * - Dynamic updates via RoomUpdatePacket
 *
 * @author Royal FlushG Team
 */
@Component
@View(id = "waitingRoom", value = "ui/templates/login.lml") // Dummy LML, we build UI programmatically
public class WaitingRoomScreen implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(WaitingRoomScreen.class);

    @Inject
    private NetworkService networkService;
    @Inject
    private SessionManager sessionManager;
    @Inject
    private InterfaceService interfaceService;

    private static final String BACKGROUND_POKER = "ui/Background_poker.png";
    private static final String BACKGROUND_TIENLEN = "ui/Background_tienlen.png";

    // Professional color palette
    private static final Color GOLD = new Color(1f, 0.84f, 0f, 1f);
    private static final Color DARK_GOLD = new Color(0.8f, 0.65f, 0f, 1f);
    private static final Color LIGHT_CREAM = new Color(1f, 0.98f, 0.9f, 1f);
    private static final Color DARK_BROWN = new Color(0.3f, 0.2f, 0.1f, 1f);
    private static final Color SUCCESS_GREEN = new Color(0.2f, 0.8f, 0.3f, 1f);
    private static final Color HOST_PURPLE = new Color(0.7f, 0.4f, 0.9f, 1f);
    private static final Color PANEL_BG = new Color(0.1f, 0.1f, 0.15f, 0.85f);

    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private boolean uiInitialized = false;

    // Current room data
    private RoomInfo currentRoom;
    private int currentUserId;

    // UI Components
    private Label roomTitleLabel;
    private Label roomSubtitleLabel;
    private Table playerListContent;
    private TextButton startGameButton;
    private TextButton leaveButton;

    // Custom styles
    private Label.LabelStyle titleStyle;
    private Label.LabelStyle subtitleStyle;
    private Label.LabelStyle headerStyle;
    private Label.LabelStyle playerNameStyle;
    private Label.LabelStyle balanceStyle;
    private Label.LabelStyle hostBadgeStyle;
    private Label.LabelStyle statusStyle;

    // Number formatter for balance
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);

    // White texture for programmatic drawables
    private Texture whiteTexture;

    public WaitingRoomScreen() {
        // Default constructor for Autumn MVC
    }

    @Override
    public void render(Stage stage, float delta) {
        if (!uiInitialized) {
            this.stage = stage;

            // Get room info from session
            currentRoom = sessionManager.getAndClearPendingRoomInfo();
            currentUserId = sessionManager.getCurrentUserId();

            if (currentRoom == null) {
                logger.error("No room info found, returning to lobby");
                interfaceService.show(LobbyController.class);
                return;
            }

            buildUI();
            setupNetworkListener();

            uiInitialized = true;
            logger.info("WaitingRoomScreen initialized for room: {}", currentRoom.getRoomName());
        }

        // Clear screen
        ScreenUtils.clear(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and render stage
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    /**
     * Build the complete UI with professional styling
     */
    private void buildUI() {
        // Set FitViewport for responsive UI scaling
        stage.setViewport(new FitViewport(1920, 1080));
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        stage.clear();

        // Load skin
        skin = UISkinManager.getInstance().getSkin();
        if (skin == null) {
            logger.error("Failed to load UI skin");
            return;
        }

        // Register white drawable if not present (needed for backgrounding)
        createWhiteTextureIfMissing();

        // Load background based on game type
        loadBackground();

        // Create custom label styles
        createStyles();

        // Root Layout using Stack
        Stack stack = new Stack();
        stack.setFillParent(true);

        // === LAYER 1: Background Image ===
        if (backgroundTexture != null) {
            Image bgImage = new Image(backgroundTexture);
            bgImage.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            bgImage.setFillParent(true);
            stack.add(bgImage);
        }

        // === LAYER 2: Semi-transparent overlay for readability ===
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(createColorDrawable(0, 0, 0, 0.4f));
        stack.add(overlay);

        // === LAYER 3: Main UI Layout ===
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(40);

        // === TOP: Room Header ===
        Table headerSection = createHeaderSection();
        mainTable.add(headerSection).fillX().padBottom(20).row();

        // === CENTER: Player List Panel ===
        Table playerPanel = createPlayerPanel();
        mainTable.add(playerPanel).expand().fill().padBottom(20).row();

        // === BOTTOM: Action Buttons ===
        Table buttonSection = createButtonSection();
        mainTable.add(buttonSection).padTop(10);

        stack.add(mainTable);
        stage.addActor(stack);

        // Update button states
        updateStartButtonState();

        Gdx.input.setInputProcessor(stage);
    }

    private void loadBackground() {
        String bgPath = (currentRoom.getGameType() == GameType.POKER) ? BACKGROUND_POKER : BACKGROUND_TIENLEN;
        try {
            backgroundTexture = new Texture(Gdx.files.internal(bgPath));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            logger.error("Failed to load background: {}", bgPath, e);
        }
    }

    /**
     * Create a 1x1 white texture for use in programmatic drawables
     * This is called once at buildUI() start
     */
    private void createWhiteTextureIfMissing() {
        if (whiteTexture == null) {
            // Create 1x1 white pixmap
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();

            whiteTexture = new Texture(pixmap);
            pixmap.dispose();
            logger.debug("Created white texture for drawables");
        }
    }

    /**
     * Create a tinted drawable from the white texture
     * This is a replacement for skin.newDrawable("white", color) which doesn't work
     */
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createColorDrawable(Color color) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(whiteTexture));
        drawable.setMinWidth(1);
        drawable.setMinHeight(1);
        return drawable.tint(color);
    }

    /**
     * Create a tinted drawable with RGBA values
     */
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createColorDrawable(float r, float g, float b, float a) {
        return createColorDrawable(new Color(r, g, b, a));
    }

    /**
     * Create custom label styles with professional colors
     */
    private void createStyles() {
        // Title style - Gold with large font
        titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("Big_blue_font");
        titleStyle.fontColor = GOLD;

        // Subtitle style - Light cream
        subtitleStyle = new Label.LabelStyle();
        subtitleStyle.font = skin.getFont("Blue_font");
        subtitleStyle.fontColor = LIGHT_CREAM;

        // Header style - Dark gold for table headers
        headerStyle = new Label.LabelStyle();
        headerStyle.font = skin.getFont("Blue_font");
        headerStyle.fontColor = DARK_GOLD;

        // Player name style - White/cream for readability
        playerNameStyle = new Label.LabelStyle();
        playerNameStyle.font = skin.getFont("Blue_font");
        playerNameStyle.fontColor = Color.WHITE;

        // Balance style - Green for money
        balanceStyle = new Label.LabelStyle();
        balanceStyle.font = skin.getFont("Green_font");
        balanceStyle.fontColor = SUCCESS_GREEN;

        // Host badge style - Purple/gold
        hostBadgeStyle = new Label.LabelStyle();
        hostBadgeStyle.font = skin.getFont("Blue_font");
        hostBadgeStyle.fontColor = HOST_PURPLE;

        // Status style
        statusStyle = new Label.LabelStyle();
        statusStyle.font = skin.getFont("Blue_font");
        statusStyle.fontColor = Color.LIGHT_GRAY;
    }

    /**
     * Create the room header section
     */
    private Table createHeaderSection() {
        Table header = new Table();
        header.pad(15);

        // Room name as main title
        String roomName = currentRoom.getRoomName() != null ? currentRoom.getRoomName() : "Room";
        roomTitleLabel = new Label(roomName, titleStyle);
        roomTitleLabel.setFontScale(1.5f);
        roomTitleLabel.setAlignment(Align.center);
        header.add(roomTitleLabel).expandX().center().row();

        // Subtitle with ID and game mode
        String subtitle = "ID: #" + currentRoom.getRoomId() + "  |  " +
                currentRoom.getGameType().name() + "  |  " +
                "Host: " + (currentRoom.getHostUsername() != null ? currentRoom.getHostUsername() : "Unknown");
        roomSubtitleLabel = new Label(subtitle, subtitleStyle);
        roomSubtitleLabel.setFontScale(0.9f);
        roomSubtitleLabel.setAlignment(Align.center);
        header.add(roomSubtitleLabel).expandX().center().padTop(5);

        return header;
    }

    /**
     * Create the player list panel with professional table layout
     */
    private Table createPlayerPanel() {
        Table panel = new Table();
        panel.pad(20);

        // Panel background
        panel.setBackground(createColorDrawable(PANEL_BG));

        // Panel title with player count
        int current = currentRoom.getCurrentPlayers();
        int max = currentRoom.getMaxPlayers();
        int min = getMinPlayers();

        Label panelTitle = new Label("PLAYERS  (" + current + "/" + max + ")", titleStyle);
        panelTitle.setFontScale(1.1f);
        panel.add(panelTitle).left().padBottom(5).row();

        // Min players indicator
        String minText = current >= min ? "Ready to start!" : "Need " + (min - current) + " more player(s)";
        Label minLabel = new Label(minText, statusStyle);
        minLabel.setFontScale(0.8f);
        minLabel.setColor(current >= min ? SUCCESS_GREEN : Color.ORANGE);
        panel.add(minLabel).left().padBottom(15).row();

        // === Table Headers ===
        Table headerRow = new Table();
        headerRow.pad(10);

        Label avatarHeader = new Label("", headerStyle);
        Label nameHeader = new Label("USERNAME", headerStyle);
        nameHeader.setFontScale(0.9f);
        Label balanceHeader = new Label("BALANCE", headerStyle);
        balanceHeader.setFontScale(0.9f);
        Label statusHeader = new Label("STATUS", headerStyle);
        statusHeader.setFontScale(0.9f);

        headerRow.add(avatarHeader).width(60).left();
        headerRow.add(nameHeader).width(200).left();
        headerRow.add(balanceHeader).width(150).center();
        headerRow.add(statusHeader).width(120).center();

        panel.add(headerRow).fillX().padBottom(10).row();

        // Separator line
        Table separator = new Table();
        separator.setBackground(createColorDrawable(DARK_GOLD));
        panel.add(separator).fillX().height(2).padBottom(10).row();

        // Player list with scroll
        playerListContent = new Table();
        playerListContent.top();
        updatePlayerList();

        ScrollPane scroll = new ScrollPane(playerListContent, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);

        panel.add(scroll).expand().fill();

        return panel;
    }

    /**
     * Update the player list content
     */
    private void updatePlayerList() {
        if (playerListContent == null || currentRoom == null)
            return;

        playerListContent.clearChildren();

        List<RoomInfo.RoomPlayerInfo> players = currentRoom.getPlayers();

        if (players == null || players.isEmpty()) {
            Label emptyLabel = new Label("Waiting for players to join...", statusStyle);
            emptyLabel.setFontScale(1.0f);
            playerListContent.add(emptyLabel).pad(30).center();
            return;
        }

        // Display each player
        for (RoomInfo.RoomPlayerInfo player : players) {
            Table row = createPlayerRow(player);
            playerListContent.add(row).fillX().padBottom(8).row();
        }
    }

    /**
     * Create a single player row for the table
     */
    private Table createPlayerRow(RoomInfo.RoomPlayerInfo player) {
        Table row = new Table();
        row.pad(8);

        // Highlight current user's row
        if (player.getUserId() == currentUserId) {
            row.setBackground(createColorDrawable(0.2f, 0.4f, 0.6f, 0.3f));
        }

        // Get player info
        String username = player.getUsername() != null ? player.getUsername() : "Player " + player.getUserId();
        boolean isHost = player.getUserId() == currentRoom.getHostUserId();
        boolean isCurrentUser = player.getUserId() == currentUserId;
        long balance = player.getBalance();

        // === Column 1: Avatar (initials in colored circle) ===
        Table avatarContainer = new Table();
        avatarContainer.setBackground(createColorDrawable(isHost ? HOST_PURPLE : DARK_GOLD));

        String initials = username.length() >= 2 ? username.substring(0, 2).toUpperCase() : username.toUpperCase();
        Label avatarLabel = new Label(initials, playerNameStyle);
        avatarLabel.setFontScale(0.8f);
        avatarLabel.setColor(Color.WHITE);
        avatarContainer.add(avatarLabel).center();

        // === Column 2: Username + Host badge ===
        Table nameCell = new Table();
        Label nameLabel = new Label(username, playerNameStyle);
        nameLabel.setFontScale(1.0f);

        // Highlight current user in gold
        if (isCurrentUser) {
            nameLabel.setColor(GOLD);
        }
        nameCell.add(nameLabel).left();

        if (isHost) {
            Label hostBadge = new Label(" [HOST]", hostBadgeStyle);
            hostBadge.setFontScale(0.85f);
            nameCell.add(hostBadge).left().padLeft(5);
        }

        if (isCurrentUser) {
            Label youLabel = new Label(" (You)", statusStyle);
            youLabel.setFontScale(0.75f);
            youLabel.setColor(Color.LIGHT_GRAY);
            nameCell.add(youLabel).left().padLeft(5);
        }

        // === Column 3: Balance ===
        String balanceText = balance > 0 ? "$" + currencyFormat.format(balance) : "$---";
        Label balanceLabel = new Label(balanceText, balanceStyle);
        balanceLabel.setFontScale(0.95f);
        if (balance <= 0) {
            balanceLabel.setColor(Color.GRAY);
        }

        // === Column 4: Status ===
        String statusText = isHost ? "Host" : "Ready";
        Label statusLabel = new Label(statusText, statusStyle);
        statusLabel.setFontScale(0.85f);
        statusLabel.setColor(SUCCESS_GREEN);

        // Add all columns
        row.add(avatarContainer).size(50, 50).left().padRight(15);
        row.add(nameCell).width(200).left();
        row.add(balanceLabel).width(150).center();
        row.add(statusLabel).width(120).center();

        return row;
    }

    /**
     * Create the action buttons section
     */
    private Table createButtonSection() {
        Table buttons = new Table();
        buttons.pad(10);

        // Leave Button - Regular style
        leaveButton = new TextButton("LEAVE ROOM", skin, "blue_text_button");
        leaveButton.getLabel().setFontScale(1.2f);
        leaveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                leaveRoom();
            }
        });

        // Start Game Button - Larger, more prominent
        startGameButton = new TextButton("START GAME", skin, "blue_text_button");
        startGameButton.getLabel().setFontScale(1.4f);
        startGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startGame();
            }
        });

        buttons.add(leaveButton).width(200).height(60).padRight(40);
        buttons.add(startGameButton).width(280).height(70);

        return buttons;
    }

    /**
     * Setup network listener for room updates
     */
    private void setupNetworkListener() {
        networkService.addPacketListener(packet -> {
            Gdx.app.postRunnable(() -> {
                if (packet instanceof RoomUpdatePacket) {
                    handleRoomUpdate((RoomUpdatePacket) packet);
                } else if (packet instanceof GameStartPacket) {
                    handleGameStart((GameStartPacket) packet);
                } else if (packet instanceof KickPacket) {
                    handleKick((KickPacket) packet);
                } else if (packet instanceof StartGameResponse) {
                    handleStartGameResponse((StartGameResponse) packet);
                }
            });
        });
    }

    /**
     * Handle room update from server
     */
    private void handleRoomUpdate(RoomUpdatePacket packet) {
        if (packet.getRoomInfo() == null)
            return;

        int packetRoomId = packet.getRoomInfo().getRoomId();
        int currentRoomId = (currentRoom != null) ? currentRoom.getRoomId() : -1;

        if (packetRoomId == currentRoomId) {
            currentRoom = packet.getRoomInfo();
            updatePlayerList();
            updateStartButtonState();
            updateRoomHeader();
            logger.debug("Room updated: {} players", currentRoom.getCurrentPlayers());
        }
    }

    /**
     * Update room header labels
     */
    private void updateRoomHeader() {
        if (roomTitleLabel != null && currentRoom != null) {
            String roomName = currentRoom.getRoomName() != null ? currentRoom.getRoomName() : "Room";
            roomTitleLabel.setText(roomName);
        }
        if (roomSubtitleLabel != null && currentRoom != null) {
            String subtitle = "ID: #" + currentRoom.getRoomId() + "  |  " +
                    currentRoom.getGameType().name() + "  |  " +
                    "Host: " + (currentRoom.getHostUsername() != null ? currentRoom.getHostUsername() : "Unknown");
            roomSubtitleLabel.setText(subtitle);
        }
    }

    /**
     * Handle game start packet - transition to GameScreen via Autumn MVC
     */
    private void handleGameStart(GameStartPacket packet) {
        if (currentRoom == null || packet.getRoomId() != currentRoom.getRoomId())
            return;

        logger.info("Game starting! Type: {}", packet.getGameType());

        // Store game parameters in session for GameScreenController to retrieve
        Gdx.app.postRunnable(() -> {
            try {
                // Set pending game data in session
                sessionManager.setPendingGameType(packet.getGameType());
                sessionManager.setPendingRoomId(packet.getRoomId());
                sessionManager.setPendingRoomInfo(currentRoom); // Pass RoomInfo for player usernames
                sessionManager.setPendingGameStartPacket(packet); // CRITICAL: Save init state

                // Navigate via Autumn MVC - GameScreenController wraps pure Java GameScreen
                interfaceService.show(GameScreenController.class);

                logger.info("Navigated to GameScreenController for {} game", packet.getGameType());
            } catch (Exception e) {
                logger.error("Failed to switch to GameScreen: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Handle kick packet (bankruptcy etc.)
     */
    private void handleKick(KickPacket packet) {
        if (currentRoom == null || packet.getRoomId() != currentRoom.getRoomId())
            return;

        String message = getKickMessage(packet.getReason());
        showKickDialog(message);
    }

    private String getKickMessage(String reason) {
        if (KickPacket.REASON_BANKRUPT.equals(reason)) {
            return "You ran out of money!";
        } else if (KickPacket.REASON_DISCONNECTED.equals(reason)) {
            return "You were disconnected.";
        } else if (KickPacket.REASON_HOST_KICK.equals(reason)) {
            return "You were kicked by the host.";
        }
        return "You have been removed from the room.";
    }

    /**
     * Show kick dialog and return to lobby
     */
    private void showKickDialog(String message) {
        Window.WindowStyle winStyle = new Window.WindowStyle(
                skin.getFont("Blue_font"),
                Color.WHITE,
                skin.getDrawable("panel1"));

        final Dialog dialog = new Dialog("", winStyle);
        dialog.pad(40);

        Table content = dialog.getContentTable();

        Label title = new Label("Notice", titleStyle);
        title.setFontScale(1.2f);
        content.add(title).padBottom(20).row();

        Label msgLabel = new Label(message, subtitleStyle);
        msgLabel.setFontScale(1.0f);
        msgLabel.setWrap(true);
        msgLabel.setAlignment(Align.center);
        content.add(msgLabel).width(300).padBottom(20).row();

        TextButton okBtn = new TextButton("OK", skin, "blue_text_button");
        okBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
                interfaceService.show(LobbyController.class);
            }
        });
        content.add(okBtn).width(120).height(50);

        dialog.show(stage);
    }

    /**
     * Handle start game response
     */
    private void handleStartGameResponse(StartGameResponse response) {
        if (!response.isSuccess()) {
            showErrorDialog(response.getErrorMessage());
        }
    }

    /**
     * Show error dialog
     */
    private void showErrorDialog(String message) {
        Window.WindowStyle winStyle = new Window.WindowStyle(
                skin.getFont("Blue_font"),
                Color.WHITE,
                skin.getDrawable("panel1"));

        final Dialog dialog = new Dialog("", winStyle);
        dialog.pad(30);

        Table content = dialog.getContentTable();

        Label title = new Label("Error", titleStyle);
        title.setFontScale(1.1f);
        title.setColor(Color.RED);
        content.add(title).padBottom(15).row();

        Label msgLabel = new Label(message != null ? message : "An error occurred", subtitleStyle);
        msgLabel.setFontScale(0.95f);
        msgLabel.setWrap(true);
        content.add(msgLabel).width(280).padBottom(15).row();

        TextButton okBtn = new TextButton("OK", skin, "blue_text_button");
        okBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        content.add(okBtn).width(100).height(45);

        dialog.show(stage);
    }

    /**
     * Update start button enabled state
     */
    private void updateStartButtonState() {
        if (startGameButton == null || currentRoom == null)
            return;

        boolean isHost = currentRoom.getHostUserId() == currentUserId;
        int minPlayers = getMinPlayers();
        boolean hasEnoughPlayers = currentRoom.getCurrentPlayers() >= minPlayers;
        boolean isWaiting = "WAITING".equals(currentRoom.getStatus());

        boolean canStart = isHost && hasEnoughPlayers && isWaiting;
        startGameButton.setDisabled(!canStart);

        // Visual feedback
        if (canStart) {
            startGameButton.setColor(SUCCESS_GREEN);
            startGameButton.setText("START GAME");
        } else if (!isHost) {
            startGameButton.setColor(0.4f, 0.4f, 0.4f, 0.8f);
            startGameButton.setText("WAITING FOR HOST...");
        } else if (!hasEnoughPlayers) {
            startGameButton.setColor(Color.ORANGE);
            startGameButton.setText("NEED " + minPlayers + "+ PLAYERS");
        } else {
            startGameButton.setColor(0.5f, 0.5f, 0.5f, 0.7f);
            startGameButton.setText("PLEASE WAIT");
        }
    }

    private int getMinPlayers() {
        // Uniform minimum of 2 players
        return 2;
    }

    /**
     * Leave the current room
     */
    private void leaveRoom() {
        if (currentRoom == null)
            return;

        LeaveRoomRequest request = new LeaveRoomRequest(currentRoom.getRoomId());
        networkService.sendPacket(request);

        logger.info("Leaving room: {}", currentRoom.getRoomId());
        interfaceService.show(LobbyController.class);
    }

    /**
     * Request to start the game (host only)
     */
    private void startGame() {
        if (currentRoom == null)
            return;

        if (currentRoom.getHostUserId() != currentUserId) {
            logger.warn("Only host can start the game");
            return;
        }

        int minPlayers = getMinPlayers();
        if (currentRoom.getCurrentPlayers() < minPlayers) {
            showErrorDialog("Need at least " + minPlayers + " players to start");
            return;
        }

        StartGameRequest request = new StartGameRequest(currentRoom.getRoomId());
        networkService.sendPacket(request);

        logger.info("Requesting game start for room: {}", currentRoom.getRoomId());
    }

    /**
     * Cleanup when screen is hidden
     */
    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        if (whiteTexture != null) {
            whiteTexture.dispose();
            whiteTexture = null;
        }
        uiInitialized = false;
    }
}
