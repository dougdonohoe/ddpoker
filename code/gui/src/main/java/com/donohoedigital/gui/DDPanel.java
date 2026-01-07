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
 * DDPanel.java
 *
 * Created on November 17, 2002, 3:47 PM
 */

package com.donohoedigital.gui;

import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;

/**
 * @author Doug Donohoe
 */
public class DDPanel extends JPanel implements DDComponent
{
    /**
     * Create panel with CenterLayout
     */
    public static DDPanel CENTER()
    {
        DDPanel newpanel = new DDPanel();
        newpanel.setLayout(new CenterLayout());
        return newpanel;
    }

    /**
     * Adjust border layout.  Will throw ClassCastException if
     * this panel is using another layout
     */
    public void setBorderLayoutGap(int vGap, int hGap)
    {
        BorderLayout layout = (BorderLayout) getLayout();
        layout.setVgap(vGap);
        layout.setHgap(hGap);
    }

    /**
     * Creates a new instance of DDPanel
     */
    public DDPanel()
    {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }

    public DDPanel(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    public DDPanel(String sName, String sStyle)
    {
        super();
        init(sName, sStyle);
    }

    private void init(String sName, String sStyle)
    {
        setOpaque(false);
        setLayout(new BorderLayout());
        GuiManager.init(this, sName, sStyle);
    }

    public void reinit(String sName, String sStyle)
    {
        GuiManager.init(this, sName, sStyle);
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    @Override
    public void repaint(long tm, int x, int y, int width, int height)
    {
        if (debug) logger.debug("REPAINT: " + this);
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, x, y, foo);
            foo.repaint(pRepaint.x, pRepaint.y, width, height);
            return;
        }

        super.repaint(tm, x, y, width, height);
    }

    Rectangle bounds_ = new Rectangle();
    private static final Logger logger = LogManager.getLogger(DDPanel.class);
    private static int CNT = 0;

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    @Override
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        if (debug)
        {
            Component foo = GuiUtils.getSolidRepaintComponent(this);
            if (foo != null && foo != this)
            {
                logger.debug("painting " + this + "\nbut solid repaint is: " + foo);
            }
            g.getClipBounds(bounds_);
            logger.debug("REPAINT COMPONENT "+(CNT++)+" ("+ImageComponent.getDebugColorName()+") portion " + bounds_.x +","+bounds_.y+" " +bounds_.width+"x"+bounds_.height);
            g.setColor(ImageComponent.getDebugColor());
            g.drawRect(bounds_.x, bounds_.y, bounds_.width - 1, bounds_.height - 1);
        }
        
        super.paintComponent(g);
    }

    private boolean debug = false;

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public String getType()
    {
        return "panel";
    }

    public void setPreferredHeight(int height)
    {
        setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), height));
    }

    public void setPreferredWidth(int width)
    {
        setPreferredSize(new Dimension(width, (int) getPreferredSize().getHeight()));
    }
}
