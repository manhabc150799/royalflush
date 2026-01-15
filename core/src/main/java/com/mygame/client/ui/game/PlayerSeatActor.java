package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerSeatActor - Clean seat widget with absolute positioning.
 * 
 * Design:
 * - Semi-transparent black background (50% alpha)
 * - Avatar left, Name/Balance right
 * - Size: 140x70 (fixed)
 * - Gold border when active
 */
public class PlayerSeatActor extends Group {

    // Fixed dimensions
    public static final float WIDTH = 140f;
    public static final float HEIGHT = 70f;

    private static final float AVATAR_SIZE = 40f;
    private static final float PADDING = 8f;
    private static final float CARD_SCALE = 0.35f;
    private static final float CARD_OFFSET = 20f;

    // Colors
    private static final Color BG_NORMAL = new Color(0f, 0f, 0f, 0.5f);
    private static final Color BG_ACTIVE = new Color(1f, 0.84f, 0f, 0.7f);
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_GREEN = new Color(0.4f, 1f, 0.5f, 1f);

    // Player data
    private int playerId;
    private String username;
    private long balance;
    private boolean isDealer;
    private boolean isCurrentTurn;
    private boolean isLocalPlayer;
    private boolean hasFolded;
    private String status = ""; // Ready/Thinking

    // UI Components
    private Skin skin;
    private TextureAtlas atlas;
    private Image avatarImage;
    private Label usernameLabel;
    private Label balanceLabel;
    private Label statusLabel;
    private Group cardGroup;
    private List<CardActor> holeCards;

    // Background texture
    private static Texture bgNormalTex;
    private static Texture bgActiveTex;
    private TextureRegion currentBg;

    static {
        createBackgrounds();
    }

    private static void createBackgrounds() {
        try {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(BG_NORMAL);
            pm.fill();
            bgNormalTex = new Texture(pm);
            pm.dispose();

            pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(BG_ACTIVE);
            pm.fill();
            bgActiveTex = new Texture(pm);
            pm.dispose();
        } catch (Exception e) {
            Gdx.app.error("PlayerSeatActor", "Failed to create backgrounds", e);
        }
    }

    public PlayerSeatActor(int playerId, String username, long balance, boolean isLocalPlayer) {
        this.playerId = playerId;
        this.username = username != null ? username : "Player " + playerId;
        this.balance = balance;
        this.isLocalPlayer = isLocalPlayer;
        this.isDealer = false;
        this.isCurrentTurn = false;
        this.hasFolded = false;
        this.holeCards = new ArrayList<>();

        skin = UISkinManager.getInstance().getSkin();
        atlas = skin.getAtlas();

        // Set fixed size immediately
        setSize(WIDTH, HEIGHT);

        currentBg = bgNormalTex != null ? new TextureRegion(bgNormalTex) : null;

        buildUI();
    }

    private void buildUI() {
        // Avatar (left)
        String avatarName = "cat" + ((playerId % 4) + 1);
        TextureAtlas.AtlasRegion avatarRegion = atlas.findRegion(avatarName);
        if (avatarRegion == null)
            avatarRegion = atlas.findRegion("cat1");

        if (avatarRegion != null) {
            avatarImage = new Image(avatarRegion);
            avatarImage.setSize(AVATAR_SIZE, AVATAR_SIZE);
            avatarImage.setPosition(PADDING, (HEIGHT - AVATAR_SIZE) / 2f);
            addActor(avatarImage);
        }

        // Name label
        Label.LabelStyle nameStyle = new Label.LabelStyle();
        nameStyle.font = skin.getFont("Blue_font");
        nameStyle.fontColor = TEXT_WHITE;

        usernameLabel = new Label(truncate(username, 8), nameStyle);
        usernameLabel.setFontScale(0.8f);
        usernameLabel.setPosition(PADDING + AVATAR_SIZE + 6, HEIGHT / 2f + 5);
        addActor(usernameLabel);

        // Balance label
        Label.LabelStyle balStyle = new Label.LabelStyle();
        balStyle.font = skin.getFont("Green_font");
        balStyle.fontColor = TEXT_GREEN;

        balanceLabel = new Label(formatMoney(balance), balStyle);
        balanceLabel.setFontScale(0.7f);
        balanceLabel.setPosition(PADDING + AVATAR_SIZE + 6, HEIGHT / 2f - 12);
        addActor(balanceLabel);

        // Status label (Ready/Thinking)
        statusLabel = new Label("", nameStyle);
        statusLabel.setFontScale(0.6f);
        statusLabel.setPosition(PADDING + AVATAR_SIZE + 6, 5);
        statusLabel.setColor(Color.YELLOW);
        addActor(statusLabel);

        // Card group (positioned to right of seat)
        cardGroup = new Group();
        cardGroup.setPosition(WIDTH - 5, HEIGHT / 2f - 20);
        addActor(cardGroup);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Draw background
        if (currentBg != null) {
            batch.setColor(1, 1, 1, parentAlpha);
            batch.draw(currentBg, getX(), getY(), getWidth(), getHeight());
        }
        super.draw(batch, parentAlpha);
    }

    // ==================== CARD METHODS ====================

    public void showBackCards(int count) {
        cardGroup.clearChildren();
        holeCards.clear();

        if (count <= 0 || hasFolded)
            return;

        float cardW = CardActor.CARD_WIDTH * CARD_SCALE;
        float cardH = CardActor.CARD_HEIGHT * CARD_SCALE;

        for (int i = 0; i < Math.min(count, 2); i++) {
            CardActor card = new CardActor(null, false);
            card.setSize(cardW, cardH);
            card.setPosition(i * CARD_OFFSET, 0);
            holeCards.add(card);
            cardGroup.addActor(card);
        }
    }

    public void updateCards(List<Card> cards) {
        cardGroup.clearChildren();
        holeCards.clear();

        if (cards == null || cards.isEmpty() || hasFolded)
            return;

        float cardW = CardActor.CARD_WIDTH * CARD_SCALE;
        float cardH = CardActor.CARD_HEIGHT * CARD_SCALE;

        for (int i = 0; i < cards.size(); i++) {
            CardActor card = new CardActor(cards.get(i), isLocalPlayer);
            card.setSize(cardW, cardH);
            card.setPosition(i * CARD_OFFSET, 0);
            holeCards.add(card);
            cardGroup.addActor(card);
        }
    }

    // ==================== STATE METHODS ====================

    public void setActive(boolean active) {
        this.isCurrentTurn = active;

        if (active && bgActiveTex != null) {
            currentBg = new TextureRegion(bgActiveTex);
            if (usernameLabel != null)
                usernameLabel.setColor(Color.YELLOW);
            setStatus("THINKING");
        } else if (bgNormalTex != null) {
            currentBg = new TextureRegion(bgNormalTex);
            if (usernameLabel != null)
                usernameLabel.setColor(TEXT_WHITE);
            setStatus("");
        }
    }

    public void setCurrentTurn(boolean isTurn) {
        setActive(isTurn);
    }

    public void setStatus(String status) {
        this.status = status;
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    public void setDealer(boolean dealer) {
        this.isDealer = dealer;
        // Could add dealer chip indicator here if needed
    }

    public void setCardCount(int count) {
        if (!isLocalPlayer) {
            showBackCards(count);
        }
    }

    public void setFolded(boolean folded) {
        this.hasFolded = folded;
        if (folded) {
            setColor(0.5f, 0.5f, 0.5f, 0.6f);
            cardGroup.clearChildren();
            holeCards.clear();
            setStatus("FOLDED");
        } else {
            setColor(Color.WHITE);
            setStatus("");
        }
    }

    public void updateBalance(long newBal) {
        this.balance = newBal;
        if (balanceLabel != null)
            balanceLabel.setText(formatMoney(balance));
    }

    public void updateUsername(String name) {
        this.username = name;
        if (usernameLabel != null)
            usernameLabel.setText(truncate(name, 8));
    }

    // ==================== HELPERS ====================

    private String truncate(String s, int max) {
        if (s == null)
            return "???";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private String formatMoney(long amt) {
        if (amt >= 1_000_000)
            return String.format("%.1fM", amt / 1e6);
        if (amt >= 1_000)
            return String.format("%.1fK", amt / 1e3);
        return "$" + amt;
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
}
