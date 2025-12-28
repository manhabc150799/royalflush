package com.mygame.client.configuration;

import com.mygame.client.configuration.preferences.ScalePreference;

/**
 * Configuration manager for client settings.
 * All text in English.
 */
public class Configuration {
    private static Configuration instance;
    
    private ScalePreference scalePreference;
    private float musicVolume = 0.7f;
    private float soundVolume = 0.8f;
    
    private Configuration() {
        scalePreference = new ScalePreference();
    }
    
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }
    
    public ScalePreference getScalePreference() {
        return scalePreference;
    }
    
    public float getMusicVolume() {
        return musicVolume;
    }
    
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    public float getSoundVolume() {
        return soundVolume;
    }
    
    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    public void save() {
        // Save configuration to preferences
    }
    
    public void load() {
        // Load configuration from preferences
    }
}
