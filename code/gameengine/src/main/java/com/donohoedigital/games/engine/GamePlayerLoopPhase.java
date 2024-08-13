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
 * GamePlayerLoopPhase.java
 *
 * Created on December 17, 2002, 3:11 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GamePlayerLoopPhase extends BasePhase 
{
    //static Logger logger = Logger.getLogger(GamePlayerLoopPhase.class);
    
    protected Game game_;
    public static final String PARAM_STARTING_INDEX = "idx";
    public static final String PARAM_SAVE_LOOP_PHASE = "loop";
    public static final int INITIAL_INDEX = -1;
    protected String sNextPhase_;
    protected String sLoopPhase_;
    private String sSaveLoopPhase_;
    private int nPlayerIndex_;
    private boolean bStartingIndexProvided_ = false;
    
    /**
     * Init phase, storing engine and gamephase
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        game_ = context.getGame();
        sNextPhase_ = gamephase_.getString("next-phase", null);
        ApplicationError.assertNotNull(sNextPhase_, "next-phase not defined in ", gamephase.getName());
        
        sLoopPhase_ = gamephase_.getString("loop-phase", null);
        ApplicationError.assertNotNull(sLoopPhase_, "loop-phase not defined in ", gamephase.getName());
        
        sSaveLoopPhase_ = gamephase_.getString(PARAM_SAVE_LOOP_PHASE, null);
        
        nPlayerIndex_ = gamephase_.getInteger(PARAM_STARTING_INDEX, INITIAL_INDEX);
        if (nPlayerIndex_ != INITIAL_INDEX)
        {
            bStartingIndexProvided_ = true;
        }
    }
    
    /**
     * Application logic goes here
     */
    public void start()
    {        
        // if a starting index was provided in params,
        // use that to begin with
        if (bStartingIndexProvided_)
        {
            bStartingIndexProvided_ = false;
            nPlayerIndex_ = adjustProvidedStartingIndex(nPlayerIndex_);
        }
        // else if INITIAL_INDEX, get starting index
        else if (nPlayerIndex_ == INITIAL_INDEX)
        {
            nPlayerIndex_ = getStartingPlayerIndex();
        }
        // else get next index (we were pulled from the
        // cache and restarted)
        else
        {
            nPlayerIndex_ = getNextPlayerIndex(nPlayerIndex_);
        }
        
        if (isDone(nPlayerIndex_))
        {
            processNextPhase();
        }
        else
        {
            processLoopPhase();
        }
    }
    
    /**
     * Sets current player and calls the loop phase
     */
    protected void processLoopPhase()
    {
        // set current player
        setCurrentPlayer(nPlayerIndex_);
        
        // call loop phase.  If the save loop phase is set,
        // we have loaded from a previous save, so go to that
        // phase directly
        String sLoop = sLoopPhase_;
        if (sSaveLoopPhase_ != null)
        {
            sLoop = sSaveLoopPhase_;
            sSaveLoopPhase_ = null;
        }
        context_.processPhase(sLoop, getLoopParams());
    }
    
    /**
     * set current player given index
     */
    protected void setCurrentPlayer(int nPlayerIndex)
    {
        game_.setCurrentPlayer(nPlayerIndex);
    }
    
    /**
     * Clear current player
     */
    protected void clearCurrentPlayer()
    {
        game_.setCurrentPlayer(Game.NO_CURRENT_PLAYER);
    }
    
    /**
     * Clears current player and calls next phase
     */
    protected void processNextPhase()
    {
        nPlayerIndex_ = INITIAL_INDEX; // unnecessary now that removeCachedPhase called, but keep it for safety
        clearCurrentPlayer();
        
        // clean up
        context_.removeCachedPhase(gamephase_);
        
        // move on
        context_.processPhase(sNextPhase_, getNextParams());
    }
    
    /**
     * Allow subclass to change starting player index if it was provided (from load)
     */
    protected int adjustProvidedStartingIndex(int nStarting)
    {
        return nStarting;
    }
    
    /**
     * Returns starting player index.  Default returns 0;
     */
    protected int getStartingPlayerIndex()
    {
        return 0;
    }
    
    /**
     * Return index of player we are processing.  Current implementation
     * returns nCurrentIndex + 1
     */
    protected int getNextPlayerIndex(int nCurrentIndex)
    {
        return nCurrentIndex + 1;
    }
    
    /**
     * Get current player index
     */
    public int getCurrentPlayerIndex()
    {
        return nPlayerIndex_;
    }
    
    /**
     * Called to see if done looping.  Default implementation
     * returns true when nCurrentIndex is greater than/equal to the player count
     */
    protected boolean isDone(int nCurrentIndex)
    {
        if (nCurrentIndex >= game_.getNumPlayers())
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Called to get params to pass to loop phase
     * Returns null (designed to be overriden);
     */
    protected TypedHashMap getLoopParams()
    {
        return null;
    }
    
    /**
     * Called to get params to pass to next phase.
     * Returns null (designed to be overriden);
     */
    protected TypedHashMap getNextParams()
    {
        return null;
    }
    
    ////
    //// Game save logic
    ////
    
    /**
     * Return this phase encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        GameStateEntry entry = super._addGameStateEntry(state);
        entry.addNameValueToken(PARAM_STARTING_INDEX, new Integer(nPlayerIndex_));
        entry.addNameValueToken(PARAM_SAVE_LOOP_PHASE, context_.getCurrentPhase().getGamePhase().getName());
        return entry;
    }
}
