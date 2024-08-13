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
 * GameboardCenterLayout.java
 *
 * Created on December 28, 2003, 4:24 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.gui.*;

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 *
 */
public class GameboardCenterLayout implements LayoutManager2, Serializable
{
    private HashMap info = new HashMap();

    public void addLayoutComponent( String s, Component component1 )
    {
        throw new UnsupportedOperationException("Not used");
    }

    public void addLayoutComponent( Component component, Object constraints )
    {
        if (constraints instanceof ScaleConstraintsFixed)
        {
            info.put( component, constraints );
        }
    }

    public void removeLayoutComponent(Component component)
    {
        info.remove(component);
    }

    public Dimension preferredLayoutSize( Container container ) 
    {
        Component c = container.getComponent( 0 );
        if ( c != null ) 
        {
            Dimension size = c.getPreferredSize();
            Insets insets = container.getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }
        else {
            return new Dimension( 0, 0 );
        }
    }

    public Dimension minimumLayoutSize(Container cont) {
    	return preferredLayoutSize(cont);
    }

    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(0x7fffffff, 0x7fffffff);
    }

    public float getLayoutAlignmentX(Container target) {
        return 0.5F;
    }

    public float getLayoutAlignmentY(Container target) {
        return 0.5F;
    }

    public void invalidateLayout(Container container) {}

    public void layoutContainer(Container target)
    {
        Insets insets = target.getInsets();
        int count = target.getComponentCount();

        for (int i = 0; i < count; i++)
        {
            Component component = target.getComponent( i );

            if ( component.isVisible() ) {
                setComponentBounds(target, component, insets);
            }
        }
    }

    static Rectangle bounds_ = new Rectangle();
    static Rectangle nubounds_ = new Rectangle();

    private void setComponentBounds(Container parent, Component child, Insets insets)
    {
        try {
            
            if (child instanceof ScrollGameboard)
            {
                // this layout sizes the gameboard to the largest size to fit the 
                // container
                ScrollGameboard sg = (ScrollGameboard) child;
                Dimension newsize = sg.getGameboard().getPreferredSize(parent.getSize());
                child.setSize(newsize);
            }
            else if (child instanceof Gameboard)
            {
                
                // this layout sizes the gameboard to the largest size to fit the 
                // container
                Gameboard b = (Gameboard) child;
                Dimension newsize = b.getPreferredSize(parent.getSize());
                Dimension cursize = child.getSize();
                //System.out.println("GAMEBOARDCENTER oldsize to: " + cursize);
                //System.out.println("GAMEBOARDCENTER resized to: " + newsize + " based on " + container.getSize() + " " + container.getName());
  
                if (!cursize.equals(newsize))
                {
                    b.setNewSize(newsize);
                }
            }
            else
            {
                Object oC = info.get(child);
                if (oC instanceof ScaleConstraintsFixed)
                {
                    ScaleLayout.layoutScaleConstraintsFixed((ScaleConstraintsFixed) oC, child, parent, insets);
                    return;
                }
                child.setSize( child.getPreferredSize() );
            }
            
            Dimension size = child.getSize();
            Dimension containerSize = parent.getSize();
            Insets containerInsets = parent.getInsets();
            containerSize.width -= containerInsets.left + containerInsets.right;
            containerSize.height -= containerInsets.top + containerInsets.bottom;
            int componentLeft = (containerSize.width / 2) - (size.width / 2);
            int componentTop = (containerSize.height / 2) - (size.height / 2);
            componentLeft += containerInsets.left;
            componentTop += containerInsets.top;

            if (child.getX() != componentLeft ||
                child.getY() != componentTop ||
                child.getWidth() != size.width ||
                child.getHeight() != size.height)
            {
                child.setBounds( componentLeft, componentTop, size.width, size.height );
            }
         }
             catch( Exception e ) {
         }
    }
}
