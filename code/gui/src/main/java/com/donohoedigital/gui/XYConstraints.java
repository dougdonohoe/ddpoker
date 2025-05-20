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
 * XYConstraints.java
 *
 * Created on October 24, 2002, 8:20 PM
 */

package com.donohoedigital.gui;

/**
 *
 * @author  Doug Donohoe
 */
import java.io.Serializable;

public class XYConstraints implements Cloneable, Serializable {
    public int x = 0;
    public int y = 0;
    public int width = 0;
    public int height = 0;

    public XYConstraints() {
        this( 0, 0, 0, 0 );
    }

    public XYConstraints( int x, int y, int width, int height ) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public XYConstraints(XYConstraints old) {
        if (old == null) return;
        this.x = old.x;
        this.y = old.y;
        this.width = old.width;
        this.height = old.height;
    }

    public int getX() {
        return x;
    }



    public void setX( int x ) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY( int y ) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth( int width ) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight( int height ) {
        this.height = height;
    }

    public int hashCode() {
        return x ^ y * 37 ^ width * 43 ^ height * 47;
    }

    public boolean equals( Object that ) {
        if ( that instanceof XYConstraints ) {
            XYConstraints other = ( XYConstraints ) that;
            return other.x == x && other.y == y && other.width == width && other.height == height;
        } else {
            return false;
        }
    }

    public Object clone() {
        return new XYConstraints( x, y, width, height );
    }

    public String toString() {
        return String.valueOf( ( new StringBuilder( "XYConstraints[" ) ).append( x ).append( "," ).append( y ).append( "," ).append( width ).append( "," ).append( height ).append( "]" ) );
    }
}
