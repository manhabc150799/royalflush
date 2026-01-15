package com.mygame.shared.game.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a hand of cards held by a player.
 * Pure Java - no LibGDX dependencies so Server can use it.
 */
public class Hand {
    private List<Card> cards;

    /**
     * No-arg constructor for KryoNet serialization.
     */
    public Hand() {
        this.cards = new ArrayList<>();
    }

    /**
     * Add a card to the hand.
     */
    public void addCard(Card c) {
        if (c != null) {
            cards.add(c);
        }
    }

    /**
     * Add multiple cards to the hand.
     */
    public void addCards(List<Card> cardsToAdd) {
        if (cardsToAdd != null) {
            for (Card c : cardsToAdd) {
                addCard(c);
            }
        }
    }

    /**
     * Remove a card from the hand.
     * 
     * @return true if card was found and removed
     */
    public boolean removeCard(Card c) {
        return cards.remove(c);
    }

    /**
     * Remove multiple cards from the hand.
     */
    public void removeCards(List<Card> cardsToRemove) {
        if (cardsToRemove != null) {
            cards.removeAll(cardsToRemove);
        }
    }

    /**
     * Sort cards by Rank ascending.
     */
    public void sort() {
        Collections.sort(cards);
    }

    /**
     * Get all cards in this hand.
     * 
     * @return a new list containing the cards (defensive copy)
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /**
     * Get the number of cards in hand.
     */
    public int size() {
        return cards.size();
    }

    /**
     * Check if hand is empty.
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Check if hand contains a specific card.
     */
    public boolean contains(Card c) {
        return cards.contains(c);
    }

    /**
     * Clear all cards from the hand.
     */
    public void clear() {
        cards.clear();
    }

    @Override
    public String toString() {
        if (cards.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cards.size(); i++) {
            sb.append(cards.get(i).getAssetName());
            if (i < cards.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
