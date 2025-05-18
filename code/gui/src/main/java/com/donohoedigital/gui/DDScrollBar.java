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
package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2005
 * Time: 8:37:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDScrollBar extends JScrollBar implements DDComponent, FocusListener, MouseListener
{
    //static Logger logger = LogManager.getLogger(DDScrollBar.class);

    // our ui
    private DDScrollBarUI ui_;
    public static final int SB_SIZE = 12;
    private Color thumbFocusOverlay_=  null;

    /**
     * Create new scrollbar
     * @param nOrientation
     */
    public DDScrollBar(int nOrientation)
    {
        super(nOrientation);
        Dimension pref = getPreferredSize();
        if (nOrientation == JScrollBar.VERTICAL)
        {
            pref.width = SB_SIZE;
        }
        else
        {
            pref.height = SB_SIZE;
        }
        setPreferredSize(pref);
        setFocusable(true); // set explicitly for JDK1.5
        setFocusTraversalKeysEnabled(true); // set explicitly for JDK1.5
        addFocusListener(this);
        setUnitIncrement(20);
        setBlockIncrement(100);
        addMouseListener(this);
    }

    /**
     * Set style of scrollbar (needs to happen post-construction due
     * to when swing creates scrollbars).
     */
    void setStyle(String sStyle)
    {
        GuiManager.init(this, GuiManager.DEFAULT, sStyle);
        ui_ = new DDScrollBarUI();
        setUI(ui_); // create ui after init from gui manager so we have colors
        setOpaque(false); // need to set here since installDefaults sets to true
    }

    /**
     *  DD component type
     */
    public String getType()
    {
        return "scrollbar";
    }

    public void focusGained(FocusEvent e)
    {
        repaint();
    }

    public void focusLost(FocusEvent e)
    {
        repaint();
    }

    public Color getThumbFocusColor()
    {
        return thumbFocusOverlay_;
    }

    public void setThumbFocusColor(Color c)
    {
        thumbFocusOverlay_ = c;
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

    ////
    //// Mouse listener to track when scrolling
    ////

    private boolean bScrolling_ = false;

    public boolean isUserScrolling()
    {
        return bScrolling_;
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        bScrolling_ = true;
        //logger.debug("Scrolling");
    }

    public void mouseReleased(MouseEvent e)
    {
        bScrolling_ = false;
        //logger.debug("Not scrolling");
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }
}
