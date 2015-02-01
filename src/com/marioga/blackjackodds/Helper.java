package com.marioga.blackjackodds;

/**
 * This is a helper class for common Blackjack tasks
 * 
 * @author marioga
 *
 */

public class Helper {
    private static final int[] VALUES = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final int SIZE = 10;
    
    public static int valueHand(int[] hand) {
        int sum = 0;
        for (int i = 0; i < SIZE; i++) {
            sum += VALUES[i] * hand[i];
        }
        if (sum <= 11 && hand[0] > 0) {
            sum += 10;
        }
        return sum;
    }
    
    public static boolean isSoft(int[] hand) {
        int sum = 0;
        for (int i = 0; i < hand.length; i++) {
            sum += VALUES[i] * hand[i];
        }
        return sum <= 11 && hand[0] > 0;
    }
    
    public static boolean isPair(int[] hand) {
        if (numberOfCards(hand) != 2) {
            return false;
        }
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] == 2) {
                return true;
            }
        }
        return false;
    }
    
    public static int numberOfCards(int[] groupOfCards) {
        int total = 0;
        for (int i : groupOfCards) {
            total += i;
        }
        return total;
    }
    
    public static boolean isBlackJack(int[] hand) {
        return numberOfCards(hand) == 2 && valueHand(hand) == 21;
    }
}
