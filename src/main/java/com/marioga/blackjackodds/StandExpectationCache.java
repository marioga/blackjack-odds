package com.marioga.blackjackodds;

/**
 * This class represents a cache for the expected
 * returns after stand. This representation is useful
 * to compute hit, double and split expectation values.
 * Upon creation, it attempts to load the cached values from
 * the database cacheDB/stand_odds.db and, if this fails, 
 * it builds the cache from scratch and saves it in
 * said database
 * 
 * @author marioga
 *
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class StandExpectationCache {
    private static final int[] OCURRENCES = 
        { 4, 4, 4, 4, 4, 4, 4, 4, 4, 16 };
    private static final int SIZE = 10;

    private BlackjackTableRules mRules;
    private int[] mWithdrawnCards;

    private Map<Long, Float> mCachedStandValues = new HashMap<Long, Float>();

    private Connection mDBConnection;
    private Statement mStatement;
    private String mTableName;
    private String mColName;

    public StandExpectationCache(BlackjackTableRules rules,
            int[] withdrawnCards) {
        mRules = rules;
        mWithdrawnCards = withdrawnCards;
        initializeCache();
    }

    private void initializeCache() {
        try {
            Class.forName("org.sqlite.JDBC");
            mDBConnection = DriverManager.
                    getConnection("jdbc:sqlite:cacheDB/stand_odds.db");
            mDBConnection.setAutoCommit(false);

            mTableName = ((mRules.isDealerStandsSoft17()) ? "S" : "H") 
                    + Integer.toString(mRules.getNumDecks());
            mColName = "C" + Integer.toString(mWithdrawnCards[0]);
            for (int i = 1; i < SIZE; i++) {
                mColName += "_" + mWithdrawnCards[i];
            }

            DatabaseMetaData dbmd = mDBConnection.getMetaData();
            ResultSet tables = dbmd.getTables(null, null, mTableName, null);

            if (!tables.next()) {
                mStatement = mDBConnection.createStatement();
                String sql = "CREATE TABLE IF NOT EXISTS " 
                        + mTableName + " (ID INT PRIMARY KEY NOT NULL)";
                mStatement.executeUpdate(sql);
                mStatement.close();

                // Generate all possible player hands
                int[] tempWithdrawnCards = new int[SIZE];
                Iterable<int[]> allHands = PlayerHandGenerator.
                        getHands(mRules.getNumDecks(), tempWithdrawnCards);
                mStatement = mDBConnection.createStatement();
                for (int[] playerHand : allHands) {
                    for (int i = 0; i < SIZE; i++) {
                        if (mRules.getNumDecks() * OCURRENCES[i]
                                >= playerHand[i] + 1) {
                            // Generate all possible player-dealer combos
                            long code = HandEncoder.
                                    encodeToHashKey(playerHand, i);
                            sql = "INSERT INTO " + mTableName 
                                    + " (ID) VALUES (" + code + ");";
                            mStatement.executeUpdate(sql);
                        }
                    }
                }
                mStatement.close();
                
                cacheValues();
            } else {
                // Table already exists
                ResultSet columns = dbmd.
                        getColumns(null, null, mTableName, mColName);
                if (columns.next()) {
                    // Column already exists
                    mStatement = mDBConnection.createStatement();
                    ResultSet pairs = mStatement.
                            executeQuery("SELECT ID, " + mColName 
                                    + " FROM " + mTableName);
                    while (pairs.next()) {
                        long code = pairs.getLong("ID");
                        float odds = pairs.getFloat(mColName);
                        mCachedStandValues.put(code, odds);
                    }
                    pairs.close();
                    mStatement.close();
                } else {
                    // Column does not exist
                    cacheValues();
                }
                columns.close();
            }
            tables.close();
            mDBConnection.commit();
            mDBConnection.close();
        } catch (ClassNotFoundException | SQLException e) {
            handleDBException(e);
        }
        System.out.println("Cache loaded successfully!");
    }

    public float getCachedValue(int[] playerHand, int[] dealerHand) {
        int dealerCard = HandEncoder.getDealerCard(dealerHand);
        long code = HandEncoder.encodeToHashKey(playerHand, dealerCard);
        return mCachedStandValues.get(code);
    }

    private void cacheValues(){
        System.out.println("Cache not found. Creating cache...");
        
        // Temporary BlackjackComputer to find stand exps
        BlackjackOddsComputer boc = new BlackjackOddsComputer(mRules);
        boc.setWithdrawnCards(mWithdrawnCards.clone());
        Iterable<int[]> playerHands = PlayerHandGenerator.
                getHands(mRules.getNumDecks(), mWithdrawnCards);
        for (int[] playerHand : playerHands) {
            for (int i = 0; i < SIZE; i++) {
                if (mRules.getNumDecks() * OCURRENCES[i]
                        >= playerHand[i]
                                + mWithdrawnCards[i] + 1) {
                    int[] dealerHand = new int[SIZE];
                    dealerHand[i] = 1;
                    boc.setPlayerHand(playerHand);
                    boc.setDealerHand(dealerHand);
                    long code = HandEncoder.encodeToHashKey(playerHand, i);
                    float standExp = boc.computeExpectationStand(true);
                    mCachedStandValues.put(code, standExp);
                }
            }
        }
        storeCachedStandValuesToDB();
    }

    private void storeCachedStandValuesToDB() {
        try {
            mStatement = mDBConnection.createStatement();
            String sql = "ALTER TABLE " + mTableName
                    + " ADD " + mColName + " REAL";
            mStatement.executeUpdate(sql);
            mStatement.close();

            mStatement = mDBConnection.createStatement();
            for (long code : mCachedStandValues.keySet()) {
                sql = "UPDATE " + mTableName +
                        " SET " + mColName + " = " 
                        + mCachedStandValues.get(code) + 
                        " WHERE ID = " + code;
                mStatement.executeUpdate(sql);
            }
            mStatement.close();


        } catch (SQLException e) {
            handleDBException(e);
        }
    }

    private void handleDBException(Exception e) {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        System.exit(0);
    }
}
