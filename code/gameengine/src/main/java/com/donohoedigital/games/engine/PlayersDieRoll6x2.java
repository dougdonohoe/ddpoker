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
 * PlayersDieRoll.java
 *
 * Created on November 23, 2002, 9:07 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.comms.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;

/**
 *
 * @author  Doug Donohoe
 */
@DataCoder('R')
public class PlayersDieRoll6x2 implements DataMarshal
{
    //static Logger logger = LogManager.getLogger(PlayersDieRoll6x2.class);
    
    int nIndex_;
    DMArrayList dieRolls_[];
    GameInfo game_;
    
    /** 
     * Empty constructor needed for marshalling
     */
    public PlayersDieRoll6x2()
    {
    }
    
    /** 
     * Creates a new instance of PlayersDieRoll6x2
     */
    public PlayersDieRoll6x2(GameInfo game) 
    {
        game_ = game;
        int nNumPlayers = game.getNumPlayers();
        GamePlayer player;
        dieRolls_ = new DMArrayList[nNumPlayers];
        for (int i = 0; i < nNumPlayers; i++)
        {
            dieRolls_[i] = new DMArrayList();
        }
        
        int nMax = -1;
        int nRolls = 0;
        while (getNumAtMax(dieRolls_, nMax, nRolls) > 1)
        {
            nMax = rollDieAtMax(dieRolls_, nMax, nRolls);
            nRolls++;
        }
        
        // done rolling dice - show result
        nIndex_ = getIndexOfMax(dieRolls_, nMax, nRolls);
    }
    
    public int getWinningPlayerID()
    {
        return nIndex_;
    }
    
    public DMArrayList[] getDieRolls()
    {
        return dieRolls_;
    }

    private int getIndexOfMax(DMArrayList dieRolls[], int nMax, int nMinRolls)
    {   
        int nCnt = 0;
        for (int i = 0; i < dieRolls.length; i++)
        {
            if (isPlayerEliminated(i)) continue;
            if ((dieRolls[i].size() >= nMinRolls) &&
                        ((DieRoll6x2)dieRolls[i].get(dieRolls[i].size() -1)).getSum() == nMax)
            {
                return i;
            }
        }
        return 0;   
    }
    
    private int getNumAtMax(DMArrayList dieRolls[], int nMax, int nMinRolls)
    {
        // special case for 1st time, need to roll all die
        if (nMax == -1) return dieRolls.length;
        
        int nCnt = 0;
        for (int i = 0; i < dieRolls.length; i++)
        {
            if (isPlayerEliminated(i)) continue;
            if ((dieRolls[i].size() >= nMinRolls) &&
                        ((DieRoll6x2)dieRolls[i].get(dieRolls[i].size() -1)).getSum() == nMax)
            {
                nCnt++;
            }
        }
        return nCnt;   
    }
    
    private int rollDieAtMax(DMArrayList dieRolls[], int nMax, int nMinRolls)
    {
        DieRoll6x2 roll;
        int nNewMax = -1;
        for (int i = 0; i < dieRolls.length; i++)
        {
            if (isPlayerEliminated(i)) continue;
            if ((dieRolls[i].size() == 0) || (
                        (dieRolls[i].size() >= nMinRolls) &&
                        ((DieRoll6x2)dieRolls[i].get(dieRolls[i].size() -1)).getSum() == nMax))
            {
                roll = new DieRoll6x2();
                dieRolls[i].add(roll);
                nNewMax = Math.max(nNewMax, roll.getSum());
            }
        }
        return nNewMax;
    }
    
    /**
     * Is player at index still in game?
     */
    private boolean isPlayerEliminated(int id)
    {
        return game_.isEliminated(id);
    }
    
    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        nIndex_ = list.removeIntToken();
        int nNum = list.removeIntToken();
        dieRolls_ = new DMArrayList[nNum];
        for (int i = 0; i < nNum; i++)
        {
            dieRolls_[i] = (DMArrayList) list.removeToken();
        }
    }
    
    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(nIndex_);
        list.addToken(dieRolls_.length);
        for (int i = 0; i < dieRolls_.length; i++)
        {
            list.addToken(dieRolls_[i]);
        }
        return list.marshal(state);
    }
}
