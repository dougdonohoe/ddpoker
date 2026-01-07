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
 * LanControllerInterface.java
 *
 * Created on May 11, 2006, 2:33 PM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.comms.*;

/**
 * Interface for game/peer2peer interaction
 *
 * @author donohoe
 */
public interface LanControllerInterface
{
    /**
     * Get key for this client
     */
    public String getPublicUseKey();

    /**
     * Get global unique id for instance of game
     */
    public String getGUID();

    /**
     * Get player name
     */
    public String getPlayerName();

    /**
     * return whether key is valid in this message
     */
    public boolean isValid(DDMessage msg);

    /**
     * handle duplicate key case
     */
    public void handleDuplicateKey(String sName, String sHost, String sIP);

    /**
     * handle duplicate ip case
     */
    public void handleDuplicateIp(String sName, String sHost, String sIP);

    /**
     * Allow duplicate copies running?
     */
    public boolean allowDuplicate();

    /**
     * Get game description
     */
    public DataMarshal getOnlineGame();

    /**
     * Return true if two online game descriptions are same
     */
    public boolean isEquivalentOnlineGame(DataMarshal one, DataMarshal two);
}
