package com.mygame.client.ui.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mygame.shared.game.card.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MyHandWidget - Interactive card hand for Tien Len.
 * 
 * Uses Group (not Table) for proper Z-order control of overlapping cards.
 * Cards on the right visually overlap cards on the left.
 * 
 * Interaction:
 * - Click a card: Translates Y +20px to indicate "selected"
 * - Click again: Deselects (returns to original Y)
 * 
 * Layout:
 * ┌────────────────────────────────────────────────────────────────┐
 * │ [ ][ ][ ][ ][ ][ ][ ][ ][ ][ ][ ][ ][ ] │
 * │ (13 cards overlapping horizontally, fit within screen) │
 * └────────────────────────────────────────────────────────────────┘
 */
public class MyHandWidget extends Group {

    private static final String TAG = "MyHandWidget";

    // Layout constants
    private static final float SELECTION_OFFSET_Y = 25f;
    private static final float MIN_OVERLAP = 35f; // Minimum visible portion per card
    private static final float PADDING = 10f;
    private static final float CARD_SCALE = 0.55f; // Scale down cards to fit

    // Card data
    private List<CardActor> cardActors;
    private List<Card> cards;

    // Dimensions
    private float handWidth;
    private float handHeight;

    /**
     * Create an empty MyHandWidget.
     * Call setCards() to populate with cards.
     */
    public MyHandWidget() {
        this.cardActors = new ArrayList<>();
        this.cards = new ArrayList<>();

        // Default size (will be recalculated)
        this.handWidth = Gdx.graphics.getWidth() * 0.8f;
        this.handHeight = CardActor.CARD_HEIGHT * CARD_SCALE + SELECTION_OFFSET_Y + PADDING;
        setSize(handWidth, handHeight);
    }

    /**
     * Set the available width for the hand layout.
     *
     * @param width Maximum width for card arrangement
     */
    public void setHandWidth(float width) {
        this.handWidth = width;
        repositionCards();
    }

    /**
     * Set the cards in this hand.
     * Clears existing cards and creates new CardActors.
     *
     * @param newCards List of cards to display
     */
    public void setCards(List<Card> newCards) {
        // Clear existing
        clearChildren();
        cardActors.clear();
        this.cards = new ArrayList<>(newCards);

        if (cards.isEmpty()) {
            return;
        }

        // Create card actors
        float cardW = CardActor.CARD_WIDTH * CARD_SCALE;
        float cardH = CardActor.CARD_HEIGHT * CARD_SCALE;

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            CardActor cardActor = new CardActor(card, true);
            cardActor.setSize(cardW, cardH);
            cardActor.setSelectable(true);

            // Add click listener for selection toggle
            final int index = i;
            cardActor.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    toggleCardSelection(index);
                    return true;
                }
            });

            cardActors.add(cardActor);
            addActor(cardActor);
        }

        repositionCards();
    }

    /**
     * Reposition all cards with proper overlap and Z-order.
     * Cards added later (higher index) appear on top (rightmost visible).
     */
    private void repositionCards() {
        if (cardActors.isEmpty()) {
            return;
        }

        float cardW = CardActor.CARD_WIDTH * CARD_SCALE;
        float cardH = CardActor.CARD_HEIGHT * CARD_SCALE;
        int count = cardActors.size();

        // Calculate overlap spacing
        // Total space needed without overlap: count * cardW
        // Available space: handWidth
        // Overlap amount per card: (count * cardW - handWidth) / (count - 1)
        float totalNeededWidth = count * cardW;
        float overlapPerCard = 0;

        if (count > 1 && totalNeededWidth > handWidth) {
            overlapPerCard = (totalNeededWidth - handWidth) / (count - 1);
            // Ensure minimum visibility
            float effectiveCardWidth = cardW - overlapPerCard;
            if (effectiveCardWidth < MIN_OVERLAP) {
                overlapPerCard = cardW - MIN_OVERLAP;
            }
        }

        float spacing = cardW - overlapPerCard;
        float totalWidth = spacing * (count - 1) + cardW;
        float startX = (handWidth - totalWidth) / 2f; // Center the hand

        // Position cards - later cards on top (higher Z-index)
        for (int i = 0; i < count; i++) {
            CardActor cardActor = cardActors.get(i);
            float x = startX + i * spacing;
            float y = PADDING; // Base Y position

            cardActor.setPosition(x, y);
            cardActor.setZIndex(i); // Later cards on top
        }

        // Update widget height
        this.handHeight = cardH + SELECTION_OFFSET_Y + PADDING * 2;
        setSize(handWidth, handHeight);
    }

    /**
     * Toggle selection state of a card.
     *
     * @param index Card index to toggle
     */
    private void toggleCardSelection(int index) {
        if (index < 0 || index >= cardActors.size()) {
            return;
        }

        CardActor cardActor = cardActors.get(index);
        boolean wasSelected = cardActor.isSelected();

        // Toggle selection
        cardActor.setSelected(!wasSelected);

        // Animate Y position
        float baseY = PADDING;
        float targetY = cardActor.isSelected() ? baseY + SELECTION_OFFSET_Y : baseY;

        cardActor.clearActions();
        cardActor.addAction(Actions.moveTo(cardActor.getX(), targetY, 0.15f, Interpolation.smooth));

        Gdx.app.log(TAG, "Card " + index + " selected: " + cardActor.isSelected());
    }

    // ==================== PUBLIC API ====================

    /**
     * Get list of currently selected cards.
     *
     * @return List of selected Card objects
     */
    public List<Card> getSelectedCards() {
        List<Card> selected = new ArrayList<>();
        for (int i = 0; i < cardActors.size(); i++) {
            if (cardActors.get(i).isSelected()) {
                selected.add(cards.get(i));
            }
        }
        return selected;
    }

    /**
     * Get selected CardActor objects.
     *
     * @return List of selected CardActor objects
     */
    public List<CardActor> getSelectedCardActors() {
        List<CardActor> selected = new ArrayList<>();
        for (CardActor cardActor : cardActors) {
            if (cardActor.isSelected()) {
                selected.add(cardActor);
            }
        }
        return selected;
    }

    /**
     * Deselect all cards.
     */
    public void unselectAll() {
        float baseY = PADDING;
        for (CardActor cardActor : cardActors) {
            if (cardActor.isSelected()) {
                cardActor.setSelected(false);
                cardActor.clearActions();
                cardActor.addAction(Actions.moveTo(cardActor.getX(), baseY, 0.15f, Interpolation.smooth));
            }
        }
    }

    /**
     * Remove selected cards from hand (after playing).
     */
    public void removeSelectedCards() {
        List<CardActor> toRemove = new ArrayList<>();
        List<Card> cardsToRemove = new ArrayList<>();

        for (int i = 0; i < cardActors.size(); i++) {
            if (cardActors.get(i).isSelected()) {
                toRemove.add(cardActors.get(i));
                cardsToRemove.add(cards.get(i));
            }
        }

        // Remove from lists
        for (CardActor cardActor : toRemove) {
            cardActors.remove(cardActor);
            cardActor.remove(); // Remove from stage
        }
        cards.removeAll(cardsToRemove);

        // Reposition remaining cards
        repositionCards();
    }

    /**
     * Sort cards by rank (for Tien Len ordering).
     * In Tien Len, 3 is lowest, 2 is highest.
     */
    public void sortCards() {
        if (cards.isEmpty()) {
            return;
        }

        // Sort by Tien Len rank order
        Collections.sort(cards, new TienLenCardComparator());

        // Rebuild card actors
        setCards(cards);

        Gdx.app.log(TAG, "Cards sorted");
    }

    /**
     * Get total card count in hand.
     *
     * @return Number of cards
     */
    public int getCardCount() {
        return cards.size();
    }

    /**
     * Check if any cards are selected.
     *
     * @return True if at least one card is selected
     */
    public boolean hasSelection() {
        for (CardActor cardActor : cardActors) {
            if (cardActor.isSelected()) {
                return true;
            }
        }
        return false;
    }

    // ==================== INNER CLASSES ====================

    /**
     * Comparator for Tien Len card ordering.
     * Tien Len order: 3 < 4 < 5 < ... < K < A < 2
     * Suit order within same rank: Spades < Clubs < Diamonds < Hearts
     */
    private static class TienLenCardComparator implements Comparator<Card> {
        @Override
        public int compare(Card a, Card b) {
            int rankA = getTienLenRank(a.getRank());
            int rankB = getTienLenRank(b.getRank());

            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }

            // Same rank - compare by suit
            return Integer.compare(getSuitOrder(a), getSuitOrder(b));
        }

        private int getTienLenRank(int standardRank) {
            // Standard: 2=2, 3=3, ..., 14=Ace
            // Tien Len: 3=0, 4=1, ..., K=10, A=11, 2=12 (highest)
            if (standardRank == 2)
                return 12; // 2 is highest
            if (standardRank == 14)
                return 11; // Ace
            return standardRank - 3; // 3=0, 4=1, etc.
        }

        private int getSuitOrder(Card card) {
            // Suit order: Spades(P) < Clubs(C) < Diamonds(D) < Hearts(H)
            switch (card.getSuit().getLetter()) {
                case "P":
                    return 0; // Spades
                case "C":
                    return 1; // Clubs
                case "D":
                    return 2; // Diamonds
                case "H":
                    return 3; // Hearts
                default:
                    return 0;
            }
        }
    }
}
