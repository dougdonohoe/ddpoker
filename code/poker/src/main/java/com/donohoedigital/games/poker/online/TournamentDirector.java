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
 * TournamentDirector.java
 *
 * Created on January 21, 2005, 10:24 AM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import org.apache.log4j.*;

import javax.swing.*;
import java.util.*;

/**
 * Class which handles tournament.
 *
 * @author donohoe
 */
@SuppressWarnings({"PublicField"})
public class TournamentDirector extends BasePhase implements Runnable, GameManager, ChatManager
{
    static Logger logger = Logger.getLogger(TournamentDirector.class);

    // debugging stmts
    public static final boolean DEBUG = false; // general debug in this class
    public static boolean DEBUG_SLEEP = false; // debug when sleeping
    public static boolean DEBUG_SAVE = false; // save
    public static boolean DEBUG_EVENT = false; // event handling in this class
    public static boolean DEBUG_EVENT_DISPLAY = false; // event display in ShowTournamentTable, PokerGameboard
    public static boolean DEBUG_CLEANUP_TABLE = DEBUG; // display stmts w.r.t. table cleanup and consolidation
    public static boolean DEBUG_REJOIN = false; // display rejoin info

    // name of phase associated with this in gamedef.xml
    public static final String PHASE_NAME = "TournamentDirector";

    // wait time before clearing wait list for non-betting wait
    public static final int NON_BETTING_TIMEOUT_MILLIS = 10 * 1000;

    // wait time before clearing wait list for new level check timeout
    // this allows extra time for answering rebuy/addon messages
    public static final int NEWLEVEL_TIMEOUT_MILLIS = 25 * 1000;

    // time to wait before clearing a pending rejoin
    public static final int REJOIN_TIMEOUT_MILLIS = 20 * 1000;

    // TD default sleep between checking for things to do
    private static final int SLEEP_MILLIS = 250;

    // AI pause in tenths for online games
    public static final int AI_PAUSE_TENTHS = 10; // one second

    // member data
    private Thread thread_;
    private Thread threadAlive_;
    private static int nThreadSeq_ = 0;
    private boolean bDone_ = false;
    private PokerGame game_;
    private OnlineManager mgr_;
    private ChatHandler chat_;
    private boolean bOnline_;

    // two variables to make code easier to read in places
    private boolean bClient_; // online client
    private boolean bHost_;   // online host (also true if practice mode)

    /**
     * Phase start
     */
    @Override
    public void start()
    {
        game_ = (PokerGame) context_.getGame();
        context_.setGameManager(this);
        bOnline_ = game_.isOnlineGame();
        bClient_ = gamephase_.getBoolean("client", false);
        bHost_ = !bClient_;

        if (bOnline_)
        {
            mgr_ = game_.getOnlineManager();
            if (mgr_ == null)
            {
                // if no online manager, create it.  This is null
                // when loading game from save file.  We create here
                // so that mgr is init'd with TD so it can properly
                // process any incoming rejoin messages
                mgr_ = game_.initOnlineManager(this);
            }
            else
            {
                mgr_.setTournamentDirector(this);
            }
        }

        if (bHost_)
        {
            // starting up - new and loaded games, need to
            // set the last change time to now to account for
            // lobby/startup time
            PokerTable table;
            int nNum = game_.getNumTables();
            for (int i = 0; i < nNum; i++)
            {
                table = game_.getTable(i);
                table.touchLastStateChangeTime();
            }

            // new game
            if (bOnline_ && game_.isStartFromLobby())
            {
                // add all remote players to wait list so we don't start until they are ready
                for (int i = 0; i < nNum; i++)
                {
                    table = game_.getTable(i);
                    if (table.isAllComputer()) continue;
                    table.addWaitAllHumans();
                }

                // host is ready
                PokerPlayer host = game_.getLocalPlayer();
                host.getTable().removeWait(host);

                // have each player display the tournament table
                PokerPlayer player;
                nNum = game_.getNumPlayers();
                for (int i = 0; i < nNum; i++)
                {
                    player = game_.getPokerPlayerAt(i);
                    if (player.isComputer() || player.isHost()) continue;
                    mgr_.onlineProcessPhase(HostStart.PHASE_CLIENT_INIT, null, false, player);
                }

                // and each observer (loop from bottom in case of error, which removes observer)
                nNum = game_.getNumObservers();
                for (int i = nNum - 1; i >= 0; i--)
                {
                    player = game_.getPokerObserverAt(i);
                    if (player.isHost()) continue;
                    mgr_.onlineProcessPhase(HostStart.PHASE_CLIENT_INIT, null, false, player);
                }
            }

            // reset timeout clock - players
            if (bOnline_)
            {
                PokerPlayer player;
                nNum = game_.getNumPlayers();
                for (int i = 0; i < nNum; i++)
                {
                    player = game_.getPokerPlayerAt(i);
                    if (player.isComputer() || player.isHost()) continue;
                    player.clearMessageReceived();
                }

                // reset timeout clock - observers
                nNum = game_.getNumObservers();
                for (int i = nNum - 1; i >= 0; i--)
                {
                    player = game_.getPokerObserverAt(i);
                    if (player.isHost()) continue;
                    player.clearMessageReceived();
                }
            }

            startWanGame();
            setStartPause();
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            thread_ = new Thread(this, "TournamentDirector-" + (nThreadSeq_++));
            thread_.start();
        }
        // client
        else if (bOnline_)
        {
            // reset timeout clock - host
            game_.getHost().clearMessageReceived();
        }

        // alive thread for online games (only needed for non-UDP transport since UDP layer handles that)
        if (bOnline_ && !mgr_.isUDP())
        {
            threadAlive_ = new TDAlive();
            threadAlive_.start();
        }
    }

    /**
     * on startup, if table is in state waiting to deal for button,
     * force a pause in online games so there is a delay between
     * when table is displayed and when deal happens
     */
    private void setStartPause()
    {
        if (bOnline_)
        {
            int tableState = game_.getHost().getTable().getTableState();
            int pendingState = game_.getHost().getTable().getPendingTableState();

            boolean bHostTableDealForButton = tableState == PokerTable.STATE_DEAL_FOR_BUTTON ||
                                              pendingState == PokerTable.STATE_DEAL_FOR_BUTTON;

            if (bHostTableDealForButton)
            {
                boolean bPauseAtStart = PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_PAUSE, false);

                if (bPauseAtStart)
                {
                    TypedHashMap params = new TypedHashMap();
                    params.setString(HostPauseDialog.PARAM_MSG_KEY, "msg.host.paused.start");
                    context_.processPhaseNow("HostPauseDialog", params);
                }
            }

            // upon start see if we are disconnected
            mgr_.checkAllConnected();
        }
    }

    /**
     * Is client?
     */
    public boolean isClient()
    {
        return bClient_;
    }

    /**
     * alive thread
     */
    private class TDAlive extends Thread
    {
        TDAlive()
        {
            super("TDAlive");
        }

        @Override
        public void run()
        {
            while (!bDone_)
            {
                try
                {
                    Utils.sleepMillis(OnlineManager.ALIVE_SLEEP_MILLIS);

                    if (!bDone_)
                    {
                        mgr_.alive(true);
                    }
                }
                catch (Throwable t)
                {
                    logger.error("TDAlive caught an unexcepted exception: " + Utils.formatExceptionText(t));
                }
            }
        }
    }


    /**
     * thread start
     */
    public void run()
    {
        boolean bSleep = false;
        // wait while clients display;
        int nSleep;
        while (!bDone_)
        {
            try
            {
                bSleep = process();
            }
            catch (Throwable t)
            {
                logger.error("TournamentDirector caught an unexcepted exception: " + Utils.formatExceptionText(t));
                bDone_ = true;

                // log current hand
                PokerContext.LogGameInfo(game_);

                // if online, try to save game
                if (bOnline_)
                {
                    try
                    {
                        saveGame("error");
                    }
                    catch (Throwable darnCantSave)
                    {
                        logger.error("Attempted to save but caught an exception: " + Utils.formatExceptionText(darnCantSave));
                    }
                }

                // show error dialog to user and restart
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                EngineUtils.displayInformationDialog(context_, Utils.fixHtmlTextFor15(
                                        PropertyConfig.getMessage("msg.tderror")));
                                context_.restart();
                            }
                        }
                );
            }

            // if not done, sleep
            if (!bDone_)
            {
                nSleep = SLEEP_MILLIS;
                if (!bSleep) nSleep = 5; // if not sleeping, sleep very small amount to avoid reving up CPU
                if (DEBUG_SLEEP) logger.debug("Sleeping " + nSleep);
                Utils.sleepMillis(nSleep);
            }
        }
    }

    /**
     * Cleanup
     */
    public void cleanup()
    {
        // stop thread
        bDone_ = true;

        // wait for alive to finish
        try
        {
            if (threadAlive_ != null)
            {
                threadAlive_.interrupt();
                threadAlive_.join();
            }
        }
        catch (InterruptedException ie)
        {
            Thread.interrupted();
        }

        // wait for TD to finish
        try
        {
            if (thread_ != null)
            {
                thread_.join();
            }
        }
        catch (InterruptedException ie)
        {
            Thread.interrupted();
        }

        // nullify
        thread_ = null;
        threadAlive_ = null;

        // clear us from OnlineManager
        if (mgr_ != null) mgr_.setTournamentDirector(null);

        // have engine restart to menu (and clear GameManager)
        context_.setGameManager(null);
        context_.restartNormal();
    }

    /**
     * Phase name
     */
    public String getPhaseName()
    {
        return PHASE_NAME;
    }

    /**
     * Return so other's can sync on saves.  We sync on this class
     * so that we don't save during a processTable()
     */
    public Object getSaveLockObject()
    {
        return this;
    }

    /**
     * Save the game (sync to prevent save twice @ same time)
     */
    void saveGame(String sDesc)
    {
        synchronized (getSaveLockObject())
        {
            if (DEBUG_SAVE) logger.debug("SAVING GAME: [" + sDesc + "]");
            game_.saveWriteGame();
        }
    }

    /**
     * return value from process
     */
    private class TDreturn implements PokerTableListener
    {
        private boolean bSave;
        private boolean bAutoSave;
        private boolean bSleep;
        private int nState;
        private int nPendingState;
        private boolean bRunOnClient;
        private boolean bAddAllHumans;
        private String sPhase;
        private DMTypedHashMap params;
        private DMArrayList<PokerTableEvent> events;
        private PokerTable table; // table we are listening to events
        private boolean bOnlySendToWaitList_;

        public void init()
        {
            setPhaseToRun(null);
            setSave(false);
            setAutoSave(false);
            setSleep(true);
            setTableState(-1);
            setPendingTableState(-1);
            setRunOnClient(false);
            setAddAllHumans(true);
            setOnlySendToWaitList(false);
            events = null;
            table = null;
        }

        public void finish()
        {
            if (table != null) table.removePokerTableListener(this, PokerTableEvent.TYPES_ALL);
        }

        public void setPhaseToRun(String s)
        {
            setPhaseToRun(s, null);
        }

        public void setPhaseToRun(String s, DMTypedHashMap p)
        {
            sPhase = s;
            params = p;
        }

        public String getPhaseToRun()
        {
            return sPhase;
        }

        public DMTypedHashMap getPhaseToRunParams()
        {
            return params;
        }

        public void setSleep(boolean b)
        {
            bSleep = b;
        }

        public boolean isSleep()
        {
            return bSleep;
        }

        public void setSave(boolean b)
        {
            bSave = b;
        }

        public boolean isSave()
        {
            return bSave;
        }

        public void setAutoSave(boolean b)
        {
            bAutoSave = b;
        }

        public boolean isAutoSave()
        {
            return bAutoSave;
        }

        public void setTableState(int n)
        {
            nState = n;
        }

        public int getTableState()
        {
            return nState;
        }

        public void setPendingTableState(int n)
        {
            nPendingState = n;
        }

        public int getPendingTableState()
        {
            return nPendingState;
        }

        public void setRunOnClient(boolean b)
        {
            bRunOnClient = b;
        }

        public boolean isRunOnClient()
        {
            return bRunOnClient;
        }

        public void setOnlySendToWaitList(boolean b)
        {
            bOnlySendToWaitList_ = b;
        }

        public boolean isOnlySendToWaitList()
        {
            return bOnlySendToWaitList_;
        }

        public void setAddAllHumans(boolean b)
        {
            bAddAllHumans = b;
        }

        public boolean isAddAllHumans()
        {
            return bAddAllHumans;
        }

        public DMArrayList<PokerTableEvent> getEvents()
        {
            return events;
        }

        public void startListening(PokerTable t)
        {
            table = t;
            table.addPokerTableListener(this, PokerTableEvent.TYPES_ALL);
        }

        public void tableEventOccurred(PokerTableEvent event)
        {
            if (events == null) events = new DMArrayList<PokerTableEvent>();
            if (DEBUG_EVENT) logger.debug("TDReturn event: " + event);
            events.add(event);
        }
    }

    // only need one instance since process() is synchronized
    private TDreturn ret_ = new TDreturn();

    // used to pause
    private boolean bPaused_ = false;
    private int nPauseCnt_ = 0;
    private long nPauseStart_;

    /**
     * pause the td (no need to synchronize since all we do is set a flag,
     * and the timing of when the TD checks it doesn't matter)
     */
    public void setPaused(boolean b)
    {
        if (b)
        {
            if (nPauseCnt_ == 0) nPauseStart_ = System.currentTimeMillis();
            nPauseCnt_++;
        }
        else
        {
            nPauseCnt_--;

            // if no longer paused, adjust time stamp on each table to
            // account for time asleep
            if (nPauseCnt_ == 0)
            {
                long asleep = System.currentTimeMillis() - nPauseStart_;
                for (int i = 0; i < game_.getNumTables(); i++)
                {
                    game_.getTable(i).adjustForPause(asleep);
                }
            }
        }

        bPaused_ = nPauseCnt_ > 0;
        //logger.debug((bPaused_? "TD paused":"TD unpaused")+" cnt: "+ nPauseCnt_);
    }

    /**
     * tournament director core logic.  Return true if action taken.
     */
    private synchronized boolean process()
    {
        // if we are paused, just return so we can take another nap
        if (bPaused_) return true;

        // process each table
        boolean bSave = false;
        boolean bSleep = true;
        boolean bAutoSave = false;

        // query getNumTables() each time since tables
        // can get removed when players bust out
        for (int i = 0; i < game_.getNumTables(); i++)
        {
            // process (init's ret_ in this call)
            processTable(game_.getTable(i));

            // handle return data
            bSave |= ret_.isSave(); // save if anybody wants to save
            bAutoSave |= ret_.isAutoSave(); // autosave if anybody wants to autosave
            bSleep &= ret_.isSleep(); // sleep only if everyone wants to sleep
        }

        // save at end if directed to
        if (bSave && bOnline_) saveGame("process");

        // auto save if directed to (only used in practice mode)
        if (bAutoSave && !bOnline_) game_.autoSave();

        return bSleep;
    }

    /**
     * process table, return true if some action taken
     */
    public synchronized void processTable(PokerTable table)
    {
        // check here since we can enter process table from places other than the above process()
        if (bPaused_) return;

        // init return data
        ret_.init();

        // skip if directed to pause
        if (table.getPause() > System.currentTimeMillis()) return;

        // skip all computer tables (handled by current table)
        // in practice mode this means that _processTable is only
        // ever called for the human table, which code run in any !bOnline_
        // blocks in the methods below are only run once where otherwise
        // it wouldn't make sense to them multiple times
        if (table.isAllComputer() && !table.isCurrent()) return;

        synchronized (table)
        {
            _processTable(table);

            // finish after processing (remove listeners)
            ret_.finish();

            // if we are to run on the client, we basically send the table in
            // its current state so that processTable can be run on the client
            // machine and whatever happens for that state happens on the client
            if (bHost_ && bOnline_ && ret_.isRunOnClient())
            {
                mgr_.sendTableUpdate(table, ret_.getEvents(), ret_.isOnlySendToWaitList(), true);
            }

            // run any phases (after send to client in case phase updates game state)
            if (table.isCurrent() && ret_.getPhaseToRun() != null)
            {
                if (DEBUG) logger.debug("Running " + ret_.getPhaseToRun());
                context_.processPhase(ret_.getPhaseToRun(), ret_.getPhaseToRunParams());
            }

            // set next state            
            if (ret_.getPendingTableState() != -1)
            {
                // only host cares about wait list
                if (bHost_ && ret_.isAddAllHumans())
                {
                    table.addWaitAllHumans();

                    // if host is eliminated or waiting, need to add
                    // them to wait list for proper display
                    if (table.isCurrent())
                    {
                        PokerPlayer host = game_.getHost();
                        // isObserver() also covers isWaiting() and isEliminated() with no seat (JDD, P2)
                        if (host.isObserver())
                        {
                            table.addWait(host);
                        }
                    }
                }
                table.setPendingTableState(ret_.getPendingTableState());
                // store phase/params for use on re-load from save
                table.setPendingPhase(ret_.getPhaseToRun());
                table.setPendingPhaseParams(ret_.getPhaseToRunParams());
                table.setTableState(PokerTable.STATE_PENDING);
            }
            else if (ret_.getTableState() != -1)
            {
                // special case - don't unset pending state if state specifically
                // set to pending (see STATE_PENDING_LOAD) 
                if (ret_.getTableState() != PokerTable.STATE_PENDING)
                {
                    table.setPendingTableState(PokerTable.STATE_NONE);
                    table.setPendingPhase(null);
                    table.setPendingPhaseParams(null);
                }
                table.setTableState(ret_.getTableState());
            }
        }
    }

    private void _processTable(PokerTable table)
    {
        // DEBUG 
        if (DEBUG && table.nDebugLast_ != table.getTableState())
        {
            String sPending = "";
            if (table.getPendingTableState() != PokerTable.STATE_NONE)
                sPending = ", pending to do " + PokerTable.getStringForState(table.getPendingTableState());
            logger.debug("=========> " + table.getName() + " at state " + table.toStringTableState() + sPending);
            table.nDebugLast_ = table.getTableState();
        }

        // handle rejoin
        if (checkRejoin(table)) return;

        int nNext;
        boolean bWait;
        HoldemHand hhand;
        switch (table.getTableState())
        {
            case PokerTable.STATE_PENDING_LOAD:
                ret_.setTableState(PokerTable.STATE_PENDING);

                // see if any player in wait list is locally controlled
                boolean bWaiting = false;
                int nNumWait = table.getWaitSize();
                PokerPlayer wait;
                for (int i = 0; i < nNumWait; i++)
                {
                    wait = table.getWaitPlayer(i);
                    if (wait.isLocallyControlled())
                    {
                        bWaiting = true;
                        break;
                    }
                }

                if (bWaiting)
                {
                    ret_.setPhaseToRun(table.getPendingPhase(), table.getPendingPhaseParams());
                }
                // Don't think we need to do this for clients since if
                // we are loading, we by definition don't have clients attached,
                // so when they re-attach, the normal re-join code will re-run
                // the pending phase.  If we did need to run for clients, we
                // would have to pass the pending list to processTable and
                // only send messages to players still on the list
                return;

            case PokerTable.STATE_PENDING:
                doPending(table);
                return;

            case PokerTable.STATE_ON_HOLD:
                if (table.getNumOccupiedSeats() > 1)
                {
                    ret_.setTableState(PokerTable.STATE_BEGIN);
                }
                return;

            case PokerTable.STATE_DEAL_FOR_BUTTON:
                dealForButton(table);
                ret_.setPendingTableState(PokerTable.STATE_BEGIN);
                return;

            case PokerTable.STATE_BEGIN:
                if (TESTING(PokerConstants.TESTING_FAST_SAVE))
                {
                    ((PokerContext) context_).setFastSaveTest(false);
                }

                if (isAutoDeal(table))
                {
                    ret_.setTableState(getTableStateStartDeal()); // use to keep in sync with doDeal()
                }
                else
                {
                    // start WaitForDeal phase
                    ret_.setPhaseToRun("TD.WaitForDeal");
                    ret_.setTableState(PokerTable.STATE_BEGIN_WAIT);
                }
                return;

            case PokerTable.STATE_BEGIN_WAIT:
                // state changed in doDeal(), below, when Deal pressed
                return;

            case PokerTable.STATE_CHECK_END_HAND:
                doCheckEndHand(table);
                ret_.setPendingTableState(PokerTable.STATE_CLEAN);
                return;

            case PokerTable.STATE_CLEAN:
                bWait = doClean(table);
                nNext = PokerTable.STATE_NEW_LEVEL_CHECK;
                if (table.getTableState() != PokerTable.STATE_ON_HOLD &&
                    table.getTableState() != PokerTable.STATE_GAME_OVER)
                {
                    if (bWait)
                    {
                        ret_.setPendingTableState(nNext);
                    }
                    else
                    {
                        ret_.setTableState(nNext);
                        ret_.setSleep(false);
                    }
                }
                return;

            case PokerTable.STATE_NEW_LEVEL_CHECK:
                bWait = doNewLevelCheck(table);
                if (bWait)
                {
                    ret_.setPhaseToRun("TD.NewLevelActions");
                    ret_.setRunOnClient(true);
                    ret_.setPendingTableState(PokerTable.STATE_COLOR_UP);
                }
                else
                {
                    ret_.setTableState(PokerTable.STATE_START_HAND);
                    ret_.setSleep(false);
                }

                return;

            case PokerTable.STATE_COLOR_UP:
                bWait = doColorUp(table);
                nNext = PokerTable.STATE_START_HAND;
                if (bWait)
                {
                    ret_.setPhaseToRun("TD.ColorUp");
                    ret_.setRunOnClient(true);
                    ret_.setPendingTableState(nNext);
                }
                else
                {
                    ret_.setTableState(nNext);
                    ret_.setSleep(false);
                }
                return;

            case PokerTable.STATE_START_HAND:
                // for tables which aren't current on host, we need
                // to do the colorup after the client has displayed it
                // to make sure the actual colorup is recorded on host.
                // note - the handling of this is kind of a special case
                // because we want the chip display to match what is
                // happening on the table - thus it is hard to do the
                // colorup all at once and have the client display the
                // colorup process after the fact
                if (bHost_ && table.isColoringUp())
                {
                    table.colorUp();
                    table.colorUpFinish();
                }

                // if current level is a break, don't start a hand
                if (game_.getProfile().isBreak(game_.getLevel()))
                {
                    doBreak(table);
                    ret_.setTableState(PokerTable.STATE_BREAK);
                    return;
                }

                doStart(table);
                ret_.setPendingTableState(PokerTable.STATE_BETTING);
                return;

            case PokerTable.STATE_BREAK:
                boolean bDone = doCheckEndBreak(table);
                if (bDone)
                {
                    ret_.setTableState(PokerTable.STATE_NEW_LEVEL_CHECK);
                }
                else
                {
                    if (bOnline_)
                    {
                        table.setPause(1000);
                    }
                }
                break;

            case PokerTable.STATE_BETTING:
                doBetting(table);
                if (!bOnline_) ret_.setSleep(false);
                return;

            case PokerTable.STATE_COMMUNITY:
                bWait = doCommunity(table);
                hhand = table.getHoldemHand();
                // wait means we have to display cards and then do round of betting
                if (bWait)
                {
                    ret_.setPendingTableState(PokerTable.STATE_BETTING);
                    if (table.isZipMode() || hhand.isDone()) ret_.setSleep(false);
                }
                // else hand is done, go to next community card or showdown
                else
                {
                    ret_.setTableState(hhand.getRound() == HoldemHand.ROUND_RIVER ?
                                       PokerTable.STATE_PRE_SHOWDOWN : PokerTable.STATE_COMMUNITY);
                    ret_.setSleep(false);
                }
                return;

            case PokerTable.STATE_PRE_SHOWDOWN:
                bWait = doPreShowdown(table);
                nNext = PokerTable.STATE_SHOWDOWN;
                if (bWait)
                {
                    ret_.setPendingTableState(nNext);
                }
                else
                {
                    ret_.setTableState(nNext);
                    ret_.setSleep(false);
                }
                return;

            case PokerTable.STATE_SHOWDOWN:
                doShowdown(table);
                ret_.setTableState(PokerTable.STATE_DONE);
                ret_.setAutoSave(true); // practice
                if (table.isCurrent()) ret_.setSave(true); // online
                ret_.setSleep(false);
                return;

            case PokerTable.STATE_DONE:
                if (isAutoDeal(table)) table.setPause(getAutoDealDelay(table));
                ret_.setTableState(PokerTable.STATE_BEGIN);
                return;

            case PokerTable.STATE_GAME_OVER:
                // mark game over & save
                if (!DebugConfig.isTestingOn() && !bOnline_) game_.autoSave();
                bDone_ = true;
        }
    }

    /**
     * Handle rejoining.  Return true if table is in middle
     * of rejoin and should not be processed.
     */
    private boolean checkRejoin(PokerTable table)
    {
        if (table.getRejoinState() == PokerTable.REJOIN_NONE) return false;

        // rejoin started ... need to wait for REJOIN_PROCESS to be set...
        if (table.getRejoinState() == PokerTable.REJOIN_START)
        {
            // ...but first see if we have waited to long for a rejoin
            if (System.currentTimeMillis() - table.getLastRejoinStateChangeTime() > REJOIN_TIMEOUT_MILLIS)
            {
                logger.info("Timeout waiting for rejoin on table " + table.getName() + "...");
                PokerPlayer player;
                for (int i = 0; i < PokerConstants.SEATS; i++)
                {
                    player = table.getPlayer(i);
                    if (player == null) continue;
                    if (player.isRejoining())
                    {
                        logger.info("   setRejoining(false) for " + player.getName());
                        player.setRejoining(false);
                        table.removeWait(player);
                    }
                }
                table.setRejoinState(PokerTable.REJOIN_NONE);
                return false;
            }
            return true;
        }


        // handle REJOIN_PROCESS for each rejoined player
        List<PokerPlayer> wait = table.getWaitList();
        PokerPlayer player;
        for (int i = wait.size() - 1; i >= 0; i--)
        {
            player = wait.get(i);
            if (player.isRejoining())
            {
                // when save happens, we set to use pending save logic
                // as used in getStateForSave(), which will cause the
                // correct state to be passed along and run on the client
                if (DEBUG_REJOIN) logger.debug("Sending rejoin table update to " + player.getName() +
                                               ", prev state: " + PokerTable.getStringForState(table.getPreviousTableState()));
                player.setRejoining(false);
                mgr_.sendTableUpdate(table, player, null, table.getPreviousTableState(), true, null, false, null, null, null);
            }
        }

        table.setRejoinState(PokerTable.REJOIN_NONE);
        return true;
    }

    /**
     * Handle save - take given state and return state that table should
     * be in upon load (handles case where save happened in a wait state)
     */
    public static int getStateForSave(GameState state, PokerTable table)
    {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        // if doing a full save, tweak the state saved to allow
        // proper reloading
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL)
        {
            switch (table.getTableState())
            {
                case PokerTable.STATE_BEGIN_WAIT:
                    return PokerTable.STATE_BEGIN;

                case PokerTable.STATE_PENDING:
                    return PokerTable.STATE_PENDING_LOAD;
            }
        }
        else if (pdetails.getOverrideState() != PokerSaveDetails.NO_OVERRIDE)
        {
            return pdetails.getOverrideState();
        }

        return table.getTableState();
    }

    /**
     * Handle table in pending state
     */
    private void doPending(PokerTable table)
    {
        // look to see if we have waited to long for
        // an action
        if (table.getWaitSize() > 0)
        {
            doPendingTimeoutCheck(table);
        }

        // wait list reduced by various phases when they call
        // removeFromWaitList(), below.  Next state initiated
        // when responses from all players received
        if (table.getWaitSize() == 0)
        {
            // we enter BEGIN state from PENDING only after dealing
            // high card for button.  If auto deal is on, we need
            // to put in the pause here (matches STATE_DONE handling below).
            if (!bClient_ &&
                table.getPendingTableState() == PokerTable.STATE_BEGIN &&
                isAutoDeal(table))
            {
                // Note - we don't do full pause for online since each client
                // has to respond to a dialog which causes a pause
                table.setPause(bOnline_ ? 1000 : getAutoDealDelay(table));
            }
            ret_.setTableState(table.getPendingTableState());
            ret_.setSleep(false);

            // This was set to true in my initial implementation because
            // we had UI stuff that triggered on change of state from
            // PENDING to BETTING.  I never liked that...and since I
            // changed when initPlayerIndex is called in HoldemHand,
            // that is no longer necessary, so we don't need this - JDD
            //ret_.setRunOnClient(true);
            return;
        }

        // if pending on betting, and if in zip mode or
        // waiting on computer, don't sleep.  In online games,
        // we still sleep so as not to overrun clients with AI actions
        if (table.getPendingTableState() == PokerTable.STATE_BETTING &&
            (table.isZipMode() || (!table.getWaitPlayer(0).isHumanControlled() && !bOnline_)))
        {
            ret_.setSleep(false);
        }
    }

    /**
     * check to see if a pending action has been waiting to long,
     * and if so, handle it
     */
    private void doPendingTimeoutCheck(PokerTable table)
    {
        // only do this in online games
        if (!bOnline_) return;

        int nLastState = table.getPreviousTableState();
        long wait = table.getMillisSinceLastStateChange();
        if (nLastState == PokerTable.STATE_BETTING)
        {
            doBettingTimeoutCheck(table, wait);
        }
        else
        {
            int nTimeout = (nLastState == PokerTable.STATE_NEW_LEVEL_CHECK) ? NEWLEVEL_TIMEOUT_MILLIS : NON_BETTING_TIMEOUT_MILLIS;
            if (wait > nTimeout)
            {
                logger.info("TIMEOUT " + PokerTable.getStringForState(nLastState) + ": " + Utils.toString(table.getWaitList()));
                sendCancel(table);
                table.removeWaitAll();
            }
        }
    }

    /**
     * betting timeout handling
     */
    private void doBettingTimeoutCheck(PokerTable table, long wait)
    {
        PokerPlayer player = table.getWaitPlayer();
        if (!player.isHumanControlled()) return;

        int nTimeoutSecs = game_.getProfile().getTimeoutSeconds();
        long nTimeout = nTimeoutSecs * 1000 + SLEEP_MILLIS; // pad time out a bit (allows 15 second message if timeout is 15)
        long nDiff = nTimeout - wait;
        int nThinkTank = player.getThinkBankMillis();
        int nThinkTankWhole = nThinkTank / 1000;

        if (nDiff > 0)
        {
            // set millis left for use in countdown timer
            player.setTimeoutMillis((int) nDiff);

            // send chat at 5 seconds to act
            int nWhole = getWholeSeconds(nDiff);
            if (nWhole > 0 && nWhole == 5 && nWhole != player.getTimeoutMessageSecondsLeft())
            {
                player.setTimeoutMessageSecondsLeft(nWhole);
                String sMsg = PropertyConfig.getMessage(nThinkTankWhole > 0 ? "msg.chat.timeout.tankleft" : "msg.chat.timeout.notank",
                                                        Utils.encodeHTML(player.getName()), getSeconds(nWhole), getSeconds(nThinkTankWhole));
                sendDealerChat(PokerConstants.CHAT_TIMEOUT, table, sMsg);
            }

            player.setThinkBankAccessed(0);
            return;
        }
        else
        {
            player.setTimeoutMillis(0);
        }

        if (nThinkTank > 0)
        {
            long nNow = System.currentTimeMillis();
            long nLastAccess = player.getThinkBankAccessed();

            int nWhole = getWholeSeconds(nThinkTank);
            // first time through, always display time left
            if (nLastAccess == 0)
            {
                nWhole = nThinkTankWhole;
                if (nWhole == 0) nWhole = 1;
            }

            // send chat at [starting] value and 5
            if (nWhole > 0 && (nLastAccess == 0 || nWhole == 5) && nWhole != player.getTimeoutMessageSecondsLeft())
            {
                player.setTimeoutMessageSecondsLeft(nWhole);
                String sMsg = PropertyConfig.getMessage("msg.chat.timeout.tank",
                                                        Utils.encodeHTML(player.getName()), getSeconds(nWhole));
                sendDealerChat(PokerConstants.CHAT_TIMEOUT, table, sMsg);
            }

            if (nLastAccess == 0) nLastAccess = nNow - 1; // first time though, subtract 1 millisecond
            int nNewThink = nThinkTank - (int) (nNow - nLastAccess);
            if (nNewThink < 0) nNewThink = 0;
            player.setThinkBankMillis(nNewThink);
            player.setThinkBankAccessed(nNow);

            return;
        }

        // cancel player
        sendCancel(table);

        // set player as sitting out now (this is sent in an update when HandAction is sent to all players)
        player.setSittingOut(true);

        // no more time - fold
        HandAction fold = new HandAction(player, table.getHoldemHand().getRound(), HandAction.ACTION_FOLD, 0, HandAction.FOLD_FORCED, "timeout");
        doHandAction(fold, true, false, false);

        // log it
        logger.info("TIMEOUT  betting: " + player);
    }

    /**
     * Send cancel message to all players on wait list
     */
    private void sendCancel(PokerTable table)
    {
        PokerPlayer player;
        int nNum = table.getWaitSize();
        for (int i = 0; i < nNum; i++)
        {
            player = table.getWaitPlayer(i);
            mgr_.doCancelAction(player);
        }
    }

    /**
     * Get seconds in a string (plurality considered)
     */
    private String getSeconds(int n)
    {
        return PropertyConfig.getMessage(n == 1 ? "msg.seconds.singular" : "msg.seconds.plural", n);
    }


    /**
     * Get number of seconds represented by given millis - special use
     * method. If the number of millis is within the TD's sleep time
     * of a round number, that round number is returned.  Otherwise, -1 is returned.
     * For example, if millis is 10000 to 10249, then 10 is returned (assuming 250 millis sleep).
     */
    private int getWholeSeconds(long nMillis)
    {
        int nRemainder = (int) (nMillis % 1000);
        if (nRemainder < SLEEP_MILLIS)
        {
            return (int) nMillis / 1000;
        }
        return -1;
    }

    /**
     * deal high card for button
     */
    private void dealForButton(PokerTable table)
    {
        if (bHost_)
        {
            // deal cards to assign button
            table.setButton();

            // do on all-ai tables
            if (table.isCurrent()) doDealForButtonAllComputers();
        }

        // start clock when deal for button
        if (bOnline_)
        {
            game_.getGameClock().start();
        }

        ret_.setPhaseToRun("TD.DealDisplayHigh");
        ret_.setRunOnClient(true);
    }

    /**
     * initialize button on all computer tables
     */
    private void doDealForButtonAllComputers()
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent())
            {
                table.setButton();
            }
        }
    }

    /**
     * check end of hand
     */
    private void doCheckEndHand(PokerTable table)
    {
        if (bHost_)
        {
            table.aiRebuy(); // note: keep in sync with doCheckHandAllComputers

            if (table.isCurrent()) doCheckEndHandAllComputers();

            // add pending rebuys
            table.addPendingRebuys();

            // boot players
            bootPlayers(table);

            // if clock expired, go to next level
            if (game_.isLevelExpired())
            {
                game_.nextLevel();
            }
        }

        // make sure clock is running (could be off after level change)
        // this is an inexpensive call, so okay to do for each hand
        if (bOnline_)
        {
            game_.getGameClock().start();
        }

        ret_.setPhaseToRun("TD.CheckEndHand");
        ret_.setRunOnClient(true);
    }

    /**
     * initial all computer tables for betting
     */
    private void doCheckEndHandAllComputers()
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent())
            {
                table.aiRebuy();
            }
        }
    }

    /**
     * boot player logic
     */
    private void bootPlayers(PokerTable table)
    {
        TournamentProfile profile = game_.getProfile();

        PokerPlayer player;
        boolean bBooted;
        String sMsg = null;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = table.getPlayer(i);
            if (player == null) continue;
            bBooted = false;

            // determine if booted
            if (profile.isBootDisconnect() && player.getHandsPlayedDisconnected() >= profile.getBootDisconnectCount())
            {
                bBooted = true;
                sMsg = PropertyConfig.getMessage("msg.chat.boot.disconnect",
                                                 Utils.encodeHTML(player.getName()),
                                                 player.getHandsPlayedDisconnected());

            }
            else if (profile.isBootSitout() && player.getHandsPlayedSitout() >= profile.getBootSitoutCount())
            {
                bBooted = true;
                sMsg = PropertyConfig.getMessage("msg.chat.boot.sitout",
                                                 Utils.encodeHTML(player.getName()),
                                                 player.getHandsPlayedSitout());
            }

            // process booted players
            if (bBooted)
            {
                player.setBooted(true);
                int nChip = player.getChipCount();
                player.setChipCount(0);
                game_.addExtraChips(-nChip);
                sendDirectorChat(sMsg, Boolean.FALSE);
            }
        }
    }

    /**
     * check end of break, return true if break over
     */
    private boolean doCheckEndBreak(PokerTable table)
    {
        boolean bEndOfBreak = false;

        if (bHost_)
        {
            // online - move clock
            if (!bOnline_)
            {
                game_.advanceClockBreak();
            }

            // if clock expired, go to next level
            if (game_.isLevelExpired())
            {
                game_.nextLevel();
            }

            // for all passes through this, if the
            // game level changed we need to proceed
            // again
            if (game_.getLevel() != table.getLevel())
            {
                bEndOfBreak = true;
            }
        }

        // make sure clock is running (could be off after level change)
        // this is an inexpensive call, so okay to do for each hand
        if (bEndOfBreak && bOnline_)
        {
            game_.getGameClock().start();
        }

        // no need to run anything on client since all we do is transition
        // out of waiting for break to end

        return bEndOfBreak;
    }

    /**
     * during clean, listen to all tables and record which tables,
     * players and observers were impacted.
     */
    private class TDClean implements PokerTableListener
    {
        List<PokerTable> tables;
        List<PokerTable> tablesRemoved = new ArrayList<PokerTable>(1);
        List<PokerPlayer> playersTouched = new ArrayList<PokerPlayer>(10);
        List<PokerTable> tablesTouched = new ArrayList<PokerTable>(3);
        List<PokerPlayer> playersBusted = new ArrayList<PokerPlayer>(3);
        List<PokerPlayer> playersWaiting = new ArrayList<PokerPlayer>(1);

        public TDClean(PokerTable active)
        {
            tables = new ArrayList<PokerTable>(game_.getTables());
            listen(true);

            // need to make sure players on current table are included
            PokerPlayer player;
            addTableTouched(active);
            for (int i = 0; i < PokerConstants.SEATS; i++)
            {
                player = active.getPlayer(i);
                if (player == null) continue;
                addPlayerTouched(player);
            }

        }

        public void finish()
        {
            listen(false);
        }

        private void listen(boolean bAdd)
        {
            int nTypes = PokerTableEvent.TYPES_PLAYERS_CHANGED |
                         PokerTableEvent.TYPES_OBSERVERS_CHANGED |
                         PokerTableEvent.TYPE_TABLE_REMOVED;

            PokerTable table;
            int nNum = tables.size();
            for (int i = 0; i < nNum; i++)
            {
                table = tables.get(i);
                if (bAdd)
                {
                    table.addPokerTableListener(this, nTypes);
                }
                else
                {
                    table.removePokerTableListener(this, nTypes);
                }
            }
        }

        private void addTableTouched(PokerTable table)
        {
            if (!tablesTouched.contains(table)) tablesTouched.add(table);
        }

        private void addTableRemoved(PokerTable table)
        {
            if (!tablesRemoved.contains(table)) tablesRemoved.add(table);
        }

        private void addPlayerTouched(PokerPlayer player)
        {
            if (!playersTouched.contains(player)) playersTouched.add(player);
        }

        public void tableEventOccurred(PokerTableEvent event)
        {
            if (DEBUG_CLEANUP_TABLE) logger.debug("TDClean event: " + event);
            PokerTable table = event.getTable();
            PokerPlayer player = event.getPlayer();
            int type = event.getType();
            switch (type)
            {
                case PokerTableEvent.TYPE_PLAYER_ADDED:
                case PokerTableEvent.TYPE_PLAYER_REMOVED:
                case PokerTableEvent.TYPE_OBSERVER_ADDED:
                case PokerTableEvent.TYPE_OBSERVER_REMOVED:
                    addTableTouched(table);
                    addPlayerTouched(player);
                    break;

                case PokerTableEvent.TYPE_TABLE_REMOVED:
                    addTableRemoved(table);
                    break;
            }

            // if player removed, they are busted (unless they are marked as waiting)
            if (type == PokerTableEvent.TYPE_PLAYER_REMOVED)
            {
                if (player.isWaiting())
                {
                    if (!playersWaiting.contains(player)) playersWaiting.add(player);
                }
                else
                {
                    if (!playersBusted.contains(player)) playersBusted.add(player);
                }
            }
            // however if they were added, then they were just moved
            if (type == PokerTableEvent.TYPE_PLAYER_ADDED)
            {
                if (playersBusted.contains(player)) playersBusted.remove(player);
                if (playersWaiting.contains(player)) playersWaiting.remove(player);
            }
        }

        public void debugPrint()
        {
            logger.debug("TDClean results ------------------------");
            printTableList("Tables Removed", tablesRemoved);
            printTableList("Tables touched", tablesTouched);
            printPlayerList("Touched", playersTouched);
            printPlayerList("Busted", playersBusted);
            printPlayerList("Waiting", playersWaiting);
        }

        private void printPlayerList(String sName, List<PokerPlayer> players)
        {
            logger.debug("  " + sName + ":");
            for (PokerPlayer player : players)
            {
                logger.debug("    => " + player.getName());
            }
        }

        private void printTableList(String sName, List<PokerTable> printtables)
        {
            logger.debug(sName + ":");
            for (PokerTable table : printtables)
            {
                logger.debug("    => " + table.getName());
            }
        }

    }

    /**
     * clean tables, return true if need to wait for human action
     */
    private boolean doClean(PokerTable table)
    {
        if (bHost_)
        {
            boolean bOneLeft = game_.isOnePlayerLeft();

            // gather events
            TDClean cleanEvents = new TDClean(table);

            // clean table(s)
            cleanTables(table, !bOneLeft);

            // consolidate table(s)
            List<PokerTable> tables;
            if (bOnline_)
            {
                // Online - see if we can break this table.
                tables = new ArrayList<PokerTable>();
                tables.add(table);

                // if current table (on host), add all computer
                // tables to list for possible consolidation
                if (table.isCurrent())
                {
                    int nNumTables = game_.getNumTables();
                    PokerTable aitable;
                    for (int i = 0; i < nNumTables; i++)
                    {
                        aitable = game_.getTable(i);
                        if (aitable.isAllComputer() && aitable != table)
                        {
                            tables.add(aitable);
                        }
                    }
                }
            }
            else
            {
                // Practice - do all tables
                tables = game_.getTables();
            }
            OtherTables.consolidateTables(game_, tables);

            // safety check - can't have added players in addition to being removed
            ApplicationError.assertTrue(!(table.getAddedList().size() > 0 && table.isRemoved()),
                                        "Table removed but has players added", table);

            // if table was removed, we need to move observers
            PokerTable newtable;
            if (table.isRemoved() && table.getNumObservers() > 0)
            {
                newtable = getNewTable(table);
                moveObservers(table, newtable);
            }

            // if players were waited listed, need to make them observers
            PokerPlayer player;
            newtable = getNewTable(table);
            for (int i = 0; i < cleanEvents.playersWaiting.size(); i++)
            {
                player = cleanEvents.playersWaiting.get(i);
                if (player.isComputer()) continue;

                // add player as an observer
                game_.addObserver(player);
                newtable.addObserver(player);

                if (DEBUG_CLEANUP_TABLE)
                {
                    logger.debug(player.getName() + " waited, added to table as observer: " + newtable.getName());
                }
            }

            // check to see if game over
            // if only one player has chips, game is done!
            if (bOneLeft)
            {
                PokerTable table1 = game_.getTable(0);
                player = null;

                for (int i1 = 0; i1 < PokerConstants.SEATS; i1++)
                {
                    player = table1.getPlayer(i1);
                    if (player == null || player.getChipCount() == 0) continue;

                    break;
                }

                game_.playerOut(player); // give chips to last player

                // chat winner (online only)
                if (bOnline_)
                {
                    sendDirectorChat(PropertyConfig.getMessage("msg.chat.finish.win",
                                                               Utils.encodeHTML(player.getDisplayName(bOnline_)),
                                                               player.getPrize()), null);
                }

                setGameOver();
            }

            if (DEBUG_CLEANUP_TABLE) cleanEvents.debugPrint();

            // instead of doing normal processing of sending
            // table updates, we handle it ourselves
            notifyPlayersCleanDone(table, cleanEvents);

            // cleanup
            cleanEvents.finish();

            // if players were added to this table, display it and wait
            // do this only in practice mode.  In online, just send chats
            List<PokerPlayer> list = table.getAddedList();
            int nNum = list.size();
            if (nNum > 0)
            {
                if (!bOnline_)
                {
                    ret_.setPhaseToRun("TD.DisplayTableMoves");
                    return true;
                }
                else
                {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < nNum; i++)
                    {
                        sb.append(Utils.encodeHTML(list.get(i).getDisplayName(bOnline_)));
                        if (i < (nNum - 1)) sb.append(", ");
                    }
                    sendDealerChat(PokerConstants.CHAT_1, table, PokerUtils.chatInformation(
                            PropertyConfig.getMessage(
                                    nNum == 1 ? "msg.chat.moved.singular" : "msg.chat.moved.plural",
                                    nNum,
                                    sb.toString())));
                }
            }
        }

        return false;
    }

    /**
     * this version is called from cleanup - it sends cleaning done event and causes
     * process table to be run for clients of given table
     */
    private void notifyPlayersCleanDone(PokerTable table, TDClean clean)
    {
        // clean done, fire event so UI can do appropriate cleanup
        if (!game_.isGameOver())
        {
            ret_.startListening(table); // start listening here to skip player add/remove events
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_CLEANING_DONE, table));
        }

        notifyPlayers(table, clean, true);

        // cleanup since we handled it here
        ret_.init();
    }

    /**
     * Notify all players of changes due to cleanup.  In the simple case, this updates all
     * players of the results of the previous hand.
     */
    private void notifyPlayers(PokerTable table, TDClean clean, boolean bCleanDoneLogic)
    {
        PokerPlayer player;

        // get list of all players and observers
        List<PokerPlayer> allPlayer = game_.getPokerPlayersCopy();
        int nNum = game_.getNumObservers();
        for (int i = 0; i < nNum; i++)
        {
            player = game_.getPokerObserverAt(i);
            if (!allPlayer.contains(player))
            {
                allPlayer.add(player);
            }
        }

        // send each a message
        String sPhase;
        boolean bSetCurrentTable;
        boolean bRunProcessTable;
        nNum = allPlayer.size();
        DMArrayList<PokerTableEvent> events;
        for (int i = 0; i < nNum; i++)
        {
            sPhase = null;
            bSetCurrentTable = false;
            bRunProcessTable = false;
            events = null;

            player = allPlayer.get(i);
            if (player.isComputer() || player.isRejoining()) continue;

            if (clean.playersBusted.contains(player) || game_.isGameOver())
            {
                // only run phase if online or (if not online) if game over
                // we do this because if not online and game not over, the
                // only reason this code is run is if the human is watching the
                // AI players finish out the game and we have already shown
                // them the GameOver dialog
                if (bCleanDoneLogic && (bOnline_ || game_.isGameOver()))
                {
                    sPhase = bOnline_ ? "OnlineGameOver" : "GameOver";
                }
                bSetCurrentTable = true;
            }

            // update current if player moved or player is observer.
            // if the table is the same, this is a noop on the client.
            if (clean.playersTouched.contains(player))
            {
                bSetCurrentTable = true;
            }

            // if current table is still alive, need to run process table and send events
            if (bCleanDoneLogic && player.getTable() == table)
            {
                bRunProcessTable = true;
                events = ret_.getEvents();
            }

            // set current table for host
            if (player.isHost() && bSetCurrentTable)
            {
                PokerTable current = player.getTable();
                game_.setCurrentTable(current);

                // in practice, if moved to new table, need to start deal automatically
                // so that user doesn't have to press 'deal' twice
                // TODO: do this online if allow dealer-controlled dealing
                // TODO: do this when TESTING_ONLINE_AUTO_DEAL_OFF on
                if (bCleanDoneLogic && !bOnline_ && current.getTableState() == PokerTable.STATE_DONE)
                {
                    current.setTableState(getTableStateStartDeal());
                }
            }

            // run phase locally
            if (bCleanDoneLogic && player.isHost() && sPhase != null)
            {
                context_.processPhase(sPhase);
            }

            // send update
            if (bOnline_)
            {
                mgr_.sendTableUpdate(table, player, events,
                                     PokerSaveDetails.NO_OVERRIDE, bRunProcessTable,
                                     sPhase, bSetCurrentTable,
                                     clean.tablesTouched, clean.playersTouched, clean.tablesRemoved);
            }
        }
    }

    /**
     * Clean given table, and if that table is the current table,
     * also clean all computer tables.  If bRemovePlayers is false,
     * then players are not removed from the table (they are still
     * processed for how they finish.  Leaving them at the table
     * is needed so the display still shows them).
     */
    public void cleanTables(PokerTable table, boolean bRemovePlayers)
    {
        List<PokerPlayer> removed = new ArrayList<PokerPlayer>();

        // clean tables (storing removed players in array)
        cleanTable(table, removed, bRemovePlayers);
        if (table.isCurrent()) doCleanAllComputers(removed);

        // record placement of all players removed
        OtherTables.recordPlayerPlacement(this, game_, removed);
    }

    /**
     * do clean of all computer tables
     */
    private void doCleanAllComputers(List<PokerPlayer> removed)
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent())
            {
                cleanTable(table, removed, true);
            }
        }
    }

    /**
     * Clean the given table.
     */
    private void cleanTable(PokerTable table, List<PokerPlayer> removed, boolean bRemovePlayers)
    {
        List<PokerPlayer> removedThisTable = new ArrayList<PokerPlayer>();
        boolean bAllComputerPrior = table.isAllComputer() && table.getNumObservers() == 0;

        // remove any left-over ai (if table used to have humans and the human was moved
        // away, it could have some AI left)
        if (bAllComputerPrior)
        {
            table.verifyAllAIRemoved();
        }

        // clean table
        OtherTables.cleanTable(table, removedThisTable, bRemovePlayers);

        // send message to removed players
        if (bRemovePlayers && !bAllComputerPrior)
        {
            processRemovedPlayers(table, removedThisTable);
        }

        removed.addAll(removedThisTable);
    }

    /**
     * Make busted out players observers and move observers from busted table.
     */
    private void processRemovedPlayers(PokerTable table, List<PokerPlayer> removed)
    {
        ApplicationError.assertTrue(bHost_, "Can only run processRemovedPlayers() on host");

        PokerTable newtable = table;

        // if table is now all computer players, change
        // the table to observe to a different table
        if (table.isAllComputer())
        {
            newtable = getNewTable(table);
        }

        // busted
        PokerPlayer player;
        int nNum = removed.size();
        for (int i = 0; i < nNum; i++)
        {
            player = removed.get(i);
            if (player.isComputer()) continue;

            // add player as an observer
            game_.addObserver(player);
            newtable.addObserver(player);

            if (DEBUG_CLEANUP_TABLE)
            {
                logger.debug(player.getName() + " eliminated, added to table as observer: " +
                             newtable.getName());
            }
        }

        // move observers if table changed
        if (table.getNumObservers() > 0 && newtable != table)
        {
            moveObservers(table, newtable);
        }
    }

    /**
     * Get a new table to replace this table for observing.
     */
    private PokerTable getNewTable(PokerTable table)
    {
        PokerTable oldtable = table;
        PokerTable newtable = game_.getHost().getTable();
        PokerTable backup = null;
        PokerTable lookat;

        // if host has no table, then host was just removed,
        // so look for a table with humans
        if (newtable == null || newtable.isRemoved())
        {
            newtable = null;
            int nNum = game_.getNumTables();
            for (int i = 0; i < nNum; i++)
            {
                lookat = game_.getTable(i);
                if (!lookat.isAllComputer())
                {
                    newtable = lookat;
                    break;
                }
                else if (backup == null)
                {
                    backup = lookat;
                }
            }
        }

        // if we found a new table to set as current,
        // use it.  Otherwise default to the all-ai
        // table that was passed in (unless that
        // was removed, then go to the backup table - some
        // other all-ai table)
        if (newtable != null) table = newtable;
        else if (table.isRemoved()) table = backup;

        ApplicationError.assertNotNull(table, "No new table given existing table", oldtable);
        return table;
    }

    /**
     * Move observers from the given table to the new one
     */
    private void moveObservers(PokerTable from, PokerTable to)
    {
        ApplicationError.assertTrue(bHost_, "Can only run moveObservers() on host");

        int nNum;
        PokerPlayer player;
        nNum = from.getNumObservers();
        for (int i = 0; i < nNum; i++)
        {
            player = from.getObserver(0); // get first in list since we are reducing size of list as we go
            from.removeObserver(player);
            to.addObserver(player);

            if (DEBUG_CLEANUP_TABLE)
            {
                logger.debug(player.getName() + " observer moved from " + from.getName() +
                             " to " + to.getName());
            }
        }
    }

    /**
     * do new level check, return whether need to wait
     */
    private boolean doNewLevelCheck(PokerTable table)
    {
        if (game_.getLevel() != table.getLevel())
        {
            // this is run after normal levels and after breaks
            if (bHost_)
            {
                table.aiRebuy(); // keep in sync with doLevelCheckAllComputers
                table.aiAddOn();

                if (table.isCurrent()) doLevelCheckAllComputers();
            }
            return true;
        }
        else
        {
            // clear any rebuy from CheckEndHand so
            // it is not redisplayed
            table.getRebuyList().clear();
            return false;
        }
    }

    /**
     * initial all computer tables for betting
     */
    private void doLevelCheckAllComputers()
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent())
            {
                table.aiRebuy();
                table.aiAddOn();
            }
        }
    }

    /**
     * do colorup determination
     */
    private boolean doColorUp(PokerTable table)
    {
        // this is run after normal levels and after breaks
        // (color ups should not occur after breaks since
        // we skip break levels in PokerGame.getNextMinChipIndex()
        // and would have colored up at end of phase prior to
        // break).
        if (bHost_)
        {
            boolean bColorup = false;
            int nMinNow = game_.getLastMinChip();
            int nMinNext = game_.getMinChip();
            if (nMinNext > nMinNow)
            {
                // colorup determination, sets isColorUp on table if
                // colorup needed
                bColorup = true;
                table.setNextMinChip(nMinNext);
                table.doColorUpDetermination();
                // table.colorUp() done in ColorUpFinish phase
                // table.colorUpFinish() done in ColorUpFinish phase
            }

            if (table.isCurrent() && bColorup) doColorUpAllComputers(nMinNext);
        }

        return table.isColoringUp();
    }

    /**
     * initial all computer tables for betting
     */
    private void doColorUpAllComputers(int nMinNext)
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent())
            {
                table.setNextMinChip(nMinNext);
                table.doColorUpDetermination();
                if (table.isColoringUp())
                {
                    table.colorUp();
                    table.colorUpFinish();
                }
            }
        }
    }

    /**
     * Start break at table
     */
    private void doBreak(PokerTable table)
    {
        if (bHost_)
        {
            // record events to send to client
            ret_.startListening(table);

            // advance level so table is now at a break level
            table.startBreak();

            // if current table, process all-computer tables and set them
            // ready to do a betting sequence
            if (table.isCurrent()) doBreakAllComputers();
        }

        ret_.setRunOnClient(true);
    }

    /**
     * start break on all computer tables
     */
    private void doBreakAllComputers()
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);

            if (table.isAllComputer() && !table.isCurrent())
            {
                table.startBreak();
            }
        }
    }

    /**
     * Start hand at table
     */
    private void doStart(PokerTable table)
    {
        if (bHost_)
        {
            // record events to send to client
            ret_.startListening(table);

            // start new hand
            table.startNewHand();

            // if current table, process all-computer tables and set them
            // ready to do a betting sequence
            if (table.isCurrent()) doStartAllComputers();

            // practice - move clock
            if (!bOnline_)
            {
                game_.advanceClock(); // Action 1 of 5 (see PokerGame)
            }
        }

        ret_.setPhaseToRun("TD.DealDisplayHand");
        ret_.setRunOnClient(true);
    }

    /**
     * initialize all computer tables for betting
     */
    private void doStartAllComputers()
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);

            if (table.isAllComputer() && !table.isCurrent())
            {
                // if not required to be on hold anymore, change state
                if (table.getTableState() == PokerTable.STATE_ON_HOLD &&
                    table.getNumOccupiedSeats() > 1)
                {
                    table.setTableState(PokerTable.STATE_DONE);
                }

                // don't process on-hold tables
                if (table.getTableState() != PokerTable.STATE_ON_HOLD)
                {
                    table.setTableState(PokerTable.STATE_BETTING);
                }
            }
        }
    }

    /**
     * do betting
     */
    private void doBetting(PokerTable table)
    {
        int nNext = PokerTable.STATE_BETTING;

        // nothing to do, hand is done
        HoldemHand hhand = table.getHoldemHand();
        if (hhand.isDone())
        {
            ret_.setTableState(nextBettingState(hhand));
        }
        // otherwise keep betting until done
        else
        {
            // Use getCurrentPlayerSpecial, which inits
            // the playerOrder list on the first round of
            // betting. This was moved here from
            // HoldemHand.deal() so that the current player
            // isn't highlighted during the dealing of cards
            PokerPlayer current = hhand.getCurrentPlayerInitIndex();

            // reset timeout message indicator
            if (bHost_)
            {
                current.setTimeoutMillis(game_.getProfile().getTimeoutSeconds() * 1000);
                current.setTimeoutMessageSecondsLeft(0);
            }

            // if player sitting out, fold
            if (current.isSittingOut() || (bOnline_ && PokerUtils.isDemoOver(context_, current, true)))
            {
                HandAction fold = new HandAction(current, table.getHoldemHand().getRound(), HandAction.ACTION_FOLD, 0, HandAction.FOLD_SITTING_OUT, "sittingout");
                doHandAction(fold, false, false, false);
                ret_.setTableState(nNext);
                current.setSittingOut(true); // make sure it is set, for case of demo user being done
                table.setPause(SLEEP_MILLIS + 100);
            }
            // local player, either host or ai
            else if (current.isLocallyControlled())
            {
                // player on UI table - use Bet phase
                if (table.isCurrent())
                {
                    if (bHost_) table.addWait(current); // avoids warning message on client
                    ret_.setPhaseToRun("TD.Bet");
                    ret_.setAddAllHumans(false);
                    ret_.setPendingTableState(nNext);

                    // pause done in Bet phase (so pause is before action)
                }
                // computer player on other table, handle here
                else
                {
                    if (!current.isComputer())
                    {
                        ApplicationError.assertTrue(current.isComputer(),
                                                    table.getName() + ": locally controlled player on non-current table is not computer",
                                                    current + " current table: " + game_.getCurrentTable());
                    }
                    HandAction action = current.getAction(false);
                    doHandAction(action, false, false, false);
                    ret_.setTableState(nNext);

                    // this isn't an all-ai table, so we should pause a bit
                    // so as not to overload clients with too many messages
                    table.setPause(AI_PAUSE_TENTHS * 100);
                }
            }
            // remote player
            else if (bHost_)
            {
                table.addWait(current);
                ret_.setRunOnClient(true);
                ret_.setOnlySendToWaitList(true);
                ret_.setAddAllHumans(false);
                ret_.setPendingTableState(nNext);
            }
        }

        // if current table, process all-computer tables and have them do betting
        // we do this to sync tables hand for hand with the human table (practice)
        // or the host's current table (online, for all-ai tables)
        if (bHost_ && table.isCurrent()) doBettingAllComputer();
    }

    /**
     * all computer betting
     */
    private void doBettingAllComputer()
    {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent())
            {
                // shortcut for subsequent calls through here after we
                // have already bet (so this isn't done over and over)
                if (table.getTableState() != PokerTable.STATE_BETTING) return;

                // do quick AI bet
                table.simulateHand();

                // set new state
                table.setTableState(PokerTable.STATE_DONE);
            }
        }
    }

    /**
     * figure out new state
     */
    private int nextBettingState(HoldemHand hhand)
    {
        boolean bDone = hhand.isDone();
        if (!bDone) return PokerTable.STATE_BETTING;

        int nRound = hhand.getRound();
        if (nRound == HoldemHand.ROUND_RIVER) return PokerTable.STATE_PRE_SHOWDOWN;

        return PokerTable.STATE_COMMUNITY;
    }

    /**
     * community card
     */
    private boolean doCommunity(PokerTable table)
    {
        HoldemHand hhand = table.getHoldemHand();
        if (bHost_)
        {
            // record events to send to client
            ret_.startListening(table);

            // flop, turn and river cards
            hhand.advanceRound();

            if (!bOnline_)
            {
                game_.advanceClock(); // Action 2, 3, 4 of 5 (flop, turn, river)
            }
        }

        // only run DealCommunity phase in practice mode (so all CardPieces are created for cheat purposes)
        // or in online mode when there are still players left in hand
        if (!bOnline_ ||
            (bOnline_ && hhand.getNumWithCards() > 1))
        {
            ret_.setPhaseToRun("TD.DealCommunity");
            ret_.setRunOnClient(true);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * pre-showdown
     */
    private boolean doPreShowdown(PokerTable table)
    {
        HoldemHand hhand = table.getHoldemHand();

        // host does pre-resolve
        if (bHost_) hhand.preResolve(bOnline_);

        // client clear wait list since hosts sends it over
        // and this avoid warning message (clients dont use wait list anyhow)
        if (!bHost_) table.removeWaitAll();

        // online games, figure out if we need to run pre-showdown step
        if (bOnline_)
        {
            List<PokerPlayer> winners = hhand.getPreWinners();
            List<PokerPlayer> losers = hhand.getPreLosers();

            DMArrayList<Integer> win_ids = null;
            PokerPlayer local = game_.getLocalPlayer();
            PokerPlayer p;
            boolean bLocalInList = false;
            for (int i = winners.size() - 1; i >= 0; i--)
            {
                p = winners.get(i);
                if (p.isHuman() && p.isAskShowWinning() && hhand.isUncontested())
                {
                    table.addWait(p);
                    if (win_ids == null) win_ids = new DMArrayList<Integer>();
                    win_ids.add(p.getID());
                    if (p == local) bLocalInList = true;
                }
            }
            for (int i = losers.size() - 1; i >= 0; i--)
            {
                p = losers.get(i);
                if (p.isHuman() && p.isAskShowLosing())
                {
                    table.addWait(p);
                    if (p == local) bLocalInList = true;
                }
            }

            if (table.getWaitSize() > 0)
            {
                // only run pre-showdown phase if local player is in the list
                if (bLocalInList)
                {
                    DMTypedHashMap params = new DMTypedHashMap();
                    if (win_ids != null) params.setObject(PreShowdown.PARAM_WINNERS, win_ids);
                    ret_.setPhaseToRun("TD.PreShowdown", params);
                }

                // run this phase only on clients in the wait list
                ret_.setRunOnClient(true);
                ret_.setAddAllHumans(false);
                ret_.setOnlySendToWaitList(true);

                return true;
            }
        }

        return false;
    }

    /**
     * showdown
     */
    private void doShowdown(PokerTable table)
    {
        HoldemHand hhand = table.getHoldemHand();

        // BUG 462 - don't re-run logic if already run (safety check)
        if (bHost_ && hhand.getRound() != HoldemHand.ROUND_SHOWDOWN)
        {
            // record events to send to client
            ret_.startListening(table);

            // showdown
            hhand.advanceRound();

            // unset zip mode so end hand event is
            // called outside zip mode
            table.setZipMode(false);

            // resolve
            hhand.resolve();

            if (!bOnline_)
            {
                game_.advanceClock(); // Action 5 of 5
            }
        }

        // store hand history - called here so
        // it happens on client and host
        if (!game_.getLocalPlayer().isObserver() || bHost_) hhand.storeHandHistory();

        ret_.setPhaseToRun("TD.Showdown");
        ret_.setRunOnClient(true);
    }

    /**
     * auto deal this table?
     */
    private boolean isAutoDeal(PokerTable table)
    {
        return (!bOnline_ && engine_.getPrefsNode().getBoolean(PokerConstants.OPTION_AUTODEAL, false)) ||
               TESTING(PokerConstants.TESTING_AUTOPILOT) ||
               (bOnline_ && !(TESTING(PokerConstants.TESTING_ONLINE_AUTO_DEAL_OFF) && table.isCurrent())) ||

               // this check does auto deal when game
               // is over or potentially over (rebuy/never go broke needed
               // to continue).  This pops up either a rebuy, tournament
               // over or never go broke dialog.
               (!bOnline_ && CheckEndHand.isGameOver(game_, false, this));
    }

    /**
     * Get auto deal delay
     *
     * @param table
     */
    private int getAutoDealDelay(PokerTable table)
    {
        int nDelay;

        if (!bOnline_ && CheckEndHand.isGameOver(game_, false, this))
        {
            nDelay = 0;
        }
        else if (TESTING(PokerConstants.TESTING_AUTOPILOT))
        {
            nDelay = 250;
        }
        else if (bOnline_)
        {
            nDelay = engine_.getPrefsNode().getInt(PokerConstants.OPTION_AUTODEALONLINE, 40) * 100;
        }
        else
        {
            HoldemHand hhand = table.getHoldemHand();
            boolean bHumanFolded = false;
            if (hhand != null)
            {
                PokerPlayer human = game_.getLocalPlayer();
                bHumanFolded = !human.isObserver() && human.getTable() == table && human.isFolded();
            }

            if (!bHumanFolded)
            {
                nDelay = engine_.getPrefsNode().getInt(PokerConstants.OPTION_AUTODEALHAND, 30) * 100;
            }
            else
            {
                nDelay = engine_.getPrefsNode().getInt(PokerConstants.OPTION_AUTODEALFOLD, 10) * 100;
            }
        }

        //logger.debug("Autodeal delay: " + nDelay);
        return nDelay;
    }

    /////
    ///// updates to director from outside - must synchronize
    /////

    /**
     * Notify of rejoining player
     */
    public synchronized void notifyPlayerRejoinStart(PokerPlayer p)
    {
        PokerTable table = p.getTable();
        if (!p.isObserver())
        {
            if (DEBUG_REJOIN) logger.debug(p.getName() + " rejoin start, table now REJOIN_START");
            table.setRejoinState(PokerTable.REJOIN_START);
        }
        p.setRejoining(true);
    }

    /**
     * Notify rejoin process is done.  This is also called when the client
     * is ready at start of tournament.
     */
    public synchronized void notifyPlayerRejoinDone(PokerPlayer p)
    {
        PokerTable table = p.getTable();
        // if this is a player and we are waiting on that player to act,
        // process them
        if (!p.isObserver() && table.isWaitListMember(p))
        {
            // if we are waiting on deal for button, then this is the start
            // of the tournament, so remove the player from the wait list
            if (table.getTableState() == PokerTable.STATE_PENDING &&
                table.getPendingTableState() == PokerTable.STATE_DEAL_FOR_BUTTON)
            {
                if (DEBUG_REJOIN) logger.debug(p.getName() + " ready for deal for button");
                table.removeWait(p);
            }
            // otherwise this player is done rejoining (TD is created and table is displayed),
            // so we can process the rejoin
            else
            {
                if (DEBUG_REJOIN) logger.debug(p.getName() + " rejoin done, table now REJOIN_PROCESS");
                table.setRejoinState(PokerTable.REJOIN_PROCESS);
            }
        }
        else
        {
            p.setRejoining(false);
            if (!p.isObserver())
            {
                if (DEBUG_REJOIN) logger.debug(p.getName() + " rejoin done, table now REJOIN_NONE");
                table.setRejoinState(PokerTable.REJOIN_NONE);
            }
        }
    }

    /**
     * handle player action from Bet or remote client
     */
    public synchronized void doHandAction(HandAction action, boolean bRemote)
    {
        doHandAction(action, true, true, bRemote);
    }

    /**
     * handle player action - internal with option to update state, call processTable()
     */
    private void doHandAction(HandAction action, boolean bRemoveWaitList, boolean bProcessTable, boolean bRemote)
    {
        if (bClient_)
        {
            mgr_.doHandAction(action);
        }
        else
        {
            // store action in hand
            PokerPlayer player = action.getPlayer();
            PokerTable table = player.getTable();
            storeHandAction(action, bRemote);

            // remove from wait list to show an action
            // occurred and the next betting round should happen
            if (bRemoveWaitList)
            {
                table.removeWait(player);
            }

            // send HandAction to all other players
            if (bOnline_)
            {
                mgr_.doHandActionCopy(action);
            }

            // process next action
            if (bProcessTable)
            {
                processTable(table);
            }
        }
    }

    /**
     * actual processing where we store action in hand, which updates the
     * state of the holdem hand
     */
    private void storeHandAction(HandAction action, boolean bValidateRemote)
    {
        PokerPlayer player = action.getPlayer();
        PokerTable table = player.getTable();

        // current state must be pending and last state must be betting
        // for it to make sense to process this action from remote client
        if (bValidateRemote)
        {
            int nState = table.getTableState();
            int nLastState = table.getPreviousTableState();
            if (nState != PokerTable.STATE_PENDING && nLastState != PokerTable.STATE_BETTING)
            {
                logger.warn("Current state: " + PokerTable.getStringForState(nState) +
                            ", last state: " + PokerTable.getStringForState(nLastState) +
                            "; incorrect for handling: " + action);
                return;
            }

            // make sure we are waiting on the right person
            PokerPlayer expected = table.getWaitPlayer();
            if (expected == null || expected != player)
            {
                logger.warn("Waiting on: " + (expected == null ? "(nobody)" : expected.getName()) + "; ignoring: " + action);
                return;
            }
        }

        // play sound b4 doing all the player processing
        if (table.isCurrent() && !table.isZipMode())
        {
            int nAction = action.getAction();
            switch (nAction)
            {
                case HandAction.ACTION_FOLD:
                    //TODO: fold audio
                    break;

                case HandAction.ACTION_CHECK:
                    PokerUtils.checkAudio();
                    break;

                case HandAction.ACTION_CHECK_RAISE:
                    PokerUtils.checkAudio();
                    break;

                case HandAction.ACTION_BET:
                    PokerUtils.betAudio();
                    break;

                case HandAction.ACTION_CALL:
                    PokerUtils.betAudio();
                    break;

                case HandAction.ACTION_RAISE:
                    PokerUtils.raiseAudio();
                    //PokerUtils.betAudio();
                    break;

                default:
                    ApplicationError.assertTrue(false, "Unknown HandAction action: " + nAction);
            }
        }

        // have player process this action
        player.processAction(action);

        // need to get action as stored in hand history
        if (table.isCurrent() && !table.isZipMode())
        {
            HoldemHand hhand = player.getHoldemHand();
            action = hhand.getLastAction();
            int nPrior = 0;
            int nAction = action.getAction();
            if (nAction == HandAction.ACTION_RAISE)
            {
                nPrior = hhand.getNumPriorRaises(player);
            }

            sendDealerChatLocal(PokerConstants.CHAT_2, action.getChat(nPrior, null, null));
        }
    }

    /**
     * handle CC of hand action
     * TODO: remove this method if decide to wait for observers in TD wait list
     */
    public void storeHandActionCC(HandAction action)
    {
        // make sure current player is set - this can be
        // not set do to timing of DealDisplay/DealCommunity
        // in observers (can receive hand action prior to
        // those finishing because host doesn't wait on
        // observers to acknowledge they finished action)
        action.getPlayer().getHoldemHand().getCurrentPlayerInitIndex();

        // do normal processing
        storeHandAction(action, false);
    }

    /**
     * remove player from wait list
     */
    public synchronized void removeFromWaitList(PokerPlayer player)
    {
        // could be null in shutdown instance
        if (player == null) return;

        // client observers never added to wait list, so don't waste processing  TODO: may change this...
        if (bClient_ && player.isObserver()) return;

        if (bClient_)
        {
            mgr_.removeFromWaitList(player);
        }
        else
        {
            PokerTable table = player.getTable();
            table.removeWait(player);
            processTable(table);
        }
    }

    /**
     * send player update
     */
    public synchronized void playerUpdate(PokerPlayer player, String sSettings)
    {
        mgr_.sendPlayerUpdate(player, sSettings);
    }

    /**
     * process player update
     */
    public synchronized void processPlayerUpdate(PokerPlayer player, String sSettings)
    {
        player.setOnlineSettings(sSettings);
    }

    /**
     * do the deal
     */
    public synchronized void doDeal(PokerTable table)
    {
        table.setTableState(getTableStateStartDeal());
        processTable(table);
    }

    /**
     * send player update
     */
    public synchronized void changeTable(PokerPlayer player, PokerTable table)
    {
        if (bHost_)
        {
            PokerTable old = player.getTable();
            if (old == table || table.isAllComputer())
            {
                // ignore request to do to same table or (perhaps a just new) all computer table
                return;
            }
            String sMsg = PropertyConfig.getMessage("msg.chat.observerchanged",
                                                    Utils.encodeHTML(player.getName()),
                                                    old.getName(),
                                                    table.getName());
            sendDealerChat(PokerConstants.CHAT_1, old, sMsg);
            sendDealerChat(PokerConstants.CHAT_1, table, sMsg);

            // gather events
            TDClean cleanEvents = new TDClean(table);
            old.removeObserver(player);
            table.addObserver(player);
            notifyPlayers(table, cleanEvents, false);
        }
        else
        {
            mgr_.changeTable(player, table);
        }
    }

    /**
     * keep this in one place
     */
    private static int getTableStateStartDeal()
    {
        return PokerTable.STATE_CHECK_END_HAND;
    }

    /**
     * game over
     */
    public synchronized void setGameOver()
    {
        // end each table
        PokerTable table;
        int nNumTables = game_.getNumTables();
        for (int i = 0; i < nNumTables; i++)
        {
            table = game_.getTable(i);
            table.setTableState(PokerTable.STATE_GAME_OVER);
            table.removeWaitAll();
        }

        // stop clock
        game_.getGameClock().stop();

        // update server
        if (bOnline_ && !game_.isGameOver() && game_.isPublic())
        {
            endWanGame();
        }

        // note game over after processing done
        game_.setGameOver(true);
    }

    /**
     * rebuy
     */
    public synchronized void doRebuy(PokerPlayer player, int nLevel, int nAmount, int nChips, boolean bPending)
    {
        PokerTable table = player.getTable();
        if (table == null) return; // safety

        if (!table.isRebuyAllowed(player, nLevel))
        {
            logger.warn("Skipping non-allowed rebuy for " + player.getName() + " level: " + nLevel +
                        " amount: " + nAmount + " chips: " + nChips + " pending: " + bPending);
            return;
        }

        if (bClient_)
        {
            player.addRebuy(nAmount, nChips, bPending);
            mgr_.doRebuy(player, nLevel, nAmount, nChips, bPending);
        }
        else
        {
            player.addRebuy(nAmount, nChips, bPending);

            sendDealerChat(PokerConstants.CHAT_1, player.getTable(), PokerUtils.chatInformation(
                    PropertyConfig.getMessage(bPending ? "chat.rebuy.pending" : "chat.rebuy",
                                              Utils.encodeHTML(player.getName()), nChips)));
        }
    }

    /**
     * rebuy
     */
    public synchronized void doAddon(PokerPlayer player, int nAmount, int nChips)
    {
        if (bClient_)
        {
            player.addAddon(nAmount, nChips);
            mgr_.doAddon(player, nAmount, nChips);
        }
        else
        {
            player.addAddon(nAmount, nChips);
            sendDealerChat(PokerConstants.CHAT_1, player.getTable(), PokerUtils.chatInformation(
                    PropertyConfig.getMessage("chat.addon",
                                              Utils.encodeHTML(player.getName()), nChips)));
        }
    }

    ////
    //// chat
    ////

    /**
     * chat handler
     */
    public void setChatHandler(ChatHandler chat)
    {
        chat_ = chat;
        if (mgr_ != null)
        {
            mgr_.setChatHandler(chat);
        }
    }

    /**
     * send chat from client
     */
    public void sendChat(String sMessage, PokerTable table, String sTestData)
    {
        if (mgr_ != null)
        {
            mgr_.sendChat(sMessage, table, sTestData);
        }
    }

    /**
     * send chat to given player
     */
    public void sendChat(int nPlayerID, String sMessage)
    {
        if (mgr_ != null)
        {
            mgr_.sendChat(nPlayerID, sMessage);
        }
    }

    /**
     * send message to all people at table
     */
    private void sendDealerChat(int nType, PokerTable table, String sMessage)
    {
        if (mgr_ != null)
        {
            mgr_.sendDealerChat(nType, sMessage, table);
        }
        else
        {
            if (table.isCurrent())
            {
                deliverChatLocal(nType, sMessage, OnlineMessage.CHAT_DEALER_MSG_ID);
            }
        }
    }

    /**
     * send message to all players
     */
    public void sendDirectorChat(String sMessage, Boolean bPauseClock)
    {
        if (mgr_ != null)
        {
            mgr_.sendDirectorChat(sMessage, bPauseClock);
        }
        else
        {
            deliverChatLocal(PokerConstants.CHAT_ALWAYS, sMessage, OnlineMessage.CHAT_DIRECTOR_MSG_ID);
        }
    }

    /**
     * display dealer chat local
     */
    public void sendDealerChatLocal(int nType, String sMessage)
    {
        deliverChatLocal(nType, sMessage, OnlineMessage.CHAT_DEALER_MSG_ID);
    }

    /**
     * In practice mode, used due to no online manager, in online
     * used to deliver messages as a result of some other action (to
     * avoid unnecessary network traffic)
     */
    public void deliverChatLocal(int nType, String sMessage, int id)
    {
        if (chat_ != null)
        {
            OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
            chat.setChat(sMessage);
            chat.setChatType(nType);
            chat.setFromPlayerID(id);
            chat_.chatReceived(chat);
        }
    }

    /**
     * Start the WAN game.
     */
    private void startWanGame()
    {
        // No server processing if not the host or not a public game.
        if (!bOnline_ || !bHost_ || !game_.isPublic())
        {
            return;
        }

        // Send a message requesting that the game be started.
        OnlineServer manager = OnlineServer.getWanManager();
        manager.startGame(game_);
    }

    /**
     * End the WAN game.
     */
    private void endWanGame()
    {
        // No server processing if not the host or not a public game.
        if (!bOnline_ || !bHost_ || !game_.isPublic())
        {
            return;
        }

        // Send a message requesting that the game be ended and results stored.
        OnlineServer manager = OnlineServer.getWanManager();
        manager.endGame(game_, true);
    }
}
