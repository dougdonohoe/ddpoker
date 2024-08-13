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

import java.awt.*;
import java.awt.geom.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 17, 2005
 * Time: 3:15:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class PillPanel extends DDPanel implements DDComponent, PillColors
{
    private Color gradFrom_;// = new Color(255,255,255,50);
    private Color gradTo_;// = new Color(255,255,255,175);

    public PillPanel(String sStyle)
    {
        super(GuiManager.DEFAULT, sStyle);
    }

    public void setGradientFrom(Color c)
    {
        gradFrom_ = c;
    }

    public Color getGradientFrom()
    {
        return gradFrom_;
    }

    public void setGradientTo(Color c)
    {
        gradTo_ = c;
    }

    public Color getGradientTo()
    {
        return gradTo_;
    }

    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

        // paint beveled area
        RoundRectangle2D rr = new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1, getHeight()-1,getHeight()-1);
        GradientPaint gp = new GradientPaint(0, 0, gradFrom_, 0, getHeight(), gradTo_);

        g.setColor(getBackground());
        g.fill(rr);
        g.setPaint(gp);
        g.fill(rr);

        super.paintComponent(g1);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    public String getType()
    {
        return "pill";
    }
}
