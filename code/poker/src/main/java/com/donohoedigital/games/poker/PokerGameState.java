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
 * PokerGameState.java
 *
 * Created on February 4, 2005, 1:17 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;

import java.beans.*;
import java.io.*;

/**
 * Our own version of GameState - used to override starting ID (primary)
 * and to deal with incoming messages as a proxy MsgState (secondary).
 *
 * @author  donohoe
 */
public class PokerGameState extends GameState implements PropertyChangeListener
{
    private PokerGame game_;

    /**
     * Used for new games
     */
    public PokerGameState(String sName, String sBegin, String sExt, String sDesc)
    {
        super(sName, sBegin, sExt, sDesc);
    }

    /**
     * Used for new games
     */
    public PokerGameState(String sName, String sDesc)
    {
        super(sName, sDesc);
    }

    /**
     * Used for loading games
     */
    public PokerGameState(File f, boolean bLoadHeader)
    {
        super(f, bLoadHeader);
    }

    /**
     * Used for loading games
     */
    public PokerGameState(String sName, File fDir, String sBegin, String sExt, String sDesc)
    {
        super(sName, fDir, sBegin, sExt, sDesc);
    }

    /**
     * Used for loading games
     */
    public PokerGameState(byte[] data)
    {
        super(data);
    }

    /**
     * Return out starting id (see note in PokerConstants.java)
     */
    @Override
    protected int getStartId()
    {
        return PokerConstants.START_OTHER_ID;
    }

    ////
    //// Stuff below only used as a MsgState for handling online game messages
    //// that have id references to items not in the message itself (e.g.,
    //// HandActions that reference a player
    ////

    /**
     * Used in online games on the receiving end
     */
    public PokerGameState(PokerGame game, boolean bInitIds) {
        super("PokerGameState", "Used in DDMessage marshaling");
        game_ = game;
        // listener to update ids when players change
        game_.addPropertyChangeListener(PokerGame.PROP_PLAYERS, this);
        game_.addPropertyChangeListener(PokerGame.PROP_OBSERVERS, this);
        game_.addPropertyChangeListener(PokerGame.PROP_TABLES, this);
        game_.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
        if (bInitIds) setIds();
    }

    /**
     * clean up
     */
    public void finish()
    {
        game_.removePropertyChangeListener(PokerGame.PROP_PLAYERS, this);
        game_.removePropertyChangeListener(PokerGame.PROP_OBSERVERS, this);
        game_.removePropertyChangeListener(PokerGame.PROP_TABLES, this);
        game_.removePropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
    }

    /**
     * When players/tables add/removed, reload ids
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        setIds();
    }

    /**
     * init ids - similar to what Game does before loading
     */
    public void setIds()
    {
        resetIds();
        Territory territories[] = Territory.getTerritoryArrayCached();
        prepopulateIds(game_, territories, game_, game_);
    }
}
