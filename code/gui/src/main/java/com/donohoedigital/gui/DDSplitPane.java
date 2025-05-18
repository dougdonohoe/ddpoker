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

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 20, 2006
 * Time: 7:20:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDSplitPane extends JSplitPane implements DDComponent
{
    // our ui
    private DDSplitPaneUI ui_;
    private Color thumbFocusOverlay_=  null;

    public DDSplitPane(String sName, String sStyle,
                       int newOrientation, Component newLeftComponent, Component newRightComponent)
    {
        super(newOrientation, newLeftComponent, newRightComponent);
        setDividerSize(10);
        setOpaque(false);
        setContinuousLayout(true);
        setResizeWeight(.5);
        setBorder(BorderFactory.createEmptyBorder());
        setFocusable(true);

        GuiManager.init(this, sName, sStyle);
        ui_ = new DDSplitPaneUI(this);
        setUI(ui_);
    }

    /**
     *  DD component type
     */
    public String getType()
    {
        return "split";
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
    
}
