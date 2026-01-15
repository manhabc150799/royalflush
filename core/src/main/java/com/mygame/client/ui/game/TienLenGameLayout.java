package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.mygame.client.ui.UISkinManager;
import com.mygame.shared.game.card.Card;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TienLenGameLayout - Main Table-based layout for Tien Len (Vietnamese Poker).
 * 
 * Layout matches the ASCII blueprint:
 * ┌───────────────────────────────────────────────────────────────────────┐
 * │ [ RIVAL TOP ] │
 * ├───────────────────────────────────────────────────────────────────────┤
 * │ [ RIVAL LEFT ] [ CENTER PILE ] [ RIVAL RIGHT ] │
 * ├───────────────────────────────────────────────────────────────────────┤
 * │ [ ACTION BUTTONS ] │
 * │ [ MY INFO ] Credits | Name │
 * │ [ MY HAND AREA (13 Cards) ] │
 * └───────────────────────────────────────────────────────────────────────┘
 */
public class TienLenGameLayout extends Table {

    private static final String TAG = "TienLenGameLayout";

    // UI Components
    private Skin skin;
    private TextureAtlas atlas;

    // Rival widgets (visual positions)
    private TienLenPlayerWidget rivalTop;
    private TienLenPlayerWidget rivalLeft;
    private TienLenPlayerWidget rivalRight;
    private Map<Integer, TienLenPlayerWidget> playerWidgets; // playerId -> widget

    // Deck area (top-left corner)
    private Image deckImage;
    private Label deckLabel;
    private static final float DECK_SCALE = 0.5f;

    // Center pile (last played cards)
    private Table centerPile;
    private Label centerPileLabel;

    // My player info
    private Table myInfoSection;
    private Label myNameLabel;
    private Label myCreditsLabel;

    // My hand
    private MyHandWidget myHandWidget;

    // Action buttons
    private Table actionButtonsRow;
    private TextButton sortButton;
    private TextButton playButton;
    private TextButton skipButton;

    // Callbacks
    private Runnable onSortAction;
    private Runnable onPlayAction;
    private Runnable onSkipAction;

    // Number formatter
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    /**
     * Create the TienLenGameLayout.
     */
    public TienLenGameLayout() {
        skin = UISkinManager.getInstance().getSkin();
        atlas = skin.getAtlas();
        playerWidgets = new HashMap<>();

        // Table configuration
        setFillParent(true);
        top();

        // DEBUG: Disabled - set to true to see wireframe layout
        setDebug(false);

        buildLayout();

        Gdx.app.log(TAG, "TienLenGameLayout initialized");
    }

    /**
     * Build the complete layout structure.
     */
    private void buildLayout() {
        defaults().pad(8);

        // === TOP ROW: Rival Top (Centered) ===
        Table topRow = buildTopRow();
        add(topRow).expandX().fillX().top();
        row();

        // === MIDDLE ROW: Rival Left | Center Pile | Rival Right ===
        Table middleRow = buildMiddleRow();
        add(middleRow).expand().fill();
        row();

        // === BOTTOM SECTION: Actions + My Info + My Hand ===
        Table bottomSection = buildBottomSection();
        add(bottomSection).expandX().fillX().bottom();
    }

    /**
     * Build the top row with Deck (left) and Rival Top (center).
     */
    private Table buildTopRow() {
        Table topRow = new Table();
        topRow.defaults().pad(10);

        // Deck area (left side)
        Table deckArea = buildDeckArea();

        // Rival Top (center)
        rivalTop = new TienLenPlayerWidget(0, "Rival Top", 0);
        playerWidgets.put(2, rivalTop); // Visual position 2 = top

        // Layout: [Deck] [expand] [Rival Top] [expand]
        topRow.add(deckArea).left().padLeft(20);
        topRow.add().expandX(); // Left spacer
        topRow.add(rivalTop).center();
        topRow.add().expandX(); // Right spacer

        return topRow;
    }

    /**
     * Build the deck display area.
     */
    private Table buildDeckArea() {
        Table deckArea = new Table();
        deckArea.defaults().pad(4);

        // Deck image (card back)
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
        deckLabel.setFontScale(0.6f);
        deckLabel.setAlignment(Align.center);
        deckArea.add(deckLabel).center();

        return deckArea;
    }

    /**
     * Build the middle row with side rivals and center pile.
     */
    private Table buildMiddleRow() {
        Table middleRow = new Table();
        middleRow.defaults().pad(10);

        // Rival Left
        rivalLeft = new TienLenPlayerWidget(0, "Rival Left", 0);
        playerWidgets.put(1, rivalLeft); // Visual position 1 = left

        // Center Pile (where played cards appear)
        centerPile = buildCenterPile();

        // Rival Right
        rivalRight = new TienLenPlayerWidget(0, "Rival Right", 0);
        playerWidgets.put(3, rivalRight); // Visual position 3 = right

        // Layout
        middleRow.add(rivalLeft).left().width(140);
        middleRow.add(centerPile).expand().fill().center();
        middleRow.add(rivalRight).right().width(140);

        return middleRow;
    }

    /**
     * Build the center pile area for last played cards.
     */
    private Table buildCenterPile() {
        Table pile = new Table();
        pile.setBackground(skin.getDrawable("panel2"));
        pile.pad(15);

        // Label for instructions or last played info
        Label.LabelStyle style = new Label.LabelStyle();
        style.font = skin.getFont("Blue_font");
        style.fontColor = new Color(0.7f, 0.7f, 0.8f, 1f);

        centerPileLabel = new Label("Play cards here", style);
        centerPileLabel.setFontScale(0.8f);
        centerPileLabel.setAlignment(Align.center);

        pile.add(centerPileLabel).center();

        return pile;
    }

    /**
     * Build the bottom section with actions, my info, and my hand.
     */
    private Table buildBottomSection() {
        Table bottomSection = new Table();
        bottomSection.defaults().pad(5);

        // Action buttons row
        actionButtonsRow = buildActionButtons();
        bottomSection.add(actionButtonsRow).expandX().center();
        bottomSection.row();

        // My info (Name + Credits) - width matches button row
        myInfoSection = buildMyInfo();
        // Width: 3 buttons * 90 + padding (8*4) = 302
        bottomSection.add(myInfoSection).width(302).center().padTop(5);
        bottomSection.row();

        // My hand widget
        myHandWidget = new MyHandWidget();
        myHandWidget.setHandWidth(Gdx.graphics.getWidth() * 0.85f);

        // Wrap in a container for proper table sizing
        Container<MyHandWidget> handContainer = new Container<>(myHandWidget);
        handContainer.fill();

        bottomSection.add(handContainer)
                .expandX().fillX()
                .height(CardActor.CARD_HEIGHT * 0.55f + 40)
                .padBottom(15);

        return bottomSection;
    }

    /**
     * Build action buttons row.
     */
    private Table buildActionButtons() {
        Table buttons = new Table();
        buttons.defaults().pad(8);

        // Sort button
        sortButton = new TextButton("SORT", skin, "blue_text_button");
        sortButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onSortAction != null)
                    onSortAction.run();
                else
                    myHandWidget.sortCards(); // Default action
            }
        });

        // Play button
        playButton = new TextButton("PLAY", skin, "blue_text_button");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onPlayAction != null)
                    onPlayAction.run();
            }
        });

        // Skip/Pass button
        skipButton = new TextButton("SKIP", skin, "blue_text_button");
        skipButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onSkipAction != null)
                    onSkipAction.run();
            }
        });

        buttons.add(sortButton).size(90, 40);
        buttons.add(playButton).size(90, 40);
        buttons.add(skipButton).size(90, 40);

        return buttons;
    }

    /**
     * Build my player info section (vertical: Name on top, Credits below).
     * Uses same background as rivals, width matches button row.
     */
    private Table buildMyInfo() {
        Table info = new Table();
        // Same background as rival widgets
        info.setBackground(skin.getDrawable("panel1"));
        info.pad(8);

        // Username label (top)
        Label.LabelStyle nameStyle = new Label.LabelStyle();
        nameStyle.font = skin.getFont("Blue_font");
        nameStyle.fontColor = Color.WHITE;

        myNameLabel = new Label("Me", nameStyle);
        myNameLabel.setFontScale(0.9f);
        myNameLabel.setAlignment(Align.center);

        info.add(myNameLabel).expandX().center();
        info.row();

        // Credits label (below)
        Label.LabelStyle creditsStyle = new Label.LabelStyle();
        creditsStyle.font = skin.getFont("Green_font");
        creditsStyle.fontColor = new Color(0.4f, 1f, 0.5f, 1f);

        myCreditsLabel = new Label("0$", creditsStyle);
        myCreditsLabel.setFontScale(0.8f);
        myCreditsLabel.setAlignment(Align.center);

        info.add(myCreditsLabel).expandX().center().padTop(2);

        return info;
    }

    // ==================== PUBLIC API ====================

    /**
     * Update a rival player's info by visual position.
     *
     * @param visualPosition 1=left, 2=top, 3=right
     * @param playerId       Player ID
     * @param username       Display name
     * @param credits        Credit amount
     * @param cardCount      Cards remaining
     */
    public void updateRival(int visualPosition, int playerId, String username, long credits, int cardCount) {
        TienLenPlayerWidget widget = null;

        switch (visualPosition) {
            case 1:
                widget = rivalLeft;
                break;
            case 2:
                widget = rivalTop;
                break;
            case 3:
                widget = rivalRight;
                break;
        }

        if (widget != null) {
            widget.updateUsername(username);
            widget.updateCredits(credits);
            widget.updateCardCount(cardCount);
            playerWidgets.put(playerId, widget);
        }
    }

    /**
     * Get rival widget by player ID.
     */
    public TienLenPlayerWidget getRivalWidget(int playerId) {
        return playerWidgets.get(playerId);
    }

    /**
     * Set active player (highlights their widget).
     *
     * @param playerId Active player ID (use -1 for local player)
     */
    public void setActivePlayer(int playerId) {
        // Deactivate all
        if (rivalLeft != null)
            rivalLeft.setActive(false);
        if (rivalTop != null)
            rivalTop.setActive(false);
        if (rivalRight != null)
            rivalRight.setActive(false);

        // Activate the one with matching ID
        TienLenPlayerWidget widget = playerWidgets.get(playerId);
        if (widget != null) {
            widget.setActive(true);
        }
    }

    /**
     * Update my player info.
     */
    public void updateMyInfo(String name, long credits) {
        if (myNameLabel != null) {
            myNameLabel.setText(name);
        }
        if (myCreditsLabel != null) {
            myCreditsLabel.setText(NUMBER_FORMAT.format(credits) + "$");
        }
    }

    /**
     * Set my hand cards.
     */
    public void setMyCards(List<Card> cards) {
        if (myHandWidget != null) {
            myHandWidget.setCards(cards);
        }
    }

    /**
     * Get selected cards from my hand.
     */
    public List<Card> getSelectedCards() {
        return myHandWidget != null ? myHandWidget.getSelectedCards() : List.of();
    }

    /**
     * Remove selected cards from my hand.
     */
    public void removeSelectedCards() {
        if (myHandWidget != null) {
            myHandWidget.removeSelectedCards();
        }
    }

    /**
     * Show played cards in center pile.
     */
    public void showPlayedCards(List<Card> cards, String playerName) {
        centerPile.clearChildren();

        if (cards == null || cards.isEmpty()) {
            centerPileLabel.setText("Play cards here");
            centerPile.add(centerPileLabel).center();
            return;
        }

        // Show who played
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("Pink_font");
        labelStyle.fontColor = Color.WHITE;

        Label playedByLabel = new Label(playerName + " played:", labelStyle);
        playedByLabel.setFontScale(0.7f);
        centerPile.add(playedByLabel).center().padBottom(5);
        centerPile.row();

        // Card display row
        Table cardsRow = new Table();
        cardsRow.defaults().pad(2);

        float cardScale = 0.5f;
        float cardW = CardActor.CARD_WIDTH * cardScale;
        float cardH = CardActor.CARD_HEIGHT * cardScale;

        for (Card card : cards) {
            CardActor cardActor = new CardActor(card, true);
            cardActor.setSize(cardW, cardH);
            cardsRow.add(cardActor).size(cardW, cardH);
        }

        centerPile.add(cardsRow).center();
    }

    /**
     * Clear the center pile.
     */
    public void clearCenterPile() {
        centerPile.clearChildren();
        centerPile.add(centerPileLabel).center();
    }

    /**
     * Enable/disable action buttons.
     */
    public void setButtonsEnabled(boolean enabled) {
        if (sortButton != null)
            sortButton.setDisabled(!enabled);
        if (playButton != null)
            playButton.setDisabled(!enabled);
        if (skipButton != null)
            skipButton.setDisabled(!enabled);
    }

    // ==================== CALLBACKS ====================

    public void setOnSortAction(Runnable action) {
        this.onSortAction = action;
    }

    public void setOnPlayAction(Runnable action) {
        this.onPlayAction = action;
    }

    public void setOnSkipAction(Runnable action) {
        this.onSkipAction = action;
    }

    /**
     * Get the hand widget for direct manipulation.
     */
    public MyHandWidget getMyHandWidget() {
        return myHandWidget;
    }

    /**
     * Disable debug mode.
     */
    public void disableDebug() {
        setDebug(false);
    }
}
