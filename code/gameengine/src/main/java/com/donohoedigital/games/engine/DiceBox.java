/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * DiceBox.java
 *
 * Created on January 18, 2003, 3:22 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.ImageConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GamePlayer;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 *
 * @author  Doug Donohoe
 */
public class DiceBox extends DDPanel implements ActionListener 
{
    //static Logger logger = LogManager.getLogger(DiceBox.class);
    
    int nDieSize_;
    int nSum_ = 0;
    Integer die1_, die2_;
    Integer nMod_ = null;
    DiceRolled notify_;
    GamePlayer player_;
    ImageComponent box_;
    DDButton button_;
    DDLabel result_;
    
    /** 
     * Creates a new instance of DiceBox, default layout to east
     */
    public DiceBox(DiceRolled notify, GamePlayer player, int nDieSize, Integer nMod, 
                                int hgap, int vgap) 
    {
        this(notify, player, nDieSize, nMod, hgap, vgap, false, true);
    }
    
    /**
     * New DiceBox
     */
    public DiceBox(DiceRolled notify, GamePlayer player, int nDieSize, Integer nMod, 
                                int hgap, int vgap, boolean bVertical, boolean bCreateDiceButton)
    {
        player_ = player;
        notify_ = notify;
        nDieSize_ = nDieSize;
        nMod_ = nMod;
        if (bVertical)
        {
            setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, hgap, vgap, VerticalFlowLayout.CENTER));
        }
        else
        {
            setLayout(new FlowLayout(FlowLayout.CENTER,hgap,vgap));
        }
        box_ = new ImageComponent("dicebox", 1.0d);
        box_.setLayout(new XYLayout());
        add(box_);
        
        // button/roll results
        DDPanel stuff = new DDPanel();
        stuff.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER,0,2,VerticalFlowLayout.CENTER));
        add(stuff);
        
        // rolldice button
        if (bCreateDiceButton)
        {
            button_ = new DDButton("rolldice", "DiceBox");
            GuiUtils.addKeyAction(button_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                    "rolldiceenter", new GuiUtils.InvokeButton(button_), 
                    KeyEvent.VK_SPACE, 0);
            ImageIcon icon = getDiceIcon();
            button_.setIcon(icon);
            button_.setBorderGap(2,2,2,2);
            button_.addActionListener(this);
            stuff.add(button_);
        }
        
        // result label
        result_ = new DDLabel(GuiManager.DEFAULT, "DiceBox");
        result_.setText("  "); // to get size
        stuff.add(result_);
    }
    
    /**
     * Return icon used for dice button
     */
    public static ImageIcon getDiceIcon()
    {
        return ImageConfig.getImageIcon("dicesmall");
    }
    
    /**
     * Return text on dice button
     */
    public static String getDiceText()
    {
        return PropertyConfig.getMessage("button.rolldice.label");
    }
    
    /**
     * Set mod 
     */
    public void setMod(Integer nMod)
    {
        nMod_ = nMod;
    }
    
    /**
     * Get mod
     */
    public int getMod()
    {
        return nMod_;
    }
    
    /**
     * Clear dice
     */
    public void clearDice()
    {
        result_.setText(" ");
        box_.removeAll();
        validate();
        repaint();
    }
    
    /**
     * Dice button action - calls rollDice()
     */
    public void actionPerformed(ActionEvent e) {
        rollDice();
    }
    

    /**
     * Roll dice
     */
    public void rollDice()
    {
        rollDice(null, null);
    }
    
    /**
     * Pass in results instead of rolling them - used
     * when results already known, typically for display
     * purposes
     */
    public void rollDice(Integer die1, Integer die2)
    {        
        die1_ = (die1 != null) ? die1 : DiceRoller.rollDie(6);
        die2_ = (die2 != null) ? die2 : DiceRoller.rollDie(6);
        
        nSum_ = die1_ + die2_;
        int nTotal = nSum_;
        
        if (nMod_ != null)
        {
            nTotal = nSum_ + nMod_;
            result_.setText(PropertyConfig.getMessage("msg.dicebox.result.mod",
                                                nSum_,
                                                nMod_,
                                                nTotal));
        }
        else
        {
            result_.setText(PropertyConfig.getMessage("msg.dicebox.result",
                                                nSum_));
        }
        
        DieImage img1 = new DieImage(die1_, nDieSize_, nDieSize_);
        DieImage img2 = new DieImage(die2_, nDieSize_, nDieSize_);
        
        int INDENT = 10;
        
        int yRange = (box_.getHeight() - img1.getHeight()) - (2*INDENT);
        int y1 = DiceRoller.rollDie(yRange) + INDENT - 1;
        int y2 = DiceRoller.rollDie(yRange) + INDENT - 1;
        
        int xRange = (box_.getWidth() - img2.getWidth()) - (2 * INDENT);
        
        boolean bOkay = false;
        int x1 = 0;
        int x2 = 0;
        while (!bOkay)
        {
            x1 = DiceRoller.rollDie(xRange) + INDENT - 1;
            x2 = DiceRoller.rollDie(xRange) + INDENT - 1;
            
            // if x's are more than a width apart, its okay
            if (Math.abs(x1 - x2) > (img1.getWidth()+1))
            {
                bOkay = true;
            }
            // if not, the y's must be more than a height apart
            else
            {
                if (Math.abs(y1 - y2) > (img1.getHeight()+1))
                {
                    bOkay = true;
                }
            }
        }

        XYConstraints xy1 = new XYConstraints(x1,
                                              y1, 
                                              img1.getWidth(), 
                                              img1.getHeight());
        XYConstraints xy2 = new XYConstraints(x2, 
                                              y2,   
                                              img2.getWidth(), 
                                              img2.getHeight());
        
        box_.removeAll();
        box_.add(img1, xy1);
        box_.add(img2, xy2);
         
        validate();
        repaint();
     
        //AudioConfig.playFX("dice", 0);
        
        if (notify_ != null) notify_.dieRolled(this, player_, nTotal);
    }
    
    /**
     * Get sum of the dice (w/out modifier)
     */
    public int getSum()
    {
        return nSum_;
    }
    
    /**
     * return die1
     */
    public Integer getDie1()
    {
        return die1_;
    }
    
    /**
     * return die2
     */
    public Integer getDie2()
    {
        return die2_;
    }
    
    /**
     * Get dice button
     */
    public DDButton getDiceButton()
    {
        return button_;
    }
    
    /** 
     * Interface to notify someone when dice have been rolled.
     * The total returned is that of the dice + mod.
     */
    public static interface DiceRolled
    {
        public void dieRolled(DiceBox box, GamePlayer player, int nTotal);
    }
}
