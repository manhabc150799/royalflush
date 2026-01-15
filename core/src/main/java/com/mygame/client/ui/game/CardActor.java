package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;

import java.util.HashMap;
import java.util.Map;

/**
 * CardActor - Scene2D Actor to display a playing card.
 * 
 * Loads card textures from PNG files in images/cards/light/ directory.
 * Uses static texture cache to avoid loading same texture multiple times.
 * 
 * File naming convention: {rank}-{SUIT}.png where SUIT is uppercase C/D/H/P
 * Examples: 10-H.png (10 of Hearts), A-P.png (Ace of Spades), BACK.png (card
 * back)
 */
public class CardActor extends Actor {

    // Card dimensions (fits well on screen)
    public static final float CARD_WIDTH = 95f;
    public static final float CARD_HEIGHT = 135f;
    public static final float SELECTION_OFFSET = 25f;

    // Path to card images
    private static final String CARD_PATH = "images/cards/light/";

    // Static texture cache to avoid reloading
    private static final Map<String, Texture> textureCache = new HashMap<>();
    private static Texture backTexture = null;

    private Card card;
    private boolean faceUp;
    private boolean selected;
    private boolean selectable;

    private TextureRegion faceRegion;
    private TextureRegion backRegion;

    private float originalY;
    private boolean initialized = false;

    /**
     * Create a CardActor displaying the given card.
     *
     * @param card   The card data from shared module
     * @param faceUp Whether to show face (true) or back (false)
     */
    public CardActor(Card card, boolean faceUp) {
        this.card = card;
        this.faceUp = faceUp;
        this.selected = false;
        this.selectable = false;

        // Load textures from PNG files
        loadTextures();

        // Set size
        setSize(CARD_WIDTH, CARD_HEIGHT);

        // Add click listener for selection toggle
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (selectable && CardActor.this.faceUp) {
                    toggleSelection();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Create a face-down card (for opponent display).
     */
    public CardActor() {
        this(null, false);
    }

    /**
     * Load textures from PNG files in images/cards/light/.
     */
    private void loadTextures() {
        // Load back texture (cached)
        if (backTexture == null) {
            try {
                backTexture = new Texture(Gdx.files.internal(CARD_PATH + "BACK.png"));
                backTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                Gdx.app.error("CardActor", "Failed to load BACK.png", e);
            }
        }
        if (backTexture != null) {
            backRegion = new TextureRegion(backTexture);
        }

        // Load face texture if card is set
        if (card != null) {
            String filename = getRegionName(card) + ".png"; // Append .png as helper returns base name
            Gdx.app.log("CardActor", "Requesting texture: " + filename); // DEBUG
            Texture faceTexture = textureCache.get(filename);

            if (faceTexture == null) {
                try {
                    faceTexture = new Texture(Gdx.files.internal(CARD_PATH + filename));
                    faceTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    textureCache.put(filename, faceTexture);
                } catch (Exception e) {
                    Gdx.app.error("CardActor", "Failed to load " + filename, e);
                }
            }

            if (faceTexture != null) {
                faceRegion = new TextureRegion(faceTexture);
            }
        }
    }

    /**
     * Get the filename for a card image.
     * Format: {rank}-{suit}.png
     * Face cards: J, Q, K, A
     */
    /**
     * Get the region name for a card image matching asset convention.
     * Format: {rank}-{suit} (e.g. "T-H", "A-P", "2-D")
     * Returns "BACK" for hidden/null cards.
     */
    private String getRegionName(Card card) {
        if (card == null) {
            return "BACK";
        }

        int rank = card.getRank();
        String rankStr;
        switch (rank) {
            case 10:
                rankStr = "T";
                break;
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
                break; // Default fallback
        }

        return rankStr + "-" + suitStr;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!initialized) {
            originalY = getY();
            initialized = true;
        }

        TextureRegion region = faceUp && faceRegion != null ? faceRegion : backRegion;

        if (region != null) {
            batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);
            batch.draw(region, getX(), getY(), getOriginX(), getOriginY(),
                    getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
        }
    }

    /**
     * Toggle card selection state (for Tien Len).
     */
    public void toggleSelection() {
        if (!selectable)
            return;

        selected = !selected;

        // Animate up/down
        clearActions();
        float targetY = selected ? originalY + SELECTION_OFFSET : originalY;
        addAction(Actions.moveTo(getX(), targetY, 0.15f, Interpolation.smooth));
    }

    /**
     * Set selection state without animation.
     */
    public void setSelected(boolean selected) {
        if (this.selected == selected)
            return;
        this.selected = selected;

        if (initialized) {
            float targetY = selected ? originalY + SELECTION_OFFSET : originalY;
            setY(targetY);
        }
    }

    /**
     * Flip the card face up or face down with animation.
     */
    public void flip(boolean faceUp) {
        this.faceUp = faceUp;
        // Could add flip animation here using scaleX
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

    // Getters and Setters

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
        loadTextures();
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    /**
     * Deselect and reset position.
     */
    public void resetSelection() {
        if (selected) {
            selected = false;
            if (initialized) {
                clearActions();
                addAction(Actions.moveTo(getX(), originalY, 0.15f, Interpolation.smooth));
            }
        }
    }

    /**
     * Update original Y position (call after positioning).
     */
    public void updateOriginalY() {
        originalY = getY();
        initialized = true;
    }
}
