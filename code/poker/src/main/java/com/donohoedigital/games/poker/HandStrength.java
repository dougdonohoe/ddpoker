/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * 
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials) 
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 * 
 * For inquiries regarding commercial licensing of this source code or 
 * the use of names, logos, images, text, or other assets, please contact 
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
/*
 * HandStrengthDash.java
 *
 * Created on March 24, 2004, 12:20 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.config.*;

/**
 *
 * @author  donohoe
 */
public class HandStrength 
{
    static Logger logger = LogManager.getLogger(HandStrength.class);
    
    // debug output
    private static boolean DEBUG = false;
    
    // number of straights made by opponents
    private int nNumStraights_ = 0;
    
    /**
     * return number of straights made during most recent call to
     * getStrength
     */
    public int getNumStraights()
    {
        return nNumStraights_;
    }
    
    /**
     * Calculate strength of hand against 1 opponents.  Return is float from
     * 0 to 1 indicating probability of this hand being the best hand
     */
    public float getStrength(Hand hole, Hand community)
    {
        // init
        float nAhead = 0;
        float nTied = 0;
        float nBehind = 0;
        nNumStraights_ = 0;
        Hand holecopy = new Hand(hole); // copy for reuse (set to 2 cards because ...
        int nType;
        
        // our rank
        HandInfoFaster FAST = new HandInfoFaster();
        int ourscore = FAST.getScore(holecopy, community);
        
        // get remaining cards (new deck less hole, community)
        Deck deck = new Deck(false);
        deck.removeCards(hole);
        deck.removeCards(community);
        
        // loop through deck
        int oppscore;
        int nSize = deck.size();
        for (int i = 0; i < nSize - 1; i++)
        {
            for (int j = i+1; j < nSize; j++)
            {
                // ... replace faster than clear/add/add)
                holecopy.setCard(0, deck.getCard(i));
                holecopy.setCard(1, deck.getCard(j));
                //logger.debug("Hole ["+i+","+j+"]: " + holecopy + " comm: " + community);
                oppscore = FAST.getScore(holecopy, community);
                
                if (ourscore > oppscore) nAhead++;
                else if (ourscore == oppscore) nTied++;
                else nBehind++;
                
                nType = HandInfoFast.getTypeFromScore(oppscore);
                if (nType == HandInfo.STRAIGHT) nNumStraights_++;
            }
        }
        
        float nStrength = (nAhead + nTied/2) / (nAhead + nTied + nBehind);

        if (DEBUG && TESTING(EngineConstants.TESTING_AI_DEBUG))
        {
            logger.debug("STRENGTH for " + hole +"," + community+ ": " +
                nAhead + " wins   " +
                nTied +  " ties   " +
                nBehind +" lose   " +
                HandStat.fPerc.form(nStrength));
        }
        return nStrength;
    }

    /**
     * Calculate strength of hand against N opponents.  Return is float from
     * 0 to 1 indicating probability of this hand being the best hand
     */
    public float getStrength(Hand hole, Hand community, int nOpponents)
    {
        float nStrength = getStrength(hole, community);
        if (DEBUG && TESTING(EngineConstants.TESTING_AI_DEBUG))
        {
            logger.debug(" raised to " + nOpponents + ": " + HandStat.fPerc.form(Math.pow(nStrength, nOpponents)));
        }
        return getStrength(nStrength, nOpponents);
    }

    /**
     * calculate strength against N opponents
     */
    public float getStrength(float nStrength, int nOpponents)
    {
        return (float)Math.pow(nStrength, nOpponents);
    }

    /**
     * HTML for calctool
     */
    public String toHTML(Hand hole, Hand community, int nOpponents)
    {
        HandInfoFast fast = new HandInfoFast();
        fast.getScore(hole, community);

        StringBuilder sb = new StringBuilder();
        float dStrength = getStrength(hole, community);
        float oppStrength;
        for (int i = 1; i <= nOpponents; i++)
        {
            oppStrength = getStrength(dStrength, i) * 100;
            sb.append(PropertyConfig.getMessage("msg.strength.row", new Integer(i),
                                                HandStat.fPerc.form(oppStrength)));
        }

        return Utils.fixHtmlTextFor15(PropertyConfig.getMessage("msg.strength", fast.toString(), sb.toString()));
    }

    /**
     * Calculate all-in percentage for all players left in the hadn
     */
    public static void doAllInPercentages(HoldemHand hhand, Hand community)
    {
        int nNumPlayers = hhand.getNumPlayers();
        PokerPlayer player;
        int nComm = community.size();
        int MORE = 5 - nComm;        

        // too expensive to calculate all 5 card boards, so just estimate
        // from the flop
        if (MORE > 3) MORE = 3;
        
        HandInfoFaster FAST = new HandInfoFaster();
        Hand commcopy = new Hand(community); // copy for reuse
        
        // get remaining cards (new deck less hole, community)
        Deck deck = new Deck(false);
        deck.removeCards(community);
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = hhand.getPlayerAt(i);
            player.clearAllInWin();
            if (player.isFolded()) continue;
            deck.removeCards(player.getHand());
        }
                
        // more init
        int nSize = deck.size();
        int nNumHands = 0;
        
        // loop over all remaining board cards
        for (int next1 = 0; next1 < (nSize - (MORE-1)); next1++)
        {
            if (MORE >= 1) {
                commcopy.addCard(deck.getCard(next1));
                if (MORE >= 2) {
                    for (int next2 = next1 + 1; next2 < (nSize - (MORE-2)); next2++)
                    {
                        commcopy.addCard(deck.getCard(next2));
                        if (MORE >= 3) {
                            for (int next3 = next2 + 1; next3 < (nSize - (MORE-3)); next3++)
                            {
                                commcopy.addCard(deck.getCard(next3));
                                if (MORE >= 4) {
                                    for (int next4 = next3 + 1; next4 < (nSize - (MORE-4)); next4++)
                                    {
                                        commcopy.addCard(deck.getCard(next4));
                                        if (MORE == 5) {
                                            for (int next5 = next4 + 1; next5 < nSize; next5++)
                                            {
                                                commcopy.addCard(deck.getCard(next5));
                                                score(FAST, hhand, commcopy);
                                                nNumHands++;

                                                commcopy.removeCard(commcopy.size() - 1);
                                            }
                                        }
                                        else
                                        {
                                            score(FAST, hhand, commcopy);
                                            nNumHands++;
                                        }
                                        commcopy.removeCard(commcopy.size() - 1);
                                    }
                                }
                                else
                                {
                                    score(FAST, hhand, commcopy);
                                    nNumHands++;
                                }
                                commcopy.removeCard(commcopy.size() - 1);
                            }
                        }
                        else
                        {
                            score(FAST, hhand, commcopy);
                            nNumHands++;
                        }
                        commcopy.removeCard(commcopy.size() - 1);
                    }
                }
                else
                {
                    score(FAST, hhand, commcopy);
                    nNumHands++;
                }
                commcopy.removeCard(commcopy.size() - 1);
            }
            else
            {
                score(FAST, hhand, commcopy);
                nNumHands++;
            }
        }
        
        // calc win percentage
        //logger.debug("Num hands: " + nNumHands + " deck size: " + nSize);
        float d;
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = hhand.getPlayerAt(i);
            if (player.isFolded()) continue;
            d = 100.0f * (float) player.getAllInWin() / (float) nNumHands;
            player.setAllInPerc(fPerc.form(d));

        }
    }
    
    private static Format fPerc = new Format("%2.1f");
    
    /** 
     * figure score for each hand and record wins (ties count as wins)
     */
    private static void score(HandInfoFaster FAST, HoldemHand hhand, Hand comm)
    {
        int nNumPlayers = hhand.getNumPlayers();
        PokerPlayer player;
        int score;
        int maxscore = 0;
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = hhand.getPlayerAt(i);
            if (player.isFolded()) continue;
            score = FAST.getScore(player.getHand(), comm);
            player.setAllInScore(score);
            if (score > maxscore) maxscore = score;
        }
        
        for (int i = 0; i < nNumPlayers; i++)
        {
            player = hhand.getPlayerAt(i);
            if (player.isFolded()) continue;
            if (player.getAllInScore() == maxscore)
            {
                player.addAllInWin();
            }
        }
    }
}
