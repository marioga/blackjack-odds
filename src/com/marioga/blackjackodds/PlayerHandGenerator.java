package com.marioga.blackjackodds;

/**
 * This class generates all possible player hands in a game
 * of Blackjack, given the number of decks and a choice of
 * withdrawn cards.
 * 
 * @author marioga
 *
 */

import java.util.ArrayList;

public class PlayerHandGenerator {
    private static final int[] VALUES = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final int[] OCURRENCES = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 16 };
    private static final int SIZE = 10;
    
    private static ArrayList<int[]> sPlayerHands = new ArrayList<>();

    private PlayerHandGenerator() {}

    private static int softValue(int[] hand) {
        int sum = 0;
        for (int i = 0; i < SIZE; i++) {
            sum += hand[i] * VALUES[i];
        }
        return sum;

    }

    private static void hands(int numDecks, int[] withdrawnCards, int fillingPosition,
            int arrayListPos) {
        for (int i = fillingPosition; i < SIZE; i++) {
            int[] tempArray = sPlayerHands.get(arrayListPos).clone();
            if (softValue(tempArray) + VALUES[i] <= 21
                    && numDecks * OCURRENCES[i] >= withdrawnCards[i] + tempArray[i] + 1) {
                tempArray[i] += 1;
                sPlayerHands.add(tempArray);
                hands(numDecks, withdrawnCards, i, sPlayerHands.size() - 1);
                if (arrayListPos == 0) {
                    // Remove hands with only one card
                    sPlayerHands.remove(tempArray);
                }
            } else {
                hands(numDecks, withdrawnCards, i + 1, sPlayerHands.size() - 1);
                break;
            }
        }
    }

    public static Iterable<int[]> getHands(int numDecks, int[] withdrawnCards) {
        sPlayerHands = new ArrayList<>();
        sPlayerHands.add(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
        hands(numDecks, withdrawnCards, 0, 0);
        sPlayerHands.remove(0); // Remove initial empty array
        return sPlayerHands;
    }
}
