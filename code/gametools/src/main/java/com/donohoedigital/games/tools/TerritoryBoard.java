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
 * TerritoryBoard.java
 *
 * Created on November 11, 2002, 3:50 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.gui.*;
import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class TerritoryBoard extends Gameboard implements KeyListener, DrawingUtilInterface {
    
    static Logger logger = LogManager.getLogger(TerritoryBoard.class);
    
    BaseFrame frame_;
    GameboardTerritoryManager manager_;
    DrawingUtil util_;
    TerritoryPoint territoryPointSelected_;
    MapPoints allPoints_;
    
    /** 
     * Creates a new instance of TerritoryBoard 
     */
    public TerritoryBoard(BaseFrame frame, GameboardTerritoryManager manager, 
                                GameboardConfig gameconfig) 
    {
        super(gameconfig, false); 
        bAntiAlias_ = false;
        manager_ = manager;
        frame_ = frame;
        allPoints_ = gameconfig.getMapPoints();
        util_ = new DrawingUtil(this, this, allPoints_, manager);
       
        // listeners
        addKeyListener(this);
    }

    /**
     * Used for our drawTerritoryPart
     */
    public static final int PART_POINTS = 100;
    
    /**
     * Paint this component by painting superclass then adding points
     */
    protected void paintComponent(Graphics g1) 
    {
        Graphics2D g = (Graphics2D) g1;
        super.paintComponent(g);
        
        drawTerritories(g, PART_POINTS);
        
        // draw line around area where borders end
        g.setColor(Color.yellow);
        Rectangle bounds = gameconfig_.getMapBounds();
        g.drawRect(
            scaleToCurrentSpace(bounds.x), 
            scaleToCurrentSpace(bounds.y), 
            scaleToCurrentSpace(bounds.width), 
            scaleToCurrentSpace(bounds.height));
    }
    
    /**
     * draw our points - otherwise call superclass (called from super.drawTerritories
     * if territory needs to be repainted)
     */
    protected void drawTerritoryPart(Graphics2D g, Territory t, GeneralPath path, 
                                    Rectangle territoryBounds, int iPart)
    {
        switch (iPart)
        {
            case PART_POINTS:
                drawTerritoryPoints(g, t);
                break;
                
            default:
                super.drawTerritoryPart(g, t, path, territoryBounds, iPart);
        }
    }
    
    /**
     * Draw all points in given territory
     */
    private void drawTerritoryPoints(Graphics2D g, Territory t)
    {
        TerritoryPoints points = t.getTerritoryPoints();
        TerritoryPoint point;
        int nNum = points.size();
        

        for (int i = 0; i < nNum; i++)
        {
            point = points.getTerritoryPoint(i);
            
            if (territoryPointSelected_ == point)
            {
                g.setColor(Color.blue);
            }
            else
            {
                g.setColor(Color.black);
            }
            
            util_.paintPoint(g, point, true);
        }
    }
    
    /**
     * Override so we can notify util of scale
     */
    public void setNewSize(Dimension size, boolean bRepaint)
    {
        super.setNewSize(size, bRepaint);
        util_.setScale(getLastScale());
    }
    
    TerritoryPointChooser chooser_;
    
    public void changeFocus(MapPoint point) {
        territoryPointSelected_ = (TerritoryPoint) point;
        util_.pointSelected_ = point;
        updateStatus(territoryPointSelected_);
        repaint();
    }   
    
    private void updateStatus(TerritoryPoint point)
    {
        if (point == null)
        {
            manager_.setStatus("No point selected.");
        }
        else
        { 
            Territory t = territoryPointSelected_.getTerritory();
            String sScale = " [" + t.getScaleImages() + "%]";
            manager_.setStatus(t.getName() + sScale +
                                " - " + point);
        }
    }
    
    public void delete(MapPoint point) {
        if (point instanceof TerritoryPoint) {
            TerritoryPoint tp = (TerritoryPoint) point;
            Territory t = tp.getTerritory();
            
            TerritoryPoint next = tp.getNearestPoint();

            if (next != null)
            {
                changeFocus(next);
            }
            t.removeTerritoryPoint(tp);
            repaintTerritory(t);
        }
    }
    
    public void moveRelative(MapPoint point, int x, int y)
    {
        _moveRelative(point, x, y, true);
    }
    
    private void _moveRelative(MapPoint point, int x, int y, boolean bRepaint) {
        
        point.setX(point.getX() + x);
        point.setY(point.getY() + y);

        if (point == territoryPointSelected_)
        {
            updateStatus(territoryPointSelected_); // to update status
        }
        if (bRepaint) repaint();
    }
    
    public void moveAllRelative(int x, int y) 
    {
        for (int i = 0; i < allPoints_.size(); i++)
        {
            _moveRelative(allPoints_.getMapPoint(i), x, y, false);
        }
        repaint();
    }
    
    public void scaleAllRelative(int amount)
    {
        // do nothing
    }
    
    
    public void processMouseClick(MouseEvent e) {
        Territory t = getTerritoryUnderMouse();
        if (t != null &&  !t.isDecoration())
        {
            // create new point
            int mapx = scaleToMapSpace(e.getPoint().x);
            int mapy = scaleToMapSpace(e.getPoint().y);
            TerritoryPoint point = new TerritoryPoint(mapx, mapy, "temp");
            
            // add to list of everything - which checks for duplicates
            if (allPoints_.contains(point))
            {
                logger.warn("New territory point already exists as a defined point.  Ignoring it! - " + point.shortDesc());
                return;
            }
            
            allPoints_.addMapPoint(point);
            t.addTerritoryPoint(point);
            util_.repaintPoint(point);
                
            if (chooser_ == null)
            {
                chooser_ = new TerritoryPointChooser(frame_, "Choose Type of Territory Point");
            }
            String sType = TerritoryPointChooser.getTerritoryPointType(chooser_, t, e.getX(), e.getY());
            if (sType != null)
            {
                point.setType(sType);
                changeFocus(point);
                repaint();
            }
            else
            {
                allPoints_.removeMapPoint(point);
                t.removeTerritoryPoint(point);
                util_.repaintPoint(point);
            }
            requestFocus();
        }
    }
    
    public boolean verifyPointUnderMouse(MapPoint point) {
        if (point instanceof TerritoryPoint) return true;
        return false;
    }
    
    ///
    /// Key Listener methods
    ///
    
    /** 
     * Handle specific key presses not covered by DrawingUtil
     */
    public void keyPressed(KeyEvent e) 
    {
        if (e.isConsumed()) return;
        
        switch (e.getKeyCode())
        {                   
            // tab amongst points in border
            case KeyEvent.VK_TAB:
                    if (territoryPointSelected_ != null) {
                        if (e.isShiftDown())
                        {
                            changeFocus(territoryPointSelected_.getPrevPoint());
                        }
                        else
                        {
                            changeFocus(territoryPointSelected_.getNextPoint());
                        }
                        e.consume();
                    }
                    break;
                    
            case KeyEvent.VK_COMMA:
            case KeyEvent.VK_LESS:
                    if (territoryPointSelected_ != null)
                    {
                        int nAngle = territoryPointSelected_.getAngle().intValue() - 1;
                        if (nAngle < 0) nAngle = 359;
                        territoryPointSelected_.setAngle(nAngle);
                        repaintTerritory(territoryPointSelected_.getTerritory());
                        updateStatus(territoryPointSelected_);
                    }
                    break;
                    
            case KeyEvent.VK_PERIOD:
            case KeyEvent.VK_GREATER:
                    if (territoryPointSelected_ != null)
                    {
                        int nAngle = territoryPointSelected_.getAngle().intValue() + 1;
                        if (nAngle >= 360) nAngle = 0;
                        territoryPointSelected_.setAngle(nAngle);
                        repaintTerritory(territoryPointSelected_.getTerritory());
                        updateStatus(territoryPointSelected_);
                    }
                    break;

            case KeyEvent.VK_LEFT_PARENTHESIS:
            case KeyEvent.VK_9:
                    if (territoryPointSelected_ != null)
                    {
                        Territory t = territoryPointSelected_.getTerritory();
                        int nPercent = t.getScaleImages() - 1;
                        if (nPercent < 10) nPercent = 10;
                        t.setScaleImages(nPercent);
                        repaintTerritory(t);
                        updateStatus(territoryPointSelected_);
                    }
                    break;
                    
            case KeyEvent.VK_RIGHT_PARENTHESIS:
            case KeyEvent.VK_0:
                    if (territoryPointSelected_ != null)
                    {
                        Territory t = territoryPointSelected_.getTerritory();
                        int nPercent = t.getScaleImages() + 1;
                        t.setScaleImages(nPercent);
                        repaintTerritory(t);
                        updateStatus(territoryPointSelected_);
                    }
                    break;

        }
    }
    
    /**
     * Empty
     */
    public void keyReleased(KeyEvent e) {
    }
    
    /**
     * Empty
     */
    public void keyTyped(KeyEvent e) {
    }

}
