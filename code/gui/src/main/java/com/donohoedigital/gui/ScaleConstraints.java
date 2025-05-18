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
 * ScaleConstraints.java
 *
 * Created on January 3, 2004, 4:19 PM
 */

package com.donohoedigital.gui;

import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class ScaleConstraints implements Cloneable
{
    public double x = 0;
    public double y = 0;
    public double scale = 0;
    public Font font = null;

    public ScaleConstraints() {
        this(0.0d, 0.0d, 1.0d, null);
    }

    /**
     * x,y should be a value between 0 and 1 representing where
     * in the parent the top-left of the widget should be drawn.
     * scale is a double which represents the size the component
     * should be in relation to the parent (for example if
     * .5 is passed in, this component will be sized so the width
     * is 50% of the parent).  The component is
     * scale to maintain the same width/height ratio as is
     * specified by getPreferredSize().  If a font is passed in,
     * then the widget's font is also scaled, using the given
     * font as the basis (corresponding to getPreferredSize())
     */ 
    public ScaleConstraints(double x, double y, double scale, Font font) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.font = font;
    }
    
    public ScaleConstraints(ScaleConstraints old) 
    {
        if (old == null) return;
        this.x = old.x;
        this.y = old.y;
        this.scale = old.scale;
        this.font = old.font;
    }

    public double getX() {
        return x;
    }
    
    public void setX( double x ) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY( double y ) {
        this.y = y;
    }

    public double getScale() {
        return scale;
    }

    public void setScale( int scale ) {
        this.scale = scale;
    }
    
    public Font getFont()
    {
        return font;
    }
    
    public void setFont(Font font)
    {
        this.font = font;
    }

    public boolean equals( Object that ) {
        if ( that instanceof ScaleConstraints ) {
            ScaleConstraints other = ( ScaleConstraints ) that;
            return other.x == x && other.y == y && other.scale == scale && other.font == font;
        } else {
            return false;
        }
    }

    public Object clone() {
        return new ScaleConstraints( x, y, scale, font);
    }

    public String toString() {
        return ("Scale " + x + "," + y+ " (" + scale +"), Font: " + font);
    }
}
