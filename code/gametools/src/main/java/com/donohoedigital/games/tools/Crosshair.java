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
 * Crosshair.java
 *
 * Created on October 29, 2002, 8:16 PM
 */

package com.donohoedigital.games.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

/**
 * Draw a cross hair cursor
 *
 * @author  Doug Donohoe
 */
public class Crosshair extends JComponent
{
    
    //static Logger logger = LogManager.getLogger(Crosshair.class);
    
    private XYConstraints xyConstraints_; // constraints used to manage this point
    
    static int defaultSize = 17;
    private boolean bDraw_ = true;
    
    /**
     * Create new border point
     */
    public Crosshair(int x, int y)
    {
        super();
        
        // placement is centered on x,y passed in
        int xPos = x - (defaultSize/2); 
        int yPos = y - (defaultSize/2);
        
        // create constraints used to manage this point
        xyConstraints_ = new XYConstraints(xPos, yPos, defaultSize, defaultSize);
       
 		// no focus on this 
        setFocusTraversalKeysEnabled(false);
    }
    
    /**
     * return xyconstraints used by this point
     */
    public XYConstraints getXYConstraints()
    {
        return xyConstraints_;
    }
    
    /**
     * Set the point size (used for new crosshairs)
     */
    public static void setDefaultSize(int nSize)
    {
        defaultSize = nSize;
    }
    
    /**
     * Set whether to draw the crosshair
     */
    public void setDrawing(boolean b)
    {
        bDraw_ = b;
        repaint();
    }
    
    /**
     * Return whether drawing
     */
    public boolean isDrawing()
    {
        return bDraw_;
    }
    
    /**
     * Get default size (used for new crosshairs)
     */
    public static int getDefaultSize()
    {
        return defaultSize;
    }
    
    /**
     * Paint this component
     */
    protected void paintComponent(Graphics g) 
    {
        if (!bDraw_) return;
        
        Dimension size = this.getSize();
        
        g.setColor(Color.red);
               
        // draw point in the middle
        int gap = 4;
        int middle = size.width/2; // assumes this is square
        g.drawLine(middle, middle, middle, middle);
        
        g.drawLine(0,0, middle-gap, middle-gap);
        g.drawLine(middle+gap, middle+gap, size.width-1, size.height-1);
        
        g.drawLine(0, size.height-1,  middle-gap,middle+gap);
        g.drawLine(middle+gap, middle-gap, size.width-1, 0);
    }
    
    /**
     * Moves this component x,y points relative to current position
     */
    public void moveRelative(int x, int y)
    {
        // adjust constraints to match
        xyConstraints_.setX(xyConstraints_.getX() + x);
        xyConstraints_.setY(xyConstraints_.getY() + y);
        
        // move point itself
        setLocation(getX() + x, getY() + y);
    }
    
    /**
     * Moves this component to x,y
     */
    public void moveTo(int x, int y)
    {
        x = x - (defaultSize/2);
        y = y - (defaultSize/2);
        // adjust constraints to match
        xyConstraints_.setX(x);
        xyConstraints_.setY(y);
        
        // move point itself
        setLocation(x,y);
    }
}
