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
 * GamePieceContainerImpl.java
 *
 * Created on January 13, 2003, 10:24 AM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;

import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GamePieceContainerImpl implements GamePieceContainer 
{
    //static Logger logger = LogManager.getLogger(GamePieceContainerImpl.class);
    
    Map pieces_ = Collections.synchronizedSortedMap(new TreeMap());
    GamePlayer player_;
    GamePieceContainer implFor_;
    
    /** 
     * Creates a new instance of GamePieceContainerImpl 
     */
    public GamePieceContainerImpl(GamePieceContainer implFor) {
        implFor_ = implFor;
    }
    
    /**
     * Return name
     */
    public String getName() {
        return "GamePieceContainerImpl";
    }
    
    /**
     * Display name
     */
    public String getDisplayName() {
        return getName();
    }
    
    /**
     * Return number of pieces in the container
     */
    public int getNumPieces()
    {
        return pieces_.size();
    }

    /**
     * Add a piece into the container.  If the gamepiece
     * already exists in the container, its contents are moved
     * using moveAllTokensTo().  Returns gp, or if already 
     * exists, the GamePiece that exists (with modified counts).
     */
    public GamePiece addGamePiece(GamePiece gp)
    {
        synchronized (pieces_)
        {
            ApplicationError.assertTrue(gp.getContainer() == null, "Adding GamePiece that already belongs to a container");
            gp.setContainer(implFor_);
            GamePiece gpExist = null;
        
            if ((gpExist = (GamePiece) pieces_.get(gp)) != null)
            {
                gp.transferAllTokensTo(gpExist);
                return gpExist;
            }
        
            pieces_.put(gp, gp);
            return gp;
        }
    }
    
    /**
     * Remove a gamepiece from the tree
     */
    public void removeGamePiece(GamePiece gp)
    {
        pieces_.remove(gp);
        gp.setContainer(null);
    }
    
    /**
     * Get all game pieces - should synchronize on getMap() around
     * call to this and use of iterator to avoid concurrent modification
     * exception
     */
    public Iterator getGamePieces()
    {
        return pieces_.keySet().iterator();
    }
    
    /**
     * Return gamepiece of given type.  If owner is provided (not null),
     * then the GamePiece's owner must match the given owner to be returned.
     * Returns null if nothing found to match.
     */
    public GamePiece getGamePiece(int nType, GamePlayer owner)
    {
        GamePiece piece;
        synchronized (pieces_)
        {
            Iterator iter = getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                if (piece.getType().intValue() == nType)
                {
                    if (owner != null)
                    {
                        if (piece.getGamePlayer() == owner)
                        {
                            return piece;
                        }
                    }
                    else
                    {
                        return piece;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Return true if this container has a piece of the given type
     * whose owner != the given owner
     */
    public boolean hasNonOwnerGamePiece(int nType, GamePlayer owner)
    {
        GamePiece piece;
        synchronized (pieces_)
        {
            Iterator iter = getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                if (piece.getType().intValue() == nType &&
                    piece.getGamePlayer() != owner)
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Get game player that owns this container
     */
    public GamePlayer getGamePlayer() {
        return player_;
    }
    
    /**
     * Set game player that owns this container
     */
    public void setGamePlayer(GamePlayer player) {
        player_ = player;
    }
    
    /** 
     * Is the class we are helping equal to the given class
     */
    public boolean equals(GamePieceContainer c) {
        return false;
    }
    
    /**
     * Return true if this territory has pieces that have moved (pieces
     * with a Token with history
     */
    public boolean hasMovedPieces()
    {
        GamePiece piece;
        synchronized (pieces_)
        {
            Iterator iter = getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                if (piece.hasMovedTokens())
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns map pieces are stored in, needed for syncing
     */
    public Map getMap() {
        return pieces_;
    }
}
