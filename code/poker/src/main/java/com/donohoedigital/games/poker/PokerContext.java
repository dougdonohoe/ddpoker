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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.config.GamePhase;
import com.donohoedigital.games.engine.EngineWindow;
import com.donohoedigital.games.engine.Game;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.online.OnlineLobby;
import com.donohoedigital.gui.GuiUtils;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 19, 2006
 * Time: 11:13:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class PokerContext extends GameContext
{

    /**
     * Constructor
     */
    public PokerContext(PokerContext context, String sName,
                        int nDesiredMinWidth, int nDesiredMinHeight)
    {
        super(context, sName, nDesiredMinWidth, nDesiredMinHeight);
    }

    /**
     * Constructor
     */
    public PokerContext(GameEngine engine, PokerGame game, String sName,
                        int nDesiredMinWidth, int nDesiredMinHeight,
                        boolean bQuitOnClose)
    {
        super(engine, game, sName, nDesiredMinWidth, nDesiredMinHeight, bQuitOnClose);
    }

    /**
     * Create our PokerWindow
     */
    @Override
    protected EngineWindow createEngineWindow(GameEngine engine, String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        return new PokerWindow(engine, this, sName, nDesiredMinWidth, nDesiredMinHeight);
    }

    /**
     * Create new instance of PokerGame
     */
    @Override
    public Game createNewGame()
    {
        return new PokerGame(this);
    }

    /**
     * Subclass window for our hot-keys
     */
    private class PokerWindow extends EngineWindow
    {
        public PokerWindow(GameEngine engine, GameContext context, String sName,
                           int nDesiredMinWidth, int nDesiredMinHeight)
        {
            super(engine, context, sName, nDesiredMinWidth, nDesiredMinHeight);
        }

        /**
         * init to given size (or full screen if passed in)
         */
        @Override
        public void init(GamePhase gamephase, boolean bMain, Dimension size, boolean bFull, String sTitle, boolean bResizable)
        {
            super.init(gamephase, bMain, size, bFull, sTitle, bResizable);

            GuiUtils.addKeyAction((JComponent) getContentPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "onlinelobby", new LobbyAction(),
                                  KeyEvent.VK_L, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

            GuiUtils.addKeyAction((JComponent) getContentPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "udpstatus", new UDPAction(),
                                  KeyEvent.VK_U, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

            GuiUtils.addKeyAction((JComponent) getContentPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "calctool", new CalcAction(),
                                  KeyEvent.VK_T, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

            GuiUtils.addKeyAction((JComponent) getContentPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "printscreen", new PrintAction(),
                                  KeyEvent.VK_P, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);
        }
    }

    /**
     * Lobby action
     */
    private class LobbyAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            if (OnlineLobby.showLobby(GameEngine.getGameEngine(), PokerContext.this, PlayerProfileOptions.getDefaultProfile()))
            {
                processPhase("OnlineLobby");
            }
        }
    }

    /**
     * UDPStatus action
     */
    private class UDPAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            processPhase("UDPStatus");
        }
    }

    /**
     * CalcTool action
     */
    private class CalcAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            processPhase("CalcTool");
        }
    }

    /**
     * Print action
     */
    private class PrintAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            PokerUtils.doScreenShot(PokerContext.this);
        }
    }

    /**
     * an error occurred - log holdem hand info if it is available
     */
    @Override
    protected void handleProcessPhaseException(Throwable e)
    {
        LogGameInfo((PokerGame) getGame());
    }

    /**
     * Log game information
     */
    public static void LogGameInfo(PokerGame game)
    {
        if (game == null) return;

        PokerTable current = game.getCurrentTable();
        PokerTable t;
        HoldemHand hhand;
        for (int i = 0; i < game.getNumTables(); i++)
        {
            t = game.getTable(i);
            if (t.isAllComputer()) continue;

            logger.debug("**** Holdem hand on {}{} when error occurred: ", t.getName(), ((current == t) ? " (current)" : ""));
            hhand = t.getHoldemHand();
            if (hhand != null)
            {
                hhand.debugPrint();
            }
            else
            {
                logger.debug("No hand.");
            }
            logger.debug("****");
        }
    }

    // game information

    private Bet currentBetPhase;
    private boolean fastSaveTest;

    public Bet getCurrentBetPhase()
    {
        return currentBetPhase;
    }

    public void setCurrentBetPhase(Bet currentBetPhase)
    {
        this.currentBetPhase = currentBetPhase;
    }

    public boolean isFastSaveTest()
    {
        return fastSaveTest;
    }

    public void setFastSaveTest(boolean fastSaveTest)
    {
        this.fastSaveTest = fastSaveTest;
    }
}
