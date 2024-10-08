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
 * Token.java
 *
 * Created on January 13, 2003, 10:11 AM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import org.apache.logging.log4j.*;

import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Token 
{
    static Logger logger = LogManager.getLogger(Token.class);
    
    GamePiece piece_;
    ArrayList actionHistory_ = new ArrayList();
    boolean bHidden_;

    /**
     * Empty constructor needed for demarshalling
     */
    public Token() {}
    
    /**
     * Creates a new instance of token with option to set hidden flag
     */
    public Token(boolean bHidden)
    {
        bHidden_ = bHidden;
    }
    
    /**
     * Get game piece we belong to
     */
    public GamePiece getGamePiece()
    {
        return piece_;
    }
    
    /**
     * Set game piece we belong to
     */
    public void setGamePiece(GamePiece piece)
    {
        piece_ = piece;
    }
    
    /**
     * Are we hidden?
     */
    public boolean isHidden()
    {
        return bHidden_;
    }
    
    /**
     * Set whether we are hidden
     */
    public void setHidden(boolean b)
    {
        bHidden_ = b;
    }
    
    /**
     * Get all actions
     */
    public ArrayList getActions()
    {
        return actionHistory_;
    }
    
    /**
     * Get number of actions
     */
    public int getNumActions()
    {
        return actionHistory_.size();
    }
    
    /**
     * Get last action performed on this token
     */
    public TokenAction getLastAction()
    {
        if (actionHistory_.size() == 0) return null;
        
        return (TokenAction) actionHistory_.get(actionHistory_.size() - 1);
    }
    
    /**
     * Get second to last action performed on this token
     */
    public TokenAction getSecondToLastAction()
    {
        if (actionHistory_.size() < 2) return null;
        
        return (TokenAction) actionHistory_.get(actionHistory_.size() - 2);
    }
    
    /**
     * Get action at given index
     */
    public TokenAction getAction(int i)
    {
        return (TokenAction) actionHistory_.get(i);
    }
    
    /**
     * Get and remove last action performed on this token
     */
    public TokenAction removeLastAction()
    {
        if (actionHistory_.size() == 0) return null;
        
        TokenAction action = getLastAction();
        if (action != null)
        {
            actionHistory_.remove(actionHistory_.size() - 1);
        }
        return action;
    }
    
    /**
     * Add an action performed on this token
     */
    public void addAction(TokenAction t)
    {
        ApplicationError.assertTrue(t.getToken() == null, "Adding tokenaction already in another action");
        actionHistory_.add(t);
        t.setToken(this);
    }
    
    /**
     * Remove an action from this token
     */
    public void removeAction(TokenAction t)
    {
        actionHistory_.remove(t);
        t.setToken(null);
    }
    
    /**
     * Get index of given action, -1 if doesn't exist
     */
    public int getIndexOfAction(TokenAction t)
    {
        return actionHistory_.indexOf(t);
    }
    
    /**
     * Clear actions
     */
    public void clearActions()
    {
        actionHistory_.clear();
    }
    
    /**
     * Move token to new container.  Return piece we moved to.
     */
    public GamePiece moveTo(GamePieceContainer newcontainer, int nMoves, 
                                    boolean bReplaceLastAction,
                                    DataMarshal oUserData)
    {
        // setup
        GamePiece oldpiece = piece_;
        GamePiece newpiece = oldpiece.duplicate();
        newpiece.clearTokens();
        TokenAction lastaction = getLastAction();
        
        // remove last action - we'll add a new one below
        if (bReplaceLastAction) {
            removeLastAction();
        }
        
        // action for this move
        TokenAction action = new TokenAction(oldpiece.getContainer(), nMoves, oUserData);
        
        // remove from old piece and add to new
        newpiece = newcontainer.addGamePiece(newpiece);
        oldpiece.removeToken(this);
        newpiece.addToken(this);
        
        // if keeping only one action, set from on this action
        // to that of original
        if (bReplaceLastAction && lastaction != null) {
            action.from_ = lastaction.from_;
        }
        
        // if the from location equals our new container, don't add action
        if (bReplaceLastAction && action.from_ == newcontainer)
        {
            // here we do nothing
        }
        else
        {
            addAction(action);
        }
        
        // if oldpiece we belonged to is now empty, remove it from its container
        if (oldpiece.getNumTokens() == 0)
        {
            oldpiece.getContainer().removeGamePiece(oldpiece);
        }
        
        return newpiece;
    }
    
    /**
     * Undo last move - move back to location identified in last action.
     * Returns new piece we belong to.
     */
    public GamePiece undoLastAction()
    {
        GamePiece oldpiece = piece_;
        GamePiece newpiece = oldpiece.duplicate();
        newpiece.clearTokens();
        TokenAction action = removeLastAction();
        ApplicationError.assertNotNull(action, "No last action");
        
        newpiece = action.from_.addGamePiece(newpiece);
        oldpiece.removeToken(this);
        newpiece.addToken(this);
        
        // if oldpiece we belonged to is now empty, remove it from its container
        if (oldpiece.getNumTokens() == 0)
        {
            oldpiece.getContainer().removeGamePiece(oldpiece);
        }
        
        return newpiece;
    }
    
    /**
     * Get sum of all moves
     */
    public int getTotalMoves()
    {
        int nSum = 0;
        for (int i = 0; i < actionHistory_.size(); i++)
        {
            nSum += getAction(i).nMovesUsed_;
        }
        return nSum;
    }
    
    public String toString()
    {
        return (piece_ != null ? piece_.getName() : "Token, No GamePiece") + " # history: " + actionHistory_.size() + " last action: " + getLastAction();
    }
    
    public void debugPrintActionHistory()
    {
        logger.debug("Action history for: " + toString());
        for (int i = 0; i < actionHistory_.size(); i++)
        {
            logger.debug("#" + i + ":" + getAction(i));
        }
    }
    
    ////
    //// Game save logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_TOKEN);
        state.addEntry(entry);
        
        // hidden
        entry.addToken(bHidden_);
        
        // actions
        TokenAction action;
        int nNumActions = getNumActions();
        entry.addToken(nNumActions);
        
        for (int i = 0; i < nNumActions; i++)
        {
            action = getAction(i);
            action.addGameStateEntry(state);
        }
        
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        bHidden_ = entry.removeBooleanToken();
        int nNum = entry.removeIntToken();
        
        TokenAction action;
        for (int i = 0; i < nNum; i++)
        {
            entry = state.removeEntry();
            action = (TokenAction) entry.getObject();
            action.loadFromGameStateEntry(state, entry);
            addAction(action);
        }
    }
}
