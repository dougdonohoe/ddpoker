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

import org.apache.log4j.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.games.poker.engine.*;

import javax.swing.text.*;
import java.awt.*;

public class DDCardView extends DDView
{
    static Logger logger = Logger.getLogger(DDCardView.class);

    private static CardThumbnail piece_ = new CardThumbnail();

    private Card card_;
    private int border_ = 1;
    public static final int HEIGHT = 26;
    public static final int WIDTH = 20;

    public DDCardView(Element elem)
    {
        super(elem);

        String card = (String)getElement().getAttributes().getAttribute("card");

        if (card != null)
        {
            card_ = Card.getCard(card);
        }
        else
        {
            card_ = Card.BLANK;
        }
    }

    /**
     * paint
     */
    public void paint(Graphics g, Shape a) 
    {
        Rectangle rect = (a instanceof Rectangle) ? (Rectangle)a : a.getBounds();

        piece_.setCard(card_);
        piece_.setUp(card_ != Card.BLANK);

        piece_.drawImageAt((Graphics2D) g, piece_.getImageComponent(), 0, 0, 0,
            rect.x+border_, rect.y+border_, rect.width-border_*2,  rect.height-border_*2,
            1.0d);
    }
    
    /** Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     * 		<code>View.Y_AXIS</code>
     * @return   the span the view would like to be rendered into.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view
     * @see javax.swing.text.View#getPreferredSpan
     *
     */
    public float getPreferredSpan(int axis) {
        if (axis == View.X_AXIS) return WIDTH;
        return HEIGHT;
    }
}
