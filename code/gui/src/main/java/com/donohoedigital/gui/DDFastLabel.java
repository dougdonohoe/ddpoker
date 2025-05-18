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
 * DDFastLabel.java
 *
 * Created on May 20, 2004, 4:45 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  donohoe
 */
public class DDFastLabel extends DDLabel
{
    private String sText_ = "";
    
    /** 
     * Creates a new instance of DDLabel 
     */
    public DDFastLabel() {
        super();
    }
    
    public DDFastLabel(String sName)
    {
        super(sName);
    }

    public DDFastLabel(String sName, String sStyle)
    {
        super(sName, sStyle);
    }

    Insets insets_ = new Insets(0,0,0,0);
    
    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;
        
        if (sText_ == null || sText_.length() == 0) return;
        
        TextUtil util = new TextUtil(g, getFont(), sText_, .3f);
        getInsets(insets_);

        
        float fx = insets_.left;
        float fy = insets_.top;
        
        int nAlign = getHorizontalAlignment();
        if (nAlign == SwingConstants.CENTER)
        {
            fx += getWidth() / 2.0f;
            fy += getHeight() / 2.0f;
        }
        else if (nAlign == SwingConstants.RIGHT)
        {
            fx += getWidth() - insets_.right - 2;
        }

        util.prepareDraw(fx, fy, null, 1.0d, true);
        util.drawString(getForeground(), null, nAlign);
        util.finishDraw();        
    }

    
    public void setText(String s)
    {
        sText_ = s;
        repaint();
    }
    
    public String getText()
    {
        return sText_;
    }
}
