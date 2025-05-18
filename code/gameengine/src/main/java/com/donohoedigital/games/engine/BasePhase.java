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
 * BasePhase.java
 *
 * Created on November 15, 2002, 3:40 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.comms.NameValueToken;
import com.donohoedigital.config.Perf;
import com.donohoedigital.games.config.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author  Doug Donohoe
 */
public abstract class BasePhase implements Phase 
{
    static Logger logger = LogManager.getLogger(GameEngine.class);
    
    protected GameEngine engine_;
    protected GameContext context_;
    protected GamePhase gamephase_;
    protected Object oResult_;
    protected Object onlineResult_;
    
    /** 
     * Creates a new instance of BasePhase 
     */
    public BasePhase() {
        if (false) Perf.construct(this, null);
    }

    /**
     * Init phase, storing engine and gamephase
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        engine_ = engine;
        context_ = context;
        gamephase_ = gamephase;
    }
    
    /**
     * Reinit phase for use, called before reset(),
     * used to store new gamephase data
     */
    public void reinit(GamePhase gamephase) 
    {
        gamephase_ = gamephase;
    }
    
    /**
     * Called by engine - passes the last non-transient phase
     * invoked before this phase (i.e., the phase that launched
     * this phase).  BasePhase ignores this (doesn't store what
     * is passed in) to avoid keeping pointers to old phases.
     * Subclasses can override this if they need to know from
     * whence they came.
     */
    public void setFromPhase(Phase phase)
    {
    }
    
    /**
     * Must declare - logic of phase goes in here
     */
    abstract public void start();
    
    /**
     * Called when a phase is removed as the main component (
     * when using engine.setMainUIComponent()) or when
     * a DialogPhase's dialog is closed.  Other phases that 
     * don't use a UI (e.g., ChainPhase or LoopPhases) are finished
     * when their start() method is done, so any cleanup can be
     * done then.
     */
    public void finish()
    {
        //logger.debug("Finish called on " + gamephase_.getName());
    }
    
    /**
     * Returns GameEngine provided during init()
     */
    public GameEngine getGameEngine()
    {
        return engine_;
    }
    
    /**
     * Returns GamePhase provided during init()
     */
    public GamePhase getGamePhase()
    {
        return gamephase_;
    }
    
    /**
     * Returns true
     */
    public boolean processButton(GameButton button)
    {
        return true;
    }
    
    /**
     * Used in modal phases to return a result
     */
    public Object getResult()
    {
        return oResult_;
    }
    
    /**
     * Set the result
     */
    public void setResult(Object o)
    {
        oResult_ = o;
    }
    
    
    /**
     * Used in online phases to return a result in action confirmation
     */
    public Object getOnlineResult()
    {
        return onlineResult_;
    }
    
    /**
     * Set the result
     */
    public void setOnlineResult(Object o)
    {
        onlineResult_ = o;
    }
    
    /**
     * By default, no phase should be added as an entry, unless it
     * specifically allows it by overriding this.  It should call
     * the _addGameStateEntry to do the default work
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        ApplicationError.assertTrue(false, "Saving phase " + gamephase_.getName() + " is not supported");
        return null;
    }
    
    /**
     * Return this phase encoded as a game state entry
     */
    protected GameStateEntry _addGameStateEntry(GameState state)
    {
        // pass null in as object - we don't need to create an id for this
        // or recreate on loading side
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_PHASE);
        state.addEntry(entry);
        entry.addToken(gamephase_.getName());
        return entry;
    }
   
    /**
     * Add empty entry
     */
    public static GameStateEntry addNamedGameStateEntry(GameState state, String sPhaseName)
    {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_PHASE);
        state.addEntry(entry);
        entry.addToken(sPhaseName);
        return entry;
    }
    
    /**
     * Add empty entry
     */
    public static GameStateEntry addEmptyGameStateEntry(GameState state)
    {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_PHASE);
        state.addEntry(entry);
        entry.addTokenNull();
        return entry;
    }
    
    /**
     * Take entry and return the phase and params in a TypedHashMap usable
     * by ChainPhase.  Returns null if no phase stored.
     */
    public static TypedHashMap getPhaseFromGameStateEntry(GameStateEntry entry)
    {
        // get phase name - this is the next phase
        String sName = entry.removeStringToken();
        if (sName == null) return null; // empty entry (as added above)
     
        // create params, save name
        TypedHashMap params = new TypedHashMap();
        params.setString(ChainPhase.PARAM_NEXT_PHASE, sName);
        
        // create placeholder for params to that phase
        TypedHashMap nextParams = new TypedHashMap();
        params.setObject(ChainPhase.PARAM_NEXT_PHASE_PARAMS, nextParams);
        
        // loop through remaining tokens and load into nextParams
        NameValueToken.loadNameValueTokensIntoMap(entry, nextParams);
        return params;
    }

    /**
     * For debugging, returns gamephase name followed by this' class name
     */
    @Override
    public String toString()
    {
        return gamephase_.getName() + ": " + this.getClass().getName();
    }
    
    /**
     * By default all phases are used in demo.  If a phase returns
     * false and the game is in demo mode, the phase is not processed.
     */
    public boolean isUsedInDemo() 
    {
        return true;
    }
    
}
