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
 * OptionTextArea.java
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
public class OptionTextArea extends DDOption implements PropertyChangeListener
{
    //static Logger logger = LogManager.getLogger(OptionTextArea.class);
    
    private DDLabel label_;
    private DDTextArea text_;
    private String sDefault_;
    private JScrollPane scroll_;

    /** 
     * Creates a new instance of OptionTextArea 
     */
    public OptionTextArea(String sPrefNode, String sName, String sStyle,
                          String sBevelStyle, TypedHashMap map, int nLengthLimit, String sRegExp, int nRows, int nWidth)
    {
        super(sPrefNode, sName, sStyle, map);
        sDefault_ = PropertyConfig.getRequiredStringProperty(getDefaultKey());
        
        // base
        setBorderLayoutGap(0, 8);
        
        // text
        text_ = new DDTextArea(GuiManager.DEFAULT, STYLE);
        text_.setRows(nRows);
        text_.setTextLengthLimit(nLengthLimit);
        if (sRegExp != null) text_.setRegExp(sRegExp);
        Dimension pref = text_.getPreferredSize(); // get size before set text
        
        // set text, save to map
        resetToPrefs();
        saveToMap();        
        
        // create scroll and add listeners
        scroll_ = new DDScrollPane(text_, STYLE, sBevelStyle, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setPreferredSize(new Dimension(nWidth,pref.height+4));
        scroll_.setOpaque(false);
        text_.setScrollPane(scroll_);
        text_.setTabChangesFocus(true);
        if (sBevelStyle != null) text_.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        text_.addPropertyChangeListener("value", this);
        text_.addMouseListener(this);
        text_.setWrapStyleWord(true);
        text_.setLineWrap(true);
        text_.setCaretPosition(0);

        // label
        label_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        label_.setVerticalAlignment(SwingConstants.TOP);
        label_.setText(getLabel());
        label_.addMouseListener(this);
        
        // put it all together
        add(GuiUtils.CENTER(scroll_), BorderLayout.EAST);
        add(label_, BorderLayout.CENTER);
    }
    
    /**
     * Get the spinner
     */
    public DDTextArea getTextArea()
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
        label_.setEnabled(b);
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
        text_.setText(map_.getString(sName_, sDefault_));
    }
}
