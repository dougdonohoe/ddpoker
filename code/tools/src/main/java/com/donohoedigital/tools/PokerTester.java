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
 * PokerTester.java
 *
 * Created on October 13, 2005, 8:55 AM 
 */

package com.donohoedigital.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.online.*;
import org.apache.log4j.*;

/**
 *
 * @author  Doug Donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "MethodOverridesStaticMethodOfSuperclass"})
public class PokerTester extends PokerMain implements ChatHandler, OnlineMessageListener
{
    // logging
    private static Logger logger = Logger.getLogger(PokerTester.class);

    private PokerGame game_;

    /**
     * Run emailer
     */
    public static void main(String[] args) {
        try {
            PokerTester tester = new PokerTester("poker", args);
            tester.init();
        }
        catch (ApplicationError ae)
        {
            System.err.println("PokerTester ending due to ApplicationError: " + ae.toString());
            System.exit(1);
        }  
        catch (java.lang.OutOfMemoryError nomem)
        {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
    }

    /** 
     * Create War from config file
     */
    public PokerTester(String sConfigName, String[] args)
    {
        super(sConfigName, sConfigName, args, true, false);
    }

    @Override
    public void init()
    {
        super.init();
        
        // get key
        String url = htOptions_.getString("url");
        String name = htOptions_.getString("name");
        boolean observer = true;

        logger.info("URL: " + url);
        logger.info("Player: " + name);

        test(url, name, !observer);
    }
    
    /**
     * Can be overridden for application specific options
     */
    @Override
    protected void setupApplicationCommandLineOptions()
    {
        CommandLine.addStringOption("url", null);
        CommandLine.setDescription("url", "Game URL", "url");
        CommandLine.setRequired("url");

        CommandLine.addStringOption("name", null);
        CommandLine.setDescription("name", "player name", "name");
        CommandLine.setRequired("name");
//
//        CommandLine.addFlagOption("obs");
//        CommandLine.setDescription("obs", "observer [def]");

    }


    private void test(String sConnect, String sName, boolean bPlayer)
    {
        // get connect url and player name
        PokerURL url = new PokerURL(sConnect);
        boolean bObs = !bPlayer;

        // create game
        game_ = new PokerGame(getDefaultContext());
        game_.setOnlineGameID(url.getGameID());
        game_.setOnlinePassword(url.getPassword());

        // temp player with connect URL provided by user
        PokerPlayer ptemp = new PokerPlayer(getPublicUseKey(), PokerConstants.PLAYER_ID_TEMP, sName, true);
        logger.info("Key: " + getPublicUseKey());
        ptemp.setConnectURL(url);
        if (bPlayer)
        {
            game_.addPlayer(ptemp);
        }
        else
        {
            game_.addObserver(ptemp);
        }

        // init to client mode and save game
        game_.initOnline(PokerGame.MODE_CLIENT);
        getDefaultContext().setGame(game_);

        // have online manager do join, which returns true
        // if successful (in which case it places the game into
        // the engine and invokes the phase returned)
        Object o = game_.getOnlineManager().joinGame(bObs, false, true);
        if (o != Boolean.TRUE)
        {
            getDefaultContext().setGame(null);
            return;
        }

        OnlineManager mgr = game_.getOnlineManager();
        mgr.setChatHandler(this);
        mgr.addOnlineMessageListener(this);
    }

    public void chatReceived(OnlineMessage omsg)
    {
        PokerPlayer player = game_.getPokerPlayerFromID(omsg.getFromPlayerID());
        logger.debug((player == null ? "[null]" : player.getName()) + " chat: " + omsg.getChat());
    }

    private int RCNT = 0;
    public void messageReceived(OnlineMessage omsg)
    {
        PokerPlayer from = game_.getPokerPlayerFromConnection(omsg.getConnection());
        if (omsg.getCategory() != OnlineMessage.CAT_CHAT)
        {
            RCNT++;
            logger.debug(RCNT + " received from " + (from == null ? "[unknown]" : from.getName()) +
                                ": " + omsg.toStringCategorySize());
        }
    }

    // TODO: host status - reconnect auto magically
    // TODO: periodic chat
    // TODO: alive message
    // TODO: avoid processPhase messages
    // TODO: respond to any messages?
    // TODO: alive check working for observers?
    //mgr_.sendChat("Received chat: "+ omsg.getChat(), null, null);
}
