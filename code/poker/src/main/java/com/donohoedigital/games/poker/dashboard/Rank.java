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

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.beans.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 4:40:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class Rank extends DashboardItem
{
    DDLabel labelInfo_;

    public Rank(GameContext context)
    {
        super(context, "rank");
        setDynamicTitle(true);
        //setTableEventsImmediate(); // we need them immediately so rank is correct
        if (game_.isOnlineGame())
        {
            trackTableEvents(PokerTableEvent.TYPE_NEW_HAND |
                             PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED |
                             PokerTableEvent.TYPE_STATE_CHANGED);
        }
        else
        {
            trackTableEvents(PokerTableEvent.TYPE_NEW_HAND |
                             PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED);
        }
        game_.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
        game_.addPropertyChangeListener(PokerGame.PROP_GAME_OVER, this);
        game_.addPropertyChangeListener(PokerGame.PROP_PLAYER_FINISHED, this);
    }

    @Override
    protected JComponent createBody()
    {
        labelInfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        return labelInfo_;

    }

    @Override
    protected Object getDynamicTitleParam()
    {
        return sRank_;
    }

    /**
     * update rank on new level change and new hand
     */
    @Override
    public void tableEventOccurred(PokerTableEvent event)
    {
        boolean bUpdate = false;
        switch (event.getType())
        {
            case PokerTableEvent.TYPE_STATE_CHANGED:
                switch (event.getNew())
                {
                    case PokerTable.STATE_NEW_LEVEL_CHECK:
                        // update for possible change in number player
                        // because this occurs after cleanup.
                        // do this only for online games because clients don't
                        // get the PROP_PLAYER_FINISHED events
                        bUpdate = true;
                        break;

                }
                break;

            default:
                bUpdate = true;
        }
        if (bUpdate) updateInfo();
    }

    /**
     * track when game loaded, might need to update rank (online games)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        if (name.equals(PokerGame.PROP_GAME_LOADED))
        {
            // if are doing a game load where the table
            // is not the current table,
            // then update in case rank changed
            GameState state = (GameState) evt.getOldValue();
            PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();
            if (pdetails.isOtherTableUpdate())
            {
                if (TournamentDirector.DEBUG_CLEANUP_TABLE) logger.debug("Update display on other table update");
                updateInfo();
            }
        }
        else if (name.equals(PokerGame.PROP_GAME_OVER) ||
                 name.equals(PokerGame.PROP_PLAYER_FINISHED))
        {
            updateInfo();
        }
        super.propertyChange(evt);
    }


    ///
    /// display logic
    ///

    /**
     * update level
     */
    @Override
    protected void updateInfo()
    {
        if (!isDisplayed()) return; // check since we override superclass

        PokerTable table = game_.getCurrentTable();
        PokerPlayer human = game_.getHumanPlayer();

        int nRank = 0;
        int nWon = 0;
        int nHandNum = table.getHandNum();
        String sMsgKey;
        if (game_.isGameOver())
        {
            nRank = human.getPlace();
            nWon = human.getPrize();
            sMsgKey = nWon == 0 ? "msg.rank.over" : "msg.rank.over.win";
        }
        else if (nHandNum == 0)
        {
            sMsgKey = "msg.rank.start";
        }
        else if (human.getPlace() != 0)
        {

            nRank = human.getPlace();
            nWon = human.getPrize();
            sMsgKey = nWon == 0 ? "msg.rank.out" : "msg.rank.out.win";
        }
        else
        {
            sMsgKey = human.isObserver() ? "msg.rank.obs" : "msg.rank";
            if (!human.isObserver()) nRank = game_.getRank(human);
        }

        sRank_ = null;
        if (nRank != 0)
        {
            sRank_ = PropertyConfig.getPlace(nRank);
        }
        int nNumTables = game_.getNumTables();
        sTextToUpdate_ = PropertyConfig.getMessage(sMsgKey,
                                                   sRank_,
                                                   game_.getNumPlayers() - game_.getNumPlayersOut(),
                                                   nNumTables == 1 ? PropertyConfig.getMessage("msg.table") :
                                                   PropertyConfig.getMessage("msg.tables", nNumTables),
                                                   nWon > 0 ? nWon : null
        );
        GuiUtils.invoke(setLabelRunner_);
    }

    /**
     * override since this isn't called in swing event loop
     */
    @Override
    protected void updateTitle()
    {
        // done below
    }

    // text to set label
    private String sTextToUpdate_ = null;
    private String sRank_;

    // runnable for setting label text in swing thread
    private Runnable setLabelRunner_ = new Runnable()
    {
        public void run()
        {
            labelInfo_.setText(sTextToUpdate_);
            setTitle(getTitle());
        }
    };

}
