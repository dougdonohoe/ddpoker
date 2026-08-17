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
import com.donohoedigital.games.config.GameState;
import com.donohoedigital.games.config.Territory;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.Gameboard;
import com.donohoedigital.games.engine.TerritorySelectionListener;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.PokerUtils;
import com.donohoedigital.games.poker.engine.PokerSaveDetails;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.online.TournamentDirector;
import com.donohoedigital.gui.DDLabel;
import com.donohoedigital.gui.DDPanel;
import com.donohoedigital.gui.GuiManager;
import com.donohoedigital.gui.GuiUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 14, 2005
 * Time: 6:43:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerInfo extends DashboardItem implements TerritorySelectionListener
{
    /** pixels between the name and the rows below it */
    private static final int NAME_GAP = 4;

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
        //
        // TYPE_PLAYER_CHIPS_CHANGED covers the one way chips move outside of a hand -
        // the cheat menu's change-chip-count option - which is a shipped user option,
        // not a debug build flag.  Rank tracks it for the same reason.
        //
        // Those are enough in a practice game, where the other tables are played out
        // inside our own hand, but not online, where they are independent: we only
        // ever listen to our own table, so nothing above fires when another table
        // finishes a hand or a player.  The rest of this mirrors Rank, which reports
        // the same two figures and needs them current for the same reasons -
        // STATE_NEW_LEVEL_CHECK included, since clients do not receive
        // PROP_PLAYER_FINISHED.
        int nEvents = PokerTableEvent.TYPE_PLAYER_REBUY |
                      PokerTableEvent.TYPE_END_HAND |
                      PokerTableEvent.TYPE_NEW_HAND |
                      PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED;
        if (game_.isOnlineGame()) nEvents |= PokerTableEvent.TYPE_STATE_CHANGED;
        trackTableEvents(nEvents);

        game_.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
        game_.addPropertyChangeListener(PokerGame.PROP_GAME_OVER, this);
        game_.addPropertyChangeListener(PokerGame.PROP_PLAYER_FINISHED, this);

        PokerUtils.getGameboard().addTerritorySelectionListener(this);
    }

    /**
     * Only the level check is of interest among the state changes - it runs after
     * clean-up, so the number of players left is settled by then.  Everything else
     * we track updates unconditionally, as the superclass does.
     */
    @Override
    public void tableEventOccurred(PokerTableEvent event)
    {
        if (event.getType() == PokerTableEvent.TYPE_STATE_CHANGED &&
            event.getNew() != PokerTable.STATE_NEW_LEVEL_CHECK) return;

        super.tableEventOccurred(event);
    }

    // these arrive on the game's thread, not the swing one - unlike the table events,
    // which the superclass already delivers in swing.  We set label text directly.
    private final Runnable updateInfoRunner_ = new Runnable()
    {
        public void run()
        {
            // invoke() is invokeLater from the game's thread, so the game can be torn
            // down before we run: finish() clears players_ and the rank lookup then
            // throws.  isDisplayed() is no help - bUiCreated_ is never reset - so this
            // is the same guard updateAll() uses, for the same reason.
            if (!game_.isFinished()) updateInfo();
        }
    };

    /**
     * Track the tournament-wide changes our own table does not tell us about - see
     * the note in the constructor, and the same three in Rank.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        if (name.equals(PokerGame.PROP_GAME_LOADED))
        {
            // a load of another table's state can move everyone's position
            GameState state = (GameState) evt.getOldValue();
            PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();
            if (pdetails.isOtherTableUpdate())
            {
                if (TournamentDirector.DEBUG_CLEANUP_TABLE) logger.debug("Update player info on other table update");
                GuiUtils.invoke(updateInfoRunner_);
            }
        }
        else if (name.equals(PokerGame.PROP_GAME_OVER) ||
                 name.equals(PokerGame.PROP_PLAYER_FINISHED))
        {
            GuiUtils.invoke(updateInfoRunner_);
        }
        super.propertyChange(evt);
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();

        // Dropped clear of the name rather than pinned flush to it: a line of text
        // carries its own leading and the graphic carries none, so flush against the
        // name the graphic reads as squashed against it.  On base's gap so that the
        // rows and the graphic drop together - the same 4 the Player Style panel puts
        // between its own two halves.
        ((BorderLayout) base.getLayout()).setVgap(NAME_GAP);

        // the player's name spans the full width, above everything else
        labelName_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        base.add(labelName_, BorderLayout.NORTH);

        // The graphic sits beside the rank rows rather than beside the panel as a
        // whole, and is pinned to the top of them, so it stays level with the
        // Tournament row however many rows are added underneath - rebuys, and the
        // online-only rows, both come and go.
        DDPanel rows = new DDPanel(GuiManager.DEFAULT, STYLE);
        ((BorderLayout) rows.getLayout()).setHgap(4);

        bands_ = new RankBandsPanel();

        // Top aligned so the first row stays level with the graphic.  A DDLabel
        // centres vertically by default, which would leave the two out of step
        // whenever the rows are shorter than the graphic - a single-table practice
        // game with no rebuys is one row against 25 pixels.
        labelInfo_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        labelInfo_.setVerticalAlignment(SwingConstants.TOP);

        rows.add(GuiUtils.NORTH(bands_), BorderLayout.WEST);
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
        // Into a local, so last_ goes on meaning "what the mouse is on".
        PokerPlayer player = last_ != null ? last_ : game_.getHumanPlayer();

        // Someone who is out is placed by where they finished, never by the chips in
        // front of them - they have none, so counting chips would tie every loser for
        // second.  PokerGame.playerOut() records the place, for the winner too.  Same
        // rule as Rank, which prefers getPlace() over getRank() for exactly this.
        int nPlace = player == null ? 0 : player.getPlace();
        boolean bOut = nPlace > 0;

        // No table means no seat to report and no rebuys to count.  A busted player
        // keeps their PokerPlayer but loses their seat: processRemovedPlayers() makes
        // them an observer, and that runs on the host - which in a practice game is
        // the human, so this is not an online-only path.  It does not happen at once,
        // though.  Both TournamentDirector.cleanTables() at the end of a tournament
        // and CheckEndHand when a practice player busts deliberately leave them
        // seated so the display still shows them, which is why having a seat cannot
        // stand in for still being in.
        PokerTable table = player == null || player.isObserver() ? null : player.getTable();
        int nTableRank = table == null ? 0 : table.getRank(player);
        boolean bSeated = nTableRank > 0;

        // One scale or two, and with it the presence of the Table row.  Decided by the
        // tournament alone so that both branches below agree: anything else and the
        // empty state pads to a different height than the filled-in state, and the
        // panel changes size as the mouse moves on and off the players.
        boolean bSingleBar = game_.getNumTables() == 1;

        if (bSeated || bOut)
        {
            String sRebuy = "";
            TournamentProfile profile = game_.getProfile();
            if (profile.isRebuys())
            {
                Object what = null;

                // an out player has nothing left to rebuy into, and without a table
                // there is no level to read anyway - both fall through to "none" below.
                // Out has to be tested in its own right: cleanTable() eliminates a
                // busted player but deliberately leaves them seated, so inside the
                // rebuy levels they still have a table to read a level from.
                int nLast = profile.getLastRebuyLevel();
                if (!bOut && table != null && table.getLevel() <= nLast)
                {
                    int nMax = profile.getMaxRebuys();
                    int nRebuys = player.getNumRebuys() + player.getNumRebuysPending();
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

            // The field the position is read against.  Someone who is out finished
            // among everyone who entered - "25th of 24" otherwise, and at the end of
            // the tournament nobody is left at all, which is the case the old count
            // had to special-case.  A player still in it is placed among those still
            // in it.
            int nTourneyRank;
            int nTourneyCount;
            if (bOut)
            {
                nTourneyRank = nPlace;
                nTourneyCount = game_.getNumPlayers();
            }
            else
            {
                nTourneyRank = game_.getRank(player);
                nTourneyCount = game_.getNumPlayers() - game_.getNumPlayersOut();
            }

            // Position at the hovered player's own table.  Dropped at a single table,
            // where the two scales would say the same thing, and left blank - but
            // still padded for - when the player is out and has no seat to report.
            int nTableCount = bSeated ? table.getNumOccupiedSeats() : 0;

            String sTable = "";
            if (!bSingleBar)
            {
                sTable = bSeated ? PropertyConfig.getMessage("msg.dash.playerinfo.table",
                                             PropertyConfig.getPlace(nTableRank),
                                             nTableCount)
                                 : PropertyConfig.getMessage("msg.dash.playerinfo.table.space");
            }

            // Disconnects and sit-outs can only happen in an online game - nothing in
            // a practice game sets either flag, so both counters would read zero for
            // the whole tournament.  Leave the rows out rather than show dead text.
            String sOnline = !game_.isOnlineGame() ? "" :
                             PropertyConfig.getMessage("msg.dash.playerinfo.online",
                                      player.getHandsPlayedDisconnected(),
                                      player.getHandsPlayedSitout());

            labelName_.setText(PropertyConfig.getMessage("msg.dash.playerinfo.name",
                                      Utils.encodeHTML(player.getName())));

            // the table row follows the tournament row directly, so the two positions
            // always read together and stay beside the graphic
            labelInfo_.setText(PropertyConfig.getMessage("msg.dash.playerinfo",
                                      PropertyConfig.getPlace(nTourneyRank),
                                      nTourneyCount,
                                      sTable,
                                      sOnline,
                                      sRebuy
            ));
            bands_.setValues(nTourneyRank, nTourneyCount, nTableRank, nTableCount, bSingleBar);
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
