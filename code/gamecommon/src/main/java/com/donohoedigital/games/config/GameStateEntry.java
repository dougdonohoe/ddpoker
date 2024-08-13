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
 * GameStateEntry.java
 *
 * Created on February 5, 2003, 2:37 PM
 */

package com.donohoedigital.games.config;


import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;

import java.io.*;

/**
 *
 * @author  Doug Donohoe
 */
@DataCoder('e')
public class GameStateEntry extends TokenizedList
{
    //static Logger logger = Logger.getLogger(GameStateEntry.class);

    private char cType_;
    private Object o_;
    private Integer id_;
    private String sClassName_;
    
    /**
     * Empty constructor needed for demarshalling
     */
    public GameStateEntry() {}
    
    /** 
     * Creates a new instance of GameStateEntry 
     */
    public GameStateEntry(MsgState state, Object o, char cType) {
        o_ = o;
        cType_ = cType;
        if (o != null)
        {
            id_ = state.getId(o);
            sClassName_ = o.getClass().getName();
        }
        addToken(id_);
        addToken(state.getClassId(sClassName_));
    }
    
    /**
     * Create a new instance of GameStateEntry from a String,
     * previously generated with write().  This constructor
     * does not parse all of the data - it only parses the
     * fields necessary to recreate the id/classname.
     * Remaining tokens can be parsed with the "finishParsing()"
     * method.
     */
    public GameStateEntry(MsgState state, String sData)
    {
        super(state, sData, 2); // only load two tokens
        initAfterRead(state);
    }
    
    /**
     * Init the entry after it was loaded via read
     */
    private void initAfterRead(MsgState mstate)
    {
        GameState state = (GameState) mstate;
        GameStateDelegate delegate = state.getDelegate();
        if (delegate == null) return;
        id_ = removeIntegerToken();
        Integer classid = removeIntegerToken();
        sClassName_ = state.getClassName(classid);
        if (id_ != null)
        {
            // see if object is already there (may have been pre-populated)
            o_ = state.getObjectNullOkay(id_);
            
            // if not, look for it
            if (o_ == null)
            {
                Class cClass = ConfigUtils.getClass(sClassName_, true);
            
                if (delegate.createNewInstance(cClass))
                {
                    o_ = ConfigUtils.newInstance(cClass);
                }
                else
                {
                    o_ = delegate.getInstance(cClass, state, this);
                }
                state.setId(o_, id_);
            }
        }
    }
    
    /**
     * Used to change type of entry
     */
    public void setType(char cType)
    {
        cType_ = cType;
    }
    
    /**
     * Get type of entry
     */
    public char getType()
    {
        return cType_;
    }
    
    /**
     * Return object
     */
    public Object getObject()
    {
        return o_;
    }
    
    /**
     * Return id of the object.  Returns -1 if no id
     */
    public int getID()
    {
        if (id_ == null) return -1;
        return id_;
    }
    
    /**
     * Write this list
     */
    public void write(MsgState state, Writer writer) throws IOException
    {
        writer.write(cType_);
        writer.write(TOKEN_DELIM);
        super.write(state, writer);
    }
    
    /**
     * Init this list from a string (opposite of write)
     */
    public void read(MsgState state, EscapeStringTokenizer tokenizer, int nReadNumTokens)
    {
        ApplicationError.assertTrue(tokenizer.hasMoreTokens(), "Nothing to read");
        String token = tokenizer.nextToken();
        ApplicationError.assertTrue(token.length() == 1, "Incorrect type", token);
        cType_ = token.charAt(0);
        super.read(state, tokenizer, nReadNumTokens);
    }

    /**
     * Recreate list from string
     */
    public void demarshal(MsgState state, String sData) {
        super.demarshal(state, sData);
        initAfterRead(state);
    }
    
    /**
     * Store list as a string
     */
    public String marshal(MsgState state) {
        return super.marshal(state);
    }
}
