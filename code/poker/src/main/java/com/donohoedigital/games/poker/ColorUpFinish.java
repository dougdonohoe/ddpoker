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
 * ColorUpFinish.java
 *
 * Created on May 14, 2004, 8:48 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;

import javax.swing.*;

/**
 *
 * @author  Doug Donohoe
 */
public class ColorUpFinish extends ChainPhase implements PlayerActionListener, Runnable
{
    static Logger logger = Logger.getLogger(ColorUpFinish.class);
    
    private PokerGame game_;
    private boolean bPhase1Done_ = false;
    private PokerTable table_;
    private boolean bPause_;
    
    // in online games, we don't show "continue" button, but auto-continue
    // after 2 seconds
    private static final int ONLINE_WAIT_TENTHS = 20;
    
    /**
     * override - we explicitly call nextPhase
     */
    public void start()
    {
        process();
    }
    
    /** 
     * Do color up
     */
    public void process()
    {
        game_ = (PokerGame) context_.getGame();
        table_ = game_.getCurrentTable();
        
        // pause game if not auto-pilot, not online game and if pref is set
        bPause_ = !TESTING(PokerConstants.TESTING_AUTOPILOT) &&
                  !game_.isOnlineGame() &&
                    engine_.getPrefsNode().getBoolean(PokerConstants.OPTION_PAUSE_COLOR, true);
                  
        
        // this is here so that a save by a user at this
        // point (after deal is shown) will restart here
        // a minor nit, but just attention to detail
        table_.setPendingPhase(gamephase_.getName());

        
        // go right to next phase if not coloring up (this phase shouldn't
        // be called if not coloring up due to new TournamentDirector,
        // but leaving it here for safety)
        if (!table_.isColoringUp())
        {
            nextPhase();
        }
        // otherwise, wait for user to press continue or invoke after a wait
        else
        {
            // exception:
            // go right to phase one if we aren't coloring up, are on auto pilot, or
            // the num with odd chips is zero (this could happen if game saved after
            // 1st continue button pressed, but before 2nd)
            if (getNumWithOddChips() == 0)
            {
                bPhase1Done_ = true;
            }
            
            // pause mode - wait for user input
            if (bPause_)
            {
                game_.setInputMode(PokerTableInput.MODE_CONTINUE);
                game_.setPlayerActionListener(this);
            }
            else
            {
                Thread t = new Thread(new ColorWait(ONLINE_WAIT_TENTHS), "ColorWait1");
                t.start();
            }
        }
    }
    
    /**
     * return number of players with odd chips
     */
    private int getNumWithOddChips()
    {
        int nOdd = 0;
        PokerPlayer player;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = table_.getPlayer(i);
            if (player == null) continue;
            if (player.getOddChips() > 0) nOdd++;
        }
        return nOdd;
    }
    
    /**
     * Thread to sleep between actions
     */
    private class ColorWait implements Runnable
    {
        int nWaitTenths;
        public ColorWait(int n)
        {
            nWaitTenths = n;
        }

        public void run()
        {
            // wait
            int nSleep = nWaitTenths * 100;
            Utils.sleepMillis(nSleep);

            // do processing
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                            playerActionPerformed(0,0);
                    }
                }
            );
        }
    }
    
    /**
     * Start/Pause button pressed
     */
    public void playerActionPerformed(int action, int nAmount)
    {
        // first time through, run thread to colorup chips, repainting
        // as we go show we have a slight delay
        if (!bPhase1Done_)
        {
            // show chips for each high card
            if (bPause_)
            {
                game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
            }
            bPhase1Done_ = true;
            Thread t = new Thread(this, "Color-up");
            t.start();
        }
        // second time through, do phase2
        else
        {
            doPhase2();
        }
    }

    /**
     * thread
     */
    public void run() 
    {
        doPhase1();
    }
    
    /**
     * Phase 1 - determine winner
     */
    private void doPhase1()
    {
        table_.colorUp();

        PokerPlayer player;

        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            player = table_.getPlayer(i);
            if (player == null) continue;

            // repaint players with non-empty hands
            if (player.getHand().size() > 0)
            {
                repaint(player);
            }
        }
        
        moveToPhase2();
    }
    
    /**
     * do transition to phase2
     */
    private void moveToPhase2()
    {        
        if (bPause_)
        {
            game_.setInputMode(PokerTableInput.MODE_CONTINUE);
        }
        else
        {
            Thread t = new Thread(new ColorWait(ONLINE_WAIT_TENTHS), "ColorWait2");
            t.start();
        }
    }
    
    /**
     * Repaint table
     */
    private void repaint(PokerPlayer player)
    {
        final Territory t = PokerUtils.getTerritoryForTableSeat(table_, player.getSeat());
        GuiUtils.invokeAndWait(
            new Runnable() {
                public void run() {
                    PokerUtils.getGameboard().repaintTerritory(t, true);
                }
            }
        );
    }
    
    /**
     * Phase 2 - cleanup
     */
    private void doPhase2()
    {        
        if (bPause_)
        {
            game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
            game_.setPlayerActionListener(null);
        }

        // no longer displaying color up, call here
        // so clearCards() also clears "won chip" icons
        table_.setColoringUpDisplay(false);

        // remove cards added previously - do before colorUpFinish
        // since the cards are removed there and we could have
        // a race condition if a repaint is requested
        PokerUtils.clearCards(true);

        // finish
        table_.colorUpFinish();
        
        nextPhase();
    }
    
    public void nextPhase()
    {        
        // notify tournament director that color up is done
        TournamentDirector td = (TournamentDirector) context_.getGameManager();
        td.removeFromWaitList(game_.getHumanPlayer());
        
        super.nextPhase();
    }
}
