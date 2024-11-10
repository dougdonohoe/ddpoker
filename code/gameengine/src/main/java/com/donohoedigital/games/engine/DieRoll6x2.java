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
 * DieRoll.java
 *
 * Created on November 23, 2002, 6:17 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.comms.DataCoder;
import com.donohoedigital.comms.DataMarshal;
import com.donohoedigital.comms.MsgState;

/**
 *
 * @author  Doug Donohoe
 */
@DataCoder('D')
public class DieRoll6x2 implements DataMarshal
{
    Integer nFirst;
    Integer nSecond;
    int nSum;

    public DieRoll6x2()
    {
        this.nFirst=DiceRoller.rollDie6();
        this.nSecond=DiceRoller.rollDie6();
        nSum = nFirst + nSecond;
    }

    public String toString()
    {
        return "(" + nFirst + "," + nSecond + ")";
    }
    
    public int getSum()
    {
        return nSum;
    }
    
    public int getFirst()
    {
        return nFirst;
    }
    
    public int getSecond()
    {
        return nSecond;
    }
    
    public void demarshal(MsgState state, String sData)
    {
        nFirst = Integer.parseInt(sData.substring(0,1));
        nSecond = Integer.parseInt(sData.substring(1));
        nSum = nFirst + nSecond;
    }
    
    public String marshal(MsgState state)
    {
        return nFirst.toString() + nSecond.toString();
    }
    
}
