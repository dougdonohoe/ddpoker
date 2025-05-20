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
 * Vote.java
 *
 * Created on May 15, 2003, 4:11 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;

/**
 *
 * @author  donohoe
 */
public class Vote
{
    private float vote_;
    StringBuilder sbDebug = null;
    private static Format format = new Format("%2.2f");
    
    /**
     * New instance, value 0.0
     */
    public Vote(String sDebug)
    {
        this(0.0f, sDebug);
    }
    
    /** 
     * Creates a new instance of Vote 
     */
    public Vote(float init, String sDebug)
    {
        if (sDebug != null)
        {
            sbDebug = new StringBuilder();
        }
        set(init, sDebug);
    }
    
    /**
     * Create vote by copying another vote
     */
    public Vote(Vote vote)
    {
        vote_ = vote.vote_;
        sbDebug = new StringBuilder(vote.sbDebug.toString());
    }
    
    /**
     * Get vote value
     */
    public float get()
    {
        return vote_;
    }
    
    /**
     * Set vote value (clears debug list history)
     */
    public void set(float value, String sDebug)
    {
        vote_ = value;
        if (sbDebug != null) sbDebug.setLength(0);
        debug(value, sDebug);
    }
    
    /**
     * add value to vote
     */
    public void add(float value, String sDebug)
    {
        vote_ += value;
        debug(value, sDebug);
    }
    
    /**
     * Add value from vote to this vote
     */
    public void add(Vote vote)
    {
        vote_ += vote.vote_;
        debug(vote);
    }
    
    /**
     * Append debug info
     */
    private void debug(float value, String sDebug)
    {
        if (sbDebug != null) {
            sbDebug.append(sDebug);
            sbDebug.append("(");
            sbDebug.append(format.form(value));
            sbDebug.append(") ");
        }
    }
    
    /**
     * Append debug info
     */
    private void debug(Vote vote)
    {
        if (sbDebug != null) {
            sbDebug.append("** ");
            sbDebug.append("ADD(");
            sbDebug.append(format.form(vote.vote_));
            sbDebug.append(")");
            sbDebug.append(" [");
            sbDebug.append(vote.sbDebug);
            sbDebug.append("] ** ");
        }
    }
    
    /**
     * String version for debugging
     */
    public String toString()
    {
        return format.form(vote_) + " = " + sbDebug.toString();
    }
}
