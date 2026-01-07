/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 * CommunityCardPiece.java
 *
 * Created on January 1, 2004, 6:11 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

/**
 *
 * @author  Doug Donohoe
 */
public class CommunityCardPiece extends CardPiece
{    
    static Logger cclogger = LogManager.getLogger(CommunityCardPiece.class);
    
    private PokerTable table_;
    /**
     * Empty constructor needed for demarshalling
     */
    public CommunityCardPiece() {}
    
    /** 
     * Creates a new instance of CommunityCardPiece 
     */
    public CommunityCardPiece(PokerTable table, String sTerritoryPoint, int nSeq) {
        super(table.getGame().getGameContext(), null, sTerritoryPoint, true, nSeq);
        table_ = table;
    }
    
    /**
     * Get card
     */
    @Override
    public Card getCard()
    {
        // be defensive here and check to make sure a hand and community cards exist
        HoldemHand hhand = table_.getHoldemHand();  if (hhand == null) return null;
        Hand comm = hhand.getCommunity(); if (comm == null) return null;
        if (nSeq_ < 0 || nSeq_ >= comm.size()) return null;
        return comm.getCard(nSeq_);
    }
    
    /**
     * is this hand folded?
     */
    @Override
    public boolean isFolded()
    {
        return false;
    }
    
    ////
    //// Save/Load logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    @Override
    public GameStateEntry addGameStateEntry(GameState state, boolean bAdd)
    {
        GameStateEntry entry = super.addGameStateEntry(state, bAdd);
        entry.addToken(state.getId(table_));
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    @Override
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        super.loadFromGameStateEntry(state, entry);
        table_ = (PokerTable) state.getObject(entry.removeIntegerToken());
    }
}
