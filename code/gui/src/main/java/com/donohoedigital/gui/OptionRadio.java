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
 * OptionRadio.java
 *
 * Created on April 16, 2003, 2:01 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  donohoe
 */
public class OptionRadio extends DDOption implements ActionListener, ChangeListener
{
    //static Logger logger = Logger.getLogger(OptionRadio.class);
    
    private DDRadioButton radio_;
    private String sOptionName_;
    private DDLabel leftlabel_;
    protected int nValue_;
    private boolean bDefault_;
    private DDOption extra_ = null;
    
    /** 
     * Creates a new instance of OptionRadio 
     */
    public OptionRadio(String sPrefNode, String sOptionName, String sStyle,
                    TypedHashMap map, String sRadioName, ButtonGroup group,
                    int nValue)
    {
        this(sPrefNode, sOptionName, sStyle, map, sRadioName, group, nValue, null);
    }
    
    /** 
     * Creates a new instance of OptionRadio.  Extra option is added to EAST
     * and disabled when this radio is not selected
     */
    public OptionRadio(String sPrefNode, String sOptionName, String sStyle,
                    TypedHashMap map, String sRadioName, ButtonGroup group,
                    int nValue, DDOption extra)
    {
        super(sPrefNode, sRadioName, sStyle, map);
        sOptionName_ = sOptionName;
        nValue_ = nValue;
        extra_ = extra;
        
        bDefault_ = PropertyConfig.getBooleanProperty(getDefaultKey(), false);
        DDPanel base = new DDPanel();

        radio_ = new DDRadioButton(GuiManager.DEFAULT, STYLE);
        group.add(radio_);
        radio_.setText(getLabel());
        
        resetToPrefs();
        stateChanged(null); // call to force setting of extra
        saveToMap();
        radio_.addActionListener(this);
        radio_.addMouseListener(this);
        radio_.addChangeListener(this);
        
        base.add(radio_, BorderLayout.CENTER);
        if (extra_ != null) base.add(extra_, BorderLayout.EAST);

        // see if we have a left label
        String sLeft = getLeftLabel();
        if (sLeft != null)
        {
            leftlabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
            leftlabel_.setText(sLeft);
            leftlabel_.addMouseListener(this);
        }

        if (leftlabel_ != null)
        {
            base.add(leftlabel_, BorderLayout.WEST);
        }

        add(base, BorderLayout.WEST);
    }
    
    public DDRadioButton getRadioButton()
    {
        return radio_;
    }

    /**
     * Set radio enabled
     */
    public void setEnabled(boolean b)
    {
        radio_.setEnabled(b);
        if (extra_ != null) extra_.setEnabled(b);
        if (leftlabel_ != null) leftlabel_.setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        return radio_.isEnabled();
    }
    
    /** Invoked when an action occurs.
     *
     */
    public void actionPerformed(ActionEvent e)
    {
        if (radio_.isSelected()) 
        {
            prefs_.putInt(sOptionName_, nValue_);
            saveToMap();
            fireStateChanged();
        }
    }
    
    /**
     * Save value to map
     */
    public void saveToMap()
    {
        if (radio_.isSelected())
        {
            map_.setInteger(sOptionName_, new Integer(nValue_));
        }
    }
    
    /** reset to default value
     *
     */
    public void resetToDefault()
    {
        if (bDefault_)
        {
            radio_.setSelected(true);
            actionPerformed(null);
        }
    }

    public void resetToPrefs()
    {
        int nSaved = prefs_.getInt(sOptionName_, -1);
        radio_.setSelected(nSaved == nValue_ || (nSaved == -1 && bDefault_));
    }

    /**
     * Used to enabled when button selected
     */
    public void stateChanged(ChangeEvent e)
    {
        if (extra_ != null) extra_.setEnabledEmbedded(radio_.isSelected());
    }
    
    /** 
     * reset to value in map
     */
    public void resetToMap()
    {
        if (map_.getInteger(sOptionName_, (bDefault_ ? nValue_ : -1)) == nValue_)
        {
            radio_.setSelected(true);
            actionPerformed(null);
        }
    }
    
}
