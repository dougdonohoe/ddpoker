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
 * PokerGameStateDelegate.java
 *
 * Created on February 6, 2003, 11:56 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerGameStateDelegate implements GameStateDelegate 
{
    
    /** 
     * Creates a new instance of PokerGameStateDelegate 
     */
    public PokerGameStateDelegate() {
    }
    
    /**
     * Should this territory be saved when game saved?
     */
    public boolean saveTerritory(Territory t) 
    {
        if (t.getNumPieces() == 0) return false;
        return true;
    }
    
    /**
     * Populate poker tables
     */
    public void prepopulateCustomIds(Object game, GameState state)
    {
        PokerGame pgame = (PokerGame) game;
        int nNum = pgame.getNumTables();
        for (int i = 0; i < nNum; i++)
        {
            state.setId(pgame.getTable(i));
        }
    }
    
    /**
     * returns true
     */
    public boolean createNewInstance(Class cClass) 
    {   
        return true;
    }
    
    /**
     * not used
     */
    public Object getInstance(Class cClass, GameState state, GameStateEntry entry) 
    {
        return null;
    }
    
    /**
     * no custom data
     */
    public void saveCustomData(GameState state) 
    {
    }
    
    /**
     * no custom data
     */
    public void loadCustomData(GameState state) 
    {
    }
    
    /**
     * Phase used to start game (i.e., display gameboard)
     */
    public String getBeginGamePhase(Object ocontext, Object game, GameState state, TypedHashMap params)
    {
        PokerGame pgame = (PokerGame) game;
        
        if (pgame.isClockMode())
        {
            return "BeginPokerNightGame";
        }
        else if (pgame.isOnlineGame())
        {
            String sSavedPhase = params.getString(ChainPhase.PARAM_NEXT_PHASE);
            if (sSavedPhase.equals(TournamentDirector.PHASE_NAME) ||
                sSavedPhase.equals("GameOver"))
            {
                return "BeginOnlineGame";
            }
            else
            {
                // special case where save file was created before game
                // actually started, like an online game in poker lobby.
                // we need to seed the history so "cancel" button work.
                GameEngine engine = GameEngine.getGameEngine();
                GameContext context = (PokerContext) ocontext;
                if (context.getNumHistory() == 0)
                {
                    context.seedHistory(engine.getGamedefconfig().getStartPhaseName());
                }
                return "ChainPhase";
            }
        }
        else
        {
            return "BeginTournamentGame";
        }
    }

}
