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

import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;

/**
     * listener for un-pausing TD
 */
public class TournamentDirectorPauser implements PlayerActionListener
{
    GameContext context_;
    TournamentDirector td_;
    PokerGame game_;
    int nLastInputMode_;
    HoldemHand hhand_;

    public TournamentDirectorPauser(GameContext context)
    {
        context_ = context;
    }

    public void pause()
    {
        game_ = (PokerGame) context_.getGame();
        game_.setPlayerActionListener(this);
        td_ = (TournamentDirector) context_.getGameManager();
        PokerTable table = game_.getCurrentTable();
        hhand_ = (table == null ? null : table.getHoldemHand());

        td_.setPaused(true);
        nLastInputMode_ = game_.getInputMode();

        if (hhand_ != null && hhand_.getRound() == HoldemHand.ROUND_PRE_FLOP)
        {
            game_.setInputMode(PokerTableInput.MODE_CONTINUE);
        }
        else
        {
            game_.setInputMode(PokerTableInput.MODE_CONTINUE_LOWER);
        }
    }

    public void playerActionPerformed(int action, int nAmount)
    {
        td_.setPaused(false);

        if (hhand_ != null)
        {
            game_.setInputMode(nLastInputMode_, hhand_, hhand_.getCurrentPlayer());
        }
        else
        {
            game_.setInputMode(nLastInputMode_);
        }
        game_.setPlayerActionListener(null);
    }
}
