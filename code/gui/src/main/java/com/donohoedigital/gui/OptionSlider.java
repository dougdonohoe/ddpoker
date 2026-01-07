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
 * OptionSlider.java
 *
 * Created on June 05, 2003, 6:06 PM
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
public class OptionSlider extends DDOption implements ChangeListener
{
    //static Logger logger = LogManager.getLogger(OptionSlider.class);
    
    private DDLabel label_;
    private DDSlider slider_;
    private Integer nDefault_;

    /** 
     * Creates a new instance of OptionSlider 
     */
    public OptionSlider(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, Integer nDefaultOverride,
                    int nMin, int nMax, int nWidth)
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
        
        int nStep = PropertyConfig.getIntegerProperty("option." + sName_ + ".step", 10);
        
        // base
        setBorderLayoutGap(0, 8);
        
        // slider
        slider_ = new DDSlider(GuiManager.DEFAULT, STYLE);
        slider_.setMaximum(nMax);
        slider_.setMinimum(nMin);
        slider_.setMajorTickSpacing(nStep);
        slider_.setMinorTickSpacing(nStep);
        slider_.setPaintTicks(false);
        slider_.setValue(nDefaultOverride != null ? nDefaultOverride.intValue() : prefs_.getInt(sName_, nDefault_.intValue()));

        Dimension pref = slider_.getPreferredSize(); // tweak size
        slider_.setPreferredSize(new Dimension(nWidth,pref.height));
        slider_.addChangeListener(this);
        slider_.addMouseListener(this);

        // label
        label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        label_.setText(getLabel());
        label_.addMouseListener(this);
        saveToMap();
        
        // put it all together
        add(GuiUtils.CENTER(slider_), BorderLayout.EAST);
        add(label_, BorderLayout.CENTER);
    }

    /**
     * Get the slider
     */
    public DDSlider getSlider()
    {
        return slider_;
    }
    
    /**
     * Get the label
     */
    public JComponent getLabelComponent()
    {
        return label_;
    }

    /**
     * set disabled
     */
    public void setEnabled(boolean b)
    {
        slider_.setEnabled(b);
        label_.setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        return slider_.isEnabled();
    }
    
    /**
     * Only disabled slider
     */
    public void setEnabledEmbedded(boolean b)
    {
        slider_.setEnabled(b);
        label_.setEnabled(b);
    }
    
    /** 
     * Invoked when slider changed
     */
    public void stateChanged(ChangeEvent e) 
    {
        prefs_.putInt(sName_, slider_.getValue());
        saveToMap();
        fireStateChanged();
    }
    
    /**
     * Save value to map
     */
    public void saveToMap()
    {
        map_.setInteger(sName_, slider_.getValue());
    }
    
    /** reset to default value
     *
     */
    public void resetToDefault()
    {
        slider_.setValue(nDefault_.intValue());
        stateChanged(null);
    }

    public void resetToPrefs()
    {
        slider_.setValue(prefs_.getInt(sName_, nDefault_.intValue()));
        stateChanged(null);
    }

    /**
     * reset to value in map
     */
    public void resetToMap()
    {
        slider_.setValue(map_.getInteger(sName_, nDefault_.intValue()));
        stateChanged(null);
    }
    
}
