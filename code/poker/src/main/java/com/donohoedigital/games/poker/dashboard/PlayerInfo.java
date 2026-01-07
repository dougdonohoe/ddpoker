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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.Territory;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.Gameboard;
import com.donohoedigital.games.engine.TerritorySelectionListener;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.PokerUtils;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.gui.DDLabel;
import com.donohoedigital.gui.GuiManager;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 14, 2005
 * Time: 6:43:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerInfo extends DashboardItem implements TerritorySelectionListener
{
    DDLabel labelInfo_;
    PokerPlayer last_;

    public PlayerInfo(GameContext context)
    {
        super(context, "playerinfo");
        trackTableEvents(PokerTableEvent.TYPE_PLAYER_REBUY);
        PokerUtils.getGameboard().addTerritorySelectionListener(this);
    }

    protected JComponent createBody()
    {
        labelInfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        return labelInfo_;
    }

    /**
     * update when new territory moused over
     */
    private void updateInfo(Territory t)
    {
        if (!isOpen() || !isDisplayed()) return;

        PokerPlayer p;

        if (t == null) p = null;
        else p = PokerUtils.getPokerPlayer(context_, t);

        if (p != last_)
        {
            last_ = p;
            updateInfo();
        }
    }

    /**
     * update observer list
     */
    protected void updateInfo()
    {
        if (last_ != null && !last_.isObserver())
        {
            String sRebuy = "";
            TournamentProfile profile = game_.getProfile();
            if (profile.isRebuys())
            {
                Object what = null;

                PokerTable table = last_.getTable();
                int nLast = profile.getLastRebuyLevel();
                if (table.getLevel() <= nLast)
                {
                    int nMax = profile.getMaxRebuys();
                    int nRebuys = last_.getNumRebuys() + last_.getNumRebuysPending();
                    int nLeft = nMax - nRebuys;

                    if (nMax == 0)
                    {
                        what = PropertyConfig.getMessage("msg.dash.rebuy.unlimited");
                    }
                    else if (nLeft > 0)
                    {                        
                        what = nLeft;
                    }
                }

                if (what == null)
                {
                    what = PropertyConfig.getMessage("msg.dash.rebuy.none");
                }

                sRebuy = PropertyConfig.getMessage("msg.dash.rebuy", what);
            }

            int numLeft = game_.getNumPlayers() - game_.getNumPlayersOut();
            // if end of tournament, list number of players in tournament
            if (numLeft == 0) numLeft = game_.getNumPlayers();
            labelInfo_.setText(PropertyConfig.getMessage("msg.dash.playerinfo",
                                      Utils.encodeHTML(last_.getName()),
                                      last_.getHandsPlayedDisconnected(),
                                      last_.getHandsPlayedSitout(),
                                      sRebuy,
                                      PropertyConfig.getPlace(game_.getRank(last_)),
                                      numLeft
            ));
        }
        else
        {
            String sRebuySpace = "";
            if (game_.getProfile().isRebuys()) sRebuySpace = PropertyConfig.getMessage("msg.dash.rebuy.space");
            labelInfo_.setText(PropertyConfig.getMessage("msg.dash.playerinfo.none", sRebuySpace));
        }

    }

    ////
    //// Territory listener - used to change display when mouse moves
    ////

    public void mouseEntered(Gameboard g, Territory t)
    {
        updateInfo(t);
    }

    public void mouseExited(Gameboard g, Territory t)
    {
        updateInfo(null);
    }

    public void territorySelected(Territory t, MouseEvent e)
    {
        // nada
    }

    public boolean allowTerritorySelection(Territory t, MouseEvent e)
    {
        return false;
    }
}
