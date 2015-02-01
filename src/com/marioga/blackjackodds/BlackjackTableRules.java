package com.marioga.blackjackodds;

/**
 * This class represents the rules of a 
 * Blackjack table.
 * 
 * @author marioga
 *
 */

public class BlackjackTableRules {
    private final int mNumDecks;
    private final boolean mDealerStandsSoft17;
    private final boolean mDoubleAfterSplit;
    private final boolean mAceReSplits;
    private final float mBlackjackPays;
    
    public int getNumDecks() {
        return mNumDecks;
    }

    public boolean isDealerStandsSoft17() {
        return mDealerStandsSoft17;
    }

    public boolean isDoubleAfterSplit() {
        return mDoubleAfterSplit;
    }

    public boolean isAceReSplits() {
        return mAceReSplits;
    }

    public float getBlackjackPays() {
        return mBlackjackPays;
    }

    public BlackjackTableRules(int numDecks, boolean dealerStandsSoft17,
            boolean doubleAfterSplit, boolean aceReSplits,
            float blackjackPays) {
        mNumDecks = numDecks;
        mDealerStandsSoft17 = dealerStandsSoft17;
        mDoubleAfterSplit = doubleAfterSplit;
        mAceReSplits = aceReSplits;
        mBlackjackPays = blackjackPays;
    }
}
