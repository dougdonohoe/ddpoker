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
 * PokerURL.java
 *
 * Created on December 2, 2004, 8:58 AM
 */

package com.donohoedigital.games.poker.network;

import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.p2p.*;
import java.util.*;

/**
 * Subclass to parse URI into Game ID and password
 *
 * @author  donohoe
 */
public class PokerURL extends P2PURL 
{
    private String sID_;
    private String sPass_;
    
    public PokerURL(String spec) {
        super(spec);
        StringTokenizer st = new StringTokenizer(getURI(), PokerConstants.ID_PASS_DELIM);
        sID_ = st.nextToken();
        sPass_ = st.nextToken();
        
    }

    public boolean isTCP()
    {
        return !isUDP(sID_);
    }

    public boolean isUDP()
    {
        return isUDP(sID_);
    }

    public String getGameID()
    {
        return sID_;
    }
    
    public String getPassword()
    {
        return sPass_;
    }

    public static boolean isUDP(String sGameID)
    {
        return sGameID.startsWith(PokerConstants.ONLINE_GAME_PREFIX_UDP);
    }
}
