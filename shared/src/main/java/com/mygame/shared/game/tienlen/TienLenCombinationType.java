package com.mygame.shared.game.tienlen;

/**
 * Enum đại diện cho loại combination trong Tiến Lên
 */
public enum TienLenCombinationType {
    SINGLE,           // Đơn
    PAIR,             // Đôi
    TRIPLE,           // Tam
    STRAIGHT,         // Sảnh (>=3 lá)
    FOUR_OF_A_KIND,   // Tứ quý (bomb)
    PAIR_SEQUENCE,    // Đôi thông (>=3 đôi)
    TRIPLE_SEQUENCE,  // Tam thông (>=2 tam)
    INVALID           // Không hợp lệ
}
