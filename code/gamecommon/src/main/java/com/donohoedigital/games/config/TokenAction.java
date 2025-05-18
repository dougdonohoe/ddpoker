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
 * TokenAction.java
 *
 * Created on January 13, 2003, 10:17 AM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.comms.*;

/**
 *
 * @author  Doug Donohoe
 */
public class TokenAction {
    
    private Token token_;
    public GamePieceContainer from_; 
    public int nMovesUsed_;
    public DataMarshal oUserData_;
    
    /**
     * Empty constructor needed for demarshalling
     */
    public TokenAction() {}
    
    /** Creates a new instance of TokenAction */
    public TokenAction(GamePieceContainer from, int nMovesUsed, DataMarshal oUserData) {
        from_ = from;
        nMovesUsed_ = nMovesUsed;
        oUserData_ = oUserData;
    }
    
    /**
     * Set token this action belongs to (typically called from Token owner)
     */
    public void setToken(Token token)
    {
        token_ = token;
    }
    
    /**
     * Return token this action belongs to
     */
    public Token getToken()
    {
        return token_;
    }
    
    public String toString()
    {
        return "Moved " + nMovesUsed_ + " from " + (from_ != null ? from_.getName() : "null");
    }
    
    ////
    //// Game save logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_TOKENACTION);
        state.addEntry(entry);
        
        // moves used
        entry.addToken(nMovesUsed_);
        
        // from
        entry.addToken(state.getId(from_));
        
        // user data
        entry.addToken(oUserData_);
        
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        nMovesUsed_ = entry.removeIntToken();
        from_ = (GamePieceContainer) state.getObject(entry.removeIntegerToken());
        oUserData_ = (DataMarshal) entry.removeToken();
    }
}
