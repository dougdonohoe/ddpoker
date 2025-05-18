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
 * EngineMailErrorInfo.java
 *
 * Created on March 13, 2003, 10:34 AM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.base.*;
import com.donohoedigital.mail.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
public class EngineMailErrorInfo implements DDMailErrorInfo
{
    private static final String DELIM = ".";
    private String sAppName_;
    private String sGameID_;
    private int nPlayerID_;
    private String sEmailID_;
    
    /** 
     * Creates a new instance of EngineMailErrorInfo 
     */
    public EngineMailErrorInfo(String sAppName, String sGameID, int nPlayerID, String sEmailID)
    {
        sAppName_ = sAppName;
        sGameID_ = sGameID;
        nPlayerID_ = nPlayerID;
        sEmailID_ = sEmailID;
    }
    
    /**
     * Create a new instance of EngineMailErrorInfo from the appname and 
     * error details (as returned by getErrorDetails)
     */
    public EngineMailErrorInfo(String sAppName, String sErrorDetails)
    {
        sAppName_ = sAppName;
        StringTokenizer tok = new StringTokenizer(sErrorDetails, DELIM);
        ApplicationError.assertTrue(tok.countTokens() == 3, "Must be 3 tokens in error details");
        sGameID_ = tok.nextToken();
        nPlayerID_ = Integer.parseInt(tok.nextToken());
        sEmailID_ = tok.nextToken();
    }
    
    /**
     * Get game id
     */
    public String getGameID()
    {
        return sGameID_;
    }
    
    /**
     * Get player id
     */
    public int getPlayerID()
    {
        return nPlayerID_;
    }
    
    /**
     * Get email id
     */
    public String getEmailID()
    {
        return sEmailID_;
    }
    
    /**
     * Return info to identify email as a string
     */
    public String getErrorDetails()
    {
        return sGameID_ + DELIM + nPlayerID_ + DELIM + sEmailID_;
    }
    
    /**
     * Return application this is for
     */
    public String getErrorAppName()
    {
        return sAppName_;
    }
    
}
