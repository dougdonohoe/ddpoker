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
 * XConnectorLines.java
 *
 * Created on October 30, 2002, 8:59 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Draws lines between points in borders
 *
 * @author  Doug Donohoe
 */
public class XConnectorLines extends ImageComponent {
    
    //static Logger logger = LogManager.getLogger(XConnectorLines.class);
    
    protected Borders myBorders_ = null;
    protected boolean bNoImage_ = false;
    
    
    /** Creates a new instance of XConnectorLines */
    public XConnectorLines(Borders borders, String sName, double dScaleFactor,
                                boolean bNoImage) 
    {
        super(sName, dScaleFactor);
        myBorders_ = borders;
        bNoImage_ = bNoImage;
    }
    
    // used for performance so a new rect and line 
    // isn't needed everytime we repaint
    private Rectangle bounds_ = new Rectangle();
    private Line2D line_ = new Line2D.Float();
    
    protected void paintComponent(Graphics g)
    {
        paintComponent(g, null);
    }
    
    /**
     * Paint this component
     */
    protected void paintComponent(Graphics g1, Border borderSelected) 
    {
        Graphics2D g = (Graphics2D) g1;

        // fill background in case image has transparent bits
        g.setColor(Color.gray);
        g.fillRect(0,0,getWidth(), getHeight());
        
        // paint image
        if (!bNoImage_) super.paintComponent(g);
        
        // get region we are painting
        g.getClipBounds(bounds_);
        
        // paint lines
        Iterator iter = myBorders_.iterator();
        ArrayList points;
        int nNum;
        Border border;
        BorderPoint p1, p2;
        
        while (iter.hasNext())
        {
            border = (Border) iter.next();
            points = border.getBorderPoints();
            if (points == null || (nNum = points.size()) <= 1) continue;
            
            // Color depends on whether border is selected
            if (border == borderSelected){
                g.setColor(Color.blue);
            } else {
                g.setColor(Color.black);
            }
          
            // loop through points and draw line
            // between successive points
            for (int i = 1; i < nNum; i++)
            {
                p1 = border.getBorderPoint(i-1);
                p2 = border.getBorderPoint(i);
                
                // draw line if it intersects the clipping area
                line_.setLine(p1.x_, p1.y_, p2.x_, p2.y_);
                if (line_.intersects(bounds_)){
                    g.draw(line_);
                }
            }
            
            // draw line between first & last point if enclosed border
            if (border.isEnclosed())
            {
                p1 = border.getBorderPoint(0);
                p2 = border.getBorderPoint(nNum - 1);
                line_.setLine(p1.x_, p1.y_, p2.x_, p2.y_);
                if (line_.intersects(bounds_)){
                    g.draw(line_);
                }
            }
        }   
    }
    
}
