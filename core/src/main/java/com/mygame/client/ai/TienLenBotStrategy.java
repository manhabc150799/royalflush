package com.mygame.client.ai;

import com.mygame.shared.game.card.Card;
import com.mygame.shared.game.tienlen.CardCollection;
import com.mygame.shared.game.tienlen.TienLenCombinationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chiến lược ra bài cho Tiến Lên.
 *
 * Trách nhiệm chính:
 * - Sinh tất cả combination hợp lệ từ một hand theo một loại nhất định.
 * - Chọn combination "nhỏ nhất có thể chặt được" (ít tốn tài nguyên nhất).
 */
public class TienLenBotStrategy {

    /**
     * Tìm combination nhỏ nhất có thể chặt được một lượt bài đã đánh.
     *
     * @param hand        bài trên tay (đã hoặc chưa được sort)
     * @param targetType  loại combination đang nằm trên bàn
     * @param targetCards chính xác các lá đang nằm trên bàn
     * @return combination nhỏ nhất có thể chặt được, hoặc null nếu không có
     */
    public List<Card> findSmallestBeatingCombination(List<Card> hand,
                                                     TienLenCombinationType targetType,
                                                     List<Card> targetCards) {
        if (hand == null || hand.isEmpty() || targetType == null || targetCards == null || targetCards.isEmpty()) {
            return null;
        }

        // Làm việc trên bản sao và sort theo thứ tự Tiến Lên
        List<Card> sortedHand = new ArrayList<>(hand);
        sortedHand.sort((a, b) -> Integer.compare(
                a.getRankValueForTienLen(),
                b.getRankValueForTienLen()
        ));

        // Sinh tất cả combination khả dĩ theo đúng type và size
        List<List<Card>> possibleCombinations = generatePossibleCombinations(sortedHand, targetType, targetCards.size());

        // Chọn combination nhỏ nhất (theo highest card) mà vẫn chặt được target
        List<Card> bestCombination = null;
        for (List<Card> combo : possibleCombinations) {
            TienLenCombinationType comboType = CardCollection.detectCombination(combo);
            if (comboType != TienLenCombinationType.INVALID
                    && CardCollection.canBeat(targetType, targetCards, comboType, combo)) {
                if (bestCombination == null || compareCombinations(combo, bestCombination) < 0) {
                    bestCombination = combo;
                }
            }
        }

        return bestCombination;
    }

    /**
     * Sinh các combination ứng viên từ một hand theo type/size.
     */
    private List<List<Card>> generatePossibleCombinations(List<Card> hand,
                                                           TienLenCombinationType type,
                                                           int targetSize) {
        List<List<Card>> combinations = new ArrayList<>();

        switch (type) {
            case SINGLE:
                for (Card card : hand) {
                    combinations.add(Arrays.asList(card));
                }
                break;

            case PAIR:
                // Tìm các đôi
                for (int i = 0; i < hand.size() - 1; i++) {
                    if (hand.get(i).getRank() == hand.get(i + 1).getRank()) {
                        combinations.add(Arrays.asList(hand.get(i), hand.get(i + 1)));
                    }
                }
                break;

            case TRIPLE:
                // Tìm các bộ ba
                for (int i = 0; i < hand.size() - 2; i++) {
                    if (hand.get(i).getRank() == hand.get(i + 1).getRank()
                            && hand.get(i).getRank() == hand.get(i + 2).getRank()) {
                        combinations.add(Arrays.asList(hand.get(i), hand.get(i + 1), hand.get(i + 2)));
                    }
                }
                break;

            case STRAIGHT:
                // Tìm các sảnh đúng độ dài
                if (targetSize >= 3) {
                    for (int start = 0; start <= hand.size() - targetSize; start++) {
                        List<Card> straight = new ArrayList<>();
                        for (int i = 0; i < targetSize; i++) {
                            straight.add(hand.get(start + i));
                        }
                        if (CardCollection.detectCombination(straight) == TienLenCombinationType.STRAIGHT) {
                            combinations.add(straight);
                        }
                    }
                }
                break;

            case PAIR_SEQUENCE:
            case TRIPLE_SEQUENCE:
            case FOUR_OF_A_KIND:
            default:
                // Những loại nâng cao (đôi thông, tam thông, tứ quý) thường được xử lý theo
                // logic chặt heo trong CardCollection.canBeat; ở đây bot chơi đơn giản nên
                // chưa chủ động tìm các bộ đó để đánh.
                break;
        }

        return combinations;
    }

    /**
     * So sánh 2 combination theo highest Tiến Lên rank (nhỏ hơn = ưu tiên hơn cho bot).
     */
    private int compareCombinations(List<Card> combo1, List<Card> combo2) {
        int max1 = combo1.stream().mapToInt(Card::getRankValueForTienLen).max().orElse(0);
        int max2 = combo2.stream().mapToInt(Card::getRankValueForTienLen).max().orElse(0);
        return Integer.compare(max1, max2);
    }
}
