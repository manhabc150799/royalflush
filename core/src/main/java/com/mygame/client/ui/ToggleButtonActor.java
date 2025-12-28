package com.mygame.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toggle button actor with 2-state PNG animation.
 * State 1: Off/Inactive
 * State 2: On/Active
 */
public class ToggleButtonActor extends Actor {
    private static final Logger logger = LoggerFactory.getLogger(ToggleButtonActor.class);
    
    private TextureRegion state1Region; // Off/Inactive state
    private TextureRegion state2Region; // On/Active state
    private boolean isOn = false;
    private ToggleButtonListener listener;
    
    public ToggleButtonActor(String state1Path, String state2Path, float width, float height) {
        try {
            Texture state1Texture = new Texture(Gdx.files.internal(state1Path));
            Texture state2Texture = new Texture(Gdx.files.internal(state2Path));
            
            state1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            state2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            
            state1Region = new TextureRegion(state1Texture);
            state2Region = new TextureRegion(state2Texture);
            
            setSize(width, height);
            setBounds(getX(), getY(), width, height);
            
            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    toggle();
                    return true;
                }
            });
        } catch (Exception e) {
            logger.error("Failed to load toggle button textures: {} / {}", state1Path, state2Path, e);
        }
    }
    
    private void toggle() {
        isOn = !isOn;
        
        // Add animation effect
        clearActions();
        addAction(Actions.sequence(
            Actions.scaleTo(0.9f, 0.9f, 0.1f),
            Actions.scaleTo(1.0f, 1.0f, 0.1f)
        ));
        
        if (listener != null) {
            listener.onToggle(isOn);
        }
    }
    
    public void setOn(boolean on) {
        if (this.isOn != on) {
            this.isOn = on;
            if (listener != null) {
                listener.onToggle(isOn);
            }
        }
    }
    
    public boolean isOn() {
        return isOn;
    }
    
    public void setListener(ToggleButtonListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (state1Region == null || state2Region == null) {
            return;
        }
        
        TextureRegion currentRegion = isOn ? state2Region : state1Region;
        batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);
        batch.draw(currentRegion, getX(), getY(), getOriginX(), getOriginY(), 
                   getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
    }
    
    @Override
    public void act(float delta) {
        super.act(delta);
    }
    
    public void dispose() {
        if (state1Region != null && state1Region.getTexture() != null) {
            state1Region.getTexture().dispose();
        }
        if (state2Region != null && state2Region.getTexture() != null) {
            state2Region.getTexture().dispose();
        }
    }
    
    /**
     * Listener interface for toggle button events
     */
    public interface ToggleButtonListener {
        void onToggle(boolean isOn);
    }
}
