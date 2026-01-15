package com.mygame.client.ui.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * PlayerSeatWidget - Table-based player seat widget for Poker.
 * 
 * Layout:
 * ┌─────────────────────────┐
 * │ [Avatar] [Name] │
 * │ [Card1][Card2]│
 * │ Credits: 1,000 │
 * └─────────────────────────┘
 */
public class PlayerSeatWidget extends Table {

    // Widget dimensions (scaled 1.25x)
    public static final float WIDGET_WIDTH = 175f;
    public static final float WIDGET_HEIGHT = 138f;
    private static final float CARD_SCALE = 0.5f;

    // Player data
    private int playerId;
    private String username;
    private long balance;
    private boolean isLocalPlayer;
    private boolean isCurrentTurn;
    private boolean isDealer;
    private boolean hasFolded;

    // UI components
    private Skin skin;
    private TextureAtlas atlas;
    private Label nameLabel;
    private Label creditsLabel;
    private Table cardsContainer;
    private CardActor card1;
    private CardActor card2;
    private Label currentBetLabel;

    // Number formatter for credits
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    /**
     * Create a PlayerSeatWidget for the given player.
     *
     * @param playerId      Player ID
     * @param username      Player display name
     * @param balance       Initial balance
     * @param isLocalPlayer Whether this is the local player
     */
    public PlayerSeatWidget(int playerId, String username, long balance, boolean isLocalPlayer) {
        this.playerId = playerId;
        this.username = username != null ? username : "Player " + playerId;
        this.balance = balance;
        this.isLocalPlayer = isLocalPlayer;
        this.isCurrentTurn = false;
        this.isDealer = false;
        this.hasFolded = false;

        skin = UISkinManager.getInstance().getSkin();
        atlas = skin.getAtlas();

        buildUI();
    }

    /**
     * Build the Table-based UI layout.
     */
    private void buildUI() {
        // Configure table
        defaults().pad(2);
        setBackground(skin.getDrawable("panel1"));

        // === Row 1: Name (centered, no avatar) ===
        Label.LabelStyle nameStyle = new Label.LabelStyle();
        nameStyle.font = skin.getFont("Blue_font");
        nameStyle.fontColor = Color.WHITE;
        nameLabel = new Label(truncateName(username, 12), nameStyle);
        nameLabel.setFontScale(0.85f);
        nameLabel.setAlignment(Align.center);

        add(nameLabel).expandX().center().padTop(4);
        row();

        // === Row 2: Cards container ===
        cardsContainer = new Table();
        cardsContainer.defaults().pad(1);

        float cardW = CardActor.CARD_WIDTH * CARD_SCALE;
        float cardH = CardActor.CARD_HEIGHT * CARD_SCALE;

        // Create placeholder cards (face down by default)
        card1 = new CardActor(null, false);
        card1.setSize(cardW, cardH);
        card2 = new CardActor(null, false);
        card2.setSize(cardW, cardH);

        cardsContainer.add(card1).size(cardW, cardH);
        cardsContainer.add(card2).size(cardW, cardH).padLeft(-10); // Overlap slightly

        add(cardsContainer).center().padTop(2);
        row();

        // === Row 3: Current Bet (Golden) ===
        Label.LabelStyle betStyle = new Label.LabelStyle();
        betStyle.font = skin.getFont("Blue_font");
        betStyle.fontColor = Color.GOLD;

        currentBetLabel = new Label("", betStyle);
        currentBetLabel.setFontScale(0.75f);
        currentBetLabel.setAlignment(Align.center);

        add(currentBetLabel).expandX().center().padBottom(2);
        row();

        // === Row 4: Credits label ===
        Label.LabelStyle creditsStyle = new Label.LabelStyle();
        creditsStyle.font = skin.getFont("Green_font");
        creditsStyle.fontColor = new Color(0.4f, 1f, 0.5f, 1f);

        creditsLabel = new Label(formatCredits(balance), creditsStyle);
        creditsLabel.setFontScale(0.7f);
        creditsLabel.setAlignment(Align.center);

        add(creditsLabel).expandX().center().padBottom(4);
    }

    // ==================== PUBLIC API ====================

    /**
     * Update the credits display with formatted amount.
     *
     * @param amount New balance amount
     */
    public void updateCredits(long amount) {
        this.balance = amount;
        if (creditsLabel != null) {
            creditsLabel.setText(formatCredits(amount));
        }
    }

    /**
     * Update the current round bet display.
     */
    public void updateCurrentBet(long amount) {
        if (currentBetLabel != null) {
            if (amount > 0) {
                currentBetLabel.setText("Bet: " + NUMBER_FORMAT.format(amount));
                currentBetLabel.setVisible(true);
            } else {
                currentBetLabel.setText("");
                currentBetLabel.setVisible(false);
            }
        }
    }

    /**
     * Update displayed cards.
     *
     * @param cards List of cards (max 2 for Poker hole cards)
     */
    public void updateCards(List<Card> cards) {
        if (hasFolded) {
            hideCards();
            return;
        }

        if (cards == null || cards.isEmpty()) {
            showBackCards();
            return;
        }

        // Update card 1
        if (cards.size() > 0 && card1 != null) {
            card1.setCard(cards.get(0));
            card1.setFaceUp(isLocalPlayer);
            card1.setVisible(true);
        }

        // Update card 2
        if (cards.size() > 1 && card2 != null) {
            card2.setCard(cards.get(1));
            card2.setFaceUp(isLocalPlayer);
            card2.setVisible(true);
        }
    }

    /**
     * Show card backs (for opponents).
     */
    public void showBackCards() {
        if (hasFolded) {
            hideCards();
            return;
        }

        if (card1 != null) {
            card1.setCard(null);
            card1.setFaceUp(false);
            card1.setVisible(true);
        }
        if (card2 != null) {
            card2.setCard(null);
            card2.setFaceUp(false);
            card2.setVisible(true);
        }
    }

    /**
     * Hide all cards (for folded players).
     */
    public void hideCards() {
        if (card1 != null)
            card1.setVisible(false);
        if (card2 != null)
            card2.setVisible(false);
    }

    /**
     * Set the current turn highlight.
     *
     * @param isTurn Whether it's this player's turn
     */
    public void setCurrentTurn(boolean isTurn) {
        this.isCurrentTurn = isTurn;

        // Use same panel1 background for all, just highlight name color
        setBackground(skin.getDrawable("panel1"));
        if (nameLabel != null) {
            nameLabel.setColor(isTurn ? Color.YELLOW : Color.WHITE);
        }
    }

    /**
     * Mark player as folded.
     *
     * @param folded Whether player has folded
     */
    public void setFolded(boolean folded) {
        this.hasFolded = folded;

        if (folded) {
            setColor(0.5f, 0.5f, 0.5f, 0.6f);
            hideCards();
        } else {
            setColor(Color.WHITE);
        }
    }

    /**
     * Set dealer indicator.
     *
     * @param dealer Whether this player is the dealer
     */
    public void setDealer(boolean dealer) {
        this.isDealer = dealer;
        // Could add dealer chip visual indicator here
    }

    /**
     * Update displayed username.
     *
     * @param name New username
     */
    public void updateUsername(String name) {
        this.username = name;
        if (nameLabel != null) {
            nameLabel.setText(truncateName(name, 10));
        }
    }

    // ==================== HELPERS ====================

    private String formatCredits(long amount) {
        return "Credits: " + NUMBER_FORMAT.format(amount);
    }

    private String truncateName(String name, int maxLength) {
        if (name == null)
            return "???";
        return name.length() <= maxLength ? name : name.substring(0, maxLength - 1) + "…";
    }

    /**
     * Set this seat as empty (dark transparent placeholder).
     * Used for Poker games with less than 5 players.
     */
    public void setEmpty(boolean empty) {
        if (empty) {
            // Hide all content
            if (nameLabel != null)
                nameLabel.setText("");
            if (currentBetLabel != null)
                currentBetLabel.setText("");
            if (creditsLabel != null)
                creditsLabel.setText("");
            if (card1 != null)
                card1.setVisible(false);
            if (card2 != null)
                card2.setVisible(false);

            // Dark semi-transparent background
            setColor(0.1f, 0.1f, 0.1f, 0.5f);
        } else {
            // Restore normal appearance
            setColor(Color.WHITE);
            if (card1 != null)
                card1.setVisible(true);
            if (card2 != null)
                card2.setVisible(true);
        }
    }

    // ==================== GETTERS ====================

    public int getPlayerId() {
        return playerId;
    }

    public boolean isLocalPlayer() {
        return isLocalPlayer;
    }

    public boolean isCurrentTurn() {
        return isCurrentTurn;
    }

    public boolean isDealer() {
        return isDealer;
    }

    public boolean hasFolded() {
        return hasFolded;
    }

    public long getBalance() {
        return balance;
    }
}
