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
 * DiceRoller.java
 *
 * Created on November 23, 2002, 5:23 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DiceRoller {
    
    static MersenneTwisterFast random_;
    
    /** Creates a new instance of DiceRoller */
    static {
        random_ = new MersenneTwisterFast();
    }

    /**
     * Roll nNum dice of nSides and return in integer array
     */
    public static Integer[] rollDice(int nSides, int nNum)
    {
        Integer results[] = new Integer[nNum];
        for (int i = 0; i < nNum; i++)
        {
            results[i] = rollDie(nSides);
        }
        
        return results;
    }
    
    /** 
     * Roll nNum die 6, return Integer array
     */
    public static Integer[] rollDice6(int nNum)
    {
        return rollDice(6, nNum);
    }
    
    /**
     * Roll one die of nSides, return Integer
     */
    public static Integer rollDie(int nSides)
    {
        int nNum = rollDieInt(nSides);
        return new Integer(nNum);
    }
    
    /**
     * Roll one die of nSides, return int
     */
    public synchronized static int rollDieInt(int nSides)
    {
        return random_.nextInt(nSides) + 1;
    }
    
    /**
     * Roll one die of nSides, return int.  Specify seed for random.  Typically
     * newSeed should be called after the (related) group of calls
     * that start with this one are done.
     */
    public synchronized static int rollDieInt(int nSides, long seed)
    {
        random_.setSeed(seed);
        return random_.nextInt(nSides) + 1;
    }
    
    /**
     * create new seed based on timestamp
     */
    public synchronized static void newSeed()
    {
        random_.setSeed(Utils.getCurrentTimeStamp());
    }

    /**
     * set the seed
     */
    public synchronized static void setSeed(long seed)
    {
        random_.setSeed(seed);
    }
    
    /**
     * Roll die 6, return Integer
     */
    public static Integer rollDie6()
    {
        return rollDie(6);
    }
    
    /**
     * Roll die 6, return int
     */
    public static int rollDie6int()
    {
        return rollDieInt(6);
    }
}
