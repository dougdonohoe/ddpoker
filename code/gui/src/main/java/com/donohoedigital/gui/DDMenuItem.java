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
 * DDMenuItem.java
 *
 * Created on January 10, 2002, 11:39 AM
 */

package com.donohoedigital.gui;


import com.donohoedigital.base.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Should be identical to DDMenu since JMenu extends JMenuItem
 *
 * @author  Doug Donohoe
 */
public class DDMenuItem extends JMenuItem implements DDHasLabelComponent 
{
    private Border BORDER_INDENT1 = BorderFactory.createEmptyBorder(1, 20, 1, 1);
    private Border BORDER_INDENT2 = BorderFactory.createEmptyBorder(1, 40, 1, 1);
    private Border BORDER_TITLE = BorderFactory.createEmptyBorder(1, 1, 1, 30);

    public static final int MODE_TITLE = 1;
    public static final int MODE_NORMAL = 2;
    public static final int MODE_INDENT1 = 3;
    public static final int MODE_INDENT2 = 4;

    Border swingBorder_;
    /** 
     * Creates a new instance of DDMenuItem 
     */
    public DDMenuItem() {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    public DDMenuItem(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    public DDMenuItem(String sName, String sStyle)
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

    public void setDisplayMode(int nType)
    {
        switch (nType)
        {
            case MODE_TITLE:
                setBorder(BORDER_TITLE);
                // FIX: detemine workaround for JDK 1.6 JMenuItem bug
                if (!Utils.IS16) setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(Color.white);
                setDisplayOnly(true);
                break;

            case MODE_NORMAL:
                if (!Utils.IS16) setHorizontalAlignment(SwingConstants.LEFT);
                break;

            case MODE_INDENT1:
                setBorder(BORDER_INDENT1);
                if (!Utils.IS16) setHorizontalAlignment(SwingConstants.LEFT);
                break;

            case MODE_INDENT2:
                setBorder(BORDER_INDENT2);
                if (!Utils.IS16) setHorizontalAlignment(SwingConstants.LEFT);
                break;
        }
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
        return "menuitem";
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
