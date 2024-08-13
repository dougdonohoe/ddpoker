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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.ai.*;

import java.awt.*;

public class WeightGridPanel extends AdvisorGridPanel
{
    private Color colors_[] = new Color[101];

    private PokerPlayer player_;

    public WeightGridPanel()
    {
        for (int i = 0; i <= 100; ++i)
        {
            colors_[i] = new Color(0, (int)(i*2.55f), 0);
        }

        setPreFlop(false);
        setMinorGrid(false);
    }

    public void setPlayer(PokerPlayer player)
    {
        player_ = player;
    }

    public int getValue(int card1, int card2)
    {
        if (player_ == null) return 0;

        HoldemHand hhand = player_.getHoldemHand();

        if (hhand == null) return 0;

        PokerTable table = hhand.getTable();

        if (table == null) return 0;

        PokerGame game = table.getGame();

        if (game == null) return 0;
        
        PokerPlayer human = game.getHumanPlayer();

        Hand exclude = null;

        if ((human != null) && (human != player_) &&  !human.isFolded())
        {
            exclude = human.getHand();
        }

        int seat = player_.getSeat();

        if (hhand == null) return 0;

        PocketMatrixFloat matrix = PocketWeights.getInstance(hhand).getWeightTable(seat);

        float v;

        if ((card1 == card2) || ((exclude != null) && (exclude.containsCard(card1) || exclude.containsCard(card2))))
        {
            return 0;
        }
        else
        {
            v = matrix.get(card1, card2);
        }

        if (Float.isNaN(v) || v < 0)
        {
            return 0;
        }
        else
        {
            return (int)(v * 100);
        }
    }

    protected Color getColor(int card1, int card2)
    {
        int v = getValue(card1, card2);

        return colors_[v];
    }
}

