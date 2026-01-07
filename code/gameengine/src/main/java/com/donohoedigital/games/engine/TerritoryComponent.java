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
 * TerritoryComponent.java
 *
 * Created on Januray 15, 2002, 3:19 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 *
 * @author  Doug Donohoe
 */
public class TerritoryComponent extends DDPanel
{
    //static Logger logger = LogManager.getLogger(TerritoryComponent.class);
    
    Territory territory_;
    boolean bLabelDrawn_ = false;
    boolean bImageDrawn_ = false;
    Gameboard board_ = null;
    
    /**
     * Creates a new instance of TerritoryComponent 
     */
    public TerritoryComponent(Territory t) 
    {
        territory_ = t;
        setForeground(Color.darkGray);
        setBackground(Color.lightGray);
    }
    
    /**
     * Set territory we are drawing
     */
    public void setTerritory(Territory t)
    {
        territory_ = t;
    }
    
    /**
     * Get Territory we are drawing
     */
    public Territory getTerritory()
    {
        return territory_;
    }
    
    /**
     * Set whether label is drawn
     */
    public void setLabelDrawn(boolean b)
    {
        bLabelDrawn_ = b;
    }
  
    /**
     * Get whether label is drawn
     */
    public boolean isLabelDrawn()
    {
        return bLabelDrawn_;
    }    
    
    /**
     * Set whether image is drawn
     */
    public void setImageDrawn(boolean b, Gameboard board)
    {
        bImageDrawn_ = b;
        board_ = board;
    }
    
    /**
     * Get whether image is drawn
     */
    public boolean isImageDrawn()
    {
        return bImageDrawn_;
    }
    
    /**
     * Do painting
     */
    public void paintComponent(Graphics g1) 
    {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;
        if (territory_ != null) {
            drawTerritory(territory_, g);
        }
    }
    
    private static Font font_ = StylesConfig.getFont("territory.label");
    // BUG 133 - static (only draw one at a time)
    private static Rectangle bounds_ = new Rectangle();    
    
    /**
     * Draw territory the piece belongs to, scaled to size of 
     * this component
     */   
    public void drawTerritory(Territory t, Graphics2D g)
    {
        g.getClipBounds(bounds_);
        GeneralPath path = t.getPath();
        Rectangle pathbounds = t.getPathBounds();
        
        AffineTransform old = g.getTransform();
        Color cOld = g.getColor();
        Stroke sOld = g.getStroke();
        
        double width = getWidth();
        double height = getHeight();
        
        int BUFFER = 10;
        double dScale =  Math.min((width-BUFFER) / pathbounds.getWidth(),
                                    (height-BUFFER) / pathbounds.getHeight());      
                             
        double pathwidth = pathbounds.getWidth() * dScale;
        double pathheight = pathbounds.getHeight() * dScale;
        
        double adjusty = 0;
        double adjustx = 0;
        
        if (pathwidth < width)
        {
            adjustx = (width - pathwidth) / 2;
        }
        
        if (pathheight < height)
        {
            adjusty = (height - pathheight) / 2;
        }
        
        
        // scale & move territory to draw
        g.scale(dScale, dScale);
        g.translate(-pathbounds.x +(adjustx / dScale), 
                    -pathbounds.y +(adjusty / dScale));
        
        if (bImageDrawn_ && board_ != null)
        {
            BufferedImage image = board_.getImage();
            
            // bounds of image which represent the border area "clipped" from
            // the territory bounds points
            Rectangle imagebounds = board_.getImageBounds();
            
            // original scale of territory path to the image
            double imagescale = board_.getGameboardConfig().getScale().doubleValue();
            
            // figure out source bounds on map
            int srcx = (int)(pathbounds.x/imagescale) + imagebounds.x;
            int srcy = (int)(pathbounds.y/imagescale) + imagebounds.y;
            int srcx2 = srcx + (int) (pathbounds.width/imagescale);
            int srcy2 = srcy + (int) (pathbounds.height/imagescale);
            
            // draw only territory
            Shape oldClip = g.getClip();
            g.clip(path);
            
            // draw image
            g.drawImage(image, (int) (pathbounds.x),
                               (int) (pathbounds.y),
                               pathbounds.x + pathbounds.width, pathbounds.y + pathbounds.height,
                               srcx, srcy, srcx2, srcy2,
                               this);
            
            // reset clip
            g.setClip(oldClip);
        }

        // always draw border of 1
        BasicStroke borderStroke = new BasicStroke((float)(1.0d / dScale),
                                                BasicStroke.CAP_BUTT,
                                                BasicStroke.JOIN_ROUND);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
                            
        // fill
        if (!bImageDrawn_)
        {
            g.setColor(getBackground());
            g.fill(path);
        }
        
        // border
        g.setColor(getForeground());
        g.setStroke(borderStroke);
        g.draw(path);

        // reset stuff
        g.setTransform(old);
        g.setColor(cOld);
        g.setStroke(sOld);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_OFF);
        
        // label
        if (bLabelDrawn_)
        {
            TerritoryPoint tp = t.getTerritoryPoint(Territory.LABEL_LOCATION);
            if (tp != null)
            {
                // need to adjust points for current scale
                double dx = (double)(tp.getX()-pathbounds.x+(adjustx / dScale)) * dScale;
                double dy = (double)(tp.getY()-pathbounds.y+(adjusty / dScale)) * dScale;
                
                // draw name
                TextUtil tu = new TextUtil(g, font_, t.getMapDisplayName());
                tu.prepareDraw(dx, dy, tp.getAngle(), dScale, true);
                tu.drawString(getForeground().brighter(), Color.white);
                tu.finishDraw();
            }
        }


    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    public void repaint()
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, 0, 0, foo);
            foo.repaint(pRepaint.x, pRepaint.y, getWidth(), getHeight());
        }
        else 
        {
            super.repaint();
        }
    }
}
