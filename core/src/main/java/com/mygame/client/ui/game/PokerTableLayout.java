package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.mygame.client.ui.UISkinManager;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PokerTableLayout - Main Table-based layout for Poker game UI.
 * 
 * Layout matches the ASCII blueprint:
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ TOP ROW: [User 2] [User 3] │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ MIDDLE ROW: [User 1] [Community Cards + Pot] [User 4] │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ BOTTOM ROW: [Deck] [Main User] [Control Panel] │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 */
public class PokerTableLayout extends Table {

    private static final String TAG = "PokerTableLayout";

    // Layout constants
    private static final float COMMUNITY_CARD_SCALE = 0.7f;
    private static final float DECK_SCALE = 0.6f;

    // UI Components
    private Skin skin;
    private TextureAtlas atlas;

    // Player seats (indexed by visual position 1-4, 0 = main user)
    private Map<Integer, PlayerSeatWidget> playerSeats;
    private PlayerSeatWidget mainUserSeat;

    // Community cards area
    private Table communityCardsTable;
    private CardActor[] communityCards;
    private Label potLabel;

    // Deck area
    private Image deckImage;
    private Label deckLabel;

    // Control panel
    private Table controlPanel;
    private Slider betSlider;
    private Label betAmountLabel;
    private TextButton checkButton;
    private TextButton callButton;
    private TextButton raiseButton;
    private TextButton foldButton;

    // Callbacks for button actions
    private Runnable onCheckAction;
    private Runnable onCallAction;
    private Runnable onFoldAction;
    private RaiseCallback onRaiseAction;

    // Number formatter
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    @FunctionalInterface
    public interface RaiseCallback {
        void onRaise(long amount);
    }

    /**
     * Create the PokerTableLayout.
     */
    public PokerTableLayout() {
        skin = UISkinManager.getInstance().getSkin();
        atlas = skin.getAtlas();
        playerSeats = new HashMap<>();
        communityCards = new CardActor[5];

        // Table configuration
        setFillParent(true);
        top();

        // DEBUG: Disabled - set to true to see wireframe layout
        setDebug(false);

        buildLayout();

        Gdx.app.log(TAG, "PokerTableLayout initialized");
    }

    /**
     * Build the complete 3-row layout structure.
     */
    private void buildLayout() {
        defaults().pad(10);

        // === TOP ROW: User 2 and User 3 ===
        Table topRow = buildTopRow();
        add(topRow).expandX().fillX().top();
        row();

        // === MIDDLE ROW: User 1 | Community + Pot | User 4 ===
        Table middleRow = buildMiddleRow();
        add(middleRow).expand().fill();
        row();

        // === BOTTOM ROW: Deck | Main User | Controls ===
        Table bottomRow = buildBottomRow();
        add(bottomRow).expandX().fillX().bottom();
    }

    /**
     * Build the top row containing User 2 and User 3.
     */
    private Table buildTopRow() {
        Table topRow = new Table();
        topRow.defaults().pad(20);

        // User 2 seat (top-left)
        PlayerSeatWidget seat2 = createPlaceholderSeat(2);
        playerSeats.put(2, seat2);

        // User 3 seat (top-right)
        PlayerSeatWidget seat3 = createPlaceholderSeat(3);
        playerSeats.put(3, seat3);

        // Layout: [expand] [User2] [spacing] [User3] [expand]
        topRow.add().expandX(); // Left spacer
        topRow.add(seat2).size(PlayerSeatWidget.WIDGET_WIDTH, PlayerSeatWidget.WIDGET_HEIGHT);
        topRow.add().width(100); // Center spacing
        topRow.add(seat3).size(PlayerSeatWidget.WIDGET_WIDTH, PlayerSeatWidget.WIDGET_HEIGHT);
        topRow.add().expandX(); // Right spacer

        return topRow;
    }

    /**
     * Build the middle row containing side players and community cards.
     */
    private Table buildMiddleRow() {
        Table middleRow = new Table();
        middleRow.defaults().pad(10);

        // User 1 seat (left side)
        PlayerSeatWidget seat1 = createPlaceholderSeat(1);
        playerSeats.put(1, seat1);

        // Community cards + Pot (center)
        Table centerArea = buildCenterArea();

        // User 4 seat (right side)
        PlayerSeatWidget seat4 = createPlaceholderSeat(4);
        playerSeats.put(4, seat4);

        // Layout: [User1] [expand Center expand] [User4]
        middleRow.add(seat1).size(PlayerSeatWidget.WIDGET_WIDTH, PlayerSeatWidget.WIDGET_HEIGHT).left();
        middleRow.add(centerArea).expand().fill().center();
        middleRow.add(seat4).size(PlayerSeatWidget.WIDGET_WIDTH, PlayerSeatWidget.WIDGET_HEIGHT).right();

        return middleRow;
    }

    /**
     * Build the center area containing community cards and pot.
     */
    private Table buildCenterArea() {
        Table centerArea = new Table();
        centerArea.defaults().pad(5);

        // Community cards container
        communityCardsTable = new Table();
        communityCardsTable.setBackground(skin.getDrawable("panel2"));
        communityCardsTable.pad(10);

        float cardW = CardActor.CARD_WIDTH * COMMUNITY_CARD_SCALE;
        float cardH = CardActor.CARD_HEIGHT * COMMUNITY_CARD_SCALE;

        // Create 5 card slots
        for (int i = 0; i < 5; i++) {
            communityCards[i] = new CardActor(null, false);
            communityCards[i].setSize(cardW, cardH);
            communityCards[i].setVisible(false); // Hidden until dealt
            communityCardsTable.add(communityCards[i]).size(cardW, cardH).pad(3);
        }

        centerArea.add(communityCardsTable);
        centerArea.row();

        // Pot label
        Label.LabelStyle potStyle = new Label.LabelStyle();
        potStyle.font = skin.getFont("Big_blue_font");
        potStyle.fontColor = Color.GOLD;

        potLabel = new Label("POT: $0", potStyle);
        potLabel.setFontScale(1.0f);
        potLabel.setAlignment(Align.center);

        centerArea.add(potLabel).padTop(10);

        return centerArea;
    }

    /**
     * Build the bottom row containing deck, main user, and controls.
     */
    private Table buildBottomRow() {
        Table bottomRow = new Table();
        bottomRow.defaults().pad(10).bottom();

        // Deck area (left)
        Table deckArea = buildDeckArea();

        // Main user seat (center)
        mainUserSeat = new PlayerSeatWidget(0, "YOU", 0, true);
        playerSeats.put(0, mainUserSeat);

        // Control panel (right)
        controlPanel = buildControlPanel();

        // Layout: [Deck] [expand MainUser expand] [Controls]
        bottomRow.add(deckArea).left().padLeft(20);
        bottomRow.add(mainUserSeat).expand().center()
                .size(PlayerSeatWidget.WIDGET_WIDTH + 20, PlayerSeatWidget.WIDGET_HEIGHT + 10);
        bottomRow.add(controlPanel).right().padRight(20);

        return bottomRow;
    }

    /**
     * Build the deck display area.
     */
    private Table buildDeckArea() {
        Table deckArea = new Table();
        deckArea.defaults().pad(4);

        // Deck image
        TextureAtlas.AtlasRegion deckBack = atlas.findRegion("BACK");
        if (deckBack != null) {
            deckImage = new Image(deckBack);
            float deckW = CardActor.CARD_WIDTH * DECK_SCALE;
            float deckH = CardActor.CARD_HEIGHT * DECK_SCALE;
            deckArea.add(deckImage).size(deckW, deckH);
        }

        deckArea.row();

        // "DECK" label
        Label.LabelStyle deckLabelStyle = new Label.LabelStyle();
        deckLabelStyle.font = skin.getFont("Blue_font");
        deckLabelStyle.fontColor = Color.WHITE;

        deckLabel = new Label("DECK", deckLabelStyle);
        deckLabel.setFontScale(0.7f);
        deckLabel.setAlignment(Align.center);
        deckArea.add(deckLabel).center();

        return deckArea;
    }

    /**
     * Build the control panel with slider and action buttons.
     */
    private Table buildControlPanel() {
        Table panel = new Table();
        panel.defaults().pad(5);
        panel.setBackground(skin.getDrawable("panel1"));
        panel.pad(10);

        // === Slider row ===
        Table sliderRow = new Table();

        // Minus button
        ImageButton minusBtn = createIconButton("btn_minus1", "btn_minus2");
        minusBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                adjustBet(-100);
            }
        });

        // Bet slider
        betSlider = new Slider(0, 10000, 100, false, skin);
        betSlider.setValue(0);
        betSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateBetLabel();
            }
        });

        // Plus button
        ImageButton plusBtn = createIconButton("btn_plus1", "btn_plus2");
        plusBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                adjustBet(100);
            }
        });

        sliderRow.add(minusBtn).size(40, 40);
        sliderRow.add(betSlider).width(150).padLeft(8).padRight(8);
        sliderRow.add(plusBtn).size(40, 40);

        panel.add(sliderRow);
        panel.row();

        // Bet amount label
        Label.LabelStyle amountStyle = new Label.LabelStyle();
        amountStyle.font = skin.getFont("Blue_font");
        amountStyle.fontColor = Color.GOLD;

        betAmountLabel = new Label("$0", amountStyle);
        betAmountLabel.setFontScale(0.9f);
        betAmountLabel.setAlignment(Align.center);

        panel.add(betAmountLabel).padTop(2).padBottom(8);
        panel.row();

        // === Button row ===
        Table buttonRow = new Table();
        buttonRow.defaults().pad(4);

        // Check button
        checkButton = createTextButton("CHECK");
        checkButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onCheckAction != null)
                    onCheckAction.run();
            }
        });

        // Call button
        callButton = createTextButton("CALL");
        callButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onCallAction != null)
                    onCallAction.run();
            }
        });

        buttonRow.add(checkButton).size(70, 35);
        buttonRow.add(callButton).size(70, 35);

        panel.add(buttonRow);
        panel.row();

        // Second button row
        Table buttonRow2 = new Table();
        buttonRow2.defaults().pad(4);

        // Raise button
        raiseButton = createTextButton("RAISE");
        raiseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onRaiseAction != null) {
                    onRaiseAction.onRaise((long) betSlider.getValue());
                }
            }
        });

        // Fold button
        foldButton = createTextButton("FOLD");
        foldButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onFoldAction != null)
                    onFoldAction.run();
            }
        });

        buttonRow2.add(raiseButton).size(70, 35);
        buttonRow2.add(foldButton).size(70, 35);

        panel.add(buttonRow2);

        return panel;
    }

    /**
     * Create a text button with the standard style.
     */
    private TextButton createTextButton(String text) {
        return new TextButton(text, skin, "blue_text_button");
    }

    /**
     * Create an ImageButton from atlas regions.
     */
    private ImageButton createIconButton(String upRegion, String downRegion) {
        TextureAtlas.AtlasRegion up = atlas.findRegion(upRegion);
        TextureAtlas.AtlasRegion down = atlas.findRegion(downRegion);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        if (up != null) {
            style.imageUp = new TextureRegionDrawable(up);
        }
        if (down != null) {
            style.imageDown = new TextureRegionDrawable(down);
        }

        return new ImageButton(style);
    }

    /**
     * Create a placeholder seat for a given visual index.
     */
    private PlayerSeatWidget createPlaceholderSeat(int visualIndex) {
        return new PlayerSeatWidget(visualIndex, "Player " + visualIndex, 0, false);
    }

    // ==================== PUBLIC API ====================

    /**
     * Update a player seat with real data.
     */
    public void updateSeat(int visualIndex, int playerId, String username, long balance, boolean isLocal) {
        PlayerSeatWidget seat = playerSeats.get(visualIndex);
        if (seat != null) {
            // Replace with new data
            seat.updateUsername(username);
            seat.updateCredits(balance);
        }
    }

    /**
     * Get player seat by visual index.
     */
    public PlayerSeatWidget getSeat(int visualIndex) {
        return playerSeats.get(visualIndex);
    }

    /**
     * Get main user seat.
     */
    public PlayerSeatWidget getMainUserSeat() {
        return mainUserSeat;
    }

    /**
     * Update pot display.
     */
    public void updatePot(long amount) {
        if (potLabel != null) {
            potLabel.setText("POT: $" + NUMBER_FORMAT.format(amount));
        }
    }

    /**
     * Set community card at position (0-4).
     */
    public void setCommunityCard(int position, com.mygame.shared.game.card.Card card) {
        if (position >= 0 && position < 5 && communityCards[position] != null) {
            communityCards[position].setCard(card);
            communityCards[position].setFaceUp(true);
            communityCards[position].setVisible(true);
        }
    }

    /**
     * Clear all community cards.
     */
    public void clearCommunityCards() {
        for (CardActor cardActor : communityCards) {
            if (cardActor != null) {
                cardActor.setCard(null);
                cardActor.setFaceUp(false);
                cardActor.setVisible(false);
            }
        }
    }

    /**
     * Set slider range.
     */
    public void setSliderRange(float min, float max) {
        if (betSlider != null) {
            betSlider.setRange(min, max);
            betSlider.setValue(min);
            updateBetLabel();
        }
    }

    /**
     * Show/hide control panel.
     */
    public void setControlsVisible(boolean visible) {
        if (controlPanel != null) {
            controlPanel.setVisible(visible);
        }
    }

    /**
     * Set Check button visibility.
     */
    public void setCheckButtonVisible(boolean visible) {
        if (checkButton != null) {
            checkButton.setVisible(visible);
        }
    }

    /**
     * Set Call button visibility and text.
     */
    public void setCallButtonVisible(boolean visible, long amount) {
        if (callButton != null) {
            callButton.setVisible(visible);
            if (visible && amount >= 0) {
                // Use legacy setText if available or recreate logic? TextButton has setText.
                callButton.setText("CALL $" + NUMBER_FORMAT.format(amount));
            } else {
                callButton.setText("CALL");
            }
        }
    }

    /**
     * setCallButtonVisible overload for simple toggle
     */
    public void setCallButtonVisible(boolean visible) {
        if (callButton != null) {
            callButton.setVisible(visible);
        }
    }

    // ==================== CALLBACKS ====================

    public void setOnCheckAction(Runnable action) {
        this.onCheckAction = action;
    }

    public void setOnCallAction(Runnable action) {
        this.onCallAction = action;
    }

    public void setOnFoldAction(Runnable action) {
        this.onFoldAction = action;
    }

    public void setOnRaiseAction(RaiseCallback action) {
        this.onRaiseAction = action;
    }

    // ==================== PRIVATE HELPERS ====================

    private void adjustBet(int delta) {
        float current = betSlider.getValue();
        float newValue = Math.max(betSlider.getMinValue(),
                Math.min(betSlider.getMaxValue(), current + delta));
        betSlider.setValue(newValue);
        updateBetLabel();
    }

    private void updateBetLabel() {
        if (betAmountLabel != null) {
            betAmountLabel.setText("$" + NUMBER_FORMAT.format((long) betSlider.getValue()));
        }
    }

    /**
     * Disable debug mode (call after verifying layout).
     */
    public void disableDebug() {
        setDebug(false);
    }
}
