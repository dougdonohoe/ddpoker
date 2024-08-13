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
 * DDMenu.java
 *
 * Created on January 12, 2002, 5:57 PM
 */

package com.donohoedigital.gui;


import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Should be identical to DDMenuItem since JMenu extends JMenuItem
 * 
 * @author  Doug Donohoe
 */
public class DDMenu extends JMenu implements DDHasLabelComponent 
{
    Border swingBorder_;
    /** 
     * Creates a new instance of DDMenu 
     */
    public DDMenu() {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    public DDMenu(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    public DDMenu(String sName, String sStyle)
    {
        super();
        init(sName, sStyle);
    }
    
    private void init(String sName, String sStyle)
    {
        GuiManager.init(this, sName, sStyle);
        swingBorder_ = getBorder();
    }
        
    
    public void reinit(String sName, String sStyle)
    {
        GuiManager.init(this, sName, sStyle);
    }

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    public void paintComponent(Graphics g1)
    {
	Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }
    
    public String getType() 
    {
        return "menu";
    }
    
    boolean bDisplayOnly_ = false;
    
    /**
     * Set item display only - useful for
     * informational menu items
     */
    public void setDisplayOnly(boolean b)
    {
        bDisplayOnly_ = b;
        setEnabled(!b);
        if (b && getIcon() != null)
        {
            setDisabledIcon(getIcon());
        }
        
        if (b)
        {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        else
        {
            setHorizontalAlignment(SwingConstants.LEFT);
        }
    }
    
    /**
     * Is menuitem display only?
     */
    public boolean isDisplayOnly()
    {
        return bDisplayOnly_;
    }

    /**
     * Overriden to set disabled icon
     * to icon also if isDisplayOnly() true
     */
    public void setIcon(Icon icon)
    {
        super.setIcon(icon);
        if (bDisplayOnly_)
        {
            setDisabledIcon(icon);
        }
    }
    
    Border newBorder_;
    
    public void setBorder(Border b)
    {
        newBorder_ = b;
        super.setBorder(b);
    }
    
    /**
     * Overridden to paint user border if set
     */
    protected void paintBorder(Graphics g) {    
        if (isBorderPainted()) {
            if (this.getModel().isArmed())
            {
                swingBorder_.paintBorder(this, g, 0, 0, getWidth(), getHeight());
            }
            else
            {
                newBorder_.paintBorder(this, g, 0, 0, getWidth(), getHeight());
            }
        }
    }
}
