package com.mygame.shared.game.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class đại diện cho bộ bài 52 lá
 */
public class Deck {
    private List<Card> cards;

    public Deck() {
        initializeDeck();
    }

    /**
     * Khởi tạo bộ bài 52 lá
     */
    private void initializeDeck() {
        cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (int rank = 2; rank <= 14; rank++) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    /**
     * Xáo bài
     */
    public void shuffle() {
        Collections.shuffle(cards, new java.util.Random());
    }

    /**
     * Chia một lá bài
     */
    public Card deal() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.remove(0);
    }

    /**
     * Chia nhiều lá bài
     */
    public List<Card> deal(int count) {
        List<Card> dealt = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (cards.isEmpty())
                break;
            dealt.add(deal());
        }
        return dealt;
    }

    /**
     * Kiểm tra còn bài không
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Số lá bài còn lại
     */
    public int remainingCards() {
        return cards.size();
    }

    /**
     * Reset và xáo lại bài
     */
    public void reset() {
        initializeDeck();
        shuffle();
    }

    /**
     * Lấy tất cả cards (không xóa)
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
}
