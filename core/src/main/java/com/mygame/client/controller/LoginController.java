package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import com.mygame.client.service.NetworkService;
import com.mygame.client.service.PacketListener;
import com.mygame.client.service.SessionManager;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.network.packets.LoginRequest;
import com.mygame.shared.network.packets.LoginResponse;
import com.mygame.shared.network.packets.RegisterRequest;
import com.mygame.shared.network.packets.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Professional Login/Register Screen with Scene2D.
 * Features:
 * - Stack-based layering (background, overlay, UI)
 * - Smooth transitions between login/register modes
 * - Uses uiskin.json for consistent styling
 * - High contrast overlay for readability
 * - Professional animations and UX
 * 
 * @author Royal FlushG Team
 */
@Component
@View(id = "login", value = "ui/templates/login.lml")
public class LoginController implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Inject
    private NetworkService networkService;
    @Inject
    private SessionManager sessionManager;
    @Inject
    private InterfaceService interfaceService;

    // UI Components
    private Stage stage;
    private Skin uiSkin;
    private Texture backgroundTexture;

    // Form containers
    private Stack rootStack;
    private Table loginTable;
    private Table registerTable;
    private Stack formStack; // Stack to toggle between login and register

    // Login fields
    private TextField loginUsernameField;
    private TextField loginPasswordField;
    private Label loginErrorLabel;

    // Register fields
    private TextField registerUsernameField;
    private TextField registerPasswordField;
    private TextField registerConfirmPasswordField;
    private Label registerErrorLabel;

    // State
    private boolean uiInitialized = false;
    private boolean isLoginMode = true;

    // Constants
    private static final String BACKGROUND_PATH = "ui/Background_login.png";
    private static final float OVERLAY_OPACITY = 0.5f; // 50% opacity for less contrast
    private static final float FORM_WIDTH = 420f;
    private static final float PANEL_PADDING = 30f; // Padding around the forms container
    private static final float TRANSITION_DURATION = 0.25f; // Animation duration

    public LoginController() {
        // Default constructor for Autumn MVC
    }

    /**
     * Setup the complete UI with Stack-based layering
     */
    private void setupUI() {
        // Set FitViewport for responsive UI scaling
        stage.setViewport(new FitViewport(1920, 1080));
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        stage.clear();

        // Load UI skin
        uiSkin = UISkinManager.getInstance().getSkin();
        if (uiSkin == null) {
            logger.error("Failed to load UI skin, UI may not render correctly");
            return;
        }

        // Load background texture
        try {
            backgroundTexture = new Texture(Gdx.files.internal(BACKGROUND_PATH));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            logger.error("Failed to load background: {}", BACKGROUND_PATH, e);
        }

        // Create root Stack for layering
        rootStack = new Stack();
        rootStack.setFillParent(true);
        stage.addActor(rootStack);

        // Layer 1: Background Image
        if (backgroundTexture != null) {
            Image bgImage = new Image(new TextureRegionDrawable(new TextureRegion(backgroundTexture)));
            bgImage.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            bgImage.setFillParent(true);
            rootStack.add(bgImage);
        }

        // Layer 2: Interactive UI Forms (overlay will be added as background to forms
        // container)
        createForms();
    }

    /**
     * Create both login and register forms with toggle behavior
     */
    private void createForms() {
        // Create main container table
        Table mainContainer = new Table();
        mainContainer.setFillParent(true);
        mainContainer.center();

        // Create container for forms with overlay background
        Table formsContainer = new Table();
        formsContainer.defaults().pad(PANEL_PADDING);

        // Set overlay background on forms container (box background)
        formsContainer.setBackground(createColorDrawable(0, 0, 0, OVERLAY_OPACITY));

        // Create form stack to toggle between login and register
        formStack = new Stack();

        // Create login form
        loginTable = createLoginForm();
        formStack.add(loginTable);

        // Create register form
        registerTable = createRegisterForm();
        formStack.add(registerTable);
        registerTable.setVisible(false); // Initially hidden

        // Add form stack to container
        formsContainer.add(formStack).width(FORM_WIDTH);

        // Add forms container to main container
        mainContainer.add(formsContainer);

        // Add main container to root stack
        rootStack.add(mainContainer);
    }

    /**
     * Create the login form
     */
    private Table createLoginForm() {
        Table form = new Table();
        form.setFillParent(false);
        form.defaults().pad(8f);

        // Username field
        loginUsernameField = new TextField("", uiSkin, "text_field_login");
        loginUsernameField.setMessageText("Username");
        loginUsernameField.setAlignment(Align.center);
        form.add(loginUsernameField).width(FORM_WIDTH - 20f).height(45f).padBottom(12f).row();

        // Password field
        loginPasswordField = new TextField("", uiSkin, "text_field_login");
        loginPasswordField.setMessageText("Password");
        loginPasswordField.setPasswordMode(true);
        loginPasswordField.setPasswordCharacter('*');
        loginPasswordField.setAlignment(Align.center);
        form.add(loginPasswordField).width(FORM_WIDTH - 20f).height(45f).padBottom(12f).row();

        // Error label (initially hidden)
        loginErrorLabel = new Label("", uiSkin, "default");
        loginErrorLabel.setAlignment(Align.center);
        loginErrorLabel.setColor(1f, 0.3f, 0.3f, 1f); // Red color
        loginErrorLabel.setVisible(false);
        form.add(loginErrorLabel).padBottom(8f).row();

        // Login button
        TextButton loginBtn = new TextButton("LOGIN", uiSkin, "blue_text_button");
        loginBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleLogin();
            }
        });
        form.add(loginBtn).width(FORM_WIDTH - 20f).height(50f).padTop(10f).row();

        // Switch to register button
        TextButton switchToRegisterBtn = new TextButton("Create Account", uiSkin, "blue_text_button");
        switchToRegisterBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchToRegister();
            }
        });
        form.add(switchToRegisterBtn).width(FORM_WIDTH - 20f).height(45f).padTop(8f).row();

        return form;
    }

    /**
     * Create the register form
     */
    private Table createRegisterForm() {
        Table form = new Table();
        form.setFillParent(false);
        form.defaults().pad(8f);

        // Username field
        registerUsernameField = new TextField("", uiSkin, "text_field_login");
        registerUsernameField.setMessageText("Username");
        registerUsernameField.setAlignment(Align.center);
        form.add(registerUsernameField).width(FORM_WIDTH - 20f).height(45f).padBottom(12f).row();

        // Password field
        registerPasswordField = new TextField("", uiSkin, "text_field_login");
        registerPasswordField.setMessageText("Password");
        registerPasswordField.setPasswordMode(true);
        registerPasswordField.setPasswordCharacter('*');
        registerPasswordField.setAlignment(Align.center);
        form.add(registerPasswordField).width(FORM_WIDTH - 20f).height(45f).padBottom(12f).row();

        // Confirm password field
        registerConfirmPasswordField = new TextField("", uiSkin, "text_field_login");
        registerConfirmPasswordField.setMessageText("Confirm Password");
        registerConfirmPasswordField.setPasswordMode(true);
        registerConfirmPasswordField.setPasswordCharacter('*');
        registerConfirmPasswordField.setAlignment(Align.center);
        form.add(registerConfirmPasswordField).width(FORM_WIDTH - 20f).height(45f).padBottom(12f).row();

        // Error label (initially hidden)
        registerErrorLabel = new Label("", uiSkin, "default");
        registerErrorLabel.setAlignment(Align.center);
        registerErrorLabel.setColor(1f, 0.3f, 0.3f, 1f); // Red color
        registerErrorLabel.setVisible(false);
        form.add(registerErrorLabel).padBottom(8f).row();

        // Register button
        TextButton registerBtn = new TextButton("REGISTER", uiSkin, "blue_text_button");
        registerBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleRegister();
            }
        });
        form.add(registerBtn).width(FORM_WIDTH - 20f).height(50f).padTop(10f).row();

        // Back to login button
        TextButton backToLoginBtn = new TextButton("Back to Login", uiSkin, "blue_text_button");
        backToLoginBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchToLogin();
            }
        });
        form.add(backToLoginBtn).width(FORM_WIDTH - 20f).height(45f).padTop(8f).row();

        return form;
    }

    /**
     * Create a colored drawable for overlay
     */
    private TextureRegionDrawable createColorDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Switch to register form with animation
     */
    private void switchToRegister() {
        if (!isLoginMode)
            return;

        isLoginMode = false;

        // Fade out login, fade in register
        loginTable.addAction(Actions.sequence(
                Actions.fadeOut(TRANSITION_DURATION),
                Actions.run(() -> {
                    loginTable.setVisible(false);
                    registerTable.setVisible(true);
                    registerTable.getColor().a = 0f;
                    registerTable.addAction(Actions.fadeIn(TRANSITION_DURATION));
                })));
    }

    /**
     * Switch to login form with animation
     */
    private void switchToLogin() {
        if (isLoginMode)
            return;

        isLoginMode = true;

        // Fade out register, fade in login
        registerTable.addAction(Actions.sequence(
                Actions.fadeOut(TRANSITION_DURATION),
                Actions.run(() -> {
                    registerTable.setVisible(false);
                    loginTable.setVisible(true);
                    loginTable.getColor().a = 0f;
                    loginTable.addAction(Actions.fadeIn(TRANSITION_DURATION));
                })));
    }

    /**
     * Action methods for LML templates (backward compatibility)
     */
    public void attemptLogin() {
        handleLogin();
    }

    public void attemptRegister() {
        handleRegister();
    }

    /**
     * Handle login button click
     */
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showLoginError("Please enter username and password");
            return;
        }

        // Ensure we're trying to connect if not connected
        if (!networkService.isConnected() && !networkService.isConnecting()) {
            logger.info("Not connected, attempting to connect to server...");
            networkService.connect("localhost", 54555, 54777);
        }

        // Show status message if connecting
        if (networkService.isConnecting() && !networkService.isConnected()) {
            showLoginError("Connecting to server, please wait...");
        } else if (!networkService.isConnected()) {
            showLoginError("Server connection failed. Please check if server is running.");
            return;
        } else {
            hideLoginError();
        }

        // Send request - NetworkService will queue it if not connected yet
        LoginRequest request = new LoginRequest();
        request.username = username;
        request.password = password;

        networkService.sendPacket(request);
        logger.info("Login request sent/queued for user: {}", username);
    }

    /**
     * Handle register button click
     */
    private void handleRegister() {
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showRegisterError("Please fill all fields");
            return;
        }

        if (password.length() < 6) {
            showRegisterError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showRegisterError("Passwords do not match");
            return;
        }

        // Ensure we're trying to connect if not connected
        if (!networkService.isConnected() && !networkService.isConnecting()) {
            logger.info("Not connected, attempting to connect to server...");
            networkService.connect("localhost", 54555, 54777);
        }

        // Show status message if connecting
        if (networkService.isConnecting() && !networkService.isConnected()) {
            showRegisterError("Connecting to server, please wait...");
        } else if (!networkService.isConnected()) {
            showRegisterError("Server connection failed. Please check if server is running.");
            return;
        } else {
            hideRegisterError();
        }

        // Send request - NetworkService will queue it if not connected yet
        // This ensures the user data is saved to database when connection is
        // established
        RegisterRequest request = new RegisterRequest();
        request.username = username;
        request.password = password;

        networkService.sendPacket(request);
        logger.info("Register request sent/queued for user: {} (will save to database when connected)", username);
    }

    /**
     * Setup network packet listeners
     */
    private void setupNetworkListener() {
        networkService.addListener(new PacketListener() {
            @Override
            public void onPacketReceived(Object packet) {
                Gdx.app.postRunnable(() -> {
                    if (packet instanceof LoginResponse) {
                        handleLoginResponse((LoginResponse) packet);
                    } else if (packet instanceof RegisterResponse) {
                        handleRegisterResponse((RegisterResponse) packet);
                    }
                });
            }

            @Override
            public void onConnected() {
                Gdx.app.postRunnable(() -> {
                    logger.info("Connected to server - ready for login/register");
                    // Clear any connection error messages
                    if (loginErrorLabel != null && loginErrorLabel.getText().toString().contains("Not connected")) {
                        hideLoginError();
                    }
                    if (registerErrorLabel != null
                            && registerErrorLabel.getText().toString().contains("Not connected")) {
                        hideRegisterError();
                    }
                });
            }

            @Override
            public void onDisconnected() {
                Gdx.app.postRunnable(() -> {
                    logger.warn("Disconnected from server");
                });
            }

            @Override
            public void onConnectionFailed(String message) {
                Gdx.app.postRunnable(() -> {
                    logger.error("Connection failed: {}", message);
                    if (isLoginMode) {
                        showLoginError("Cannot connect to server. Is server running?");
                    } else {
                        showRegisterError("Cannot connect to server. Is server running?");
                    }
                });
            }
        });
    }

    /**
     * Handle login response from server
     */
    private void handleLoginResponse(LoginResponse response) {
        if (response.success) {
            sessionManager.setPlayerProfile(response.playerProfile);
            hideLoginError();
            logger.info("Login successful for user: {}", response.playerProfile.username);

            // Navigate to lobby screen
            interfaceService.show(LobbyController.class);
        } else {
            showLoginError(response.errorMessage != null ? response.errorMessage : "Login failed");
        }
    }

    /**
     * Handle register response from server
     */
    private void handleRegisterResponse(RegisterResponse response) {
        if (response.success) {
            sessionManager.setPlayerProfile(response.playerProfile);
            hideRegisterError();
            showRegisterSuccess("Account created successfully! Please log in.");
            // Switch back to login form
            switchToLogin();
            logger.info("Registration successful for user: {}", response.playerProfile.username);
        } else {
            showRegisterError(response.errorMessage != null ? response.errorMessage : "Registration failed");
        }
    }

    /**
     * Show error message in login form
     */
    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
        loginErrorLabel.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.2f)));
    }

    /**
     * Hide error message in login form
     */
    private void hideLoginError() {
        loginErrorLabel.setVisible(false);
    }

    /**
     * Show error message in register form
     */
    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
        registerErrorLabel.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.2f)));
    }

    /**
     * Hide error message in register form
     */
    private void hideRegisterError() {
        registerErrorLabel.setVisible(false);
    }

    private void showRegisterSuccess(String message) {
        registerErrorLabel.setColor(0.2f, 1f, 0.2f, 1f); // green
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
        registerErrorLabel.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.2f)));
    }

    @Override
    public void render(Stage stage, float delta) {
        if (!uiInitialized) {
            this.stage = stage;
            setupUI();
            setupNetworkListener();
            uiInitialized = true;

            // Ensure stage can receive keyboard input
            Gdx.input.setInputProcessor(stage);
        }

        // Clear screen
        ScreenUtils.clear(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and render stage
        stage.act(Math.min(delta, 1 / 30f)); // Cap delta to prevent large jumps
        stage.draw();
    }

    /**
     * Handle window resize
     */
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    /**
     * Cleanup resources when controller is no longer needed
     */
    public void dispose() {
        // Dispose resources
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }

        // Note: Don't dispose skin here - it's managed by UISkinManager
        // Note: Don't dispose stage here - it's managed by Autumn MVC

        logger.info("LoginController disposed");
    }
}
