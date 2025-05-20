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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.gui.*;

import java.awt.*;

public class StyleQuadrantsGridPanel extends DDPanel
{
    private float tightness_ = Float.NaN;
    private float aggression_ = Float.NaN;

    public StyleQuadrantsGridPanel()
    {
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(25, 25));
    }

    public void setValues(float tightness, float aggression)
    {
        tightness_ = tightness;
        aggression_ = aggression;
        repaint();
    }

    public void paintComponent(Graphics g1)
    {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 25, 25);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(1, 1, 22, 22);
        g.drawLine(12, 1, 12, 23);
        g.drawLine(2, 12, 24, 12);

        float tightness = Float.isNaN(tightness_) ? 0.5f : tightness_;
        float aggression = Float.isNaN(aggression_) ? 0.5f : aggression_;

        if (!Float.isNaN(tightness_) || !Float.isNaN(aggression_))
        {
            int x = 11 - (int)((tightness - 0.5f) / 0.05f);
            int y = 11 + (int)((aggression - 0.5f) / 0.05f);
            g.setColor(new Color(64, 128, 255));
            g.drawLine(x-1, y+1, x+3, y+1);
            g.drawLine(x+1, y-1, x+1, y+3);
            g.setColor(Color.WHITE);
            g.drawLine(x+1, y+1, x+1, y+1);
        }
    }
}