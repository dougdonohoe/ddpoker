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
 * XPoints.java
 *
 * Created on November 6, 2002, 4:54 PM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.games.config.*;
import org.apache.logging.log4j.*;

import javax.swing.FocusManager;
import java.awt.*;
import java.awt.event.*;


/**
 *
 * @author  Doug Donohoe
 */
public class XPoints extends XConnectorLines implements KeyListener,
                                                DrawingUtilInterface {
    
    static Logger logger = LogManager.getLogger(XPoints.class);
    protected MapPoints allPoints_;
    private GameboardConfig gameconfig_;
    private GameboardBorderManager manager_;
        
    DrawingUtil util_;
    BorderPoint borderPointSelected_;   // border point selected (with focus)
    Border      borderSelected_;  /// border that is selected
    
    /** 
     * Creates a new instance of XPoints 
     */
    public XPoints(GameboardBorderManager manager, GameboardConfig gameconfig, 
                        double dScaleFactor, boolean bNoImage) 
    {
        super(gameconfig.getBorders(), gameconfig.getImage(), dScaleFactor, bNoImage);
        
        util_ = new DrawingUtil(this, this, gameconfig.getMapPoints(), manager);
        gameconfig_ = gameconfig;
        allPoints_ = gameconfig_.getMapPoints();
        manager_ = manager;
        
        // listeners
        addKeyListener(this);
    }
    
    /**
     * Set point size
     */
    public void setPointSize(int nPointSize)
    {
        util_.setPointSize(nPointSize);
    }
    
    // used for performance so new rect isn't needed everytime we repaint
    private Rectangle bounds_ = new Rectangle();
    
    ///
    /// Drawing methods
    ///
    
    /**
     * Paint this component
     */
    protected void paintComponent(Graphics g1) 
    {
        Graphics2D g = (Graphics2D) g1;
        super.paintComponent(g, borderSelected_);
        
        // get region we are painting
        g.getClipBounds(bounds_);
        //logger.debug("Repainting: " + bounds_);
        
        // calc min/max x,y so that
        // off-screen points will be drawn
        int nPointRadius = util_.getPointRadius();
        
        int minx = bounds_.x - nPointRadius;
        int maxx = bounds_.x + bounds_.width + nPointRadius;
        int miny = bounds_.y - nPointRadius;
        int maxy = bounds_.y + bounds_.height + nPointRadius;
        
        int nSize = allPoints_.size();
        MapPoint point;
        
        // paint all points needed to be repainted
        for (int i = 0; i < nSize; i++)
        {
            point = allPoints_.getMapPoint(i);
            if (point.x_ >= minx && point.x_ <= maxx &&
                point.y_ >= miny && point.y_ <= maxy)
            {
                if (point instanceof BorderPoint)
                {
                    paintBorderPoint(g, (BorderPoint) point, false);
                }
            }
        }
    }
    
    /**
     * Paint a point
     */
    private void paintBorderPoint(Graphics2D g, BorderPoint point, boolean bDragging)
    {
        Border border = point.getCurrentBorder();
        
        if (bDragging)
        {
            g.setColor(Color.white);
        }
        else if (borderPointSelected_ == point)
        {
            g.setColor(Color.yellow);
        }
        else
        {
            if (border == null)
            {
                g.setColor(Color.red);
            }
            else if (border == borderSelected_)
            {
                g.setColor(Color.blue);
            }
            else
            {
                g.setColor(Color.black);
            }
        }
        
        util_.paintPoint(g, point, point.isAnchor());
    }
    
    ///
    /// convienence methods
    ///
    
    /**
     * Change focus to this point
     */
    public void changeFocus(MapPoint point)
    {
        borderPointSelected_ = null;
        if (point != null)
        {
            if (point instanceof BorderPoint)
            {
                BorderPoint bp = (BorderPoint) point;
                changeFocus(bp, bp.getCurrentBorder());
            }
        }
        else
        {
            manager_.borderPointSelected(null,null);
            repaint();
        }
    }

    /**
     * Change which border point / border has focus
     */
    private void changeFocus(BorderPoint point, Border border)
    {
        //logger.debug("changeFocus to: " + point);
        util_.pointSelected_ = point;
        borderPointSelected_ = point;
        borderSelected_ = border;
        if (point != null)
        {
            point.setCurrentBorder(border);
        }
        manager_.borderPointSelected(border, point);
        repaint();
    }
    
    /**
     * does a point have focus?
     */
    public boolean borderPointHasFocus()
    {
        return (borderPointSelected_ != null);
    }
    
    public boolean verifyPointUnderMouse(MapPoint point) {
        if (point instanceof BorderPoint) return true;
        return false;
    }

    /**
     * Create new BorderPoint and add it to map
     */
    public void processMouseClick(MouseEvent e)
    {
        // create new point
        BorderPoint point = new BorderPoint(e.getPoint().x, e.getPoint().y);
        
        // add to list of everything - which checks for duplicates
        if (allPoints_.contains(point))
        {
            logger.warn("New border point already exists as a defined point.  Ignoring it! - " + point.longDesc(null));
            return;
        }

        allPoints_.addMapPoint(point);
        util_.repaintPoint(point);
        
        // get currently selected border if there is one
        Border addToBorder = null;
        
        if (borderPointHasFocus())
        {
            addToBorder = borderPointSelected_.getCurrentBorder();
        }
        
        // if mouse button 2 pressed, add new border
        if (e.getButton() == e.BUTTON3 || addToBorder == null)
        {    
            Component cFocus = FocusManager.getCurrentManager().getFocusOwner();
            Border b = manager_.chooseBorder("Create New Border", point.getX(), point.getY(), false);
            if (b != null)
            {
                addToBorder = b;
            }
            else
            {
                allPoints_.removeMapPoint(point);
                util_.repaintPoint(point);
                requestFocus();
                return;
            }
        }

        // get index in array of selected point (returns -1 if none selected, i.e., borderPointSelected_==null)
        int nPoint = -1;
        if (borderPointHasFocus())
        {
            nPoint = addToBorder.getPointIndex(borderPointSelected_);
        }
        
        // add after current point (if -1, will add at end of last point)
        if (nPoint >=0) nPoint++; 
        addToBorder.addBorderPoint(point, nPoint);
        changeFocus(point, addToBorder);
        
        requestFocus();
    }
    
    /**
     * Deletes this point
     */
    public void delete(MapPoint point)
    {
        if (point instanceof BorderPoint) {
            delete((BorderPoint) point);
        }
    }
    
    /**
     * logic to delete borderpoint
     */
    private void delete(BorderPoint point)
    {
        if (point == null) return;
        
        Border nextBorder = point.nextBorder();
        Border currentBorder = point.getCurrentBorder();
        BorderPoint next = point.getNearestPoint();

        point.removeFromBorder(currentBorder);
        
        // if more borders, set next one as selected/current
        if (nextBorder != currentBorder)
        {
            changeFocus(point, nextBorder);
        } 
        // else get nearest point in border
        else 
        {
            if (point == borderPointSelected_)
            {
                changeFocus(next, currentBorder);
            }
            else
            {
                repaint();
            }
            allPoints_.removeMapPoint(point);
            if (point == util_.pointAtMouse_) util_.pointAtMouse_ = null;
        }
    }

    public void moveRelative(MapPoint point, int x, int y)
    {
        _moveRelative(point, x, y, true);
    }
    
    /**
     * Moves this component x,y points relative to current position
     */
    private void _moveRelative(MapPoint point, int x, int y, boolean bRepaint)
    {
        // update BorderPoint
        point.setX(point.getX() + x);
        point.setY(point.getY() + y);
        
        // update status bar
        if (point == borderPointSelected_) {
            manager_.borderPointSelected(borderSelected_,  borderPointSelected_);
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
        for (int i = 0; i < allPoints_.size(); i++)
        {
            _scaleRelative(allPoints_.getMapPoint(i), amount);
        }
        repaint();
    }
    
    private void _scaleRelative(MapPoint point, int amount)
    {
        double factor = amount * .0001;
        point.setX((int)(point.getX() + (point.getX() * factor)));
        point.setY((int)(point.getY() + (point.getY() * factor)));
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
            // add new border to selected point
            case KeyEvent.VK_B:
                    
                    if (borderSelected_ != null)
                    {
                        if (borderPointHasFocus())
                        {
                            Border newShared = manager_.chooseBorder("Add Shared Border to point " + 
                                                borderPointSelected_.shortDesc(),
                                                borderPointSelected_.getX(), borderPointSelected_.getY(), true);
                            if (newShared != null && !borderSelected_.equals(newShared))
                            {
                                newShared.addBorderPoint(borderPointSelected_); // add this point to new border
                                changeFocus(borderPointSelected_, newShared); // to update status bar
                                requestFocus(); // send keyboard focus back here
                            }
                        }
                    }
                    e.consume();
                    break;
                    
            // tab amongst points in border
            case KeyEvent.VK_TAB:
                    if (borderPointHasFocus()) {
                        if (e.isShiftDown())
                        {
                            changeFocus(borderPointSelected_.getPrevPoint(), borderSelected_);
                        }
                        else
                        {
                            changeFocus(borderPointSelected_.getNextPoint(), borderSelected_);
                        }
                        e.consume();
                    }
                    break;
                    
            // change the current border
            case KeyEvent.VK_C:
                    if (borderPointHasFocus()) {
                        Border next = borderPointSelected_.nextBorder();
                        changeFocus(borderPointSelected_, next);
                        e.consume();
                    }
                    break;
                    
           // toggle whether border is enclosed
           case KeyEvent.VK_E:
                    if (borderPointHasFocus()) {
                        Border eborder = borderPointSelected_.getCurrentBorder();
                        if (eborder != null)
                        {
                            eborder.setEnclosed(!eborder.isEnclosed());
                            changeFocus(borderPointSelected_, borderSelected_); // update status
                            repaint();
                        }
                        e.consume();
                    }
                    break;
                    
           // toggle whether border is wrapped
           case KeyEvent.VK_W:
                    if (borderPointHasFocus()) {
                        Border eborder = borderPointSelected_.getCurrentBorder();
                        if (eborder != null)
                        {
                            eborder.setWrapAround(!eborder.isWrapAround());
                            changeFocus(borderPointSelected_, borderSelected_); // update status
                            repaint();
                        }
                        e.consume();
                    }
                    break;
                    
            // crtl-arrows - shift point in border's array
            case KeyEvent.VK_DOWN:
                    if (borderPointHasFocus()) {
                        if (e.isControlDown())
                        {
                            borderPointSelected_.shift(true);
                            repaint();
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_UP:
                    if (borderPointHasFocus()) {
                        if (e.isControlDown())
                        {
                            borderPointSelected_.shift(false);
                            repaint();
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_LEFT:
                    if (borderPointHasFocus()) {
                        if (e.isControlDown())
                        {
                            borderPointSelected_.shift(false);
                            repaint();
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_RIGHT:
                    if (borderPointHasFocus()) {
                        if (e.isControlDown())
                        {
                            borderPointSelected_.shift(true);
                            repaint();
                            e.consume();
                        }
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
