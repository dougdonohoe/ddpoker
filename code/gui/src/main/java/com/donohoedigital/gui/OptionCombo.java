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
 * OptionCombo.java
 *
 * Created on April 13, 2005, 9:09 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
/**
 *
 * @author  donohoe
 */
public class OptionCombo extends DDOption implements ItemListener
{
    //static Logger logger = LogManager.getLogger(OptionCombo.class);
    private DDLabel label_;
    private DDComboBox combo_;
    private String sDefault_;

    /** 
     * Creates a new instance of OptionCombo 
     */
    public OptionCombo(String sPrefNode, String sName, String sDataElement, String sStyle,
                    TypedHashMap map, int nWidth, boolean bLabel)
    {
        super(sPrefNode, sName, sStyle, map);
        sDefault_ = PropertyConfig.getRequiredStringProperty(getDefaultKey());

        // base
        setBorderLayoutGap(0, 8);

        // text
        combo_ = new DDComboBox(sDataElement, STYLE);
        resetToPrefs();
        saveToMap();
        Dimension pref = combo_.getPreferredSize(); // tweak size
        combo_.setPreferredSize(new Dimension(nWidth, pref.height));
        combo_.addItemListener(this);
        combo_.addMouseListener(this);

        // label
        if (bLabel)
        {
            label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            label_.setText(getLabel());
            label_.addMouseListener(this);

            // put it all together
            add(GuiUtils.CENTER(combo_), BorderLayout.EAST);
            add(label_, BorderLayout.CENTER);
        }
        else
        {
            add(combo_, BorderLayout.CENTER);
            setPreferredSize(combo_.getPreferredSize());
        }
    }
    
    /**
     * Get the spinner
     */
    public DDComboBox getComboBox()
    {
        return combo_;
    }

    /**
     * Get the label
     */
    public JComponent getLabelComponent()
    {
        return label_;
    }

    /**
     * Is valid?
     */
    public boolean isValidData()
    {
        return combo_.isValidData();
    }
    
    /**
     * Set display only
     */
    public void setDisplayOnly(boolean b)
    {
        //TODO: this doesn't currently do anything
        //combo_.setEditable(!b);
    }
    
    /**
     * set disabled
     */
    public void setEnabled(boolean b)
    {
        combo_.setEnabled(b);
        if (label_ != null) label_.setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        return combo_.isEnabled();
    }
    
    /**
     * Only disabled spinner
     */
    public void setEnabledEmbedded(boolean b)
    {
        combo_.setEnabled(b);
    }

    /**
     * Selected item changed
     */
    public void itemStateChanged(ItemEvent e)
    {
        fireStateChanged(); // for validation listeners
        if (!combo_.isValid()) return;
        prefs_.put(sName_, (String)combo_.getSelectedValue());
        saveToMap();
    }


    /**
     * Save value to map
     */
    public void saveToMap()
    {
        //logger.debug("Setting map " + sName_ +  " from text  "+ combo_.getText().trim());
        map_.setString(sName_, (String)combo_.getSelectedValue());
    }
    
    /** reset to default value
     *
     */
    public void resetToDefault()
    {
        combo_.setSelectedItem(sDefault_);
    }

    public void resetToPrefs()
    {
        combo_.setSelectedItem(prefs_.get(sName_, sDefault_));
    }

    /**
     * reset to value in map
     */
    public void resetToMap()
    {
        //only update if changed (faster to compare then repaint text)
        String value = map_.getString(sName_, sDefault_);
        String current = (String) combo_.getSelectedValue();
        if (!value.equals(current))
        {
            //logger.debug("Setting text " + sName_ + " from map " + value);
            combo_.setSelectedItem(value);
        }
    }
}
