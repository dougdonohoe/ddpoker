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
 * DDLabel.java
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
public class DDLabel extends JLabel implements DDHasLabelComponent, DDText, DDCustomHelp//, ComponentListener
{
    static Logger logger = LogManager.getLogger(DDLabel.class);

    private boolean bOpaque_ = false;
    private Insets ignoreInsets_ = null;
    private static Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /**
     * Creates a new instance of DDLabel
     */
    public DDLabel()
    {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }

    public DDLabel(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    public DDLabel(String sName, String sStyle)
    {
        super();
        init(sName, sStyle);
    }

    private void init(String sName, String sStyle)
    {
        setOpaque(false);
        //addComponentListener(this);
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
     * Override to store locally since we handle opaque
     */
    @Override
    public void setOpaque(boolean b)
    {
        bOpaque_ = b;
    }

    /**
     * set insets indicating background to not paint
     */
    public void setIgnoreInsets(Insets insets)
    {
        ignoreInsets_ = insets;
    }

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    @Override
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
        Object old = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // when opaque, we paint everything except the border
        // this allows for borders with alpha to paint correctly
        // example: dd poker chat text field
        if (bOpaque_)
        {
            paintBackground(g);
        }

        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    /**
     * paint background (only called when opaque)
     */
    protected void paintBackground(Graphics2D g)
    {
        g.setColor(getBackground());
        Insets insets = ignoreInsets_ == null ? EMPTY_INSETS : ignoreInsets_;
        g.fillRect(insets.left, insets.top, getWidth() - (insets.left + insets.right), getHeight() - (insets.top + insets.bottom));
    }

    public String getType()
    {
        return "label";
    }

    public void setPreferredHeight(int height)
    {
        setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), height));
    }

    public void setPreferredWidth(int width)
    {
        setPreferredSize(new Dimension(width, (int) getPreferredSize().getHeight()));
    }

    @Override
    public void setText(String s)
    {
        GuiUtils.requireSwingThread();

        // empty length strings cause issues, so avoid it
        if (s != null && s.length() == 0) s = " ";

        // 133 - don't set if equal to current (saves memory when processing HTML)
        String sCurrent = getText();
        if (sCurrent != null && sCurrent.equals(s)) return;
        super.setText(s);

        //debugView();
    }

    /**
     * special case where text should be empty
     */
    public void clearText()
    {
        super.setText("");
    }

    ////
    //// Custom help
    ////

    private String sHelp_;

    public String getHelpText()
    {
        return sHelp_;
    }

    public void setHelpText(String s)
    {
        sHelp_ = s;
    }

//    private void debugView()
//    {
//        String s = getText();
//        if (BasicHTML.isHTMLString(s) && DEBUG_VIEW)
//        {
//            View view = (View) getClientProperty(BasicHTML.propertyKey);
//            //view.setSize(164,30);
//            logger.debug("Set Text: " + s);
//            logger.debug("Preferred Size: " + getPreferredSize());
//            logger.debug("View size: " + view.getPreferredSpan(View.X_AXIS) +","+view.getPreferredSpan(View.Y_AXIS));
//            //setPreferredSize(getPreferredSize());
//        }
//    }
//
//
//    public boolean DEBUG_VIEW = false;
//
//
//    public void componentResized(ComponentEvent e)
//    {
//        //To change body of implemented methods use File | Settings | File Templates.
//        if (DEBUG_VIEW) logger.debug("Resized: " + getSize());
//        debugView();
//    }
//
//    public void componentMoved(ComponentEvent e)
//    {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    public void componentShown(ComponentEvent e)
//    {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    public void componentHidden(ComponentEvent e)
//    {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
}
