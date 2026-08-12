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
import com.donohoedigital.gui.DDPanel;
import com.donohoedigital.gui.GuiManager;
import com.donohoedigital.gui.GuiUtils;

import javax.swing.*;
import java.awt.BorderLayout;
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
    /** pixels between the name above and the top of the position graphic */
    private static final int BANDS_TOP_GAP = 3;

    DDLabel labelInfo_;
    PokerPlayer last_;
    private DDLabel labelName_;
    private RankBandsPanel bands_;

    public PlayerInfo(GameContext context)
    {
        super(context, "playerinfo");

        // TYPE_PLAYER_REBUY keeps the rebuy count honest.  The two hand events keep
        // the ranks honest: the hovered player's position moves as the rest of the
        // tournament plays, and the mouse can sit still across many hands, so
        // without them the figures freeze at whatever they were when hovered.
        // TYPE_END_HAND is the settled moment - see the note in Rank.
        trackTableEvents(PokerTableEvent.TYPE_PLAYER_REBUY |
                         PokerTableEvent.TYPE_END_HAND |
                         PokerTableEvent.TYPE_NEW_HAND);
        PokerUtils.getGameboard().addTerritorySelectionListener(this);
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();

        // the player's name spans the full width, above everything else
        labelName_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        base.add(labelName_, BorderLayout.NORTH);

        // The graphic sits beside the rank rows rather than beside the panel as a
        // whole, and is pinned to the top of them, so it stays level with the
        // Tournament row however many rows are added underneath - rebuys, and the
        // online-only rows, both come and go.
        DDPanel rows = new DDPanel(GuiManager.DEFAULT, STYLE);
        ((BorderLayout) rows.getLayout()).setHgap(4);

        // Dropped clear of the name above rather than pinned flush to it.  A line of
        // text carries its own leading and the graphic carries none, so flush against
        // the name the graphic reads as squashed against it.
        bands_ = new RankBandsPanel();
        JComponent bandsTop = GuiUtils.NORTH(bands_);
        bandsTop.setBorder(BorderFactory.createEmptyBorder(BANDS_TOP_GAP, 0, 0, 0));

        // Top aligned so the first row stays level with the graphic.  A DDLabel
        // centres vertically by default, which would leave the two out of step
        // whenever the rows are shorter than the graphic - a single-table practice
        // game with no rebuys is one row against 25 pixels.
        labelInfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        labelInfo_.setVerticalAlignment(SwingConstants.TOP);

        rows.add(bandsTop, BorderLayout.WEST);
        rows.add(labelInfo_, BorderLayout.CENTER);
        base.add(rows, BorderLayout.CENTER);

        return base;
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
        if (!isOpen() || !isDisplayed()) return;

        // Fall back to ourselves, as the Player Style panel does, so the panel says
        // something useful before the mouse has been anywhere.  Done here rather than
        // where the mouse is read: that only runs once the mouse has crossed the
        // gameboard, and the panel is drawn - and starts tracking hands - well before.
        if (last_ == null) last_ = game_.getHumanPlayer();

        // A player who is out has no table.  PokerGame.playerOut() marks them
        // eliminated but only an online game converts them to an observer, so in a
        // practice game someone watching the AI after busting is still a plain player
        // whose seat OtherTables.cleanTable() has already taken away.  There is no
        // position to report for them, and the rebuy row below needs a table, so they
        // get the same empty state as no player at all.
        PokerTable table = last_ == null || last_.isObserver() ? null : last_.getTable();
        int nTableRank = table == null ? 0 : table.getRank(last_);
        boolean bSeated = nTableRank > 0;

        // One scale or two, and with it the presence of the Table row.  Decided by the
        // tournament alone so that both branches below agree: anything else and the
        // empty state pads to a different height than the filled-in state, and the
        // panel changes size as the mouse moves on and off the players.
        boolean bSingleBar = game_.getNumTables() == 1;

        if (bSeated)
        {
            String sRebuy = "";
            TournamentProfile profile = game_.getProfile();
            if (profile.isRebuys())
            {
                Object what = null;

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
            int nTourneyRank = game_.getRank(last_);

            // position at the hovered player's own table.  Dropped at a single table,
            // where the two scales would say the same thing.
            int nTableCount = table.getNumOccupiedSeats();

            String sTable = bSingleBar ? "" :
                            PropertyConfig.getMessage("msg.dash.playerinfo.table",
                                      PropertyConfig.getPlace(nTableRank),
                                      nTableCount);

            // Disconnects and sit-outs can only happen in an online game - nothing in
            // a practice game sets either flag, so both counters would read zero for
            // the whole tournament.  Leave the rows out rather than show dead text.
            String sOnline = !game_.isOnlineGame() ? "" :
                             PropertyConfig.getMessage("msg.dash.playerinfo.online",
                                      last_.getHandsPlayedDisconnected(),
                                      last_.getHandsPlayedSitout());

            labelName_.setText(PropertyConfig.getMessage("msg.dash.playerinfo.name",
                                      Utils.encodeHTML(last_.getName())));

            // the table row follows the tournament row directly, so the two positions
            // always read together and stay beside the graphic
            labelInfo_.setText(PropertyConfig.getMessage("msg.dash.playerinfo",
                                      PropertyConfig.getPlace(nTourneyRank),
                                      numLeft,
                                      sTable,
                                      sOnline,
                                      sRebuy
            ));
            bands_.setValues(nTourneyRank, numLeft, nTableRank, nTableCount, bSingleBar);
        }
        else
        {
            // keep the same height as when filled in, so the panel does not jump
            // about as the mouse moves on and off the players.  Each row that the
            // filled-in state can show is padded for on the same condition it appears.
            String sTableSpace = bSingleBar ? "" :
                                 PropertyConfig.getMessage("msg.dash.playerinfo.table.space");
            String sOnlineSpace = !game_.isOnlineGame() ? "" :
                                  PropertyConfig.getMessage("msg.dash.playerinfo.online.space");
            String sRebuySpace = "";
            if (game_.getProfile().isRebuys()) sRebuySpace = PropertyConfig.getMessage("msg.dash.rebuy.space");
            labelName_.setText(PropertyConfig.getMessage("msg.dash.playerinfo.name.none"));
            labelInfo_.setText(PropertyConfig.getMessage("msg.dash.playerinfo.none",
                                      sTableSpace, sOnlineSpace, sRebuySpace));

            // empty, but the same number of scales as when filled in - otherwise the
            // graphic changes shape as the mouse moves on and off the players
            bands_.setValues(0, 0, 0, 0, bSingleBar);
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
