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
 * GamePrefsDialog.java
 *
 * Created on December 16, 2003, 8:19 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.dashboard.*;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GamePrefsDialog extends OptionMenuDialog
{
    //static Logger logger = LogManager.getLogger(GamePrefsDialog.class);
    
    private GamePrefsPanel panel_;
    
    /**
     * Get component with options, also fill array with same options
     */
    protected JComponent getOptions()
    {      
        panel_ = new GamePrefsPanel(engine_, context_, "OptionsDialog", null, true, this);
        return panel_;
    }
    
    protected int getTextPreferredHeight()
    {
        return 97;
    }
    
    protected Component getFocusComponent()
    {
        return panel_.getFocusComponent();
    }

    protected boolean isDDOptionsValid()
    {
        return panel_.isValidData();
    }

    /**
     * Okay button - check face up option
     */
    public void okayButton()
    {
        updatePrefs(context_);
        CheatDash.updatePrefs();
        PokerUtils.updateChat();
    }

    /**
     * Used to handle when prefs changed
     * @param context
     */
    public static void updatePrefs(GameContext context)
    {
        // get game, skip if home tournament
        PokerGame game = (PokerGame) context.getGame();
        if (game.isClockMode()) return;
        if (game.isOnlineGame() && !game.isInProgress()) return;

        // update felt colors
        if (PokerUtils.getPokerGameboard() != null)
        {
            PokerUtils.getPokerGameboard().updateFelt(true);
        }

        // get cards for human player
        PokerTable table = game.getCurrentTable();
        HoldemHand hhand = table.getHoldemHand();
        if (hhand == null) return;

        // all-in showdown - don't mess with card display
        if (hhand.isAllInShowdown()) return;

        // notify table so display can update
        hhand.getTable().prefsChanged();
    }
}
