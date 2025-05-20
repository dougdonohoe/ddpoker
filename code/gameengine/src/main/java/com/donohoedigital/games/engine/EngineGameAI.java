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
 * WarGameAI.java
 *
 * Created on April 25, 2003, 8:47 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;

/**
 *
 * @author  donohoe
 */
@DataCoder('@')
public abstract class EngineGameAI extends GameAI
{
    protected GamePlayer gamePlayer_;
    protected Territory my_[];
    protected int myNum_ = 0;

    /** 
     * Creates a new instance of EngineGameAI.  Stores the current
     * engine and game (so this class needs to be created after a game
     * has been created)
     */
    public EngineGameAI()
    {
        this(true);
    }
    
    /**
     * Version which controls whether territory cache is created
     */
    public EngineGameAI(boolean bUseTerritoryCache)
    {
        // array to hold our territories for perf
        // we use nNum_ to hold length
        if (bUseTerritoryCache) my_ = new Territory[Territory.getTerritoryArrayCached().length];
    }
    
    /**
     * Set player we are AI for
     */
    public void setGamePlayer(GamePlayer player)
    {
        gamePlayer_ = player;
    }
    
    /**
     *  Get player we are AI for
     */
    public GamePlayer getGamePlayer()
    {
        return gamePlayer_;
    }
    
    /**
     * Figure out territories owned by player
     */
    protected void determineMyTerritories()
    {
        Territory ts[] = Territory.getTerritoryArrayCached();
        myNum_ = 0;
        for (int i = 0; i < ts.length; i++)
        {
            if (ts[i].getGamePlayer() == gamePlayer_)
            {
                my_[myNum_++] = ts[i];
            }
        }
    }
    
    /**
     * Return array of territories belonging to this player - note that
     * the array only contains getNumMyTerritories() entries, so don't
     * use the length of the array in a loop!
     */
    public Territory[] getMyTerritories()
    {
        return my_;
    }
    
    /**
     * Return number of entries in myTerritories (not equal to length
     * of array!
     */
    public int getNumMyTerritories()
    {
        return myNum_;
    }

    /////
    ///// Marshalling code
    /////
    
    /**
     * read from string
     */
    public void demarshal(MsgState state, String sData) 
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        gamePlayer_ = (GamePlayer) state.getObject(list.removeIntegerToken());
        demarshal(state, list);
    }
    
    /**
     * For subclass to get stuff from list
     */
    protected void demarshal(MsgState state, TokenizedList list)
    {
    }
    
    /**
     * store to string
     */
    public String marshal(MsgState state) 
    {
        TokenizedList list = new TokenizedList();
        list.addToken(state.getId(gamePlayer_));
        marshal(state, list);
        return list.marshal(state);
    }
    
    /**
     * For subclass to add stuff to list
     */
    protected void marshal(MsgState state, TokenizedList list)
    {
    }
}
