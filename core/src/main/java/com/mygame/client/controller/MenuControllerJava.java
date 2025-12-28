package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.RoyalFlushG;
import com.mygame.client.ui.UIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MenuController viết bằng Java thuần, không sử dụng LML templates
 * Sử dụng UIHelper và UIResourceManager để tạo UI components
 * 
 * @author Royal FlushG Team
 */
@View(id = "menuJava", value = "ui/templates/menu_java.lml", themes = "music/theme.ogg")
public class MenuControllerJava implements ViewRenderer {
    private static final Logger logger = LoggerFactory.getLogger(MenuControllerJava.class);
    
    @Inject private InterfaceService interfaceService;
    
    private Stage stage;
    private Viewport viewport;
    private UIHelper uiHelper;
    
    // UI Components
    private Image backgroundImage;
    private Table mainTable;
    private Label titleLabel;
    private ImageButton startButton;
    private ImageButton exitButton;
    
    /**
     * Khởi tạo UI khi view được show lần đầu
     */
    public void initialize() {
        logger.info("Khởi tạo MenuControllerJava...");
        
        // Khởi tạo viewport
        viewport = new FitViewport(RoyalFlushG.WIDTH, RoyalFlushG.HEIGHT);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);
        
        // Khởi tạo helpers
        uiHelper = new UIHelper();
        
        // Tạo UI
        createUI();
        
        logger.info("MenuControllerJava đã khởi tạo thành công");
    }
    
    /**
     * Tạo tất cả UI components
     */
    private void createUI() {
        // Tạo background
        createBackground();
        
        // Tạo main table container
        createMainTable();
        
        // Tạo title
        createTitle();
        
        // Tạo buttons
        createButtons();
        
        // Layout
        layoutUI();
    }
    
    /**
     * Tạo background image
     */
    private void createBackground() {
        // Thử load background từ ui folder
        try {
            backgroundImage = UIHelper.createBackground("ui/Background_lobby_mainscreen.png");
            stage.addActor(backgroundImage);
            logger.debug("Đã load background: ui/Background_lobby_mainscreen.png");
        } catch (Exception e) {
            logger.warn("Không thể load background, sử dụng màu đen: {}", e.getMessage());
            // Nếu không load được, sẽ dùng ScreenUtils.clear() trong render()
        }
    }
    
    /**
     * Tạo main table container
     */
    private void createMainTable() {
        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(20f);
        mainTable.align(Align.center);
        stage.addActor(mainTable);
    }
    
    /**
     * Tạo title label
     */
    private void createTitle() {
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = com.kotcrab.vis.ui.VisUI.getSkin().getFont("default-font");
        titleStyle.fontColor = new com.badlogic.gdx.graphics.Color(1f, 0.81f, 0.98f, 1f); // Pink color
        
        titleLabel = new Label("ROYAL FLUSHG", titleStyle);
        titleLabel.setAlignment(Align.center);
    }
    
    /**
     * Tạo các buttons
     */
    private void createButtons() {
        // Tạo Start button
        // Thử dùng CatUI button trước, nếu không có thì dùng raw texture
        try {
            startButton = uiHelper.createCatUIButtonWithListener("btn_play", () -> {
                logger.info("Start button clicked");
                if (interfaceService != null) {
                    try {
                        interfaceService.show(com.mygame.client.controller.LoginController.class);
                    } catch (Exception e) {
                        logger.error("Lỗi khi chuyển sang LoginController: {}", e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("Không thể tạo CatUI button, tạo button đơn giản: {}", e.getMessage());
            // Fallback: tạo button đơn giản từ raw texture
            startButton = createSimpleButton("images/CatUI/btn_play1.png", "images/CatUI/btn_play2.png", () -> {
                logger.info("Start button clicked");
                if (interfaceService != null) {
                    interfaceService.show(com.mygame.client.controller.LoginController.class);
                }
            });
        }
        
        // Tạo Exit button
        try {
            exitButton = uiHelper.createCatUIButtonWithListener("btn_cancel", () -> {
                logger.info("Exit button clicked");
                Gdx.app.exit();
            });
        } catch (Exception e) {
            logger.warn("Không thể tạo CatUI exit button, tạo button đơn giản: {}", e.getMessage());
            exitButton = createSimpleButton("images/CatUI/btn_cancel1.png", "images/CatUI/btn_cancel2.png", () -> {
                logger.info("Exit button clicked");
                Gdx.app.exit();
            });
        }
        
        // Set size cho buttons
        if (startButton != null) {
            startButton.setSize(220, 60);
        }
        if (exitButton != null) {
            exitButton.setSize(220, 60);
        }
    }
    
    /**
     * Tạo button đơn giản từ raw textures (fallback)
     */
    private ImageButton createSimpleButton(String upPath, String downPath, Runnable onClick) {
        try {
            ImageButton button = uiHelper.createImageButtonWithListener(upPath, downPath, onClick);
            return button;
        } catch (Exception e) {
            logger.error("Lỗi khi tạo button từ {} và {}: {}", upPath, downPath, e.getMessage());
            // Tạo button placeholder
            return createPlaceholderButton(onClick);
        }
    }
    
    /**
     * Tạo placeholder button khi không load được texture
     */
    private ImageButton createPlaceholderButton(Runnable onClick) {
        // Tạo texture đơn giản
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(220, 60, 
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.2f, 0.2f, 1f);
        pixmap.fill();
        
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
        
        ImageButton button = new ImageButton(style);
        if (onClick != null) {
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onClick.run();
                }
            });
        }
        
        return button;
    }
    
    /**
     * Layout UI components
     */
    private void layoutUI() {
        mainTable.clear();
        
        // Add title
        mainTable.add(titleLabel).colspan(2).padBottom(30f).row();
        
        // Add buttons
        if (startButton != null) {
            mainTable.add(startButton).width(220).height(60).padBottom(15f).row();
        }
        
        if (exitButton != null) {
            mainTable.add(exitButton).width(220).height(60).row();
        }
    }
    
    @Override
    public void render(Stage stage, float delta) {
        // Clear screen
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        
        // Update viewport nếu window resize
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        
        // Update và render stage
        stage.act(delta);
        stage.draw();
    }
    
    /**
     * Dispose resources khi view bị destroy
     */
    public void dispose() {
        logger.info("Đang dispose MenuControllerJava...");
        
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        
        // Note: Không dispose UIResourceManager vì nó là singleton và được dùng chung
        // Chỉ dispose khi app shutdown
        
        logger.info("Đã dispose MenuControllerJava");
    }
}

