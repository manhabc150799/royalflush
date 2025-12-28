package com.mygame.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UIHelper for Scene2D-based UI creation and management.
 * Provides utilities for creating UI elements using PNG assets.
 */
public class UIHelper {
    private static final Logger logger = LoggerFactory.getLogger(UIHelper.class);
    
    /**
     * Creates a background image actor from a PNG file
     */
    public static Image createBackground(String assetPath) {
        try {
            Texture texture = new Texture(Gdx.files.internal(assetPath));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegion region = new TextureRegion(texture);
            Image image = new Image(new TextureRegionDrawable(region));
            image.setFillParent(true);
            return image;
        } catch (Exception e) {
            logger.error("Failed to load background: {}", assetPath, e);
            return null;
        }
    }
    
    /**
     * Creates a centered table container
     */
    public static Table createCenteredTable() {
        Table table = new Table();
        table.setFillParent(true);
        table.center();
        return table;
    }
    
    /**
     * Creates an image actor from a PNG file
     */
    public static Image createImage(String assetPath) {
        try {
            Texture texture = new Texture(Gdx.files.internal(assetPath));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegion region = new TextureRegion(texture);
            return new Image(new TextureRegionDrawable(region));
        } catch (Exception e) {
            logger.error("Failed to load image: {}", assetPath, e);
            return null;
        }
    }
    
    /**
     * Creates a toggle button with 2-state PNG animation
     */
    public static ToggleButtonActor createToggleButton(String state1Path, String state2Path, float width, float height) {
        return new ToggleButtonActor(state1Path, state2Path, width, height);
    }
    
    /**
     * Adds actor to stage and centers it
     */
    public static void centerActor(Stage stage, Actor actor, float x, float y) {
        actor.setPosition(x - actor.getWidth() / 2, y - actor.getHeight() / 2, Align.center);
        stage.addActor(actor);
    }
    
    /**
     * Creates a container group for organizing UI elements
     */
    public static Group createContainer(float x, float y, float width, float height) {
        Group container = new Group();
        container.setBounds(x, y, width, height);
        return container;
    }
    
    /**
     * Disposes texture resources
     */
    public static void disposeTexture(Texture texture) {
        if (texture != null) {
            texture.dispose();
        }
    }
    
    /**
     * Creates a full-screen background group
     */
    public static Group createFullScreenBackground(Stage stage, String backgroundPath) {
        Group backgroundGroup = new Group();
        Image background = createBackground(backgroundPath);
        if (background != null) {
            backgroundGroup.addActor(background);
            stage.addActor(backgroundGroup);
        }
        return backgroundGroup;
    }
    
    /**
     * Creates a Cat UI button with listener from individual PNG files
     * Uses naming convention: buttonName1.png (up) and buttonName2.png (down)
     * Example: btn_play1.png and btn_play2.png for "btn_play"
     */
    public static ImageButton createCatUIButtonWithListener(String buttonName, Runnable onClick) {
        try {
            // Try individual PNG files first (buttonName1.png and buttonName2.png)
            String upPath = "images/CatUI/" + buttonName + "1.png";
            String downPath = "images/CatUI/" + buttonName + "2.png";
            
            // Check if files exist, if not try alternative naming
            if (!Gdx.files.internal(upPath).exists()) {
                // Try with _up and _down suffix
                upPath = "images/CatUI/" + buttonName + "_up.png";
                downPath = "images/CatUI/" + buttonName + "_down.png";
            }
            
            // If still not found, try just buttonName.png
            if (!Gdx.files.internal(upPath).exists()) {
                upPath = "images/CatUI/" + buttonName + ".png";
                downPath = upPath; // Use same for both states
            }
            
            return createImageButtonWithListener(upPath, downPath, onClick);
        } catch (Exception e) {
            logger.error("Failed to create Cat UI button: {}", buttonName, e);
            return null;
        }
    }
    
    /**
     * Creates a Cat UI panel/background from individual PNG file
     * Uses naming convention: panel1.png, panel2.png, etc.
     */
    public static Image createCatUIPanel(String panelName) {
        try {
            String panelPath = "images/CatUI/" + panelName + ".png";
            return createImage(panelPath);
        } catch (Exception e) {
            logger.error("Failed to create Cat UI panel: {}", panelName, e);
            return null;
        }
    }
    
    /**
     * Creates a card image from individual PNG file
     * Uses Card.getAssetPath() for path resolution
     */
    public static Image createCardImage(com.mygame.shared.game.card.Card card) {
        String assetPath = card.getAssetPath();
        return createImage(assetPath);
    }
    
    /**
     * Creates a card back image from individual PNG file
     */
    public static Image createCardBackImage() {
        String assetPath = com.mygame.shared.game.card.Card.getBackAssetPath();
        return createImage(assetPath);
    }
    
    /**
     * Creates an image button with up/down states and listener
     */
    public static ImageButton createImageButtonWithListener(String upPath, String downPath, Runnable onClick) {
        try {
            Texture upTexture = new Texture(Gdx.files.internal(upPath));
            Texture downTexture = new Texture(Gdx.files.internal(downPath));
            
            upTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            downTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            style.imageUp = new TextureRegionDrawable(new TextureRegion(upTexture));
            style.imageDown = new TextureRegionDrawable(new TextureRegion(downTexture));
            
            ImageButton button = new ImageButton(style);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                    if (onClick != null) {
                        onClick.run();
                    }
                }
            });
            
            return button;
        } catch (Exception e) {
            logger.error("Failed to create image button: {} / {}", upPath, downPath, e);
            return null;
        }
    }
    
    /**
     * Loads a FreeType font from assets
     */
    public static BitmapFont loadFont(String fontPath, int size, Color color) {
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.color = color;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            BitmapFont font = generator.generateFont(parameter);
            generator.dispose();
            return font;
        } catch (Exception e) {
            logger.error("Failed to load font: {}", fontPath, e);
            return new BitmapFont(); // Fallback to default
        }
    }
    
    /**
     * Creates a TextField with nine patch background from CatUI
     */
    public static TextField createTextFieldWithNinePatch(String ninePatchPath, BitmapFont font, String messageText) {
        try {
            Texture texture = new Texture(Gdx.files.internal(ninePatchPath));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            // Nine patch with proper padding (left, right, top, bottom)
            NinePatch ninePatch = new NinePatch(new TextureRegion(texture), 8, 8, 8, 8);
            NinePatchDrawable background = new NinePatchDrawable(ninePatch);
            
            TextField.TextFieldStyle style = new TextField.TextFieldStyle();
            style.font = font;
            style.fontColor = Color.WHITE;
            style.focusedFontColor = new Color(1f, 0.81f, 0.9f, 1f); // Pink when focused
            style.background = background;
            style.focusedBackground = background; // Same background when focused
            style.messageFont = font;
            style.messageFontColor = new Color(1f, 1f, 1f, 0.6f);
            
            // Create simple cursor (white rectangle) - use default cursor
            // LibGDX will create a default cursor if not provided
            style.cursor = null; // Let LibGDX use default cursor
            
            TextField textField = new TextField("", style);
            textField.setMessageText(messageText);
            return textField;
        } catch (Exception e) {
            logger.error("Failed to create text field with nine patch: {}", ninePatchPath, e);
            // Fallback to default
            TextField.TextFieldStyle style = new TextField.TextFieldStyle();
            style.font = font;
            style.fontColor = Color.WHITE;
            return new TextField("", style);
        }
    }
    
    /**
     * Creates an ImageButton with text label overlay
     */
    public static Table createImageButtonWithText(String buttonName, String buttonText, BitmapFont font, Runnable onClick) {
        ImageButton button = createCatUIButtonWithListener(buttonName, onClick);
        if (button == null) {
            return null;
        }
        
        Table buttonTable = new Table();
        buttonTable.add(button);
        
        // Add text label on top
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        Label textLabel = new Label(buttonText, labelStyle);
        textLabel.setAlignment(Align.center);
        
        Table container = new Table();
        container.add(button);
        container.row();
        container.add(textLabel).padTop(-button.getHeight() / 2 - 5);
        
        return container;
    }
}
