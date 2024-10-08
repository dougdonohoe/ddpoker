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
 * DDImageView.java
 *
 * Created on March 29, 2003, 3:42 PM
 */
package com.donohoedigital.games.poker;

import org.apache.logging.log4j.*;
import com.donohoedigital.gui.*;

import javax.swing.text.*;
import java.awt.*;

public class DDHandGroupView extends ComponentView
{
    static Logger logger = LogManager.getLogger(DDHandGroupView.class);

    public static final int DEFAULT_HEIGHT = 200;
    public static final int DEFAULT_WIDTH = 200;

    private HandGroup group_;
    private int width_ = 0;
    private int height_ = 0;

    private HandGroupGridPanel panel_;

    protected Component createComponent()
    {
        panel_ = new HandGroupGridPanel(true);
        panel_.setEnabled(false);
        panel_.setOpaque(true);
        if (width_ > 0) panel_.setPreferredWidth(width_);
        if (height_ > 0) panel_.setPreferredHeight(height_);

        // wrap to prevent opaqueness
        DDPanel wrap = new DDPanel();
        panel_.setOpaque(false);
        wrap.add(panel_, BorderLayout.CENTER);
        return wrap;
    }

    public DDHandGroupView(Element elem)
    {
        super(elem);

        group_ = HandGroup.parse((String)getElement().getAttributes().getAttribute("cards"), 0);

        String width = (String) getElement().getAttributes().getAttribute("width");

        if (width != null)
        {
            try
            {
                width_ = Integer.parseInt(width);
            } catch (NumberFormatException e)
            {
            }
        }

        String height = (String) getElement().getAttributes().getAttribute("height");

        if (height != null)
        {
            try
            {
                height_ = Integer.parseInt(height);
            } catch (NumberFormatException e)
            {
            }
        }
    }

    /**
     * paint
     */
    public void paint(Graphics g, Shape a) 
    {
        panel_.setHandGroup(group_);

        super.paint(g, a);
    }
}
