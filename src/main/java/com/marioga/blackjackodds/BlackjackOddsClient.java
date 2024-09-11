package com.marioga.blackjackodds;

/**
 * Test client that computes expected returns of a Blackjack hand
 *
 * @author marioga
 */
public class BlackjackOddsClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BlackjackTableRules rules = new BlackjackTableRules(
                8, true, true, false, 1.5f);
        int[] playerHand = new int[]{0, 0, 0, 0, 0, 0, 2, 0, 0, 0};
        int[] dealerHand = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 0};
        int[] withdrawnCards = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        BlackjackOddsComputer boc = new BlackjackOddsComputer(rules,
                playerHand, dealerHand, withdrawnCards);
        // Compute stand expectation
        System.out.println("Stand expectation: "
                + boc.computeExpectationStand(true));
        StandExpectationCache sec = new StandExpectationCache(
                rules, withdrawnCards);
        boc.setCachedStandValues(sec);
        System.out.println("Hit expectation: "
                + boc.computeExpectationHit(true));
        System.out.println("Double expectation: "
                + boc.computeExpectationDouble(true));
        System.out.println("Split expectation: "
                + boc.computeExpectationSplit(true, 2));
    }
}
