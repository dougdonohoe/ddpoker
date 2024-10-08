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
 * OptionBoolean.java
 *
 * Created on April 16, 2003, 2:01 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  donohoe
 */
public class OptionBoolean extends DDOption implements ActionListener, ChangeListener
{
    //static Logger logger = LogManager.getLogger(OptionBoolean.class);
    
    private DDCheckBox box_;
    private boolean bDefault_;
    private boolean bPaintIconBackground_;
    private DDOption extra_ = null;

    /** 
     * Creates a new instance of OptionBoolean 
     */
    public OptionBoolean(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, boolean bPaintIconBackground)
    {
        this(sPrefNode, sName, sStyle, map, bPaintIconBackground, null);
    }
    
    /** 
     * Creates a new instance of OptionBoolean. Extra option is added to EAST
     * and disabled when this checkbox is not selected
     */
    public OptionBoolean(String sPrefNode, String sName, String sStyle,
                    TypedHashMap map, boolean bPaintIconBackground,
                    DDOption extra)
    {
        super(sPrefNode, sName, sStyle, map);
        bDefault_ = PropertyConfig.getRequiredBooleanProperty(getDefaultKey());
        bPaintIconBackground_ = bPaintIconBackground;
        extra_ = extra;

        DDPanel base = new DDPanel();

        box_ = new DDCheckBox(GuiManager.DEFAULT, STYLE);
        box_.setIconBackgroundPainted(bPaintIconBackground_);
        box_.setText(getLabel());
        box_.setSelected(prefs_.getBoolean(sName_, bDefault_));
        saveToMap();
        box_.addActionListener(this);
        box_.addMouseListener(this);
        box_.addChangeListener(this);
        
        base.add(box_, BorderLayout.CENTER);
        if (extra_ != null) 
        {
            base.add(extra_, BorderLayout.EAST);
        
            // call to force extra component to be correct state
            stateChanged(null);
        }
        
        add(base, BorderLayout.WEST);
    }
    
    public DDCheckBox getCheckBox()
    {
        return box_;
    }

    /**
     * Set button enabled
     */
    public void setEnabled(boolean b)
    {
        box_.setEnabled(b);
        if (extra_ != null) extra_.setEnabled(b);
    }
    
    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        return box_.isEnabled();
    }
    
    /** Invoked when an action occurs.
     *
     */
    public void actionPerformed(ActionEvent e)
    {
        prefs_.putBoolean(sName_, box_.isSelected());
        saveToMap();
        fireStateChanged();
    }
    
    /**
     * Save value to map
     */
    public void saveToMap()
    {
        map_.setBoolean(sName_, box_.isSelected() ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /** reset to default value
     *
     */
    public void resetToDefault()
    {
        box_.setSelected(bDefault_);
        actionPerformed(null);
    }

    public void resetToPrefs()
    {
        box_.setSelected(prefs_.getBoolean(sName_, bDefault_));
        actionPerformed(null);
    }

    /**
     * Used to enabled when button selected
     */
    public void stateChanged(ChangeEvent e)
    {
        if (extra_ != null) extra_.setEnabledEmbedded(box_.isSelected());
    }
    
    /** 
     * reset to value in map
     */
    public void resetToMap()
    {
        box_.setSelected(map_.getBoolean(sName_, bDefault_));
        actionPerformed(null);
    }
    
}
