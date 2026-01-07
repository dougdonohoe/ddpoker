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
/*
 * CenterLayout.java
 *
 * Created on November 15, 2002, 4:19 PM
 */
package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 *
 */
public class CenterLayout implements LayoutManager, Serializable {
    public void addLayoutComponent(String name, Component comp) { }
    public void removeLayoutComponent(Component comp) { }

    public Dimension preferredLayoutSize( Container container ) {
	Component c = container.getComponent( 0 );
	if ( c != null ) {
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

    public void layoutContainer(Container container) 
    {
        try 
        {
            // component
            Component c = container.getComponent( 0 );
            Dimension pref = c.getPreferredSize();

            // parent size
            Dimension containerSize = container.getSize();
            Insets containerInsets = container.getInsets();
            containerSize.width -= containerInsets.left + containerInsets.right;
            containerSize.height -= containerInsets.top + containerInsets.bottom;

            // if component is less than parent and is scrollable, 
            //set it to size of parent so scrollbars work
            if (c instanceof JScrollPane)
            {
                pref = new Dimension(pref);
                if (pref.width > containerSize.width) pref.width = containerSize.width;
                if (pref.height > containerSize.height) pref.height = containerSize.height;
            }
            c.setSize(pref);

            // figure out position
            int componentLeft = (containerSize.width / 2) - (pref.width / 2);
            int componentTop = (containerSize.height / 2) - (pref.height / 2);
            componentLeft += containerInsets.left;
            componentTop += containerInsets.top;

            // set it
            c.setBounds( componentLeft, componentTop, pref.width, pref.height );
        }
        catch( Exception e ) {
        }
    }
}
