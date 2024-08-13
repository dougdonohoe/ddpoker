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
 * DealTester.java
 *
 * Created on October 13, 2005, 8:55 AM 
 */

package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;
import com.donohoedigital.udp.*;
import com.donohoedigital.games.poker.engine.*;

import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DealTester extends BaseCommandLineApp
{
    // logging
    private Logger logger;

    /**
     * Run emailer
     */
    public static void main(String[] args) {
        try {
            Prefs.setRootNodeName("poker2");
            new DealTester("poker", args);
        }

        catch (ApplicationError ae)
        {
            System.err.println("DealTester ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }  
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
        catch (Throwable t)
        {
            System.err.println("DealTester ending due to ApplicationError: " + Utils.formatExceptionText(t));
            System.exit(1);
        }
    }
    
    /**
     * Can be overridden for application specific options
     */
    protected void setupApplicationCommandLineOptions()
    {

    }

    UDPServer udp_;

    /**
     * Create War from config file
     */
    public DealTester(String sConfigName, String[] args)
    {
        super(sConfigName, args);
        ConfigManager.getConfigManager().loadGuiConfig();

        // init
        logger = Logger.getLogger(getClass());

        // write cards
        testCards();
    }

    /**
     * write cards
     */
    private void testCards()
    {
        int[][] hand = new int[52][52];

        boolean ADJUST = true;
        int RUN = 10000000;

        Deck d;
        Card c1,c2;
        int seed;
        for (int i = 0; i < RUN; i++)
        {
            seed = 0;
            //if (ADJUST) HoldemHand.ADJUST_SEED();
            //seed = ADJUST ? HoldemHand.NEXT_SEED() : 0;
            if ((i+1) % 25000 == 0)
            {
                logger.debug("Processed " + (i+1) + " (last seed was " + seed+")");
            }

            d = new Deck(true, seed);
            c1 = d.nextCard();
            c2 = d.nextCard();
            if (c1.getIndex() < c2.getIndex())
            {
                hand[c1.getIndex()][c2.getIndex()]++;
            }
            else
            {
                hand[c2.getIndex()][c1.getIndex()]++;
            }
        }

        for (int i = 0; i < 52; i++)
        {
            for (int j = i; j >= 0; j--)
            {
                c1 = Card.getCard(i);
                c2 = Card.getCard(j);
                if (hand[i][j] != 0)
                {
                    logger.debug("WARN: "+c1.toStringSingle() + c2.toStringSingle() +": "+hand[i][j]);
                }
            }
        }

        ArrayList<DealInfo> sort = new ArrayList<DealInfo>(0);
        int min = Integer.MAX_VALUE;
        int max = 0;
        int sum = 0;
        int CNT = 0;
        int num;
        Hand h_max = new Hand();
        Hand h_min = new Hand();
        String shand;
        for (int i = 0; i < 52; i++)
        {
            for (int j = i+1; j < 52; j++)
            {
                c1 = Card.getCard(i);
                c2 = Card.getCard(j);
                num = hand[i][j];
                shand = c1.toStringSingle() + c2.toStringSingle();
                CNT++;
                //logger.debug(CNT+". "+ shand +": "+num);
                min = Math.min(num, min);
                if (min == num) { h_min.clear(); h_min.addCard(c1); h_min.addCard(c2); }
                max = Math.max(num, max);
                if (max == num) { h_max.clear(); h_max.addCard(c1); h_max.addCard(c2); }
                sum += num;
                sort.add(new DealInfo(shand, num));
            }
        }

        double avg = (double) sum / (double) CNT;
        Collections.sort(sort);

        for (DealInfo info : sort)
        {
            System.out.println(info.sHand + ", "+ info.cnt);
        }

        logger.debug("min: "+ min +", max: " + max + " avg: " + avg);
        logger.debug("min: "+h_min.toStringRankSuit()+", max: "+h_max.toStringRankSuit());

    }

    private class DealInfo implements Comparable
    {
        String sHand;
        int cnt;

        DealInfo(String s, int c)
        {
            sHand = s;
            cnt = c;
        }

        public int compareTo(Object o)
        {
            DealInfo d = (DealInfo) o;
            return cnt - d.cnt;
        }
    }
}
