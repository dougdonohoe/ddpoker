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
 * PrizePoolDialog.java
 *
 * Created on April 24, 2004, 8:15 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

/**
 *
 * @author  donohoe
 */
public class PrizePoolDialog extends DialogPhase implements ChangeListener
{
    //static Logger logger = Logger.getLogger(PrizePoolDialog.class);
    
    private DDNumberSpinner amount_;
    private JScrollPane scroll_;
    private DDHtmlArea html_;
    private PokerGame game_;
    private TournamentProfile profile_;
    private TournamentProfileHtml profileHtml_;
    private DDLabel housecut_;

    /**
     * focus
     */
    protected Component getFocusComponent()
    {
        return amount_;
    }
    
    /**
     * Create dialog
     */
    public JComponent createDialogContents() 
    {
        // init
        game_ = (PokerGame) context_.getGame();
        profile_ = game_.getProfile();
        profileHtml_ = new TournamentProfileHtml(profile_);
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(5, 0);
        base.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // prizepool
        DDPanel top = new DDPanel();
        
        // only allow prize pool adjustments if pool is auto or percent pages
        if (!profile_.isAllocFixed()) 
        {
            base.add(GuiUtils.CENTER(top), BorderLayout.NORTH);
            top.setBorderLayoutGap(0, 10);
            DDLabel label = new DDLabel("prizepool", GuiManager.DEFAULT);
            top.add(label, BorderLayout.CENTER);
            amount_ = new DDNumberSpinner(profile_.getTrueBuyin(), Integer.MAX_VALUE, profile_.getTrueBuyin(), GuiManager.DEFAULT, "PrizePool");
            amount_.setUseBigStep(true);
            amount_.setBigStep(profile_.getTrueBuyin() * 10);
            amount_.setEditable(true);
            int nCash = game_.getClockCash();
            if (nCash == 0) nCash = profile_.getNumPlayers() * profile_.getBuyinCost();
            amount_.setValue(nCash);
            amount_.addChangeListener(this);
            top.add(amount_, BorderLayout.EAST);

            housecut_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            housecut_.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            base.add(housecut_, BorderLayout.SOUTH);
        }
        
        // html area
        DDLabelBorder border = new DDLabelBorder("alloc", STYLE);
        border.setPreferredSize(new Dimension(200, 246));
        base.add(border, BorderLayout.CENTER);
        html_ = new DDHtmlArea(GuiManager.DEFAULT, "PrizePool");
        html_.setDisplayOnly(true);
        html_.setBorder(BorderFactory.createEmptyBorder(2,3,2,3));
        scroll_ = new JScrollPane(html_);
        scroll_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll_.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setBackground(html_.getBackground());
        scroll_.setForeground(html_.getForeground());
        scroll_.setBorder(BorderFactory.createEmptyBorder());
        scroll_.setOpaque(false);
        scroll_.getViewport().setOpaque(false);
        scroll_.getVerticalScrollBar().setUnitIncrement(20);
        scroll_.getVerticalScrollBar().setBlockIncrement(100);
        border.add(scroll_, BorderLayout.CENTER);
        
        // init
        if (profile_.isAllocFixed())
        {
            setPool();
        }
        else
        {
            stateChanged(null);
        }
        
        return base;
    }
    
    /**
     * Spinner changed (pool amount)
     */
    public void stateChanged(ChangeEvent e)
    {
        if (!amount_.isValidData()) return;
        
        profile_.setPrizePool(amount_.getValue(), true);
        game_.setClockCash(amount_.getValue());
        int nHouseCut = amount_.getValue() - profile_.getPoolAfterHouseTake(amount_.getValue());
        if (housecut_ != null)
        {
            housecut_.setText(PropertyConfig.getMessage("msg.housecut", nHouseCut));
        }
        setPool();
    }
    
    /**
     * Set pool html based on settings
     */
    private void setPool()
    {
        html_.setText(profileHtml_.toHTMLSpots());
        scroll_.revalidate();
        html_.setCaretPosition(0); // scroll to top
    }
}
