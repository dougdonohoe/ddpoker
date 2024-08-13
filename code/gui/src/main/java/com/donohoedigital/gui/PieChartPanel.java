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

public class PieChartPanel extends DDPanel
{
    PieChartModel model_ = DefaultPieChartModel.INSTANCE;

    public PieChartPanel()
    {
        super();
    }

    public void paintComponent(Graphics g1)
    {
        super.paintComponent(g1);
		Graphics2D g = (Graphics2D) g1;

 		Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // get number of wedges
        int wedgeCount = model_.getWedgeCount();

        // get wedge values (in case model is inefficient)
        // also compute total of all values
        // and determine if more than one wedge has a non-zero value

        double wedgeValue[] = new double[wedgeCount];
        double totalValues = 0;
        int nonZeroWedgeCount = 0;
        int lastNonZeroWedge = 0;

        for (int index = 0; index < wedgeCount; ++index)
        {
            wedgeValue[index] = model_.getWedgeValue(index);
            totalValues += wedgeValue[index];
            if (wedgeValue[index] > 0)
            {
                ++nonZeroWedgeCount;
                lastNonZeroWedge = index;
            }
        }

        // paint nothing if no non-zero wedges
        if (nonZeroWedgeCount == 0)
        {
            return;
        }

        // position of pie in component (upper left for now)

        double top = 0;
        double left = 0;

        Dimension size = getSize();

        // size of pie (equal height and width for now); guarantee oddness, for good axis rendering

        double width = ((int)(Math.min(size.getWidth(), size.getHeight()) / 2) * 2) - 1;
        double height = width;

        // special easy case if one non-zero wedge
        if (nonZeroWedgeCount == 1)
        {
            g.setColor(model_.getWedgeColor(lastNonZeroWedge));
            g.fillOval((int) top, (int) left, (int) width, (int) height);
            g.setColor(new Color(0, 0, 0, 128));
            g.drawOval((int) top, (int) left, (int) width, (int) height);
            return;
        }

        // compute center of pie

        double centerX = left + width / 2;
        double centerY = top + height / 2;

        // fill pie wedges

        double totalSoFar;

        totalSoFar = 0;

        for (int index = 0; index < wedgeCount; ++index)
        {
            if (wedgeValue[index] > 0)
            {
                double startDegrees = 360.0 * totalSoFar / totalValues;
                double spanDegrees = 360.0 * wedgeValue[index] / totalValues;
                g.setColor(model_.getWedgeColor(index));
                g.fillArc((int) top, (int) left, (int) width, (int) height, (int) startDegrees, (int) spanDegrees);
                totalSoFar += wedgeValue[index];
            }
        }

        // draw wedge outlines

        totalSoFar = 0;

        //g.setColor(Color.BLACK);
        g.setColor(new Color(0, 0, 0, 128));

        for (int index = 0; index < wedgeCount; ++index)
        {
            if (wedgeValue[index] > 0)
            {
                double startDegrees = 360.0 * totalSoFar / totalValues;
                double startRadians = Math.PI * startDegrees / 180;
                g.drawLine((int) (left + centerX), (int) (top + centerY),
                        (int)(centerX + (width / 2 - 1) * Math.cos(startRadians)),
                        (int)(centerY - (height / 2 - 1) * Math.sin(startRadians)));
                totalSoFar += wedgeValue[index];
            }
        }

        // draw outline of pie

        g.drawOval((int)top, (int)left, (int)width, (int)height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    public PieChartModel getModel()
    {
        return model_;
    }

    public void setModel(PieChartModel model)
    {
        model_ = model;
        repaint();
    }

    private static class DefaultPieChartModel implements PieChartModel
    {
        private static final DefaultPieChartModel INSTANCE = new DefaultPieChartModel();

        private double value_[] = new double[] { 10, 25, 65 };

        public int getWedgeCount()
        {
            return 3;
        }

        public void setWedgeValue(int index, double value)
        {
            switch (index)
            {
                case 0:
                case 1:
                case 2:
                    value_[index] = value;
                    break;
            }
        }

        public double getWedgeValue(int index)
        {
            switch (index)
            {
                case 0:
                case 1:
                case 2:
                    return value_[index];
                default:
                    return 0;
            }
        }

        public Color getWedgeColor(int index)
        {
            switch (index)
            {
                case 0:
                    return Color.RED;
                case 1:
                    return Color.YELLOW;
                case 2:
                    return Color.GREEN;
                default:
                    return Color.WHITE;
            }
        }
    }
}
