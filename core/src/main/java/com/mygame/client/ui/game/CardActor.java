package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mygame.shared.game.card.Card;

import java.util.HashMap;
import java.util.Map;

/**
 * CardActor - Scene2D Actor to display a playing card.
 * 
 * Refactored to extend Group as requested:
 * - Layer 1: Image (PNG background)
 * - Layer 2: Invisible Actor (Overlay for hit detection)
 */
public class CardActor extends Group {

    // Card dimensions
    public static final float CARD_WIDTH = 95f;
    public static final float CARD_HEIGHT = 135f;
    public static final float SELECTION_OFFSET = 38f; // Matches MyHandWidget config

    // Path to card images
    private static final String CARD_PATH = "images/cards/light/";

    // Static texture cache
    private static final Map<String, Texture> textureCache = new HashMap<>();
    private static Texture backTexture = null;

    private Card card;
    private boolean faceUp;
    private boolean selected;
    private boolean selectable;

    // UI Components
    private Image cardImage;
    private Actor touchOverlay;

    // State
    private float originalY;
    private boolean initialized = false;

    public CardActor(Card card, boolean faceUp) {
        this.card = card;
        this.faceUp = faceUp;
        this.selected = false;
        this.selectable = false;

        // Initialize Group settings
        setTransform(false); // Optimization, set to true if rotation needed

        // 1. Create Card Image (Background)
        cardImage = new Image();
        cardImage.setSize(CARD_WIDTH, CARD_HEIGHT);
        updateCardTexture(); // Load texture
        addActor(cardImage);

        // 2. Create Invisible Touch Overlay (Top Layer)
        touchOverlay = new Actor();
        touchOverlay.setSize(CARD_WIDTH, CARD_HEIGHT);
        touchOverlay.setTouchable(Touchable.enabled); // Ensures this catches events
        // touchOverlay.debug(); // Uncomment to see debug lines
        addActor(touchOverlay);

        // Set Group size
        setSize(CARD_WIDTH, CARD_HEIGHT);

        // Note: Internal InputListener REMOVED to avoid conflict with MyHandWidget
    }

    public CardActor() {
        this(null, false);
    }

    private void updateCardTexture() {
        Texture textureToUse = null;

        if (!faceUp || card == null) {
            // Load Back
            if (backTexture == null) {
                try {
                    backTexture = new Texture(Gdx.files.internal(CARD_PATH + "BACK.png"));
                    backTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                } catch (Exception e) {
                    Gdx.app.error("CardActor", "Failed to load BACK.png", e);
                }
            }
            textureToUse = backTexture;
        } else {
            // Load Face
            String filename = getRegionName(card) + ".png";
            textureToUse = textureCache.get(filename);

            if (textureToUse == null) {
                try {
                    textureToUse = new Texture(Gdx.files.internal(CARD_PATH + filename));
                    textureToUse.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    textureCache.put(filename, textureToUse);
                } catch (Exception e) {
                    Gdx.app.error("CardActor", "Failed to load " + filename, e);
                }
            }
        }

        if (textureToUse != null) {
            cardImage.setDrawable(new TextureRegionDrawable(new TextureRegion(textureToUse)));
        }
    }

    private String getRegionName(Card card) {
        if (card == null)
            return "BACK";

        int rank = card.getRank();
        String rankStr;
        switch (rank) {
            case 11:
                rankStr = "J";
                break;
            case 12:
                rankStr = "Q";
                break;
            case 13:
                rankStr = "K";
                break;
            case 14:
                rankStr = "A";
                break;
            default:
                rankStr = String.valueOf(rank);
                break;
        }

        String suitStr;
        switch (card.getSuit()) {
            case SPADES:
                suitStr = "P";
                break;
            case HEARTS:
                suitStr = "H";
                break;
            case DIAMONDS:
                suitStr = "D";
                break;
            case CLUBS:
                suitStr = "C";
                break;
            default:
                suitStr = "P";
                break;
        }

        return rankStr + "-" + suitStr;
    }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        if (cardImage != null)
            cardImage.setSize(width, height);
        if (touchOverlay != null)
            touchOverlay.setSize(width, height);
    }

    // ==================== SELECTION LOGIC ====================

    /**
     * Set selection state.
     * Controlled externally by MyHandWidget.
     */
    public void setSelected(boolean selected) {
        if (this.selected == selected)
            return;
        this.selected = selected;

        // Visual pop-up logic handled by MyHandWidget animations mostly,
        // but we can ensure state consistency here.
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    // ==================== GETTERS/SETTERS ====================

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
        updateCardTexture();
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
        updateCardTexture();
    }

    // Legacy methods kept for compatibility with animations if needed
    public void updateOriginalY() {
        // Managed by MyHandWidget layout
    }

    /**
     * Animate dealing this card from a source position.
     */
    public void animateDeal(float startX, float startY, float endX, float endY, float delay) {
        // Start at source position
        setPosition(startX, startY);
        setScale(0.5f);
        getColor().a = 0f;

        // Random slight rotation for natural look
        float randomRotation = (float) (Math.random() * 6 - 3); // -3 to +3 degrees

        // Animate to destination
        addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.parallel(
                        Actions.moveTo(endX, endY, 0.4f, Interpolation.swingOut),
                        Actions.scaleTo(1f, 1f, 0.3f, Interpolation.smooth),
                        Actions.rotateTo(randomRotation, 0.4f),
                        Actions.fadeIn(0.2f))));

        // Update originalY after animation completes
        addAction(Actions.sequence(
                Actions.delay(delay + 0.4f),
                Actions.run(() -> {
                    originalY = getY();
                    initialized = true;
                })));
    }

    /**
     * Deselect and reset position.
     */
    public void resetSelection() {
        if (selected) {
            selected = false;
            // Only animate return if we have a valid originalY
            // (initialized implicitly by being in layout)
            clearActions();
            addAction(Actions.moveTo(getX(), originalY, 0.15f, Interpolation.smooth));
        }
    }
}
