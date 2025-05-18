/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * HoldemExpert.java
 *
 * Created on March 7, 2004, 7:27 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
public class HoldemExpert {
    
    /** Creates a new instance of HoldemExpert */
    public HoldemExpert() {
    }
    
    ////
    //// Sklansky's starting hands
    ////
    
    public static final int MULT = 100;
    
    /** MAX GROUP1 value **/
    public static final int MAXGROUP1 = (MULT * 2) - 1;
    public static final int MAXGROUP2 = (MULT * 3) - 1;
    public static final int MAXGROUP3 = (MULT * 4) - 1;
    public static final int MAXGROUP4 = (MULT * 5) - 1;
    public static final int MAXGROUP5 = (MULT * 6) - 1;
    public static final int MAXGROUP6 = (MULT * 7) - 1;
    public static final int MAXGROUP7 = (MULT * 8) - 1;
    public static final int MAXGROUP8 = (MULT * 9) - 1;
    
    /**
     * Return Sklansky group num based on rank
     */
    public static int getGroupFromRank(int nRank)
    {
        return nRank / MULT;
    }
    
    /**
     * Return sklansky rank.  This is 100 * group # + index.  Lower is better.
     */
    public static int getSklanskyRank(HandSorted hand)
    {
        int n;
        for (int i = 0; i < GROUPS.length; i++)
        {
            if ((n = getIndex(GROUPS[i], hand)) != -1)
            {
                return (MULT * (i+1)) + (n + 1);
            }
        }
        
        // not in one of the groups, so return rank * 10 (i.e., group 10)
        return MULT * 10;
    }
    
    /**
     * Is hand part of group?  Return index or -1
     */
    public static int getIndex(ArrayList group, HandSorted hand)
    {
        HandSorted h;
        for (int i = 0; i < group.size(); i++)
        {
            h = (HandSorted) group.get(i);
            if (h.isEquivalent(hand)) return i;
        }
        return -1;
    }
    
    // groups
    private static ArrayList[] GROUPS = new ArrayList[8];
    private static ArrayList aGROUP1 = new ArrayList();
    private static ArrayList aGROUP2 = new ArrayList();
    private static ArrayList aGROUP3 = new ArrayList();
    private static ArrayList aGROUP4 = new ArrayList();
    private static ArrayList aGROUP5 = new ArrayList();
    private static ArrayList aGROUP6 = new ArrayList();
    private static ArrayList aGROUP7 = new ArrayList();
    private static ArrayList aGROUP8 = new ArrayList();
    
    // specific hands
    public static final HandSorted AA = new HandSorted(Card.CLUBS_A, Card.SPADES_A);
    public static final HandSorted KK = new HandSorted(Card.CLUBS_K, Card.SPADES_K);
    public static final HandSorted QQ = new HandSorted(Card.CLUBS_Q, Card.SPADES_Q);
    public static final HandSorted JJ = new HandSorted(Card.CLUBS_J, Card.SPADES_J);
    public static final HandSorted TT = new HandSorted(Card.CLUBS_T, Card.SPADES_T);
    
    public static final HandSorted AKs = new HandSorted(Card.CLUBS_A, Card.CLUBS_K);
    public static final HandSorted AQs = new HandSorted(Card.CLUBS_A, Card.CLUBS_Q);
    public static final HandSorted AJs = new HandSorted(Card.CLUBS_A, Card.CLUBS_J);
    public static final HandSorted KQs = new HandSorted(Card.CLUBS_K, Card.CLUBS_Q);
    
    public static final HandSorted AKo = new HandSorted(Card.CLUBS_A, Card.SPADES_K);
    public static final HandSorted AQo = new HandSorted(Card.CLUBS_A, Card.SPADES_Q);
    public static final HandSorted KQo = new HandSorted(Card.CLUBS_K, Card.SPADES_Q);
    
    public static final HandSorted T9s = new HandSorted(Card.CLUBS_T, Card.CLUBS_9);
    public static final HandSorted p99 = new HandSorted(Card.CLUBS_9, Card.SPADES_9);
    public static final HandSorted p88 = new HandSorted(Card.CLUBS_8, Card.SPADES_8);
    
    // init hands - suit is unimportant due to comparisons
    // based on suitedness, handedness in HandSorted.  order in
    // array matches Sklansky's order
    static {
        aGROUP1.add(AA);  // AA
        aGROUP1.add(KK);  // KK
        aGROUP1.add(QQ);  // QQ
        aGROUP1.add(JJ);  // JJ
        aGROUP1.add(AKs); // AKs
        
        aGROUP2.add(TT);  // TT
        aGROUP2.add(AQs); // AQs
        aGROUP2.add(AJs); // AJs
        aGROUP2.add(KQs); // KQs
        aGROUP2.add(AKo); // AK
        
        aGROUP3.add(p99); // 99
        aGROUP3.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_T));  // JTs
        aGROUP3.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_J));  // QJs
        aGROUP3.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_J));  // KJs
        aGROUP3.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_T));  // ATs
        aGROUP3.add(new HandSorted(Card.CLUBS_A, Card.SPADES_Q)); // AQ
        
        aGROUP4.add(T9s);  // T9s
        aGROUP4.add(new HandSorted(Card.CLUBS_K, Card.SPADES_Q)); // KQ
        aGROUP4.add(p88); // 88
        aGROUP4.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_T));  // QTs
        aGROUP4.add(new HandSorted(Card.CLUBS_9, Card.CLUBS_8));  // 98s
        aGROUP4.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_9));  // J9s
        aGROUP4.add(new HandSorted(Card.CLUBS_A, Card.SPADES_J)); // AJ
        aGROUP4.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_T));  // KTs
        
        aGROUP5.add(new HandSorted(Card.CLUBS_7, Card.SPADES_7)); // 77
        aGROUP5.add(new HandSorted(Card.CLUBS_8, Card.CLUBS_7));  // 87s
        aGROUP5.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_9));  // Q9s
        aGROUP5.add(new HandSorted(Card.CLUBS_T, Card.CLUBS_8));  // T8s
        aGROUP5.add(new HandSorted(Card.CLUBS_K, Card.SPADES_J)); // KJ
        aGROUP5.add(new HandSorted(Card.CLUBS_Q, Card.SPADES_J)); // QJ
        aGROUP5.add(new HandSorted(Card.CLUBS_J, Card.SPADES_T)); // JT
        aGROUP5.add(new HandSorted(Card.CLUBS_7, Card.CLUBS_6));  // 76s
        aGROUP5.add(new HandSorted(Card.CLUBS_9, Card.CLUBS_7));  // 97s
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_9));  // AXs 9
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_8));  // AXs 8
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_7));  // AXs 7
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_6));  // AXs 6
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_5));  // AXs 5
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_4));  // AXs 4
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_3));  // AXs 3
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_2));  // AXs 2
        aGROUP5.add(new HandSorted(Card.CLUBS_6, Card.CLUBS_5));  // 65s
        
        aGROUP6.add(new HandSorted(Card.CLUBS_6, Card.SPADES_6));  // 66
        aGROUP6.add(new HandSorted(Card.CLUBS_A, Card.SPADES_T));  // AT
        aGROUP6.add(new HandSorted(Card.CLUBS_5, Card.SPADES_5));  // 55
        aGROUP6.add(new HandSorted(Card.CLUBS_8, Card.CLUBS_6));   // 86s
        aGROUP6.add(new HandSorted(Card.CLUBS_K, Card.SPADES_T));  // KT
        aGROUP6.add(new HandSorted(Card.CLUBS_Q, Card.SPADES_T));  // QT
        aGROUP6.add(new HandSorted(Card.CLUBS_5, Card.CLUBS_4));   // 54s
        aGROUP6.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_9));   // K9s
        aGROUP6.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_8));   // J8s
        aGROUP6.add(new HandSorted(Card.CLUBS_7, Card.CLUBS_5));   // 75s
        
        aGROUP7.add(new HandSorted(Card.CLUBS_4, Card.SPADES_4));  // 44
        aGROUP7.add(new HandSorted(Card.CLUBS_J, Card.SPADES_9));  // J9
        aGROUP7.add(new HandSorted(Card.CLUBS_4, Card.CLUBS_3));   // 43s
        aGROUP7.add(new HandSorted(Card.CLUBS_T, Card.SPADES_9));  // T9
        aGROUP7.add(new HandSorted(Card.CLUBS_3, Card.SPADES_3));  // 33
        aGROUP7.add(new HandSorted(Card.CLUBS_9, Card.SPADES_8));  // 98
        aGROUP7.add(new HandSorted(Card.CLUBS_6, Card.CLUBS_4));   // 64s
        aGROUP7.add(new HandSorted(Card.CLUBS_2, Card.SPADES_2));  // 22
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_8));   // KXs 8
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_7));   // KXs 7
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_6));   // KXs 6
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_5));   // KXs 5
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_4));   // KXs 4
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_3));   // KXs 3
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_2));   // KXs 2
        aGROUP7.add(new HandSorted(Card.CLUBS_T, Card.CLUBS_7));   // T7s
        aGROUP7.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_8));   // Q8s
        
        aGROUP8.add(new HandSorted(Card.CLUBS_8, Card.SPADES_7));  // 87
        aGROUP8.add(new HandSorted(Card.CLUBS_5, Card.CLUBS_3));   // 53s
        aGROUP8.add(new HandSorted(Card.CLUBS_A, Card.SPADES_9));  // A9
        aGROUP8.add(new HandSorted(Card.CLUBS_Q, Card.SPADES_9));  // Q9
        aGROUP8.add(new HandSorted(Card.CLUBS_7, Card.SPADES_6));  // 76
        aGROUP8.add(new HandSorted(Card.CLUBS_4, Card.CLUBS_2));   // 42s
        aGROUP8.add(new HandSorted(Card.CLUBS_3, Card.CLUBS_2));   // 32s
        aGROUP8.add(new HandSorted(Card.CLUBS_9, Card.CLUBS_6));   // 96s
        aGROUP8.add(new HandSorted(Card.CLUBS_8, Card.CLUBS_5));   // 85s
        aGROUP8.add(new HandSorted(Card.CLUBS_J, Card.SPADES_8));  // J8
        aGROUP8.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_7));   // J7s
        aGROUP8.add(new HandSorted(Card.CLUBS_6, Card.SPADES_5));  // 65
        aGROUP8.add(new HandSorted(Card.CLUBS_5, Card.SPADES_4));  // 54
        aGROUP8.add(new HandSorted(Card.CLUBS_7, Card.CLUBS_4));   // 74s
        aGROUP8.add(new HandSorted(Card.CLUBS_K, Card.SPADES_9));  // K9
        aGROUP8.add(new HandSorted(Card.CLUBS_T, Card.SPADES_8));  // T8
        
        GROUPS[0] = aGROUP1;
        GROUPS[1] = aGROUP2;
        GROUPS[2] = aGROUP3;
        GROUPS[3] = aGROUP4;
        GROUPS[4] = aGROUP5;
        GROUPS[5] = aGROUP6;
        GROUPS[6] = aGROUP7;
        GROUPS[7] = aGROUP8;
        
    }
    
}
