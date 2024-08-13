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
 * DDTabbedPane.java
 *
 * Created on June 16, 2003, 6:36 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.config.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDTabbedPane extends JTabbedPane implements DDHasLabelComponent, ChangeListener,
                                                         MouseMotionListener, MouseListener
{
    static Logger logger = Logger.getLogger(DDTabbedPane.class);

    private static Color DEFAULT_SELECTED = new Color(255,255,255,175);
    private Color cSelectedTab_ = DEFAULT_SELECTED;

    public DDTabbedPane(String sStyle, int nPlacement)
    {
        this(sStyle, sStyle, nPlacement);
    }

    public DDTabbedPane(String sStyle, String sBevelStyle, int nPlacement)
    {
        super(nPlacement);
        init(GuiManager.DEFAULT, sStyle, sBevelStyle);
    }

    private void init(String sName, String sStyle, String sBevelStyle)
    {        
        GuiManager.init(this, sName, sStyle);
        setUI(new DDTabbedPaneUI(this, sBevelStyle));
        addChangeListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void addTab(String sTitleKey, Icon icon, Icon error, DDTabPanel tab)
    {
        super.addTab(PropertyConfig.getMessage(sTitleKey), icon, tab, null);
        tab.setTabNum(getTabCount() - 1);
        tab.setIcon(icon);
        tab.setErrorIcon(error);
        tab.setTabPane(this);
        tab.setHelpText(PropertyConfig.getStringProperty(sTitleKey + ".help", null, false));
    }


    /**
     * tab changed - clear help
     */
    public void stateChanged(ChangeEvent e)
    {
        setHelp(getSelectedIndex());
    }

    /**
     * set help text for given tab
     */
    private void setHelp(int i)
    {
        Component c = getComponentAt(i);
        if (c instanceof DDTabPanel)
        {
            String sHelp = ((DDTabPanel)c).getHelpText();
            DDWindow window = GuiUtils.getHelpManager(c);
            if (sHelp != null && window != null) {
                window.setHelpMessage(sHelp);
            }
        }
    }

    /**
     * Do valid check over all tabs
     */
    public boolean doValidCheck()
    {
        boolean bValid = true;
        int nNum = getTabCount();
        Component c;
        for (int i = 0; i < nNum; i++)
        {
            c = getComponentAt(i);
            if (c instanceof DDTabPanel)
            {
                bValid &= ((DDTabPanel)c).doValidCheck();
            }
        }

        return bValid;
    }

    public String getType()
    {
        return "tab";
    }
    
    // needed for DDHasLabel interface
    public String getText() {
        return null;
    }
    
    public void setText(String s) {
    }

    // always anti alias?
    private boolean bAlwaysAntiAlias_ = true;

    public Color getSelectedTabColor()
    {
        return cSelectedTab_;
    }

    public void setSelectedTabColor(Color c)
    {
        cSelectedTab_ = c;
    }

    /**
     * set whether anti aliases should always occur,
     * overriding GuiUtils.drawAntiAlias()
     */
    public void setAlwaysAntiAlias(boolean b)
    {
        bAlwaysAntiAlias_ = b;
    }

    /**
     * is GuiUtils.drawAntiAlias() overriden
     */
    public boolean isAlwaysAntiAlias()
    {
        return bAlwaysAntiAlias_;
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    public void repaint(long tm, int x, int y, int width, int height)
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, x, y, foo);
            foo.repaint(pRepaint.x, pRepaint.y, width, height);
            return;
        }

        super.repaint(tm, x, y, width, height);
    }

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    public void paintComponent(Graphics g1)
    {
	    Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (bAlwaysAntiAlias_ || GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    ///
    /// mouse events to do mouse over help
    ///

    public void mouseDragged(MouseEvent e)
    {
    }

    private int nLastTab = -2;
    public void mouseMoved(MouseEvent e)
    {
        int i = getUI().tabForCoordinate(this, e.getX(), e.getY());
        if (i != nLastTab)
        {
            nLastTab = i;
            if (i != -1) setHelp(i);
        }
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
        nLastTab = -2;
    }
}
