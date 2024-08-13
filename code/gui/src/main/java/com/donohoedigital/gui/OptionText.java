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
 * OptionText.java
 *
 * Created on April 16, 2003, 2:01 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import java.awt.*;
import java.beans.*;
/**
 *
 * @author  donohoe
 */
public class OptionText extends DDOption implements PropertyChangeListener
{
    //static Logger logger = Logger.getLogger(OptionText.class);
    private DDLabel label_;
    private DDTextField text_;
    private String sDefault_;

    public OptionText(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, int nLengthLimit, String sRegExp, int nWidth)
    {
        this(sPrefNode, sName, sStyle, map, nLengthLimit, sRegExp, nWidth, true);
    }

    /** 
     * Creates a new instance of OptionText 
     */
    public OptionText(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, int nLengthLimit, String sRegExp, int nWidth, boolean bLabel)
    {
        this(sPrefNode, sName, sStyle, null, map, nLengthLimit, sRegExp, nWidth, bLabel);
    }

    public OptionText(String sPrefNode, String sName, String sStyle, String sBevelStyle,
                    TypedHashMap map, int nLengthLimit, String sRegExp, int nWidth, boolean bLabel)
    {
        super(sPrefNode, sName, sStyle, map);
        sDefault_ = PropertyConfig.getRequiredStringProperty(getDefaultKey());

        // base
        setBorderLayoutGap(0, 8);

        // text
        text_ = new DDTextField(GuiManager.DEFAULT, STYLE, sBevelStyle);
        text_.setTextLengthLimit(nLengthLimit);
        if (sRegExp != null) text_.setRegExp(sRegExp);
        resetToPrefs();
        saveToMap();
        Dimension pref = text_.getPreferredSize(); // tweak size
        text_.setPreferredSize(new Dimension(nWidth, pref.height));
        text_.addPropertyChangeListener("value", this);
        text_.addMouseListener(this);

        // label
        if (bLabel)
        {
            label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            label_.setText(getLabel());
            label_.addMouseListener(this);

            // put it all together
            add(GuiUtils.CENTER(text_), BorderLayout.EAST);
            add(label_, BorderLayout.CENTER);
        }
        else
        {
            add(text_, BorderLayout.CENTER);
            setPreferredSize(text_.getPreferredSize());
        }
    }
    
    /**
     * Get the spinner
     */
    public DDTextField getTextField()
    {
        return text_;
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
        return text_.isValidData();
    }
    
    /**
     * Set display only
     */
    public void setDisplayOnly(boolean b)
    {
        text_.setDisplayOnly(b);
    }
    
    /**
     * set disabled
     */
    public void setEnabled(boolean b)
    {
        text_.setEnabled(b);
        if (label_ != null) label_.setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        return text_.isEnabled();
    }
    
    /**
     * Only disabled spinner
     */
    public void setEnabledEmbedded(boolean b)
    {
        text_.setEnabled(b);
    }

    /** 
     * text field change
     */
    public void propertyChange(PropertyChangeEvent evt) 
    {
        fireStateChanged(); // for validation listeners
        if (!text_.isValidData()) return;
        prefs_.put(sName_, text_.getText().trim());
        saveToMap();
    }
    
    
    /**
     * Save value to map
     */
    public void saveToMap()
    {
        //logger.debug("Setting map " + sName_ +  " from text  "+ text_.getText().trim());
        map_.setString(sName_, text_.getText().trim());
    }
    
    /** reset to default value
     *
     */
    public void resetToDefault()
    {
        text_.setText(sDefault_);
    }

    public void resetToPrefs()
    {
        text_.setText(prefs_.get(sName_, sDefault_));
    }

    /**
     * reset to value in map
     */
    public void resetToMap()
    {
        //only update if changed (faster to compare then repaint text)
        String value = map_.getString(sName_, sDefault_);
        String current = text_.getText();
        if (!value.equals(current))
        {
            //logger.debug("Setting text " + sName_ + " from map " + value);
            text_.setText(value);
        }
    }
}
