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
 * ScaleLayout.java
 *
 * Created on January 3, 2004, 4:32 PM
 */

package com.donohoedigital.gui;


import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;


/**
 *
 * @author  Doug Donohoe
 */
public class ScaleLayout implements LayoutManager2, Cloneable 
{
    static Logger logger = LogManager.getLogger(ScaleLayout.class);

    
    private static final ScaleConstraints defaultConstraints = new ScaleConstraints();
    
    private HashMap info = new HashMap();
    
    public ScaleLayout() {
    }

    public void addLayoutComponent( String s, Component component1 ) 
    {
        throw new UnsupportedOperationException("Not used");
    }
    
    public void addLayoutComponent( Component component, Object constraints ) {
        if ( constraints instanceof ScaleConstraints  ||
             constraints instanceof ScaleConstraintsFixed) {
            info.put( component, constraints );
        }
    }

    public void removeLayoutComponent(Component component) {
        info.remove(component);
    }

    public Dimension preferredLayoutSize(Container target) {
        return target.getPreferredSize();
    }

    public Dimension minimumLayoutSize(Container target) {
        return target.getMinimumSize();
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

        //logger.debug("SCALE LAYOUT ********************* " + target.getSize());
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
        Object oC = info.get(child);

        if (oC instanceof ScaleConstraints)
        {
            ScaleConstraints constraints = (ScaleConstraints) oC;

            if (constraints == null) {
                constraints = defaultConstraints;
            }

            Dimension childSize = child.getPreferredSize();
            Dimension parentSize = parent.getSize();

            int x = (int) (parentSize.width * constraints.x);
            int y = (int) (parentSize.height * constraints.y);
            int width = (int) (parentSize.width * constraints.scale);
            int height = (width * childSize.height) / childSize.width;

            if (constraints.font != null)
            {
                float oldSize = constraints.font.getSize2D();
                float newSize = (width * oldSize) / childSize.width;

                Font fOld = child.getFont();
                Font fNew = constraints.font.deriveFont(newSize);
                if (fNew.getSize2D() != fOld.getSize2D())
                {
                    //logger.debug("New font size for " + child.getName() + " is " + newSize + "   old is " + fOld.getSize2D());
                    child.setFont(fNew);
                }
            }

            Rectangle bounds = child.getBounds(bounds_);

            nubounds_.x = insets.left + x;
            nubounds_.y = insets.top + y;
            nubounds_.width = width;
            nubounds_.height = height;
            if (nubounds_.equals(bounds)) return;

            //logger.debug("New bounds for " + child.getName() + " is " + nubounds_);
            //logger.debug("Old bounds for " + child.getName() + " is " + bounds_);
            child.setBounds(nubounds_);
        }
        else if (oC instanceof ScaleConstraintsFixed)
        {
            layoutScaleConstraintsFixed((ScaleConstraintsFixed) oC, child, parent, insets);
        }
    }

    public static void layoutScaleConstraintsFixed(ScaleConstraintsFixed constraints, Component child,
                                            Container parent, Insets insets)
    {
        Dimension childSize = child.getPreferredSize();
        Dimension parentSize = parent.getSize();

        int x=0, y=0;
        int width = childSize.width;
        int height = childSize.height;
        switch (constraints.getHorizontalAlign())
        {
            case SwingConstants.LEFT:
                x = 0;
                break;

            case SwingConstants.CENTER:
                x = (parentSize.width / 2) - (width / 2);
                break;

            case SwingConstants.RIGHT:
                x = parentSize.width - width;
                break;
        }

        switch (constraints.getVerticalAlign())
        {
            case SwingConstants.TOP:
                y = 0;
                break;

            case SwingConstants.CENTER:
                y = (parentSize.height / 2) - (height / 2);
                break;

            case SwingConstants.BOTTOM:
                y = parentSize.height - height;
                break;
        }

        Rectangle bounds = child.getBounds(bounds_);

        nubounds_.x = insets.left + x;
        nubounds_.y = insets.top + y;
        nubounds_.width = width;
        nubounds_.height = height;
        if (nubounds_.equals(bounds)) return;

        //logger.debug("New bounds for " + child.getName() + " is " + nubounds_);
        //logger.debug("Old bounds for " + child.getName() + " is " + bounds_);
        child.setBounds(nubounds_);
    }
}
