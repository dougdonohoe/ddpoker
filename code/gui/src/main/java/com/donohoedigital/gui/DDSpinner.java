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
 * DDSpinner.java
 *
 * Created on November 17, 2002, 3:47 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDSpinner extends JSpinner implements DDTextVisibleComponent
{
    //static Logger logger = LogManager.getLogger(DDSpinner.class);
    
    /** 
     * Creates a new instance of DDSpinner 
     */
    public DDSpinner(SpinnerModel model) {
        super(model);
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    public DDSpinner(SpinnerModel model, String sName)
    {
        super(model);
        init(sName, GuiManager.DEFAULT);
    }

    public DDSpinner(SpinnerModel model,String sName, String sStyle)
    {
        super(model);
        init(sName, sStyle);
    }
    
    private void init(String sName, String sStyle)
    {
        setOpaque(false);
        GuiManager.init(this, sName, sStyle);
        setFocusable(false); // TODO: set not focusable until we can paint focus
        fixBordersAndStuff();
    }
    
    private void fixBordersAndStuff()
    {
        //GuiUtils.printChildren(this, 0);
        Component[] children = getComponents();
        for (int i = 0; i < children.length; i++)
        {
            
            if (children[i] instanceof JButton)
            {   // this doesn't seem to do anything - must be do to UI
                //((JButton)children[i]).setBorder(BorderFactory.createEmptyBorder());//.createBevelBorder(BevelBorder.RAISED));
                children[i].setFocusable(false);
            }
            else if (children[i] instanceof JPanel)
            {
                Component child = ((JPanel)children[i]).getComponent(0);
                if (child instanceof JTextComponent)
                {
                    ((JTextComponent) child).setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(0, 0, 0, 1),
                            BorderFactory.createBevelBorder(BevelBorder.LOWERED)));
                     ((JTextComponent) child).setEditable(false);
                     ((JTextComponent) child).setFocusable(false);
                }
            }
        }
        setBorder(BorderFactory.createEmptyBorder());
    }
    
    boolean bProtected_ = false;
    /**
     * Set protected - can't edit (buttons disabled)
     */
    public void setProtected(boolean b)
    {
        if (bProtected_ == b) return;
        bProtected_ = b;
        Component[] children = getComponents();
        for (int i = 0; i < children.length; i++)
        {
            
            if (children[i] instanceof JButton)
            {   
                children[i].setEnabled(!b);
            }
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
        return "spinner";
    }

    /**
     * Set font 
     */
    public void setFont(Font f)
    {
        GuiUtils.setFontChildren(this, f);
        super.setFont(f);
    }
    
    public void setBackground(Color c)
    {
        GuiUtils.setBackgroundChildren(this, c);
        super.setBackground(c);
    }

    public void setForeground(Color c)
    {
        GuiUtils.setForegroundChildren(this, c);
        super.setForeground(c);
    }
    public void addMouseListener(MouseListener listener)
    {
        if (listener instanceof GuiManager)
        {
            GuiUtils.addMouseListenerChildren(this, listener);
        }
        super.addMouseListener(listener);
    }   
    public void removeMouseListener(MouseListener listener)
    {
        if (listener instanceof GuiManager)
        {
            GuiUtils.removeMouseListenerChildren(this, listener);
        }
        super.removeMouseListener(listener);
    }   
}
