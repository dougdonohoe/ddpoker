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
package com.donohoedigital.gui;

import javax.swing.plaf.basic.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 20, 2006
 * Time: 7:48:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDSplitPaneDivider extends BasicSplitPaneDivider
{
    private DDSplitPane split_;

    private DDMetalBumps ddbumps_;
    private Color thumbColor;
    private int inset = 2;

    public DDSplitPaneDivider(BasicSplitPaneUI ui, DDSplitPane split)
    {
        super(ui);
        split_ = split;

        // set colors based on background
        thumbColor = split_.getBackground();
        if (thumbColor == null) thumbColor = Color.gray;
        Color thumbShadow = thumbColor.darker();
        Color thumbHighlightColor = thumbColor.brighter();

        // our bumps
        ddbumps_ = new DDMetalBumps(6, 6, thumbHighlightColor, thumbShadow, thumbColor );
    }

    public void paint(Graphics g)
    {
        //g.setColor(thumbColor);
        //Rectangle clip = g.getClipBounds();
        Insets insets = getInsets();
        //g.fillRect(clip.x, clip.y, clip.width, clip.height);
        Dimension  size = getSize();
        size.width -= inset * 2;
        size.height -= inset * 2;
        int drawX = inset;
        int drawY = inset;
        if (insets != null)
        {
            size.width -= (insets.left + insets.right);
            size.height -= (insets.top + insets.bottom);
            drawX += insets.left;
            drawY += insets.top;
        }
        int oldh = size.height;
        size.height = Math.min(size.height, 15);
        drawY += (oldh - size.height)/2;
        ddbumps_.setBumpArea(size);
        ddbumps_.paintIcon(this, g, drawX, drawY);
        //super.paint(g);

        // if sb has focus, overlay focus color
        if (split_.hasFocus())
        {
            Color focus = split_.getThumbFocusColor();
            if (focus != null)
            {
                g.setColor(focus);
                g.fillRect(drawX,  drawY, size.width, size.height);
            }
        }
    }
}
