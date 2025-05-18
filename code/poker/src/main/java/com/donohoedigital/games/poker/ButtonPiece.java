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
/*
 * ButtonPiece.java
 *
 * Created on January 2, 2004, 1:11 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.gui.*;
import com.donohoedigital.games.poker.engine.*;

import java.awt.*;
import java.awt.geom.*;


/**
 *
 * @author  Doug Donohoe
 */
public class ButtonPiece extends PokerGamePiece
{
    private static GeneralPath path_ = GuiUtils.drawSVGpath(
            "M26.018,77.979v-5.375h5.375v-43.5h-5.375v-5.375h26.769c8.594,0,15.301,2.305,20.123,6.91c4.82,4.605,7.232,11.014,7.232,19.225c0,8.529-2.338,15.348-7.014,20.455s-10.918,7.66-18.73,7.66H26.018z M42.768,72.104h6.213c6.343,0,11.103-1.863,14.277-5.594c3.172-3.73,4.76-9.313,4.76-16.75c0-6.896-1.648-12.063-4.941-15.5c-3.295-3.438-8.236-5.156-14.822-5.156h-5.486V72.104z",
            false);
    private static ButtonImageComponent ic_ = new ButtonImageComponent();

    /**
     * Creates a new instance of ButtonPiece 
     */
    public ButtonPiece() {
        super(PokerConstants.PIECE_BUTTON, null, "button", "button");
    }
    
    /**
     * No mouse over detection
     */
    public boolean allowMouseOver()
    {
        return false;
    }
    
    /**
     * Fix scale
     */
    public double getScale()
    {
        return .5d;
    }

    /**
     * override so no image given
     */
    public ImageComponent getImageComponent()
    {
        return ic_;
    }

    /**
     * Class to draw card bg
     */
    public static class ButtonImageComponent extends EmptyImageComponent
    {

        public ButtonImageComponent()
        {
            // size based on original png image
            super(100, 100);
        }

        public void drawImageAt(Graphics2D g, int x, int y, int width, int height)
        {
            // current settings
            Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
            AffineTransform txOld = g.getTransform();

            // shadow
            g.setColor(PokerChip.shadow_);
            g.fillOval(x+2,y+1,width-1,height-1);

            // button
            g.setColor(Color.white);
            g.fillOval(x, y, width-1, height-1);
            g.setColor(PokerChip.shadow_);
            g.drawOval(x, y, width-1, height-1);

            // "D"
            g.setColor(Color.black);
            g.translate(x,y);
            g.scale(width/100.0d, height/100.0d);
            g.fill(path_);

            // reset
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
            g.setTransform(txOld);
        }
    }
}
