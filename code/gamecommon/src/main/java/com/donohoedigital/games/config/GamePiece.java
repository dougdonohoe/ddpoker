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
 * GamePiece.java
 *
 * Created on December 9, 2002, 11:30 AM
 */

package com.donohoedigital.games.config;


import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.PropertyConfig;

import java.util.ArrayList;

/**
 *
 * @author  Doug Donohoe
 */
public abstract class GamePiece implements Comparable
{
    //static Logger logger = LogManager.getLogger(GamePiece.class);
    
    protected String sName_ = null;
    protected Integer nType_ = null;
    protected GamePlayer player_ = null;
    protected GamePieceContainer container_ = null;
    ArrayList tokens_ = new ArrayList();
    
    /**
     * Empty constructor needed for demarshalling
     */
    public GamePiece() {}
    
    /**
     * Create new piece
     */
    public GamePiece(int nType, GamePlayer player, String sName)
    {
        nType_ = nType;
        player_ = player;
        sName_ = sName;
        addToken(createToken(false));
    }
    
    /**
     * Returns new Token(this).  Here so can 
     * be overriden to provide subclass of Token
     */
    protected Token createToken(boolean bHidden)
    {
        return new Token(bHidden);
    }
    
    /**
     * Get list of all tokens
     */
    public ArrayList getTokens()
    {
        return tokens_;
    }
    
    /**
     * Get number of tokens
     */
    public int getNumTokens()
    {
        return tokens_.size();
    }
    
    /**
     * Get token at given index
     */
    public Token getToken(int i)
    {
        return (Token) tokens_.get(i);
    }
    
    /**
     * Add token
     */
    public void addToken(Token token)
    {
        ApplicationError.assertTrue(token.getGamePiece() == null, "Adding token already in another piece");
        token.setGamePiece(this);
        tokens_.add(token);
    }
    
    /**
     * Add nQuantity new tokens (bHidden defaulted to false)
     */
    public void addNewTokens(int nQuantity)
    {
        addNewTokens(nQuantity, false);
    }
    
    /**
     * Add nQuantity new tokens of given hidden-ness
     */
    public void addNewTokens(int nQuantity, boolean bHidden)
    {
        for (int i = 0; i < nQuantity; i++)
        {
            addToken(createToken(bHidden));
        }
    }
    
    /**
     * Remove token
     */
    public void removeToken(Token token)
    {
        ApplicationError.assertTrue(tokens_.contains(token), "Removing token not in this piece");
        tokens_.remove(token);
        token.setGamePiece(null);
    }
    
    /**
     * Get index of given token.  Return -1 if not in this piece
     */
    public int getIndexOfToken(Token token)
    {
        return tokens_.indexOf(token);
    }
    
    /**
     * Clear all tokens
     */
    public void clearTokens()
    {
        tokens_.clear();
    }
    
    /**
     * Remove 1st token we find that matches the hidden flag
     *
     * @return removed token, null if nothing removed
     */
    public Token removeFirstMatchingToken(boolean bHidden)
    {
        for (int i = 0; i < tokens_.size(); i++)
        {
            if (getToken(i).isHidden() == bHidden)
            {
                Token token = getToken(i);
                tokens_.remove(i);
                return token;
            }
        }
        
        return null;
    }
    
    /**
     * Move all my tokens to the given piece (move history not effected)
     */
    void transferAllTokensTo(GamePiece piece)
    {
        for (int i = 0; i < tokens_.size(); i++)
        {
            getToken(i).setGamePiece(piece);
        }
         
        piece.tokens_.addAll(tokens_);
        tokens_.clear();
    }
    
    /**
     * Return true if this piece has a token that has been moved
     */
    public boolean hasMovedTokens()
    {
        for (int i = 0; i < tokens_.size(); i++)
        {
            if (getToken(i).getNumActions() > 0)
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Subclasses implement this - return duplicate of self
     */
    public abstract GamePiece duplicate();
    
    /**
     * Set name of this piece
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }
    
    /**
     * Get name of this piece
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Get Displayable name of this piece.
     * Lookup "gamepiece." + getName() in PropertyConfig
     */
    public String getNameDisplay()
    {
        return getNameDisplay(null);
    }
    
    /**
     * Get displayable name of form "gamepiece." + getName() + "." + sType
     */
    public String getNameDisplay(String sType)
    {
        String sLookup = "gamepiece." + sName_;
        if (sType != null) sLookup += "." + sType;
        
        return PropertyConfig.getStringProperty(sLookup, sLookup);
    }
    
    /**
     * Set all tokens to given hidden state
     */
    public void setTokensHidden(boolean bHidden)
    {
        for (int i = 0; i < tokens_.size(); i++)
        {
            getToken(i).setHidden(bHidden);
        }
    }
    
    /**
     * Get number of visible tokens
     */
    public int getQuantity()
    {
        int nCnt = 0;
        for (int i = 0; i < tokens_.size(); i++)
        {
            if (!getToken(i).isHidden())
            {
                nCnt++;
            }
        }
        
        return nCnt;
    }
    
    /**
     * Get number of hidden tokens
     */
    public int getHiddenQuantity()
    {
        int nCnt = 0;
        for (int i = 0; i < tokens_.size(); i++)
        {
            if (getToken(i).isHidden())
            {
                nCnt++;
            }
        }
        
        return nCnt;
    }
    
    /**
     * Set player that owns this piece
     */
    public void setGamePlayer(GamePlayer player)
    {
        player_ = player;
    }
    
    /**
     * Get player that owns this piece
     */
    public GamePlayer getGamePlayer()
    {
        return player_;
    }
    
    /**
     * set integer type of this player
     */
    public void setType(Integer nType)
    {
        nType_ = nType;
    }
    
    /**
     * Get integer type of this player
     */
    public Integer getType()
    {
        return nType_;
    }
    
    /**
     * Set container this piece is in
     */
    public void setContainer(GamePieceContainer t)
    {
        container_ = t;
    }
    
    /**
     * Get container this piece is in
     */
    public GamePieceContainer getContainer()
    {
        return container_;
    }
    
    /**
     * Get territory this piece is in (casts container
     * to territory if appropriate).  Otherwise returns null.
     */
    public Territory getTerritory()
    {
        if (container_ instanceof Territory)
        {
            return (Territory) container_;
        }
        
        return null;
    }
    
    /**
     * Get string representation of piece - used for debugging
     */
    public String toString()
    {
        String sPlayer = "";
        if (player_ != null) sPlayer += player_.getName();
        return sName_ + " ("+nType_ + ") in " + container_ + " owner " + sPlayer;
    }
    
    /**
     * Two pieces are equal if the integer type, container
     * and owner are the same
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof GamePiece)) return false;
        
        GamePiece gp = (GamePiece) o;
        
        if (nType_.equals(gp.nType_) &&
            container_ == gp.container_ && // == should work
            player_ == gp.player_)         // == should work
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Compares two pieces.  Unequal pieces are ordered by
     * type first.  if type is equal, pieces which are owned
     * by the owner of a container are order first (over pieces
     * owned by "invaders".  This is important because the 
     * compareTo order dicates the order in which they are drawn.
     */
    public int compareTo(Object o) 
    {
        ApplicationError.assertTrue(o instanceof GamePiece, "Comparing GamePiece to non GamePiece");
        
        GamePiece gp = (GamePiece) o;
        
        // if totally equal to each other, return 0
        if (equals(gp)) return 0;
        
        // containers should always be equal when doing compareTo
        ApplicationError.assertTrue(container_.equals(gp.container_), 
                                "Comparing game pieces in two different containers.");
        
        // if types aren't same, compare types
        if (!nType_.equals(gp.nType_))
        {
            return nType_.compareTo(gp.nType_);
        }
        
        // *** at this point, type is same ***
        
        if (player_ != null && gp.player_ != null &&
            container_ != null && gp.container_ != null)
        {
            
            // players are same so pieces are equal
            if (player_.equals(gp.player_)) 
            {
                return 0;
            }
            
            // if the owner of the container is the owner of this piece,
            // then this piece should be listed before other one
            if (isOwnerContainerOwner())
            {
                return Integer.MIN_VALUE;
            }
            else if (gp.isOwnerContainerOwner())
            {
                return Integer.MAX_VALUE;
            }
            
            // last case, compare players
            return player_.compareTo(gp.player_);
        }
            
        ApplicationError.assertTrue(false, "player and or container is null, this: " + this + ", other: " + gp);
        return 0; // won't get here
    }
    
    /**
     * Returns true if the owner of this piece is also the owner of 
     * the container in which the piece resides
     */
    public boolean isOwnerContainerOwner()
    {
        GamePlayer t_owner = container_.getGamePlayer();
        
        if (player_ == null && t_owner == null) {
            return true;
        }
        
        if (player_ != null && player_.equals(t_owner))
        {
            return true;
        }
        
        return false;
    }
 
    protected boolean bSelected_ = false;
    
    /**
     * Set this piece as selected
     */
    public void setSelected(boolean b)
    {
        bSelected_ = b;
    }
    
    /**
     * Is this piece selected?
     */
    public boolean isSelected()
    {
        return bSelected_;
    }
    
    protected boolean bUnderMouse_ = false;
    
    /**
     * Set this piece as under mouse
     */
    public void setUnderMouse(boolean b)
    {
        bUnderMouse_ = b;
    }
    
    /**
     * Is this piece under mouse?
     */
    public boolean isUnderMouse()
    {
        return bUnderMouse_;
    }
    
    ////
    //// Game save logic
    ////
    
    /**
     * Return this piece encoded as a game state entry.   If bAdd is true,
     * the entry is added to the given GameState
     */
    public GameStateEntry addGameStateEntry(GameState state, boolean bAdd)
    {
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_GAMEPIECE);
        if (bAdd) state.addEntry(entry);
        
        // type
        entry.addToken(nType_);
        
        // name
        entry.addToken(sName_);
        
        // index
        entry.addToken(state.getId(getGamePlayer()));
        
        // total number of tokens
        int nNumTokens = getNumTokens();
        Token token;
        entry.addToken(nNumTokens);
        
        // hidden quantity (visible is total - hidden)
        entry.addToken(getHiddenQuantity());
        
        // write tokens?
        boolean bWriteTokens = hasMovedTokens() || alwaysSaveTokens();
        entry.addToken(bWriteTokens);
        
        if (bWriteTokens)
        {
            // add each token
            for (int i = 0; i < nNumTokens; i++)
            {
                token = getToken(i);
                token.addGameStateEntry(state);
            }
        }
        
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        nType_ = entry.removeIntegerToken();
        sName_ = entry.removeStringToken();
        setGamePlayer((GamePlayer) state.getObject(entry.removeIntegerToken()));
        int nNumTokens = entry.removeIntToken();
        int nHiddenTokens = entry.removeIntToken();
        int nVisibleTokens = nNumTokens - nHiddenTokens;
        boolean bWriteTokens = entry.removeBooleanToken();
        
        // if the tokens weren't written, just create appropriate # of visible/hidden
        if (!bWriteTokens)
        {
            addNewTokens(nVisibleTokens, false);
            addNewTokens(nHiddenTokens, true);
        }
        else
        {
            Token token;
            for (int i = 0; i < nNumTokens; i++)
            {
                entry = state.removeEntry();
                token = (Token) entry.getObject();
                token.loadFromGameStateEntry(state, entry);
                addToken(token);
            }
        }     
    }
    
    /**
     * Return true if tokens should always be written out
     * on a save.  Defaults to false, which means tokens
     * are only written out when hasMovedTokens() is true -
     * this saves space in save file
     */
    protected boolean alwaysSaveTokens()
    {
        return false;
    }
}
