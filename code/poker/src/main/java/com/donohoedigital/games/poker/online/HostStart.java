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
 * HostStart.java
 *
 * Created on January 17, 2005, 5:01 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import org.apache.log4j.*;

import java.awt.event.*;

/**
 *
 * @author  donohoe
 */
public class HostStart extends ChainPhase implements ActionListener
{
    static Logger logger = Logger.getLogger(HostStart.class);
    
    public static final String PHASE_CLIENT_INIT = "InitializeOnlineGameClient";
    private static final int ONE_SEC = 1000;

    private int DELAY;
    private int DELAY_SECS;
    
    private PokerGame game_;
    private OnlineManager mgr_;
    private javax.swing.Timer timer_;
    
    /**
     * Override so we can call nextPhase when we choose
     */
    public void start()
    {
        process();
    }
    
    /** 
     * Begin an online game
     */
    public void process() 
    {
        game_ = (PokerGame) context_.getGame();
        mgr_ = game_.getOnlineManager();

        // close registration
        game_.setOnlineMode(PokerGame.MODE_PLAY);
        mgr_.sendDirectorChat(PropertyConfig.getMessage("msg.chat.regclosed"), null);

        // wake alive thread to send message that registration is closed
        ((PokerMain)engine_).getLanManager().wakeAliveThread();
        
        // delay
        DELAY = engine_.getPrefsNode().getInt(PokerConstants.OPTION_ONLINESTART, 10);
        DELAY_SECS = ONE_SEC * DELAY;

        // determine number of ai players needed
        TournamentProfile profile = game_.getProfile();
        int nMaxPlayers = profile.getMaxOnlinePlayers();
        int nNumHumans = game_.getNumPlayers();
        int nNumAI = profile.isFillComputer() ? nMaxPlayers - nNumHumans : 0;
        int nTotalPlayers = nNumHumans + nNumAI;

        // log
        String sURL = game_.getPublicConnectURL();
        if (sURL == null) sURL = game_.getLanConnectURL();
        logger.info("Registration closed, online game starting with " + nNumHumans + " humans and " + nNumAI + " ai: " + sURL);

        // if num players doesn't match profile, need to update profile
        // and, potentially, payout structure
        if (nTotalPlayers != profile.getNumPlayers())
        {
            profile.updateNumPlayers(nTotalPlayers);
        }

        // proceed with tournament setup now that we have all
        // human players defined
        game_.setupTournament(true, profile.isFillComputer(), nTotalPlayers);
        
        // notify players if AI players added
        if (nNumAI > 0)
        {
            mgr_.sendDirectorChat(PropertyConfig.getMessage(
                    (nNumAI > 1 ? "msg.chat.ai.added.plural" : "msg.chat.ai.added.singular"),
                    new Integer(nNumAI)), null);
        }
        
        // set current table for host
        game_.setCurrentTable(game_.getLocalPlayer().getTable());

        // send all players update as tables and chips are now set
        // this sets current table for clients
        game_.setAllPlayersDirty(true);
        game_.setAllObserversDirty(true);
        mgr_.sendDirtyPlayerUpdateToAll(true, true);
        game_.setAllPlayersDirty(false);
        game_.setAllObserversDirty(false);

        // do countdown
        mgr_.sendDirectorChat(PropertyConfig.getMessage(
                    (DELAY > 1 ? "msg.chat.starts.plural" : "msg.chat.starts.singular"),
                    new Integer(DELAY)), null);
        
        timer_ = new javax.swing.Timer(ONE_SEC,  this);
        timer_.start();
    }

    int nTime_ = 0;
    /**
     * action performed by timer - print message at [begin], evert 5 seconds
     */
    public void actionPerformed(ActionEvent e)
    {
        if (nTime_ == 0) nTime_ += timer_.getInitialDelay();
        else nTime_ += timer_.getDelay();
        if (nTime_ >= DELAY_SECS)
        {
            timer_.stop();
            beginGame();
        }
        else
        {
            int left = (DELAY_SECS - nTime_) / ONE_SEC;
            if (left % 5 == 0)
            {
                mgr_.sendDirectorChat(PropertyConfig.getMessage(
                    (left > 1 ? "msg.chat.starts.plural" : "msg.chat.starts.singular"),
                    new Integer(left)), null);
            }
        }
    }
    
    /**
     * Begin the game
     */
    private void beginGame()
    {
        // save - we are done initing so specify TournamentDirector in save file
        context_.setSpecialSavePhase(TournamentDirector.PHASE_NAME);
        game_.saveWriteGame();
        context_.setSpecialSavePhase(null);

        // move along
        game_.setStartFromLobby(true);
        nextPhase();
    }
}
