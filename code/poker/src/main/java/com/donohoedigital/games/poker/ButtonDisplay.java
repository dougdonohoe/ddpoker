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
/*
 * ButtonDisplay.java
 *
 * Created on January 2, 2004, 1:24 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

import javax.swing.*;

/**
 *
 * @author  Doug Donohoe
 */
public class ButtonDisplay extends ChainPhase implements Runnable
{
    static Logger logger = LogManager.getLogger(ButtonDisplay.class);
    
    private PokerGame game_;
    private PokerTable table_;
    private static int BUTTON_DELAY = 100;
    
    /** 
     * Creates a new instance of ButtonDisplay 
     */
    public ButtonDisplay() 
    {
    }
    
    /**
     * Override to not call nextPhase here (do at end of thread)
     */
    public void start()
    {
        process();
    }
    
    /**
     * display cards
     */
    public void process() 
    {
        game_ = (PokerGame) context_.getGame();
        table_ = game_.getCurrentTable();
        
        Thread tButton = new Thread(this, "ButtonDisplay");
        tButton.start();
    }
    
    /**
     * thread
     */
    public void run() 
    {
        displayButton(table_, BUTTON_DELAY);
        // move on
        nextPhase();
    }
    
    /**
     * Display button
     */
    public static void displayButton(PokerTable table, int nSleepMillis)
    {
        // get seat button should be at (and assoc territory)
        int nSeat = table.getButton();
        if (nSeat == PokerTable.NO_SEAT) return;
        final Territory t = PokerUtils.getTerritoryForTableSeat(table, nSeat);
        ApplicationError.assertNotNull(t, "No territory for seat " + nSeat);
        
        // get button
        ButtonPiece piece = getButton();
        
        // if button in another region, remove it
        final Territory old = piece.getTerritory();
        if (old != null)
        {
            old.removeGamePiece(piece);
        }
        t.addGamePiece(piece);

        // sleep
        if (nSleepMillis > 0) Utils.sleepMillis(nSleepMillis); 

        // repaint board
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                   if (old != null) PokerUtils.getGameboard().repaintTerritory(old, false);
                    PokerUtils.getGameboard().repaintTerritory(t, false);

                }
            }
        );
    }
 
    /**
     * Gets the button from whatever seat it is at and
     * returns it.  If not there, returns a new button
     */
    private static ButtonPiece getButton()
    {
        Territory t;
        ButtonPiece piece;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            t = PokerUtils.getTerritoryForDisplaySeat(i);
            piece = (ButtonPiece) t.getGamePiece(PokerConstants.PIECE_BUTTON, null);
            if (piece != null)
            {
                return piece;
            }
        }
        return new ButtonPiece();
    }
}
