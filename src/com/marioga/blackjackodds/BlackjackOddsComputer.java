package com.marioga.blackjackodds;

import static java.lang.Math.max;

/**
 * This class represents a blackjack game situation. 
 * It computes the expected return on the different
 * actions in the game, given the current status.
 * 
 * @author marioga
 *
 */

public class BlackjackOddsComputer {
    private static final int[] OCURRENCES = 
        { 4, 4, 4, 4, 4, 4, 4, 4, 4, 16 };
    private static final int DECK_SIZE = 52;
    private static final int SIZE = 10;
    
    private static StandExpectationCache sCachedStandValues;
    
    public void setCachedStandValues (StandExpectationCache sec) {
        // Must have same rules and withdrawn cards
        sCachedStandValues = sec;
    }
    
    private final BlackjackTableRules mRules;
    private int[] mPlayerHand;
    private int[] mDealerHand;
    private int[] mWithdrawnCards;
    
    public int[] getPlayerHand() {
        return mPlayerHand;
    }

    public int[] getDealerHand() {
        return mDealerHand;
    }

    public int[] getWithdrawnCards() {
        return mWithdrawnCards;
    }
    
    public void setPlayerHand(int[] playerHand) {
        mPlayerHand = playerHand;
    }

    public void setDealerHand(int[] dealerHand) {
        mDealerHand = dealerHand;
    }

    public void setWithdrawnCards(int[] withdrawnCards) {
        mWithdrawnCards = withdrawnCards;
    }
    
    public BlackjackOddsComputer(BlackjackTableRules rules) {
        mRules = rules;       
    }
    
    public BlackjackOddsComputer(BlackjackTableRules rules,
            int[] playerHand, int[] dealerHand, int[] withdrawnCards) {
        mRules = rules;
        mPlayerHand = playerHand;
        mDealerHand = dealerHand;
        mWithdrawnCards = withdrawnCards;        
    }
    
    /**
     * This method computes the exact expected return for 
     * the player after (s)he chooses to stand.
     * @param afterPeek has the dealer peeked at the hole card?
     * @return expected return upon choosing to stand
     */
    
    public float computeExpectationStand(boolean afterPeek) {
        return expectationStand(mPlayerHand, mDealerHand,
                mWithdrawnCards, afterPeek, 1);
    }
    
    private float expectationStand(int[] playerHand,
            int[] dealerHand, int[] withdrawnCards,
            boolean afterPeek, float prob) {
        //This variable will represent the expected return
        float expReturn = 0;
        /*
         * This vector will contain the probabilities for
         * the rank of the next card in the shoe 
         */
        float[] probabilities;
        // This variable keeps track of all cards not in the shoe
        int[] cardsOutSoFar = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cardsOutSoFar[i] = playerHand[i] + dealerHand[i]
                    + withdrawnCards[i];
        }
        
        if (!afterPeek) { // Hole card could be anything
            probabilities = computeStandProbabilities(mRules.getNumDecks(),
                    cardsOutSoFar, -1);
        } else { // If the dealer has an ace or 10 showing,
                 // we know the dealer does not have blackjack
            if (dealerHand[9] == 1 && 
                    Helper.numberOfCards(dealerHand) == 1) {
                // Dealer hand is a 10
                probabilities = computeStandProbabilities(mRules.getNumDecks(),
                        cardsOutSoFar, 0);
            } else if (dealerHand[0] == 1 && 
                    Helper.numberOfCards(dealerHand) == 1) {
                // Dealer hand is an Ace
                probabilities = computeStandProbabilities(mRules.getNumDecks(),
                        cardsOutSoFar, 9);
            } else { 
                // Dealer never peeked; hole card is arbitrary
                // or dealer has at least two cards
                probabilities = computeStandProbabilities(mRules.getNumDecks(),
                        cardsOutSoFar, -1);
            }
        }

        if (Helper.valueHand(dealerHand) <= 16
                || (Helper.valueHand(dealerHand) == 17
                && Helper.isSoft(dealerHand) 
                && !mRules.isDealerStandsSoft17())) {
            // Dealer hits
            for (int i = 0; i < SIZE; i++) {
                if (OCURRENCES[i] * mRules.getNumDecks() >= cardsOutSoFar[i] + 1) {
                    int[] newDealerHand = dealerHand.clone();
                    newDealerHand[i] += 1;
                    if ((i == 0 && dealerHand[9] == 1 && Helper.numberOfCards(dealerHand) == 1
                            && afterPeek) || (i == 9 && dealerHand[0] == 1
                            && Helper.numberOfCards(dealerHand) == 1 && afterPeek)) {
                        continue;
                    }
                    if (Helper.valueHand(newDealerHand) <= 21) { // Not Busted
                        expReturn += expectationStand(playerHand,
                                newDealerHand, withdrawnCards, afterPeek,
                                prob * probabilities[i]);
                    } else {
                        if (Helper.isBlackJack(playerHand)) {
                            expReturn += mRules.getBlackjackPays() * prob
                                    * probabilities[i];
                        } else {
                            expReturn += prob * probabilities[i];
                        }
                    }
                }

            }
        } else { // Dealer stands
            if (Helper.isBlackJack(playerHand)
                    && !Helper.isBlackJack(dealerHand)) {
                expReturn = mRules.getBlackjackPays() * prob;
            } else {
                if (Helper.valueHand(dealerHand)
                        < Helper.valueHand(playerHand)) {
                    expReturn = prob;
                } else if (Helper.valueHand(dealerHand)
                        > Helper.valueHand(playerHand)
                        || (Helper.isBlackJack(dealerHand)
                                && !Helper.isBlackJack(playerHand))) {
                    expReturn = -prob;
                }
            }
        }
        return expReturn;
    }
    
    private static float[] computeStandProbabilities(int numDecks,
            int[] cardsOut, int holeCardIsNotThis) {
        float[] result = new float[SIZE];
        // Number of cards left
        final int TOTAL = numDecks * DECK_SIZE
                - Helper.numberOfCards(cardsOut);
        // Number of cards of type i left
        int total_i;
        if (holeCardIsNotThis != -1) { // We know the hole card
                                       // is not equal to this value
            // Number of cards of type holeCardIsNotThis left
            int total_hole = OCURRENCES[holeCardIsNotThis] * numDecks
                    - cardsOut[holeCardIsNotThis];
            for (int i = 0; i < SIZE; i++) {
                if (i == holeCardIsNotThis) continue;
                total_i = OCURRENCES[i] * numDecks - cardsOut[i];
                result[i] = (float) (total_i)
                        / (TOTAL - total_hole);
            }
        } else { // Next card is arbitrary
            for (int i = 0; i < SIZE; i++) {
                total_i = OCURRENCES[i] * numDecks - cardsOut[i];
                result[i] = (float) (total_i) / TOTAL;
            }
        }
        return result;
    }
    
    /**
     * This method computes the exact expected return for 
     * the player after (s)he chooses to hit.
     * @param usingCachedValues do we use cached stand values?
     * @return expected return upon choosing to hit
     */
    
    public float computeExpectationHit(boolean usingCachedValues) {
        return expectationHit(mPlayerHand, mDealerHand,
                mWithdrawnCards, usingCachedValues, 1);
    }

    private float expectationHit(int[] playerHand,
            int[] dealerHand, int[] withdrawnCards,
            boolean usingCachedValues, float prob) {
        float expReturn = 0;
        float[] probabilities;
        int[] cardsOutSoFar = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cardsOutSoFar[i] = playerHand[i] + dealerHand[i]
                    + withdrawnCards[i];
        }
        if (dealerHand[9] == 1 && Helper.numberOfCards(dealerHand) == 1) {
            // Dealer hand is a 10
            probabilities = computeHitProbabilities(mRules.getNumDecks(), cardsOutSoFar, 0);
        } else if (dealerHand[0] == 1 && Helper.numberOfCards(dealerHand) == 1) { 
            // Dealer hand is an Ace
            probabilities = computeHitProbabilities(mRules.getNumDecks(), cardsOutSoFar, 9);
        } else {
            probabilities = computeHitProbabilities(mRules.getNumDecks(), cardsOutSoFar, -1);
        }

        for (int i = 0; i < SIZE; i++) {
            if (OCURRENCES[i] * mRules.getNumDecks() >= cardsOutSoFar[i] + 1) {
                int[] newPlayerHand = playerHand.clone();
                newPlayerHand[i] += 1;
                int value = Helper.valueHand(newPlayerHand);
                if (value <= 11) {// Not Busted
                    float hitExp = expectationHit(newPlayerHand,
                            dealerHand, withdrawnCards,
                            usingCachedValues, prob * probabilities[i]);
                    expReturn += hitExp;
                } else if (value <= 21) {
                    float hitExp = expectationHit(newPlayerHand,
                            dealerHand, withdrawnCards,
                            usingCachedValues,
                            prob * probabilities[i]);
                    float standExp = 0;
                    if (usingCachedValues) {
                         standExp = sCachedStandValues.
                                getCachedValue(newPlayerHand, dealerHand);
                        standExp *= prob * probabilities[i];
                    } else {
                        standExp = expectationStand(newPlayerHand,
                                dealerHand, withdrawnCards, true,
                                prob * probabilities[i]);
                    }
                    expReturn += max(hitExp, standExp);
                } else {
                    expReturn -= prob * probabilities[i];
                }
            }
        }
        return expReturn;
    }
    
    private static float[] computeHitProbabilities(int numDecks,
            int[] cardsOut, int holeCardIsNotThis) {
        float[] result = new float[SIZE];
        // Number of cards left
        final int TOTAL = numDecks * DECK_SIZE
                - Helper.numberOfCards(cardsOut);
        // Number of cards of type i left
        int total_i;
        if (holeCardIsNotThis != -1) {
            // Number of cards of type holeCardIsNotThis left
            int total_hole = OCURRENCES[holeCardIsNotThis] * numDecks
                    - cardsOut[holeCardIsNotThis];
            for (int i = 0; i < SIZE; i++) {
                // P(Next card = i | Hole card is not holeCardIsNotThis) 
                if (i == holeCardIsNotThis) {
                    result[i] = (float) (total_hole) / (TOTAL - 1);
                } else {
                    total_i = OCURRENCES[i] * numDecks - cardsOut[i];
                    result[i] = (float) (total_i) * (TOTAL - total_hole - 1)
                            / (TOTAL - 1) / (TOTAL - total_hole);
                }
            }
        } else {
            for (int i = 0; i < SIZE; i++) {
                total_i = OCURRENCES[i] * numDecks - cardsOut[i];
                result[i] = (float) (total_i) / TOTAL;
            }
        }
        return result;
    }
    
    /**
     * This method computes the exact expected return for 
     * the player after (s)he chooses to double.
     * @param usingCachedValues do we use cached stand values?
     * @return expected return upon choosing to double
     */
    
    public float computeExpectationDouble(boolean usingCachedValues) {
        return expectationDouble(mPlayerHand, mDealerHand,
                mWithdrawnCards, usingCachedValues);
    }

    private float expectationDouble(int[] playerHand,
            int[] dealerHand, int[] withdrawnCards,
            boolean usingCachedValues) {
        float expReturn = 0;
        float[] probabilities;
        int[] cardsOutSoFar = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cardsOutSoFar[i] = playerHand[i] + dealerHand[i]
                    + withdrawnCards[i];
        }
        if (dealerHand[9] == 1 &&
                Helper.numberOfCards(dealerHand) == 1) {
            // Dealer hand is a 10
            probabilities = computeHitProbabilities(
                    mRules.getNumDecks(), cardsOutSoFar, 0);
        } else if (dealerHand[0] == 1 &&
                Helper.numberOfCards(dealerHand) == 1) {
            // Dealer hand is an Ace
            probabilities = computeHitProbabilities(
                    mRules.getNumDecks(), cardsOutSoFar, 9);
        } else {
            probabilities = computeHitProbabilities(
                    mRules.getNumDecks(), cardsOutSoFar, -1);
        }

        for (int i = 0; i < SIZE; i++) {
            if (OCURRENCES[i] * mRules.getNumDecks() >= cardsOutSoFar[i] + 1) {
                int[] newPlayerHand = playerHand.clone();
                newPlayerHand[i] += 1;
                if (Helper.valueHand(newPlayerHand) <= 21) {
                    // Not Busted
                    float standExp = 0;
                    if (usingCachedValues) {
                        standExp = sCachedStandValues.
                                getCachedValue(newPlayerHand, dealerHand);
                        standExp *= probabilities[i];
                    } else {
                        standExp = expectationStand(newPlayerHand,
                                dealerHand, withdrawnCards, true,
                                probabilities[i]);
                    }
                    expReturn += 2 * standExp;
                } else {
                    expReturn -= 2 * probabilities[i];
                }
            }
        }
        return expReturn;
    }
    
    /**
     * This method computes an approximate expected return for 
     * the player after (s)he chooses to split. A full-composition
     * dependent implementation is currently infeasible.
     * @param usingCachedValues do we use cached stand values?
     * @param splitsLeft number of splits are left
     * @return expected return upon choosing to split
     */
    
    public float computeExpectationSplit(boolean usingCachedValues,
            int splitsLeft) {
        if (mPlayerHand[0] == 2 && !mRules.isAceReSplits()) {
            // Pair of Aces that cannot be further split
            return expectationSplitCacher(usingCachedValues, 0);
        } else {
            return expectationSplitCacher(usingCachedValues, splitsLeft);
        }
    }
    
    private float[] mTempCachedSplits;

    private float expectationSplitCacher(boolean usingCachedValues,
            int splitsLeft) {
        mTempCachedSplits = new float[splitsLeft + 1];
        for (int i = 0; i < splitsLeft + 1; i++) {
            mTempCachedSplits[i] = expectationSplit(usingCachedValues, i);
        }
        return mTempCachedSplits[splitsLeft];
    }
    
    private float expectationSplit(boolean usingCachedValues,
            int splitsLeft) {
        // Here we just want to approximate.
        // Cannot be done full composition dependent.
        boolean splitAces = false;
        int[] playerHand1 = mPlayerHand.clone();
        int[] playerHand2 = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        int pair = -1;
        for (int i = 0; i < SIZE; i++) {
            if (mPlayerHand[i] == 2) {
                pair = i;
                if (pair == 0) {
                    splitAces = true;
                }
                playerHand1[pair]--;
                playerHand2[pair]++;
                break;
            }
        }
        float expReturn = 0;
        float[] probabilities;
        int[] cardsOutSoFar = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cardsOutSoFar[i] = mPlayerHand[i] + mDealerHand[i]
                    + mWithdrawnCards[i];
        }
        if (mDealerHand[9] == 1
                && Helper.numberOfCards(mDealerHand) == 1) {
            // Dealer hand is a 10
            probabilities = computeHitProbabilities(
                    mRules.getNumDecks(), cardsOutSoFar, 0);
        } else if (mDealerHand[0] == 1
                && Helper.numberOfCards(mDealerHand) == 1) {
            // Dealer hand is an Ace
            probabilities = computeHitProbabilities(
                    mRules.getNumDecks(), cardsOutSoFar, 9);
        } else {
            probabilities = computeHitProbabilities(
                    mRules.getNumDecks(), cardsOutSoFar, -1);
        }
        for (int i = 0; i < SIZE; i++) {
            if (OCURRENCES[i] * mRules.getNumDecks()
                    >= cardsOutSoFar[i] + 1) {
                int[] newPlayerHand1 = playerHand1.clone();
                newPlayerHand1[i]++;
                int[] cardsOutSoFar2 = cardsOutSoFar.clone();
                cardsOutSoFar2[i]++;
                float[] probabilities2;
                if (mDealerHand[9] == 1
                        && Helper.numberOfCards(mDealerHand) == 1) {
                    // Dealer hand is a 10
                    probabilities2 = computeHitProbabilities(
                            mRules.getNumDecks(), cardsOutSoFar2, 0);
                } else if (mDealerHand[0] == 1
                        && Helper.numberOfCards(mDealerHand) == 1) {
                    // Dealer hand is an Ace
                    probabilities2 = computeHitProbabilities(
                            mRules.getNumDecks(), cardsOutSoFar2, 9);
                } else {
                    probabilities2 = computeHitProbabilities(
                            mRules.getNumDecks(), cardsOutSoFar2, -1);
                }
                for (int j = 0; j < SIZE; j++) {
                    if (OCURRENCES[j] * mRules.getNumDecks()
                            >= cardsOutSoFar2[j] + 1) {
                        int[] newPlayerHand2 = playerHand2.clone();
                        newPlayerHand2[j] += 1;
                        float prob;
                        if (i != pair && j != pair) {
                            prob = expectationPairDistinct(newPlayerHand1,
                                    newPlayerHand2, mDealerHand,
                                    mWithdrawnCards, splitAces,
                                    usingCachedValues);
                        } else {
                            if (splitsLeft == 2) {
                                if (i == pair && j == pair) {
                                    prob = expectationPairTwoEqual(
                                            newPlayerHand1, newPlayerHand2, 
                                            mDealerHand, mWithdrawnCards,
                                            splitAces, usingCachedValues,
                                            splitsLeft);
                                } else if (i == pair && j != pair) {
                                    prob = expectationPairOneEqual(
                                            newPlayerHand1, newPlayerHand2, 
                                            mDealerHand, mWithdrawnCards,
                                            splitAces, usingCachedValues,
                                            splitsLeft);
                                } else {
                                    prob = expectationPairOneEqual(
                                            newPlayerHand2, newPlayerHand1, 
                                            mDealerHand, mWithdrawnCards,
                                            splitAces, usingCachedValues,
                                            splitsLeft);
                                }
                            } else if (splitsLeft == 1) {
                                if (i == pair) {
                                    prob = expectationPairOneEqual(
                                            newPlayerHand1, newPlayerHand2, 
                                            mDealerHand, mWithdrawnCards,
                                            splitAces, usingCachedValues,
                                            splitsLeft);
                                } else {
                                    prob = expectationPairOneEqual(
                                            newPlayerHand2, newPlayerHand1, 
                                            mDealerHand, mWithdrawnCards,
                                            splitAces, usingCachedValues,
                                            splitsLeft);
                                }
                            } else {
                                prob = expectationPairDistinct(newPlayerHand1,
                                        newPlayerHand2, mDealerHand,
                                        mWithdrawnCards, splitAces,
                                        usingCachedValues);
                            }
                        }
                        expReturn += 
                                probabilities[i] * probabilities2[j] * prob;
                    }
                }
            }
        }
        return expReturn;
    }
    
    private float expectationPairTwoEqual(int[] playerHand1, 
            int[] playerHand2, int[] dealerHand,
            int[] withdrawnCards, boolean splitAces,
            boolean usingCachedValues, int splitsLeft) {
        // Expectation if do not further split any pair
        float firstPossibility = expectationPairDistinct(playerHand1,
                playerHand2, dealerHand, withdrawnCards,
                splitAces, usingCachedValues);
        // Expectation if we split exactly one pair;
        // it is irrelevant which one as they are both identical
        float secondPossibility = mTempCachedSplits[splitsLeft - 1]
                + expectationAfterNormalPlay(playerHand2, dealerHand,
                        withdrawnCards, splitAces, usingCachedValues);
        // Expectation after further splitting both pairs
        float thirdPossibility = 2 * mTempCachedSplits[splitsLeft - 2];
        return max(firstPossibility, max(secondPossibility, thirdPossibility));
    }
    
    private float expectationPairOneEqual(int[] playerHand1, 
            int[] playerHand2, int[] dealerHand,
            int[] withdrawnCards, boolean splitAces,
            boolean usingCachedValues, int splitsLeft) {
        // Expectation if we do not further split the new pair
        float firstPossibility = expectationPairDistinct(playerHand1,
                playerHand2, dealerHand, withdrawnCards,
                splitAces, usingCachedValues);
        // Expectation if we further split the new pair
        float secondPossibility = mTempCachedSplits[splitsLeft - 1]
                + expectationAfterNormalPlay(playerHand2, dealerHand,
                        withdrawnCards, splitAces, usingCachedValues);
        return max(firstPossibility, secondPossibility);

    }
    
    private float expectationPairDistinct(int[] playerHand1, 
            int[] playerHand2, int[] dealerHand,
            int[] withdrawnCards, boolean splitAces,
            boolean usingCachedValues) {
        // Here we are assuming independence of the returns
        // of both hands, which is not true in practice.
        // This method provides a good approximation.
        // The exact computation of composition-dependent
        // expectation is not feasible
        float exp1 = expectationAfterNormalPlay(playerHand1,
                dealerHand, withdrawnCards, splitAces, usingCachedValues);
        float exp2 = expectationAfterNormalPlay(playerHand2,
                dealerHand, withdrawnCards, splitAces, usingCachedValues);
        return exp1 + exp2;
    }
    
    private float expectationAfterNormalPlay(int[] playerHand,
            int[] dealerHand, int[] withdrawnCards,
            boolean splitAces, boolean usingCachedValues) {
        float expStand = 0;
        if (usingCachedValues) {
            expStand = sCachedStandValues.
                    getCachedValue(playerHand, dealerHand);
        } else {
            expStand = expectationStand(playerHand,
                    dealerHand, withdrawnCards, true, 1);
        }
        if (splitAces) {
            // We follow the common rule that split Aces
            // only get an extra card
            if (playerHand[SIZE - 1] == 1) {
                // In this case, A + 10 is not Blackjack
                expStand /= mRules.getBlackjackPays();
            }
            return expStand;
        } else {
            float expHit = expectationHit(playerHand, dealerHand, 
                    withdrawnCards, usingCachedValues, 1);
            float expDouble = -10;
            if (mRules.isDoubleAfterSplit()) {
                expDouble = expectationDouble(playerHand, dealerHand,
                        withdrawnCards, usingCachedValues);
            }
            return max(expStand, max(expHit, expDouble));
        }
    }
    
//    public float exactAcesSplitExpectation() {
//        int[] fixedHand = new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        int[] dealerHand = new int[] {0, 0, 0, 0, 0, 0, 1, 0, 0, 0};
//        float expReturn = 0;
//        float[] probabilities;
//        int[] cardsOutSoFar = new int[SIZE];
//        for (int i = 0; i < SIZE; i++) {
//            cardsOutSoFar[i] = 2 * fixedHand[i] + dealerHand[i];
//        }
//        if (dealerHand[9] == 1) {
//            // Dealer hand is a 10
//            probabilities = computeHitProbabilities(
//                    mRules.getNumDecks(), cardsOutSoFar, 0);
//        } else if (dealerHand[0] == 1) {
//            // Dealer hand is an Ace
//            probabilities = computeHitProbabilities(
//                    mRules.getNumDecks(), cardsOutSoFar, 9);
//        } else {
//            probabilities = computeHitProbabilities(
//                    mRules.getNumDecks(), cardsOutSoFar, -1);
//        }
//        for (int i = 0; i < SIZE; i++) {
//            int[] newPlayerHand1 = fixedHand.clone();
//            newPlayerHand1[i]++;
//            int[] cardsOutSoFar2 = cardsOutSoFar.clone();
//            cardsOutSoFar2[i]++;
//            float[] probabilities2;
//            if (dealerHand[9] == 1) {
//                // Dealer hand is a 10
//                probabilities2 = computeHitProbabilities(mRules.getNumDecks(),
//                        cardsOutSoFar2, 0);
//            } else if (dealerHand[0] == 1) {
//                // Dealer hand is an Ace
//                probabilities2 = computeHitProbabilities(mRules.getNumDecks(),
//                        cardsOutSoFar2, 9);
//            } else {
//                probabilities2 = computeHitProbabilities(mRules.getNumDecks(),
//                        cardsOutSoFar2, -1);
//            }
//            for (int j = 0; j < SIZE; j++) {
//                if (j == 0 && i == 0 && dealerHand[0] == 1
//                      && mRules.getNumDecks() == 1) continue;
//                int[] newPlayerHand2 = fixedHand.clone();
//                newPlayerHand2[j]++;
//                float expStand1 = expectationStand(newPlayerHand1,
//                        dealerHand, newPlayerHand2, true, 1);
//                float expStand2 = expectationStand(newPlayerHand2,
//                        dealerHand, newPlayerHand1, true, 1);
//                if (newPlayerHand1[SIZE - 1] == 1) {
//                    expStand1 /= mRules.getBlackjackPays();
//                }
//                if (newPlayerHand2[SIZE - 1] == 1) {
//                    expStand2 /= mRules.getBlackjackPays();
//                }
//                expReturn += probabilities[i]
//                        * probabilities2[j] * (expStand1 + expStand2);
//            }   
//        }
//        return expReturn;
//    }
}
