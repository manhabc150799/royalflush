package com.mygame.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý tất cả UI resources (TextureAtlas, Textures)
 * Singleton pattern để đảm bảo chỉ có một instance
 * 
 * @author Royal FlushG Team
 */
public class UIResourceManager implements Disposable {
    private static final Logger logger = LoggerFactory.getLogger(UIResourceManager.class);
    
    private static UIResourceManager instance;
    
    // TextureAtlas cho CatUI
    private TextureAtlas catUIAtlas;
    
    // Cache cho raw textures
    private final Map<String, Texture> textureCache = new HashMap<>();
    
    // Paths
    private static final String CAT_UI_ATLAS_PATH = "ui/UICat/catUI.atlas";
    private static final String CAT_UI_IMAGES_PATH = "images/CatUI/";
    private static final String UI_BACKGROUND_PATH = "ui/";
    
    private UIResourceManager() {
        // Private constructor để enforce singleton
    }
    
    /**
     * Lấy instance của UIResourceManager (Singleton)
     */
    public static synchronized UIResourceManager getInstance() {
        if (instance == null) {
            instance = new UIResourceManager();
            instance.initialize();
        }
        return instance;
    }
    
    /**
     * Khởi tạo resources
     */
    private void initialize() {
        try {
            // Load TextureAtlas nếu file tồn tại và không rỗng
            if (Gdx.files.internal(CAT_UI_ATLAS_PATH).exists()) {
                try {
                    catUIAtlas = new TextureAtlas(Gdx.files.internal(CAT_UI_ATLAS_PATH));
                    logger.info("Đã load TextureAtlas từ: {}", CAT_UI_ATLAS_PATH);
                } catch (Exception e) {
                    logger.warn("Không thể load TextureAtlas từ {}, sẽ sử dụng raw textures: {}", 
                            CAT_UI_ATLAS_PATH, e.getMessage());
                    catUIAtlas = null;
                }
            } else {
                logger.info("TextureAtlas không tồn tại tại {}, sẽ sử dụng raw textures", CAT_UI_ATLAS_PATH);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi khởi tạo UIResourceManager: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Lấy TextureRegion từ Atlas theo tên
     * Nếu không tìm thấy trong Atlas, sẽ load từ raw texture
     */
    public com.badlogic.gdx.graphics.g2d.TextureRegion getRegion(String regionName) {
        if (catUIAtlas != null) {
            com.badlogic.gdx.graphics.g2d.TextureRegion region = catUIAtlas.findRegion(regionName);
            if (region != null) {
                return region;
            }
        }
        
        // Fallback: load từ raw texture
        logger.debug("Không tìm thấy region '{}' trong Atlas, load từ raw texture", regionName);
        Texture texture = getTexture(CAT_UI_IMAGES_PATH + regionName + ".png");
        return new com.badlogic.gdx.graphics.g2d.TextureRegion(texture);
    }
    
    /**
     * Lấy Texture từ cache hoặc load mới
     * Thread-safe và có error handling
     */
    public Texture getTexture(String path) {
        if (textureCache.containsKey(path)) {
            return textureCache.get(path);
        }
        
        try {
            Texture texture = new Texture(Gdx.files.internal(path));
            textureCache.put(path, texture);
            logger.debug("Đã load texture: {}", path);
            return texture;
        } catch (Exception e) {
            logger.error("Lỗi khi load texture từ {}: {}", path, e.getMessage(), e);
            // Trả về một texture trống để tránh crash
            return createPlaceholderTexture();
        }
    }
    
    /**
     * Lấy texture từ thư mục CatUI
     */
    public Texture getCatUITexture(String filename) {
        String fullPath = CAT_UI_IMAGES_PATH + filename;
        return getTexture(fullPath);
    }
    
    /**
     * Lấy background texture
     */
    public Texture getBackgroundTexture(String filename) {
        String fullPath = UI_BACKGROUND_PATH + filename;
        return getTexture(fullPath);
    }
    
    /**
     * Tạo placeholder texture khi không load được
     */
    private Texture createPlaceholderTexture() {
        // Tạo texture 1x1 màu đen
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, 
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 1);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    /**
     * Kiểm tra xem Atlas có sẵn không
     */
    public boolean hasAtlas() {
        return catUIAtlas != null;
    }
    
    /**
     * Lấy TextureAtlas (có thể null)
     */
    public TextureAtlas getCatUIAtlas() {
        return catUIAtlas;
    }
    
    /**
     * Clear texture cache (giữ lại atlas)
     */
    public void clearTextureCache() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
        logger.info("Đã clear texture cache");
    }
    
    @Override
    public void dispose() {
        logger.info("Đang dispose UIResourceManager...");
        
        // Dispose textures
        clearTextureCache();
        
        // Dispose atlas
        if (catUIAtlas != null) {
            catUIAtlas.dispose();
            catUIAtlas = null;
        }
        
        instance = null;
        logger.info("Đã dispose UIResourceManager");
    }
}

