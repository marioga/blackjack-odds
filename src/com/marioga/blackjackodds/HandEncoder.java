package com.marioga.blackjackodds;

/**
 * This class encodes (and decodes) a player hand and a dealer card
 * into a long integer. It consists of the matrix representation of
 * a player hand viewed as a base 23 integer, which is then multiplied
 * by 10 and added the dealer card. Such repr. is unique.
 * 
 * @author marioga
 *
 */

public class HandEncoder {
    private static final int SIZE = 10;
    
    private HandEncoder(){ }
    
    public static long encodeToHashKey(int[] playerHand, int dealerCard) {
        long sum = 0;
        for (int i = SIZE - 1; i >= 0; i--) {
            sum = 23 * sum + playerHand[i];
        }
        return 10 * sum + dealerCard;
    }

    public static int[] getPlayerFromHashCode(long code) {
        int[] playerHand = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        code = code / 10;
        for (int i = 0; i < SIZE; i++) {
            playerHand[i] = (int) (code % 23);
            code = code / 23;
        }
        return playerHand;
    }

    public static int[] getDealerFromHashCode(long code) {
        int[] dealerHand = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        dealerHand[(int) (code % 10)] = 1;
        return dealerHand;
    }
    
    public static int getDealerCard(int[] dealerHand) {
        for (int i = 0; i < SIZE; i++) {
            if (dealerHand[i] == 1) {
                return i;
            }
        }
        return -1;
    }
}
