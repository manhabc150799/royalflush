package com.mygame.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the custom UI skin loaded from uiskin.json.
 * Singleton pattern to ensure single instance and proper resource management.
 * 
 * @author Royal FlushG Team
 */
public class UISkinManager implements Disposable {
    private static final Logger logger = LoggerFactory.getLogger(UISkinManager.class);
    
    private static UISkinManager instance;
    private Skin uiSkin;
    
    private static final String UI_SKIN_PATH = "ui/uiskin.json";
    private static final String UI_SKIN_ATLAS_PATH = "ui/uiskin.atlas";
    
    private UISkinManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized UISkinManager getInstance() {
        if (instance == null) {
            instance = new UISkinManager();
            instance.initialize();
        }
        return instance;
    }
    
    /**
     * Initialize and load the UI skin
     */
    private void initialize() {
        try {
            // Load TextureAtlas first (required by skin)
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(UI_SKIN_ATLAS_PATH));
            
            // Load skin with atlas
            uiSkin = new Skin(Gdx.files.internal(UI_SKIN_PATH), atlas);
            
            logger.info("UI Skin loaded successfully from: {}", UI_SKIN_PATH);
        } catch (Exception e) {
            logger.error("Failed to load UI skin from: {}", UI_SKIN_PATH, e);
            // Create fallback empty skin to prevent crashes
            uiSkin = new Skin();
        }
    }
    
    /**
     * Get the UI skin instance
     * @return The loaded skin, or null if not initialized
     */
    public Skin getSkin() {
        if (uiSkin == null) {
            initialize();
        }
        return uiSkin;
    }
    
    /**
     * Check if skin is loaded
     */
    public boolean isLoaded() {
        return uiSkin != null && !uiSkin.getAtlas().getTextures().isEmpty();
    }
    
    @Override
    public void dispose() {
        if (uiSkin != null) {
            uiSkin.dispose();
            uiSkin = null;
        }
        instance = null;
        logger.info("UISkinManager disposed");
    }
}

