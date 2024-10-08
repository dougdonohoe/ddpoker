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
package com.donohoedigital.gui;

import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.plaf.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 22, 2005
 * Time: 1:34:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlassButtonUI extends DDButtonUI
{
    static Logger logger = LogManager.getLogger(DDButtonUI.class);

    private static final String SVG_HIGHLIGHT_PATH = "M52,14c-11.25,0-28.75,0-40,0c-8.092,0-10.999,2.547-12,4V0h64v10C62.999,11.453,60.092,14,52,14z";
    private static final GeneralPath HIGHLIGHT_PATH = GuiUtils.drawSVGpath(SVG_HIGHLIGHT_PATH, true);
    private static final Rectangle2D PATH_BOUNDS = HIGHLIGHT_PATH.getBounds2D();
    private static final double HEIGHT_RANGE = 18.0d / 28.0d; // from photoshop path in poker-buttons.psd
    private static final Color BG = new Color(187,187,187);
    private static final Color BRIGHT_REFLECTION = new Color(255,255,255,150);
    private static final Color DARK_REFLECTION = new Color(0,0,0,38);
    private static final Color SHADOW = new Color(0,0,0,100);
    private static final Color HILITE = new Color(255,255,255,75);

    private final static GlassButtonUI gButtonUI = new GlassButtonUI();

    public static ComponentUI createUI(JComponent c) {
        return gButtonUI;
    }

    public void paint(Graphics g1, JComponent c)
    {
        Graphics2D g = (Graphics2D) g1;
        GlassButton gb = (GlassButton) c;

        // get area we are painting
        g.getClipBounds(bounds_);
        cliparea_ = new java.awt.geom.Area(bounds_);

        // background
        paintBackground(g, gb);

        // text, icons
        super.paint(g, c);

        // reflection
        paintReflections(g, gb);
    }

    private Rectangle bounds_ = new Rectangle();
    private java.awt.geom.Area cliparea_;
    private Shape button_;
    private Area buttonarea_;
    private Rectangle2D buttonbounds_;
    private Shape oldClip_;

    protected void paintBackground(Graphics2D g, GlassButton gb)
    {
        Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

        float width = gb.getWidth();
        float height = gb.getHeight();

        // button area
        button_ = new Rectangle2D.Double(1, 1, width-2, height-2);
        buttonarea_ = new Area(button_);
        buttonbounds_ = button_.getBounds2D();

        // draw border
        g.setColor(SHADOW);
        g.drawLine(0,0,(int) width-1,0);
        g.drawLine(0,1, 0, (int) (height-1));

        g.setColor(HILITE);
        g.drawLine((int)width-1, 1, (int)width-1, (int)height-1);
        g.drawLine(1, (int) height-1, (int)width-2, (int)height-1);

        // clip
        oldClip_ = g.getClip(); // to restore later
        Area newClip = new java.awt.geom.Area(cliparea_);
        newClip.intersect(buttonarea_);

        // set clip (instead of filling button, fill all,
        // which looks slightly better)
        g.setClip(newClip);

        g.setColor(BG);
        g.fillRect(0,0,(int)width,(int)height);

        GradientPaint p = new GradientPaint(0, (float) buttonbounds_.getMaxY(),
                                            Color.white,
                                            0, (float) buttonbounds_.getY(),
                                            GuiUtils.TRANSPARENT);
        g.setPaint(p);
        g.fillRect(0,0,(int)width,(int)height);
        g.fillRect(0,0,(int)width,(int)height);


        // shading if disabled, mouseover or down
        Color extra = null;
        ButtonModel model = gb.getModel();
        if (!gb.isEnabled())
        {
            extra = gb.getDisabledColor();
        }
        else if (model.isArmed() && model.isPressed())
        {
            extra = gb.getDownColor();
        }
        else if (model.isRollover())
        {
            extra = gb.getOverColor();
        }
        else if (gb.hasFocus())
        {
            extra = new Color(255,255,0,155);
        }

        if (extra != null)
        {
            g.setColor(extra);
            g.fillRect(0,0,(int)width,(int)height);
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    protected void paintReflections(Graphics2D g, GlassButton gb)
    {
        Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);


        float width = gb.getWidth();
        float height = gb.getHeight();

        // shift down/right to look like press
        ButtonModel model = gb.getModel();
        int nLess = 0;
        if (model.isArmed() && model.isPressed())
        {
            g.translate(1,1);
            nLess = 1;
        }

        Shape buttonReflection = new Rectangle2D.Double(buttonbounds_.getX()-nLess,
                                                             buttonbounds_.getY()-nLess,
                                                             buttonbounds_.getWidth(),
                                                             buttonbounds_.getHeight());

        double scalex = width / PATH_BOUNDS.getWidth();
        double scaley = (height * HEIGHT_RANGE) / PATH_BOUNDS.getHeight();
        AffineTransform tx = AffineTransform.getScaleInstance(scalex, scaley);
        Shape highlite = HIGHLIGHT_PATH.createTransformedShape(tx);

        // figure out areas to paint hilite
        Area topHighlite = new Area(highlite);
        Area bottomHighlite = new Area(buttonReflection);
        topHighlite.intersect(bottomHighlite);
        bottomHighlite.subtract(topHighlite);

        // top hilite
        float quarterHeight = height * .25f;
        GradientPaint p = new GradientPaint(-5, quarterHeight,
                              BRIGHT_REFLECTION,
                              width + 5, height - quarterHeight,
                              GuiUtils.TRANSPARENT);
        g.setPaint(p);
        g.fill(topHighlite);

        // bottom hilite
        p = new GradientPaint(-5, quarterHeight,
                              GuiUtils.TRANSPARENT,
                              width + 5, height - quarterHeight,
                              DARK_REFLECTION);

        g.setPaint(p);
        g.fill(bottomHighlite);

        // reset stuff
        if (model.isArmed() && model.isPressed())
        {
            g.translate(-1,-1);
        }
        g.setClip(oldClip_);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    protected void paintButtonPressed(Graphics g, AbstractButton b)
    {
        // we handle this above
    }


}
