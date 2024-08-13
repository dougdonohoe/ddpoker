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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;

public class PreFlopBias
{
    private static String openavgfull =
            "E E E M M M L L L D D D D "+
            "E E M L L L D D           "+
            "M L E M l D d             "+
            "M L l E l D d             "+
            "L l D D M D D             "+
            "L D d   d M D d           "+
            "L         d M D d         "+
            "D             M D d       "+
            "D               L d       "+
            "D                 L d     "+
            "D                   l     "+
            "D                     l   "+
            "D                       l ";
    
    private static String opentightfull =
            "E E M M M L L l D D D D D "+
            "E E M L L D D d           "+
            "M L E M l D d             "+
            "M L D M l D d             "+
            "L D D D M D d             "+
            "L D d   d M D d           "+
            "L         d L D d         "+
            "D             L d d       "+
            "D               L d       "+
            "d                 l d     "+
            "d                   D     "+
            "d                     D   "+
            "d                       D ";
            
    private static String openloosefull =
            "E E E E M M M M L L L D D "+
            "E E M L L L D D D d       "+
            "M L E M l D D d           "+
            "M L L E l D D d           "+
            "M L D D M D D d           "+
            "L D d D D M D d d         "+
            "L d d d d d M D d         "+
            "l d       d d M D d       "+
            "D               L D       "+
            "D                 L d     "+
            "D                   L     "+
            "D                     L   "+
            "D                       L ";
    
    private static String openavgshort =
            "E E E E E M M M M L L L L "+
            "E E E E M L L D D d d     "+
            "E L E E M L D D d         "+
            "E L L E M D D D           "+
            "M L D D E D D D           "+
            "M D D D D E D D d         "+
            "M D d d d d E D d d       "+
            "M d d     d d E D d       "+
            "L d           d M D d     "+
            "L                 M d     "+
            "L                   M     "+
            "L                     L   "+
            "L                       L ";
    
    public static float getOpenPotWeight(Card card1, Card card2, int position, float tightness)
    {
        float avg = getMatrixValue(openavgfull, card1, card2, position);
        float ext = getMatrixValue((tightness < 0.5) ? openloosefull : opentightfull, card1, card2, position);
        float rel = (float)Math.abs(tightness - 0.5) * 2f;

        return ext * rel + avg * (1f-rel);
    }

    private static float getMatrixValue(String matrix, Card card1, Card card2, int position)
    {
        int rank1 = card1.getRank();
        int rank2 = card2.getRank();

        int index;

        if ((rank1 < rank2) == (card1.getSuit() == card2.getSuit()))
        {
            index = (Card.ACE - rank2) * 26 + (Card.ACE - rank1) * 2;
        }
        else
        {
            index = (Card.ACE - rank1) * 26 + (Card.ACE - rank2) * 2;
        }

        switch (matrix.charAt(index))
        {
            case 'E':
                return 1f;
            case 'M':
                switch (position)
                {
                    case PokerAI.POSITION_EARLY:
                        return .25f;
                    default:
                        return 1f;
                }
            case 'L':
                switch (position)
                {
                    case PokerAI.POSITION_EARLY:
                        return .125f;
                    case PokerAI.POSITION_MIDDLE:
                        return .25f;
                    default:
                        return 1f;
                }
            case 'l':
                switch (position)
                {
                    case PokerAI.POSITION_EARLY:
                        return .0625f;
                    case PokerAI.POSITION_MIDDLE:
                        return .125f;
                    case PokerAI.POSITION_LATE:
                        return .5f;
                    default:
                        return 1f;
                }
            case 'D':
                switch (position)
                {
                    case PokerAI.POSITION_EARLY:
                        return .0625f;
                    case PokerAI.POSITION_MIDDLE:
                        return .125f;
                    case PokerAI.POSITION_LATE:
                        return .25f;
                    default:
                        return 1f;
                }
            case 'd':
                switch (position)
                {
                    case PokerAI.POSITION_EARLY:
                        return .03125f;
                    case PokerAI.POSITION_MIDDLE:
                        return .0625f;
                    case PokerAI.POSITION_LATE:
                        return .125f;
                    case PokerAI.POSITION_BUTTON:
                        return .5f;
                    default:
                        return 1f;
                }
            default:
                return 0f;
        }
    }
}

