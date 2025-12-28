package com.mygame.client.lwjgl3;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.czyzby.autumn.fcs.scanner.DesktopClassScanner;
import com.github.czyzby.autumn.mvc.application.AutumnApplication;
import com.mygame.client.RoyalFlushG;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new AutumnApplication(new DesktopClassScanner(), RoyalFlushG.class),
            getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Royal FlushG");
        
        // Get the primary monitor's display mode for fullscreen windowed
        Graphics.DisplayMode primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        if (primaryMode != null) {
            // Set to fullscreen windowed (borderless fullscreen) - takes up entire screen
            configuration.setWindowedMode(primaryMode.width, primaryMode.height);
        } else {
            // Fallback to default windowed mode if display mode unavailable
            configuration.setWindowedMode(RoyalFlushG.WIDTH, RoyalFlushG.HEIGHT);
        }
        
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        configuration.setResizable(false); // Prevent resizing for consistent UI
        return configuration;
    }
}