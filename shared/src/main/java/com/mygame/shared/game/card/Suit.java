package com.mygame.shared.game.card;

/**
 * Enum đại diện cho chất bài
 */
public enum Suit {
    CLUBS("C", "Clubs"),
    DIAMONDS("D", "Diamonds"),
    HEARTS("H", "Hearts"),
    SPADES("P", "Spades"); // P trong assets là Spades
    
    private final String letter;
    private final String name;
    
    Suit(String letter, String name) {
        this.letter = letter;
        this.name = name;
    }
    
    public String getLetter() {
        return letter;
    }
    
    public String getName() {
        return name;
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
