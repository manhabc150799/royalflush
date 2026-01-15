package com.mygame.shared.game.card;

/**
 * Class đại diện cho một lá bài
 * Rank: 2-14 (2-10, 11=J, 12=Q, 13=K, 14=A)
 */
public class Card implements Comparable<Card> {
    private int rank; // 2-14
    private Suit suit;

    /**
     * No-arg constructor for Kryo serialization.
     */
    public Card() {
    }

    public Card(int rank, Suit suit) {
        if (rank < 2 || rank > 14) {
            throw new IllegalArgumentException("Rank must be between 2 and 14");
        }
        this.rank = rank;
        this.suit = suit;
    }

    public int getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }

    /**
     * Lấy rank value cho Tiến Lên (3=1, 4=2, ..., 2=13, A=14)
     */
    public int getRankValueForTienLen() {
        if (rank == 3)
            return 1;
        if (rank == 4)
            return 2;
        if (rank == 5)
            return 3;
        if (rank == 6)
            return 4;
        if (rank == 7)
            return 5;
        if (rank == 8)
            return 6;
        if (rank == 9)
            return 7;
        if (rank == 10)
            return 8;
        if (rank == 11)
            return 9; // J
        if (rank == 12)
            return 10; // Q
        if (rank == 13)
            return 11; // K
        if (rank == 14)
            return 12; // A
        if (rank == 2)
            return 13; // 2
        return 0; // Should not happen
    }

    /**
     * Get suit value for Tien Len (Spades < Clubs < Diamonds < Hearts)
     * Spades (P) = 1
     * Clubs (C) = 2
     * Diamonds (D) = 3
     * Hearts (H) = 4
     */
    public int getSuitValueForTienLen() {
        switch (suit) {
            case SPADES:
                return 1;
            case CLUBS:
                return 2;
            case DIAMONDS:
                return 3;
            case HEARTS:
                return 4;
            default:
                return 0;
        }
    }

    /**
     * Lấy đường dẫn asset cho lá bài này
     */
    public String getAssetPath() {
        String rankStr;
        if (rank == 11)
            rankStr = "J";
        else if (rank == 12)
            rankStr = "Q";
        else if (rank == 13)
            rankStr = "K";
        else if (rank == 14)
            rankStr = "A";
        else
            rankStr = String.valueOf(rank);

        return "images/cards/light/" + rankStr + "-" + suit.getLetter() + ".png";
    }

    /**
     * Get asset name in format "Rank-SuitCode" for UI mapping.
     * Uses lowercase suit codes: c, d, h, p
     * Example: Ace of Spades = "14-p", Queen of Hearts = "12-h"
     */
    public String getAssetName() {
        return rank + "-" + suit.getCode();
    }

    /**
     * Lấy đường dẫn asset cho mặt sau
     */
    public static String getBackAssetPath() {
        return "images/cards/light/BACK.png";
    }

    @Override
    public int compareTo(Card other) {
        int rankCompare = Integer.compare(this.rank, other.rank);
        if (rankCompare != 0)
            return rankCompare;
        return this.suit.compareTo(other.suit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Card card = (Card) obj;
        return rank == card.rank && suit == card.suit;
    }

    @Override
    public int hashCode() {
        return rank * 31 + suit.hashCode();
    }

    @Override
    public String toString() {
        String rankStr;
        if (rank == 11)
            rankStr = "J";
        else if (rank == 12)
            rankStr = "Q";
        else if (rank == 13)
            rankStr = "K";
        else if (rank == 14)
            rankStr = "A";
        else
            rankStr = String.valueOf(rank);

        return rankStr + suit.getLetter();
    }
}
