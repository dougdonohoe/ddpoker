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
 * PokerGamePiece.java
 *
 * Created on January 1, 2004, 5:41 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PokerGamePiece extends EngineGamePiece
{
    static Logger logger = LogManager.getLogger(PokerGamePiece.class);
    
    private String icName_;
    private ImageComponent ic_ = null;
    protected PokerPlayer pokerplayer_;
    
    /**
     * Empty constructor needed for demarshalling
     */
    public PokerGamePiece() {}
    
    /** 
     * Creates a new instance of PokerGamePiece 
     */
    public PokerGamePiece(int nType, PokerPlayer player, String tpName, String sName) {
        super(nType, player, tpName, sName);
        icName_ = sName;
        pokerplayer_ = player;
    }
    
    /**
     * Required, but not used in this game
     */
    public GamePiece duplicate()
    {
        ApplicationError.assertTrue(false, "duplicate() not supported");
        return null;
    }
    
    /**
     * Get name used to get image
     */
    public String getICName()
    {
        return icName_;
    }
    
    /**
     * Set name used to get image
     */
    public void setICName(String icName)
    {
        if (icName_ == null || !icName_.equals(icName))
        {
            icName_ = icName;
            ic_ = null;
        }
    }

    /**
     * Get player as PokerPlayer
     */
    public PokerPlayer getPokerPlayer()
    {
        return pokerplayer_;
    }
    
    /**
     * Sets poker game player too
     */
    public void setGamePlayer(GamePlayer player)
    {
        pokerplayer_ = (PokerPlayer) player;
        super.setGamePlayer(player);
    }
    
    /**
     * Get image component normally used by this game piece
     */
    public ImageComponent getImageComponent() 
    {
        if (ic_ == null) 
        {    
            ic_ = ImageComponent.getImage(icName_, 1.0);
        }
        return ic_;
    }
    
    /**
     *  Should the quantity be drawn over piece? Default is to return false
     */
    protected boolean isQuantityDrawn(int nNum, int nHiddenNum)
    {
        return false;
    }

    /**
     * Return xadjust when drawing multiple pieces in a region
     */
    public int getXAdjust()
    {
        return 0;
    }
    
    /**
     * Return yadjust when drawing multiple pieces in a region
     */
    public int getYAdjust()
    {
        return 0;
    }
    
    /**
     * Center at middle of territory point (default is bottom of piece is on point)
     */
    protected boolean isPositionedAtBottom()
    {
        return false;
    }
        
    ////
    //// Game save logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state, boolean bAdd)
    {
        GameStateEntry entry = super.addGameStateEntry(state, bAdd);
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        super.loadFromGameStateEntry(state, entry);
        setICName(getName());
        pokerplayer_ = (PokerPlayer) getGamePlayer();
    }
    
}
