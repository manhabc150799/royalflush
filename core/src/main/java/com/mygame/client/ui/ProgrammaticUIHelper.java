package com.mygame.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * Helper class for creating UI components programmatically without LML templates.
 * Provides default styles and common UI creation methods.
 */
public class ProgrammaticUIHelper {
    
    private static Skin defaultSkin;
    
    /**
     * Get or create default skin for UI components
     */
    public static Skin getDefaultSkin() {
        if (defaultSkin == null) {
            defaultSkin = new Skin();
            // Create default font
            BitmapFont font = new BitmapFont();
            defaultSkin.add("default-font", font);
            
            // Create default label style
            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = font;
            labelStyle.fontColor = Color.WHITE;
            defaultSkin.add("default", labelStyle);
            
            // Create default text button style
            TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
            buttonStyle.font = font;
            buttonStyle.fontColor = Color.WHITE;
            defaultSkin.add("default", buttonStyle);
            
            // Create default text field style
            TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
            textFieldStyle.font = font;
            textFieldStyle.fontColor = Color.WHITE;
            defaultSkin.add("default", textFieldStyle);
            
            // Create default progress bar style
            ProgressBar.ProgressBarStyle progressStyle = new ProgressBar.ProgressBarStyle();
            defaultSkin.add("default", progressStyle);
        }
        return defaultSkin;
    }
    
    /**
     * Create a label with default style
     */
    public static Label createLabel(String text) {
        return new Label(text, getDefaultSkin());
    }
    
    /**
     * Create a text button with default style
     */
    public static TextButton createButton(String text) {
        return new TextButton(text, getDefaultSkin());
    }
    
    /**
     * Create a text field with default style
     */
    public static TextField createTextField(String placeholder) {
        TextField field = new TextField("", getDefaultSkin());
        field.setMessageText(placeholder);
        return field;
    }
    
    /**
     * Create a progress bar with default style
     */
    public static ProgressBar createProgressBar(float min, float max, float stepSize) {
        return new ProgressBar(min, max, stepSize, false, getDefaultSkin());
    }
    
    /**
     * Dispose resources
     */
    public static void dispose() {
        if (defaultSkin != null) {
            defaultSkin.dispose();
            defaultSkin = null;
        }
    }
}
