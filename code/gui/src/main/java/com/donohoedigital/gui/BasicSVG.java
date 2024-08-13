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

import com.donohoedigital.base.*;
import org.apache.log4j.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 2, 2005
 * Time: 2:28:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicSVG
{
    static Logger logger = Logger.getLogger(BasicSVG.class);

    ArrayList paths_ = new ArrayList();
    Rectangle2D bounds_;

    public void addPath(String sFill, String sStroke, String sPath)
    {
        SVGPath sp = new SVGPath(Utils.getHtmlColor(sFill),
                                 Utils.getHtmlColor(sStroke),
                                 GuiUtils.drawSVGpath(sPath, false));
        paths_.add(sp);
    }

    public void calculateBounds()
    {
        bounds_ = new Rectangle2D.Double(0,0,0,0);
        Rectangle2D b;
        int n = paths_.size();
        SVGPath path;
        for (int i = 0; i < n; i ++)
        {
            path = (SVGPath) paths_.get(i);
            b = path.path.getBounds2D();
            bounds_.setRect(
                    Math.min(bounds_.getX(), b.getX()),
                    Math.min(bounds_.getY(), b.getY()),
                    Math.max(bounds_.getWidth(), b.getMaxX()),
                    Math.max(bounds_.getHeight(), b.getMaxY()));
        }
    }

    public Rectangle2D getBounds()
    {
        return bounds_;
    }

    public void draw(Graphics2D g, double x, double y, double width, double height)
    {
        AffineTransform old = g.getTransform();
        Object oldaa =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

        double wScale = width / bounds_.getWidth();
        double hScale = height / bounds_.getHeight();

        AffineTransform nu = new AffineTransform(old);
        nu.translate(x, y);
        nu.scale(wScale, hScale);

        g.setTransform(nu);

        int n = paths_.size();
        SVGPath path;
        for (int i = 0; i < n; i ++)
        {
            path = (SVGPath) paths_.get(i);

            if (path.fill != null)
            {
                g.setColor(path.fill);
                g.fill(path.path);
            }

            if (path.stroke != null)
            {
                g.setColor(path.stroke);
                g.draw(path.path);
            }
        }

        g.setTransform(old);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldaa);
    }

    private class SVGPath
    {
        Color fill;
        Color stroke;
        GeneralPath path;

        SVGPath(Color fill, Color stroke, GeneralPath path)
        {
            this.fill = fill;
            this.stroke = stroke;
            this.path = path;
        }
    }
}
