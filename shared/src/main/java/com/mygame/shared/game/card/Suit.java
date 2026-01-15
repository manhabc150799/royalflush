package com.mygame.shared.game.card;

/**
 * Enum đại diện cho chất bài
 */
public enum Suit {
    CLUBS("C", "Clubs", 1, "c"),
    DIAMONDS("D", "Diamonds", 2, "d"),
    HEARTS("H", "Hearts", 3, "h"),
    SPADES("P", "Spades", 4, "p");

    private final String letter;
    private final String name;
    private final int value;
    private final String code; // Lowercase code for asset naming

    Suit(String letter, String name, int value, String code) {
        this.letter = letter;
        this.name = name;
        this.value = value;
        this.code = code;
    }

    public String getLetter() {
        return letter;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    /**
     * Get lowercase code for asset naming (c, d, h, p).
     */
    public String getCode() {
        return code;
    }

    /**
     * Lấy Suit từ letter (C, D, H, P)
     */
    public static Suit fromLetter(String letter) {
        for (Suit suit : values()) {
            if (suit.letter.equals(letter)) {
                return suit;
            }
        }
        return CLUBS; // Default
    }
}
