package com.mygame.client.configuration.preferences;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.VisUI.SkinScale;

/**
 * Scale preference for UI scaling.
 * All text in English.
 */
public class ScalePreference {
    private SkinScale scale;
    
    public ScalePreference() {
        // Default to first available scale
        SkinScale[] scales = SkinScale.values();
        this.scale = scales.length > 0 ? scales[0] : null;
    }
    
    public SkinScale get() {
        if (scale == null) {
            SkinScale[] scales = SkinScale.values();
            scale = scales.length > 0 ? scales[0] : null;
        }
        return scale;
    }
    
    public void set(SkinScale scale) {
        this.scale = scale != null ? scale : get();
    }
    
    public SkinScale extractFromActor(Actor actor) {
        // Extract scale from actor if needed
        return get();
    }
    
    public void save() {
        // Save scale preference
    }
    
    public void load() {
        // Load scale preference
    }
}
