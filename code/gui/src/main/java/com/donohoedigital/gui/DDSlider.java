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
 * DDSlider.java
 *
 * Created on June 5, 2003, 6:03 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDSlider extends JSlider implements DDComponent 
{
    //static Logger logger = Logger.getLogger(DDSlider.class);

    private Color thumbFocusOverlay_=  null;
    private Color thumbBg_ = null;

    /**
     * Creates a new instance of DDSlider 
     */
    public DDSlider() {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    public DDSlider(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    public DDSlider(String sName, String sStyle)
    {
        super();
        init(sName, sStyle);
    }
    
    private void init(String sName, String sStyle)
    {
        GuiManager.init(this, sName, sStyle);
        setUI(new DDSliderUI());
        setOpaque(false);
    }
    
    public String getType() 
    {
        return "slider";
    }

    public Color getThumbFocusColor()
    {
        return thumbFocusOverlay_;
    }

    public void setThumbFocusColor(Color c)
    {
        thumbFocusOverlay_ = c;
    }

    public Color getThumbBackgroundColor()
    {
        return thumbBg_;
    }

    public void setThumbBackgroundColor(Color c)
    {
        thumbBg_ = c;
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
