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
 * GlassButton.java
 *
 * Created on March 22, 2005, 1:40 PM
 */

package com.donohoedigital.gui;

import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.plaf.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GlassButton extends DDButton
{
    static Logger logger = Logger.getLogger(GlassButton.class);

    Color cDown_;
    Color cOver_;
    Color cFocusBG_;

    /**
     * Creates a new instance of GlassButton. BevelStyle passed in is
     * used to create a DDBevelBorder, used to define colors
     * for proper inset look.
     */
    public GlassButton(String sName, String sStyleName) {
        super(sName, sStyleName);
        setAlwaysAntiAlias(true);
    }

    /**
     * init - get image icons and adjust other items
     */
    protected void init(String sName, String sStyleName)
    {
        super.init(sName, sStyleName);
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setBorderGap(5,8,3,8);
        setDisableMode(DDButton.DISABLED_NONE); // GlassButtonUI handles this
    }

    /**
     * set border size
     */
    public void setBorderGap(int top, int left, int bottom, int right)
    {
        setBorder(BorderFactory.createEmptyBorder(top,left,bottom,right));
    }

    /**
     * Get UI
     */
    protected ComponentUI createUI()
    {
        return GlassButtonUI.createUI(this);
    }

    /**
     * set down color
     */
    public void setDownColor(Color c)
    {
        cDown_ = c;
    }

    /**
     * get down color
     */
    public Color getDownColor()
    {
        return cDown_;
    }

    /**
     * set over color
     */
    public void setOverColor(Color c)
    {
        cOver_ = c;
    }

    /**
     * get over color
     */
    public Color getOverColor()
    {
        return cOver_;
    }

    /**
     * set focus bg color
     */
    public void setFocusColor(Color c)
    {
        cFocusBG_ = c;
    }

    /**
     * get focus bg color
     */
    public Color getFocusColor()
    {
        return cFocusBG_;
    }
}
