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
 * DrawingUtil.java
 *
 * Created on November 12, 2002, 5:03 PM
 */

package com.donohoedigital.games.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.geom.*;

import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
/**
 *
 * @author  Doug Donohoe
 */
public class DrawingUtil implements KeyListener, MouseMotionListener, MouseListener {
    
    JComponent component_;
    int nPointSize_ = 8;
    int nPointRadius_ = nPointSize_ / 2;
    StatusDisplay status_;
    MapPoints allPoints_;
    DrawingUtilInterface drawee_;
    double dScale_ = 1.0;
    
    private boolean bDragging_ = false; // currently being dragged?
    private boolean bCtrlDown_ = false;  // shift key currently down?
    private boolean bShiftDown_ = false; // alt key currently down?
    
    MapPoint pointAtMouse_; // point under mouse
    MapPoint pointSelected_;   // point selected (with focus)
    
    /** 
     * Creates a new instance of DrawingUtil 
     */
    public DrawingUtil(DrawingUtilInterface drawee, JComponent component, MapPoints allPoints, StatusDisplay status) {
        drawee_ = drawee;
        component_ = component;
        component_.addKeyListener(this);
        component_.addMouseListener(this);
        component_.addMouseMotionListener(this);
        component_.setFocusable(true);
        component_.setFocusTraversalKeysEnabled(false);
        status_ = status;
        allPoints_ = allPoints;
    }
    
    /**
     * Set point size
     */
    public void setPointSize(int nPointSize)
    {
        nPointSize_ = nPointSize;
        nPointRadius_ = nPointSize_ / 2; // used often so store it
    }
    
    /**
     * Get point size
     */
    public int getPointSize()
    {
        return nPointSize_;
    }    
    
    /**
     * Get point size
     */
    public int getPointRadius()
    {
        return nPointRadius_;
    }    
    
   /**
     * Change point size
     */
    public void changePointSize(int nIncrementBy)
    {
        if (nPointSize_ + nIncrementBy < 0) return;
        setPointSize(getPointSize() + nIncrementBy);
        status_.setStatus("Point size now " + getPointSize());
        component_.repaint();
    }
    
    /**
     * set scale currently in use
     */
    public void setScale(double dScale)
    {
        dScale_ = dScale;
    }
    
    /**
     * scale value from current space to map space
     */
    public int scaleToMapSpace(int nValue)
    {
        return (int) (nValue / dScale_);
    }
    
    /**
     * Scale value from map space to current space
     */
    public int scaleToCurrentSpace(int nValue)
    {
        return (int) (nValue * dScale_);
    }
    
    /**
     * Paint given point (assumes color set in g before call).  x/y are scaled correctly
     * to current space
     */
    public void paintPoint(Graphics2D g, MapPoint point, boolean bDrawCross)
    {   
        if (nPointSize_ == 0) return;
        
        int x =  scaleToCurrentSpace(point.getX());
        int y =  scaleToCurrentSpace(point.getY());
        
        // figure out bounds
        int xleft = x - nPointRadius_;
        int xright = x + nPointRadius_;
        int ytop = y - nPointRadius_;
        int ybottom = y + nPointRadius_;
        
        // draw circle
        g.drawOval(xleft, ytop, nPointSize_, nPointSize_);
        
        // draw point in the middle
        g.drawLine(x,y,x,y);
        
        if (bDrawCross)
        {
            // draw + to indicate anchor
            g.drawLine(xleft + 1, y, xright-1, y); // left to right
            g.drawLine(x, ytop + 1, x, ybottom-1);  // top to bottom
        }
    }  
    
    // paint area around point
    public void repaintPoint(MapPoint point)
    {
        int nPointSize = getPointSize();
        Rectangle rect = new Rectangle(scaleToCurrentSpace(point.x_) - nPointSize, 
                                        scaleToCurrentSpace(point.y_)-nPointSize, 
                                       nPointSize *2, nPointSize*2);
        component_.repaint(rect);
    }
    
    public MapPoint findPointUnderMouse(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        
        // figure out bounds around pointer
        int nPointRadius = getPointRadius();
        int minx = x - nPointRadius;
        int maxx = x + nPointRadius;
        int miny = y - nPointRadius;
        int maxy = y + nPointRadius;
        
        MapPoint point;
        int nSize = allPoints_.size();
        int pointx, pointy;
        
        // TODO: faster way to find point under mouse?
        for (int i = 0; i < nSize; i++)
        {
            point = allPoints_.getMapPoint(i);
            pointx = scaleToCurrentSpace(point.x_);
            pointy = scaleToCurrentSpace(point.y_);
            
            if (pointx >= minx && pointx <= maxx &&
                pointy >= miny && pointy <= maxy)
            {
                if (drawee_.verifyPointUnderMouse(point))
                {
                    return point;
                }
            }
        }
        return null;
    }

    /**
     * Set cursor and focus depending on where mouse is and what's going on
     */
    private void setCursor()
    {
        // if we are dragging, make sure we have the MOVE cursor
        if (bDragging_) 
        {
            setNewCursor(Cursors.MOVE);
            return;
        }
        
        if (pointAtMouse_ != null)
        {
            if (bCtrlDown_)
            {
                setNewCursor(Cursors.DELETE);
            }
            else if (bShiftDown_)
            {
                setNewCursor(Cursors.MOVE);
            }
            else
            {
                setNewCursor(Cursors.SELECT);
            }
        }  
        else // return mouse to normal if not inside this component
        {
            setNewCursor(null);
        }
    }
    
    /**
     * Sets a new cursor if different than current one
     */
    private void setNewCursor(Cursor c)
    {
        Cursor old = component_.getCursor();
        if (c != old)
        {
            component_.setCursor(c);
        }
    }
    
    /** Invoked when a key has been pressed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key pressed event.
     *
     */
    public void keyPressed(KeyEvent e) {
        if (e.isConsumed()) return;
        
        int mult = 1;
        if (e.isShiftDown()) mult = 25;
        
        switch (e.getKeyCode())
        {
            case KeyEvent.VK_I:
                mult = 100;
                if (e.isShiftDown()) mult = 2500;
                if (e.isControlDown()) mult = 1;
                drawee_.scaleAllRelative(-mult);
                break;
                
            case KeyEvent.VK_O:
                mult = 100;
                if (e.isShiftDown()) mult = 2500;
                if (e.isControlDown()) mult = 1;
                drawee_.scaleAllRelative(mult);
                break;
                
            case KeyEvent.VK_DOWN:
                    if (e.isAltDown())
                    {
                        drawee_.moveAllRelative(0,1*mult);
                    }
                    else if (pointSelected_ != null) {
                        if (e.isShiftDown())
                        {
                            drawee_.moveRelative(pointSelected_,0,1);
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_UP:
                    if (e.isAltDown())
                    {
                        drawee_.moveAllRelative(0,-1*mult);
                    }
                    else if (pointSelected_ != null) {
                        if (e.isShiftDown())
                        {
                            drawee_.moveRelative(pointSelected_,0,-1);
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_LEFT:
                    if (e.isAltDown())
                    {
                        drawee_.moveAllRelative(-1*mult,0);
                    }
                    else if (pointSelected_ != null) {
                        if (e.isShiftDown())
                        {
                            drawee_.moveRelative(pointSelected_,-1,0);
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_RIGHT:
                    if (e.isAltDown())
                    {
                        drawee_.moveAllRelative(1*mult,0);
                    }
                    else if (pointSelected_ != null) {
                        if (e.isShiftDown())
                        {
                            drawee_.moveRelative(pointSelected_,1,0);
                            e.consume();
                        }
                    }
                    break;
            case KeyEvent.VK_DELETE:
                    drawee_.delete(pointSelected_);
                    e.consume();
                    break;
            case KeyEvent.VK_SHIFT:
                    bShiftDown_ = true;
                    setCursor();
                    break;
            case KeyEvent.VK_CONTROL:
                    bCtrlDown_ = true;
                    setCursor();
                    break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                    changePointSize(1);
                    e.consume();
                    break;

            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
                    changePointSize(-1);
                    e.consume();
                    break;
        }
    }
    
    /**
     * Empty
     */
    public void keyReleased(KeyEvent e) {
        switch(e.getKeyCode())
        {
            case KeyEvent.VK_SHIFT:
                        bShiftDown_ = false;
                        setCursor();
                        break;
            case KeyEvent.VK_CONTROL:
                        bCtrlDown_ = false;
                        setCursor();
                        break;
            default:
                        break;
        }
    }
    
    /** 
     * Empty
     */
    public void keyTyped(KeyEvent e) {
    }
    
    
    ///
    /// Mouse Motion Listener methods
    ///
    
    /**
     * Set flag to indicate whether mouse is inside this component
     * and checks whether shift is down (need to check since we
     * wouldn't have gotten that key event)
     */
    private void checkMouseKeys(MouseEvent e)
    {
        bShiftDown_ = e.isShiftDown();
        bCtrlDown_ = e.isControlDown();
    }
    
    public void mouseDragged(MouseEvent e) {
        checkMouseKeys(e);
        
        if (pointAtMouse_ != null)
        {
            if (bCtrlDown_ || !bShiftDown_) {
                if (bDragging_) component_.repaint();
                bDragging_ = false;
                return;
            }
        
            bDragging_ = true;
            setCursor();

            drawee_.moveRelative(pointAtMouse_, e.getPoint().x-scaleToCurrentSpace(pointAtMouse_.x_), 
                                                e.getPoint().y-scaleToCurrentSpace(pointAtMouse_.y_));
        }
    }

    public void mouseMoved(MouseEvent e) {
        checkMouseKeys(e);
        pointAtMouse_ = findPointUnderMouse(e);
        setCursor();
    }
    
    ///
    /// Mouse Listener methods
    ///
    
    public void mouseClicked(MouseEvent e) {
        component_.requestFocus(); // make sure we have focus
    }
    
    public void mouseReleased(MouseEvent e) 
    {
        checkMouseKeys(e);
        
        if (pointAtMouse_ != null)
        {
            if (bDragging_) {
                bDragging_ = false;
                component_.repaint();
                setCursor();
                return;
            }

            setCursor();

            // ctrl-click to remove
            if (bCtrlDown_)
            {
                drawee_.delete(pointAtMouse_);
            }
            else
            {
                pointSelected_ = pointAtMouse_;
                drawee_.changeFocus(pointAtMouse_);
            }
            e.consume();
        }
        else
        {
            if (!e.isControlDown())
            {
                drawee_.processMouseClick(e);
                e.consume();
            }
            
        }
    }
    
    public void mouseEntered(MouseEvent e) {
        checkMouseKeys(e);
        setCursor();
    }
    
    public void mouseExited(MouseEvent e) {
        checkMouseKeys(e);
        setCursor();
    }
    
    public void mousePressed(MouseEvent e) {
    }
}
