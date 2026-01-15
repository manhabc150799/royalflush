package com.mygame.client.ui.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.mygame.client.ui.UISkinManager;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * TienLenPlayerWidget - Text-focused rival player widget for Tien Len.
 * 
 * No avatars - uses typography and backgrounds for visual grouping.
 * 
 * Layout:
 * ┌─────────────────────────┐
 * │ [Name: User X] │
 * │ [Cards: 13 🂠] │
 * │ Credits: 1,000$ │
 * └─────────────────────────┘
 */
public class TienLenPlayerWidget extends Table {

    // Visual constants
    private static final float PADDING = 8f;
    private static final Color ACTIVE_COLOR = new Color(1f, 0.84f, 0f, 0.8f); // Gold highlight
    private static final Color INACTIVE_BG = new Color(0.1f, 0.1f, 0.15f, 0.85f);

    // Player data
    private int playerId;
    private String username;
    private int cardCount;
    private long credits;
    private boolean isActive;

    // UI Components
    private Skin skin;
    private Label nameLabel;
    private Label cardCountLabel;
    private Label creditsLabel;

    // Number formatter
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    /**
     * Create a TienLenPlayerWidget for a rival player.
     *
     * @param playerId Player ID
     * @param username Display name
     * @param credits  Initial credits
     */
    public TienLenPlayerWidget(int playerId, String username, long credits) {
        this.playerId = playerId;
        this.username = username != null ? username : "Player " + playerId;
        this.cardCount = 13; // Tien Len starts with 13 cards
        this.credits = credits;
        this.isActive = false;

        skin = UISkinManager.getInstance().getSkin();

        buildUI();
    }

    /**
     * Build the text-focused UI layout.
     */
    private void buildUI() {
        // Configure table
        defaults().pad(2).center();
        setBackground(skin.getDrawable("panel1"));
        pad(PADDING);

        // === Name Label (Bold, larger) ===
        Label.LabelStyle nameStyle = new Label.LabelStyle();
        nameStyle.font = skin.getFont("Blue_font");
        nameStyle.fontColor = Color.WHITE;

        nameLabel = new Label(username, nameStyle);
        nameLabel.setFontScale(0.9f);
        nameLabel.setAlignment(Align.center);

        add(nameLabel).expandX().center();
        row();

        // === Card Count Label ===
        Label.LabelStyle cardStyle = new Label.LabelStyle();
        cardStyle.font = skin.getFont("Pink_font");
        cardStyle.fontColor = new Color(1f, 0.85f, 0.4f, 1f); // Warm yellow

        cardCountLabel = new Label(formatCardCount(cardCount), cardStyle);
        cardCountLabel.setFontScale(0.85f);
        cardCountLabel.setAlignment(Align.center);

        add(cardCountLabel).expandX().center().padTop(4);
        row();

        // === Credits Label ===
        Label.LabelStyle creditsStyle = new Label.LabelStyle();
        creditsStyle.font = skin.getFont("Green_font");
        creditsStyle.fontColor = new Color(0.4f, 1f, 0.5f, 1f);

        creditsLabel = new Label(formatCredits(credits), creditsStyle);
        creditsLabel.setFontScale(0.75f);
        creditsLabel.setAlignment(Align.center);

        add(creditsLabel).expandX().center().padTop(2);
    }

    // ==================== PUBLIC API ====================

    /**
     * Set active state (current turn).
     * Changes background to highlight when active.
     *
     * @param active Whether it's this player's turn
     */
    public void setActive(boolean active) {
        this.isActive = active;

        if (active) {
            // Highlight - use same panel but tint it yellow (like "Me" section)
            setBackground(skin.getDrawable("panel1"));
            setColor(1f, 1f, 0.5f, 1f);
            if (nameLabel != null) {
                nameLabel.setColor(Color.YELLOW);
            }
        } else {
            // Normal state
            setBackground(skin.getDrawable("panel1"));
            setColor(1f, 1f, 1f, 1f);
            if (nameLabel != null) {
                nameLabel.setColor(Color.WHITE);
            }
        }
    }

    /**
     * Update card count display.
     *
     * @param count Number of cards remaining
     */
    public void updateCardCount(int count) {
        this.cardCount = count;
        if (cardCountLabel != null) {
            cardCountLabel.setText(formatCardCount(count));
        }
    }

    /**
     * Update credits display.
     *
     * @param amount New credit amount
     */
    public void updateCredits(long amount) {
        this.credits = amount;
        if (creditsLabel != null) {
            creditsLabel.setText(formatCredits(amount));
        }
    }

    /**
     * Update username display.
     *
     * @param name New username
     */
    public void updateUsername(String name) {
        this.username = name;
        if (nameLabel != null) {
            nameLabel.setText(name);
        }
    }

    /**
     * Mark player as finished (0 cards left = winner).
     */
    public void setFinished() {
        updateCardCount(0);
        if (cardCountLabel != null) {
            cardCountLabel.setText("FINISHED!");
            cardCountLabel.setColor(Color.GOLD);
        }
    }

    // ==================== HELPERS ====================

    private String formatCardCount(int count) {
        if (count == 0) {
            return "FINISHED!";
        }
        return "Cards: " + count + " \uD83C\uDCA0"; // 🂠 card back unicode
    }

    private String formatCredits(long amount) {
        return "Credits: " + NUMBER_FORMAT.format(amount) + "$";
    }

    // ==================== GETTERS ====================

    public int getPlayerId() {
        return playerId;
    }

    public int getCardCount() {
        return cardCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getCredits() {
        return credits;
    }
}
