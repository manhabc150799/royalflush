package com.mygame.client.controller;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.mygame.client.ui.UIHelper;

/**
 * MenuController using Scene2D for main menu UI.
 * All text in English.
 */
public class MenuController {
    private final Stage stage;
    
    public MenuController(Stage stage) {
        this.stage = stage;
        setupUI();
    }
    
    private void setupUI() {
        stage.clear();
        
        // Create background
        UIHelper.createFullScreenBackground(stage, "ui/Background_lobby_mainscreen.png");
        
        // Main menu container
        Table mainTable = UIHelper.createCenteredTable();
        stage.addActor(mainTable);
        
        // Title
        Label titleLabel = new Label("Royal FlushG", new Label.LabelStyle());
        titleLabel.setFontScale(2.0f);
        mainTable.add(titleLabel).padBottom(50);
        mainTable.row();
        
        // Menu buttons
        TextButton playButton = new TextButton("Play", new TextButton.TextButtonStyle());
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Navigate to lobby
            }
        });
        mainTable.add(playButton).size(200, 50).padBottom(20);
        mainTable.row();
        
        TextButton settingsButton = new TextButton("Settings", new TextButton.TextButtonStyle());
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Show settings
            }
        });
        mainTable.add(settingsButton).size(200, 50).padBottom(20);
        mainTable.row();
        
        TextButton exitButton = new TextButton("Exit", new TextButton.TextButtonStyle());
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.exit(0);
            }
        });
        mainTable.add(exitButton).size(200, 50);
    }
    
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }
    
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    
    public void dispose() {
        stage.dispose();
    }
}
