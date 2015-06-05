package com.marioga.blackjackodds;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class BlackjackOddsDBWriter {
    private static final int[] OCURRENCES = 
        { 4, 4, 4, 4, 4, 4, 4, 4, 4, 16 };
    private static final int SIZE = 10;
    
    /**
     * This class writes the expected values
     * into a database, given a choice of rules
     * and withdrawn cards.
     *
     * @author marioga
     *
     */
    
    private final String[] TABLE_NAMES = 
            new String[] {"Stand", "Hit", "Double", "Split"};

    private BlackjackTableRules mRules;
    private int[] mWithdrawnCards;
    
    private Connection mDBConnection;
    private Statement mStatement;
    
    public BlackjackOddsDBWriter(BlackjackTableRules rules, int[] withdrawnCards) {
        mRules = rules;
        mWithdrawnCards = withdrawnCards;
    }
    
    public void saveOddsToDB (String fileName) {
        if (!initializeDB(fileName)) {
            // File already exists
            return;
        }
        createTables();
        
        // Temporary BlackjackComputer to find stand exps
        BlackjackOddsComputer boc = new BlackjackOddsComputer(mRules);
        boc.setWithdrawnCards(mWithdrawnCards.clone());
        StandExpectationCache sec = new StandExpectationCache(
                mRules, mWithdrawnCards);
        boc.setCachedStandValues(sec);
        Iterable<int[]> playerHands = PlayerHandGenerator.
                getHands(mRules.getNumDecks(), mWithdrawnCards);
        for (int[] playerHand : playerHands) {
            for (int i = 0; i < SIZE; i++) {
                if (mRules.getNumDecks() * OCURRENCES[i]
                        >= playerHand[i]
                                + mWithdrawnCards[i] + 1) {
                    int[] dealerHand = new int[SIZE];
                    dealerHand[i]++;
                    boc.setPlayerHand(playerHand);
                    boc.setDealerHand(dealerHand);
                    long code = HandEncoder.encodeToHashKey(playerHand, i);
                    writeToTable(TABLE_NAMES[0], code,
                            sec.getCachedValue(playerHand, dealerHand));
                    if (Helper.valueHand(playerHand) < 21) {
                        float hitValue = boc.computeExpectationHit(true);
                        writeToTable(TABLE_NAMES[1], code, hitValue);
                        if (Helper.numberOfCards(playerHand) == 2) {
                            float doubleValue = boc.
                                    computeExpectationDouble(true);
                            writeToTable(TABLE_NAMES[2], code, doubleValue);
                            if (Helper.isPair(playerHand)) {
                                float splitValue = boc.
                                        computeExpectationSplit(true, 2);
                                writeToTable(TABLE_NAMES[3], code, splitValue);
                            }
                        }
                    }
                }
            }
        }
        closeDB();
    }
    
    private void writeToTable(String tableName, long id, float oddsValue) {
        try {
            mStatement = mDBConnection.createStatement();
            String sql = "INSERT INTO " + tableName + "(ID, Odds) VALUES (" +
                    + id + ", " + oddsValue + ");";
            mStatement.executeUpdate(sql);
            mStatement.close();
        } catch (SQLException e) {
            handleDBException(e);
        }
    }
    
    private void createTables() {
        try {
            mStatement = mDBConnection.createStatement();
            String sql;
            for (int i = 0; i < TABLE_NAMES.length; i++) {
                sql = "CREATE TABLE " 
                        + TABLE_NAMES[i] 
                        + " (ID INT PRIMARY KEY NOT NULL, Odds REAL)";
                mStatement.executeUpdate(sql);
            }
            mStatement.close();
        } catch (SQLException e) {
            handleDBException(e);
        }
    }
    
    private boolean initializeDB(String fileName) {
        final File f = new File(fileName);
        if (f.exists()) {
            return false;
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
                mDBConnection = DriverManager.
                        getConnection("jdbc:sqlite:" + fileName);
                mDBConnection.setAutoCommit(false);
            } catch (ClassNotFoundException | SQLException e) {
                handleDBException(e);
            }
            return true;
        }
    }
    
    private void closeDB() {
        try {
            mDBConnection.commit();
            mDBConnection.close();
        } catch (SQLException e) {
            handleDBException(e);
        }
    }
    
    private void handleDBException(Exception e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        System.exit(0);
    }
}
