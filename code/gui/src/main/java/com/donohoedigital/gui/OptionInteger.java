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
 * OptionInteger.java
 *
 * Created on April 16, 2003, 2:01 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.config.PropertyConfig;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 *
 * @author  donohoe
 */
public class OptionInteger extends DDOption implements ChangeListener
{
    //static Logger logger = LogManager.getLogger(OptionInteger.class);
    
    private DDLabel label_;
    private DDLabel leftlabel_;
    private DDNumberSpinner spinner_;
    private Integer nDefault_;

    /** 
     * Creates a new instance of OptionInteger 
     */
    public OptionInteger(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, Integer nDefaultOverride,
                    int nMin, int nMax, int nWidth)
    {
        this(sPrefNode, sName, sStyle, map, nDefaultOverride,  nMin, nMax, nWidth, false);
    }
    
    /** 
     * Creates a new instance of OptionInteger 
     */
    public OptionInteger(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, Integer nDefaultOverride,
                    int nMin, int nMax, int nWidth, boolean bEditable)
    {
        this(sPrefNode, sName, sStyle, map, nDefaultOverride,  nMin, nMax, nWidth, bEditable, false);
    }

    public OptionInteger(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, Integer nDefaultOverride,
                    int nMin, int nMax, int nWidth, boolean bEditable, boolean bAlignTop)
    {
        super(sPrefNode, sName, sStyle, map);
        if (nDefaultOverride != null)
        {
            nDefault_ = nDefaultOverride;
        }
        else
        {
            nDefault_ = PropertyConfig.getRequiredIntegerProperty(getDefaultKey());
        }
        
        int nStep = PropertyConfig.getIntegerProperty("option." + sName_ + ".step", 1);
        int nBigStep = PropertyConfig.getIntegerProperty("option." + sName_ + ".bigstep", nStep);
        
        // base
        setBorderLayoutGap(0, 8);

        // spinner
        spinner_ = new DDNumberSpinner(nMin, nMax, nStep, GuiManager.DEFAULT, STYLE);
        Dimension pref = spinner_.getPreferredSize(); // tweak size
        if (nWidth > 0) spinner_.setPreferredSize(new Dimension(nWidth,pref.height-4));
        spinner_.setValue(nDefaultOverride != null ? nDefaultOverride :
                                prefs_.getInt(sName_, nDefault_));
        spinner_.setBigStep(nBigStep);
        spinner_.addChangeListener(this);
        spinner_.addMouseListener(this);
        spinner_.setEditable(bEditable);
        GuiUtils.addMouseListenerChildren(spinner_, this);

        // label
        String sLabel = getLabel();
        if (sLabel.length() > 0)
        {
            label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            if (bAlignTop) label_.setVerticalAlignment(SwingConstants.TOP);
            label_.setText(getLabel());
            label_.addMouseListener(this);
        }

        // see if we have a left label
        String sLeft = getLeftLabel();
        if (sLeft != null)
        {
            leftlabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            leftlabel_.setText(sLeft);
            leftlabel_.addMouseListener(this);
        }
        
        // save
        saveToMap();
        
        // put it all together
        
        if (label_ != null) add(label_, BorderLayout.CENTER);
        if (leftlabel_ != null) 
        {
            DDPanel left = new DDPanel();
            left.setBorderLayoutGap(0, getLeftGap());
            left.add(leftlabel_, BorderLayout.WEST);
            left.add(bAlignTop ? GuiUtils.NORTH(spinner_):GuiUtils.CENTER(spinner_), BorderLayout.CENTER);
            add(left, BorderLayout.WEST);
        }
        else
        {
            add(bAlignTop ? GuiUtils.NORTH(spinner_):GuiUtils.CENTER(spinner_), BorderLayout.WEST);
        }
    }
    
    /**
     * Get the spinner
     */
    public DDNumberSpinner getSpinner()
    {
        return spinner_;
    }

    /**
     * Get the label
     */
    public JComponent getLabelComponent()
    {
        if (leftlabel_ != null) return leftlabel_;
        return label_;
    }

    /**
     * Is valid?
     */
    public boolean isValidData()
    {
        return spinner_.isValidData();
    }
    
    /**
     * Set spinner text field editable (not editable by default)
     */
    public void setEditable(boolean b)
    {
        spinner_.setEditable(b);
    }
    
    /**
     * Change maximum
     */
    public void setMaximum(int n)
    {
        spinner_.setMax(n);
    }
    
    /**
     * set disabled
     */
    public void setEnabled(boolean b)
    {
        spinner_.setEnabled(b);
        if (label_ != null) label_.setEnabled(b);
        if (leftlabel_ != null) leftlabel_.setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        return spinner_.isEnabled();
    }
    
    /**
     * Only disabled spinner
     */
    public void setEnabledEmbedded(boolean b)
    {
        spinner_.setEnabled(b);
    }
    
    /** 
     * Invoked when spinner changed
     */
    public void stateChanged(ChangeEvent e) 
    {
        prefs_.putInt(sName_, spinner_.getValue());
        saveToMap();
        fireStateChanged();
    }
    
    /**
     * Save value to map
     */
    public void saveToMap()
    {
        map_.setInteger(sName_, spinner_.getValue());
    }
    
    /** reset to default value (triggers stateChanged())
     *
     */
    public void resetToDefault()
    {
        spinner_.setValue(nDefault_);
    }

    public void resetToPrefs()
    {
        spinner_.setValue(prefs_.getInt(sName_, nDefault_));
    }

    /**
     * reset to value in map (triggers stateChanged())
     */
    public void resetToMap()
    {
        spinner_.setValue(map_.getInteger(sName_, nDefault_));
    }
    
}
