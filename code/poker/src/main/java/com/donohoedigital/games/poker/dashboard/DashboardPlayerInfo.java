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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.ai.gui.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.base.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DashboardPlayerInfo extends DashboardItem implements TerritorySelectionListener
{
    private DDPanel stylePanel_;
    private StyleQuadrantsGridPanel styleQuadsPanel_;
    private DDHtmlArea styleSummaryLabel_;
    private WeightGridPanel weightGrid_;
    PokerPlayer last_;

    public DashboardPlayerInfo(GameContext context)
    {
        super(context, "playerstyle");

        trackTableEvents(PokerTableEvent.TYPE_NEW_HAND);
        PokerUtils.getGameboard().addTerritorySelectionListener(this);
    }

    protected JComponent createBody()
    {
        DDPanel base = new DDPanel();

        stylePanel_ = new DDPanel(GuiManager.DEFAULT, STYLE);
        styleQuadsPanel_ = new StyleQuadrantsGridPanel();
        styleSummaryLabel_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        styleSummaryLabel_.setBorder(BorderFactory.createEmptyBorder());

        ((BorderLayout)stylePanel_.getLayout()).setHgap(4);
        stylePanel_.add(GuiUtils.CENTER(styleQuadsPanel_), BorderLayout.WEST);
        stylePanel_.add(styleSummaryLabel_, BorderLayout.CENTER);

        ((BorderLayout)base.getLayout()).setVgap(4);

        base.add(stylePanel_, BorderLayout.NORTH);

        if (TESTING(PokerConstants.TESTING_HAND_WEIGHT_GRID))
        {
            weightGrid_ = new WeightGridPanel();
            weightGrid_.setPreferredSize(new Dimension(200,200));
            base.add(weightGrid_, BorderLayout.SOUTH);
        }

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

        if (p == null) p = game_.getHumanPlayer();

        if (p != last_)
        {
            last_ = p;
            updateInfo();
        }
    }

    /**
     * update display
     */
    protected void updateInfo()
    {
        if (last_ != null && !last_.isObserver() && last_.getOpponentModel() != null)
        {
            OpponentModel model = last_.getOpponentModel();
            StringBuilder buf = new StringBuilder();

            float tightness = model.getPreFlopTightness(-1, Float.NaN);
            float aggression = model.getPreFlopAggression(-1, Float.NaN);

            styleQuadsPanel_.setValues(tightness, aggression);

            String sDisplay = Utils.encodeHTML(last_.getName());
            buf.append(PropertyConfig.getMessage("msg.playerstyle", sDisplay));
            int mark = buf.length();

            if (Float.isNaN(tightness) && Float.isNaN(aggression))
            {
                buf.append(PropertyConfig.getMessage("msg.playerstyle.unknown"));
            }

            if (tightness > 0.6f)
            {
                buf.append(PropertyConfig.getMessage("msg.playerstyle.tight"));
            }
            else if (tightness < 0.4f)
            {
                buf.append(PropertyConfig.getMessage("msg.playerstyle.loose"));
            }

            if (aggression > 0.6f)
            {
                if (buf.length() > mark) buf.append(" / ");
                buf.append(PropertyConfig.getMessage("msg.playerstyle.aggressive"));
            }
            else if (aggression < 0.4f)
            {
                if (buf.length() > mark) buf.append(" / ");
                buf.append(PropertyConfig.getMessage("msg.playerstyle.passive"));
            }

            // middle of the road
            if (buf.length() == mark)
            {
                buf.append(PropertyConfig.getMessage("msg.playerstyle.moderate"));
            }

            styleSummaryLabel_.setText(buf.toString());

            if (TESTING(PokerConstants.TESTING_HAND_WEIGHT_GRID))
            {
                weightGrid_.setPlayer(last_);
                weightGrid_.repaint();
            }
        }
        else
        {
            styleSummaryLabel_.setText(PropertyConfig.getMessage("msg.playerstyle.none"));
            styleQuadsPanel_.setValues(Float.NaN, Float.NaN);
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
