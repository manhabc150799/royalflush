package com.mygame.client.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.asset.AssetService;
import com.github.czyzby.autumn.mvc.component.ui.controller.ViewRenderer;
import com.github.czyzby.autumn.mvc.stereotype.View;
import com.mygame.client.ui.UIHelper;
import com.mygame.client.ui.ProgrammaticUIHelper;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.io.FileWriter;
import java.io.IOException;

/** Loading screen created programmatically without LML templates.
 *
 * This is the first application's view, shown right after the application starts. It will hide after all assets are
 * loaded. */
@View(id = "loading", value = "ui/templates/loading.lml", first = true)
public class LoadingController implements ViewRenderer {
    /** Will be injected automatically. Manages assets. Used to display loading progress. */
    @Inject private AssetService assetService;
    @Inject private com.github.czyzby.autumn.mvc.component.ui.InterfaceService interfaceService;
    /** CRITICAL: Inject LoginController to ensure it's instantiated early and action container is registered */
    @Inject private com.mygame.client.controller.LoginController loginController;
    
    private ProgressBar loadingBar;
    private Label loadingLabel;
    private boolean uiInitialized = false;
    private boolean hasNavigated = false;

    // Since this class implements ViewRenderer, it can modify the way its view is drawn. Additionally to drawing the
    // stage, this view also updates assets manager and reads its progress.
    @Override
    public void render(final Stage stage, final float delta) {
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoadingController.java:35\",\"message\":\"render() called\",\"data\":{\"uiInitialized\":" + uiInitialized + ",\"stageNull\":" + (stage == null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
        
        // Initialize UI on first render
        if (!uiInitialized) {
            // #region agent log
            try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
                fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoadingController.java:40\",\"message\":\"Setting up UI programmatically\",\"data\":{}}\n");
            } catch (IOException ignored) {}
            // #endregion
            setupUI(stage);
            uiInitialized = true;
            // #region agent log
            try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
                fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoadingController.java:44\",\"message\":\"UI setup complete\",\"data\":{\"loadingBarNull\":" + (loadingBar == null) + "}}\n");
            } catch (IOException ignored) {}
            // #endregion
        }
        
        // Clear screen before rendering to avoid ghosting/artifacts:
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        assetService.update();
        float progress = assetService.getLoadingProgress();
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoadingController.java:52\",\"message\":\"Progress update\",\"data\":{\"progress\":" + progress + ",\"loadingBarNull\":" + (loadingBar == null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
        if (loadingBar != null) {
            loadingBar.setValue(progress);
        }
        
        // Navigate to login screen when loading is complete
        if (progress >= 1.0f && !hasNavigated) {
            hasNavigated = true;
            // Small delay to show 100% briefly
            Gdx.app.postRunnable(() -> {
                try {
                    Thread.sleep(300); // 300ms delay
                } catch (InterruptedException e) {
                    // Ignore
                }
                Gdx.app.postRunnable(() -> {
                    interfaceService.show(com.mygame.client.controller.LoginController.class);
                });
            });
        }
        
        stage.act(delta);
        stage.draw();
    }
    
    private void setupUI(Stage stage) {
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoadingController.java:71\",\"message\":\"setupUI() entry - building UI programmatically\",\"data\":{\"stageNull\":" + (stage == null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
        
        stage.clear();
        
        // Create background
        UIHelper.createFullScreenBackground(stage, "ui/Background_lobby_mainscreen.png");
        
        // Create main container
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);
        
        // Create centered container
        Table centerTable = new Table();
        
        // Title label
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = new BitmapFont(); // Default font
        Label titleLabel = new Label("Loading", titleStyle);
        titleLabel.setFontScale(1.5f);
        centerTable.add(titleLabel).padBottom(20);
        centerTable.row();
        
        // Progress bar - using CatUI bar assets
        ProgressBar.ProgressBarStyle progressStyle = new ProgressBar.ProgressBarStyle();
        // Use CatUI bar assets instead of uiskin
        try {
            Image barBg = UIHelper.createCatUIPanel("bar1");
            Image barKnob = UIHelper.createCatUIPanel("bar2");
            if (barBg != null) {
                progressStyle.background = barBg.getDrawable();
            }
            if (barKnob != null) {
                progressStyle.knobBefore = barKnob.getDrawable();
            }
        } catch (Exception e) {
            // Use default style if image loading fails
        }
        loadingBar = new ProgressBar(0f, 1f, 0.01f, false, progressStyle);
        loadingBar.setWidth(400);
        loadingBar.setHeight(30);
        centerTable.add(loadingBar).width(400).height(30).padBottom(10);
        centerTable.row();
        
        // Loading text
        Label.LabelStyle loadingStyle = new Label.LabelStyle();
        loadingStyle.font = new BitmapFont(); // Default font
        loadingLabel = new Label("Loading assets...", loadingStyle);
        centerTable.add(loadingLabel);
        
        mainTable.add(centerTable).center();
        
        // #region agent log
        try (FileWriter fw = new FileWriter("c:\\Users\\LENOVO\\Downloads\\Royal FlushG\\.cursor\\debug.log", true)) {
            fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"LoadingController.java:111\",\"message\":\"setupUI() exit - UI built programmatically\",\"data\":{\"loadingBarCreated\":" + (loadingBar != null) + ",\"loadingLabelCreated\":" + (loadingLabel != null) + "}}\n");
        } catch (IOException ignored) {}
        // #endregion
    }
}
