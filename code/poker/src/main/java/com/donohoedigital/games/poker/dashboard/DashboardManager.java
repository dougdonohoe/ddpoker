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
package com.donohoedigital.games.poker.dashboard;

import org.apache.log4j.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.base.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 17, 2005
 * Time: 9:15:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class DashboardManager
{
    static Logger logger = Logger.getLogger(DashboardManager.class);

    private ArrayList items_;
    private DMTypedHashMap prefs_;
    private String sPrefName_;
    private PokerGame game_;

    public DashboardManager(PokerGame game)
    {
        game_ = game;
        items_ = new ArrayList();
        sPrefName_ = PokerConstants.PREF_DASHBOARD + "-";
        if (game_.isOnlineGame())
        {
            sPrefName_ += "online";
        }
        else if (game.isClockMode())
        {
            sPrefName_ += "clock";
        }
        else if (game.isSimulatorMode())
        {
            sPrefName_ += "simulator";
        }
        else
        {
            sPrefName_ += "practice";
        }

        loadPrefs();
    }

    /**
     * Get game
     */
    public PokerGame getGame()
    {
        return game_;
    }

    public void addItem(DashboardItem item, boolean bOpenByDefault)
    {
        item.setOpen(bOpenByDefault);
        items_.add(item);
        String pref = prefs_.getString(item.getName(), null);
        if (pref != null)
        {
            // ignore demarshal errors (registry edited?)
            try {
                item.demarshal(null, pref);
            } catch (Throwable e) {
                logger.error("Error demarshalling " + pref + ": " + Utils.formatExceptionText(e));
            }
        }

        // set position at end if not yet set
        if (item.getPosition() == DashboardItem.NOT_SET)
        {
            item.setPosition(items_.size() - 1);
        }
    }

    public int getNumItems()
    {
        return items_.size();
    }

    public DashboardItem getItem(int i)
    {
        return (DashboardItem) items_.get(i);
    }

    public void moveItem(DashboardItem move, boolean bUp)
    {
        int nIndex = items_.indexOf(move);
        ApplicationError.assertTrue(nIndex != -1, "Dashboard item not found", move);

        // figure out index of who we are swapping with
        int nSwap = bUp ? (nIndex - 1) : (nIndex + 1);
        if (nSwap < 0 || nSwap >= getNumItems()) return;
        DashboardItem swappee = getItem(nSwap);

        // move
        items_.set(nSwap, move);
        items_.set(nIndex, swappee);

        // change indicies
        swappee.setPosition(nIndex);
        move.setPosition(nSwap);
    }

    /**
     * sort based on position value and reset position
     */
    public void sort()
    {
        Collections.sort(items_);
        int nNum = getNumItems();
        DashboardItem item;
        for (int i = 0; i < nNum; i++)
        {
            item = getItem(i);
            item.setPosition(i);
        }
    }

    /**
     * notify manager of state change so prefs can be updated
     */
    public void stateChanged()
    {
        savePrefs();
    }

    /**
     *  Save prefs for each DashboardItem
     */
    private void savePrefs()
    {
        prefs_ = new DMTypedHashMap();
        int nNum = getNumItems();
        DashboardItem item;
        for (int i = 0; i < nNum; i++)
        {
            item = getItem(i);
            prefs_.put(item.getName(), item.marshal(null));
        }
        String sPrefs = prefs_.marshal(null);
        GameEngine.getGameEngine().getPrefsNode().put(sPrefName_, sPrefs);
    }

    /**
     * load prefs
     */
    private void loadPrefs()
    {
        String sPrefs = GameEngine.getGameEngine().getPrefsNode().get(sPrefName_, null);
        prefs_ = new DMTypedHashMap();
        if (sPrefs != null)
        {
            // ignore demarshal errors (registry edited?)
            try {
                prefs_.demarshal(null, sPrefs);
            } catch (Throwable e) {
                prefs_.clear();
                
                logger.error("Error demarshalling " + sPrefs + ": " + Utils.formatExceptionText(e));
            }

        }
    }
}
