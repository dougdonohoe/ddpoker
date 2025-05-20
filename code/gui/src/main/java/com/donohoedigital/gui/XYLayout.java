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
 * XYLayout.java
 *
 * Created on October 24, 2002, 8:19 PM
 */

package com.donohoedigital.gui;

/**
 *
 * @author  Doug Donohoe
 */
import java.awt.*;
import java.io.Serializable;
import java.util.*;


public class XYLayout implements LayoutManager2, Serializable , Cloneable {
    private static final long serialVersionUID = 00L;
    int width;
    int height;
    HashMap info;
    static final XYConstraints defaultConstraints = new XYConstraints();

    public XYLayout() {
        info = new HashMap();
    }

    public XYLayout( int width, int height ) {
        info = new HashMap();
        this.width = width;
        this.height = height;
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

    public String toString() {
        return String.valueOf( ( new StringBuilder( "XYLayout[width=" ) ).append( width ).append( ",height=" ).append( height ).append( "]" ) );
    }

    public void addLayoutComponent( String s, Component component1 ) {}

    public void removeLayoutComponent( Component component ) {
        info.remove( component );
    }

    public Dimension preferredLayoutSize( Container target ) {
        return getLayoutSize( target, true );
    }

    public Dimension minimumLayoutSize( Container target ) {
        return getLayoutSize( target, false );
    }

    public void layoutContainer( Container target ) {
        Insets insets = target.getInsets();
        int count = target.getComponentCount();

        for ( int i = 0; i < count; i++ ) {
            Component component = target.getComponent( i );

            if ( component.isVisible() ) {
                Rectangle r = getComponentBounds( component, true );
                component.setBounds( insets.left + r.x, insets.top + r.y, r.width, r.height );
            }
        }

    }

    public void addLayoutComponent( Component component, Object constraints ) {
        if ( constraints instanceof XYConstraints ) {
            info.put( component, constraints );
        }
    }

    public Dimension maximumLayoutSize( Container target ) {
        return new Dimension( 0x7fffffff, 0x7fffffff );
    }

    public float getLayoutAlignmentX( Container target ) {
        return 0.5F;
    }

    public float getLayoutAlignmentY( Container target ) {
        return 0.5F;
    }

    public void invalidateLayout( Container container ) {}

    Rectangle getComponentBounds( Component component, boolean doPreferred ) {
        XYConstraints constraints = ( XYConstraints ) info.get( component );

        if ( constraints == null ) {
            constraints = defaultConstraints;
        }

        Rectangle r = new Rectangle( constraints.x, constraints.y, constraints.width, constraints.height );

        if ( r.width <= 0 || r.height <= 0 ) {
            Dimension d = doPreferred ? component.getPreferredSize()
                          : component.getMinimumSize();

            if ( r.width <= 0 ) {
                r.width = d.width;
            }

            if ( r.height <= 0 ) {
                r.height = d.height;
            }
        }

        return r;
    }

    Dimension getLayoutSize( Container target, boolean doPreferred ) {
        Dimension dim = new Dimension( 0, 0 );

        if ( width <= 0 || height <= 0 ) {
            int count = target.getComponentCount();

            for ( int i = 0; i < count;i++ ) {
                Component component = target.getComponent( i );

                if ( component.isVisible() ) {
                    Rectangle r = getComponentBounds( component, doPreferred );
                    dim.width = Math.max( dim.width, r.x + r.width );
                    dim.height = Math.max( dim.height, r.y + r.height );
                }
            }

        }

        if ( width > 0 ) {
            dim.width = width;
        }

        if ( height > 0 ) {
            dim.height = height;
        }

        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return dim;
    }
    
}
