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
 * ScrollGameboard.java
 *
 * Created on December 13, 2002, 8:36 AM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.prefs.*;

/**
 *
 * @author  Doug Donohoe
 */
public class ScrollGameboard extends JViewport implements 
                MouseMotionListener, MouseListener, MouseWheelListener, 
                ComponentListener, FocusListener,
                AWTEventListener, InternalDialog.ModalBlockerListener
{
    //static Logger logger = Logger.getLogger(ScrollGameboard.class);
    
    GameEngine engine_;
    Gameboard board_;
    int nSmallWidth_, nSmallHeight_;
    int nStartingWidth_, nStartingHeight_;
    
    /**
     * Create new scrollgameboard
     */
    public ScrollGameboard(GameEngine engine, GameContext context, GameboardConfig gameconfig,
                           boolean bGameMode,
                           Territory tUpperLeft,
                           int nSmallWidth, int nSmallHeight)
    {
        this(engine, context, gameconfig, bGameMode, tUpperLeft, nSmallWidth, nSmallHeight, nSmallWidth, nSmallHeight, true);
    }
    
    /**
     * Create new scrollgameboard
     */
    public ScrollGameboard(GameEngine engine, GameContext context, GameboardConfig gameconfig,
                           boolean bGameMode,
                           Territory tUpperLeft,
                           int nSmallWidth, int nSmallHeight,
                           int nStartingWidth, int nStartingHeight,
                           boolean bScaleBig)
    {
        super();
        engine_ = engine;
        getClickToScroll();
        board_ = new Gameboard(engine, context, gameconfig, bGameMode, tUpperLeft, bScaleBig);
        board_.setScrollGameboard(this);
        setView(board_);
        setOpaque(false);
        nSmallWidth_ = nSmallWidth;
        nSmallHeight_ = nSmallHeight;
        nStartingHeight_ = nStartingHeight;
        nStartingWidth_ = nStartingWidth;
        init();
    }
    
    /**
     * Return gameboard in this scroller
     */
    public Gameboard getGameboard()
    {
        return board_;
    }
    
    private void init()
    {        
        // clear board's original listeners and add ours.  We propogate down to the board.
        board_.clearMouseListeners(); 
        board_.addMouseMotionListener(this);
        board_.addMouseListener(this);
        board_.addMouseWheelListener(this);
        board_.addFocusListener(this);
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        
        // use blit for fastest scrolling
        setScrollMode(JViewport.BLIT_SCROLL_MODE);

        // set initial resize sizes
        setupSize(nStartingWidth_, nStartingHeight_);
 
        // listener for size changes
        addComponentListener(this);
        
        // set the mouse blocker for modals
        InternalDialog.setModalBlockerListener(this);
    }
    
    public void cleanup()
    {
        board_.cleanup();
        board_.removeFocusListener(this);
        board_.removeMouseMotionListener(this);
        board_.removeMouseListener(this);
        board_.removeMouseWheelListener(this);
        board_.removeFocusListener(this);
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        removeComponentListener(this);
        InternalDialog.setModalBlockerListener(null);
        board_.setScrollGameboard(null);
        setView(null);
        board_ = null;
    }
    
    //////
    ////// SIZE CHANGE logic
    //////
    
    /**
     * Setup resize sizes given starting nWidth/nHeight (smallest size)
     */
    private void setupSize(int nWidth, int nHeight)
    {
        // display size of board is size of scroll minus border
        Dimension newsize = new Dimension(nWidth, nHeight);

        // size first fits board in initial window, second uses default size of image
        Dimension dPref = board_.getPreferredSize(newsize);
        
        // create resize dimensions based on starting size - also sets board to this initialize size
        board_.setResizeDimensions(dPref);
    }
        
    /**
     * Increase size
     */
    private void increaseSize()
    {
        changeGameboardSize(INCREASE, false, true);
    }
    
    /**
     * Decrease size
     */
    private void decreaseSize()
    {
        changeGameboardSize(DECREASE, false, true);
    }
    
    /**
     * Increase size to largest size
     */
    private void largestSize()
    {
        changeGameboardSize(INCREASE, true, true);
    }
    
    /**
     * Increase size to largest size
     */
    private void smallestSize()
    {
        changeGameboardSize(DECREASE, true, true);
    }
    
    
    /**
     * Toggle fill color
     */
    private void toggleColor()
    {
        EnginePrefs prefs = engine_.getPrefsNode();
        boolean bFill = prefs.getBoolean(EngineConstants.PREF_FILL, true);
        prefs.putBoolean(EngineConstants.PREF_FILL, !bFill);
        board_.getFillSetting();
    }
    
    private boolean bAllowScroll_= true;
    
    /**
     * Set whether this allowed to scroll
     */
    public void setAllowScroll(boolean b)
    {
        bAllowScroll_ = b;
    }
    
    /**
     * Is this allowed to scroll?
     */
    public boolean isAllowScroll()
    {
        return bAllowScroll_;
    }
    
    private boolean bAllowSizeChange_= true;
    
    /**
     * Set whether this allowed to change size
     */
    public void setAllowSizeChange(boolean b)
    {
        bAllowSizeChange_ = b;
    }
    
    /**
     * Is this allowed to change size?
     */
    public boolean isAllowSizeChange()
    {
        return bAllowSizeChange_;
    }
    
    public static int INCREASE = 1;
    public static int DECREASE = 2;
    
    public boolean canChangeSize(int nType)
    {
        if (nType == INCREASE)
        {
            return board_.canIncreaseSize();
        }
        else
        {
            return board_.canDecreaseSize();
        }
    }
    
    // used to avoid creating lots of objects
    private Point ptopleft = new Point();
    
    /**
     * increase/decease size of game board
     */
    public void changeGameboardSize(int nType, boolean bAllTheWay, boolean bZoomToMouse)
    {
        if (!bAllowSizeChange_) return;

        // figure out center of current space/size
        int pdimwidth = getWidth();
        int pdimheight = getHeight();
        int pcenterx = -getView().getX();
        int pcentery = -getView().getY();
        pcenterx += (pdimwidth / 2);
        pcentery += (pdimheight / 2);

        // convert center to the map space (as defined in gameboard.xml)
        pcenterx = board_.scaleToMapSpace(pcenterx);
        pcentery = board_.scaleToMapSpace(pcentery);

        // store last mouse coordinates before resize/move
        // and figure out point under mouse in map space
        Point pBeforeMap = board_.getLastMousePoint();
        
        // convert point on map to viewport
        Point pBefore = SwingUtilities.convertPoint(board_, pBeforeMap, this);
        
        // convert point on map to map space
        pBeforeMap.x = board_.scaleToMapSpace(pBeforeMap.x);
        pBeforeMap.y = board_.scaleToMapSpace(pBeforeMap.y);
        
        // change size
        if (nType == INCREASE)
        {
            if (!board_.increaseSize(bAllTheWay)) return;    
        }
        else 
        { 
            if (!board_.decreaseSize(bAllTheWay)) return;
        }
        
        // if zooming to center, remember old center
        int oldx = pcenterx;
        int oldy = pcentery;
        
        // if zooming to mouse, the old x/y is where the 
        // mouse was on the map
        if (bZoomToMouse)
        {
            oldx = pBeforeMap.x;
            oldy = pBeforeMap.y;
        }
            
        // convert old x,y to new spot on the board's current turn
        int newx = board_.scaleToCurrentSpace(oldx);
        int newy = board_.scaleToCurrentSpace(oldy);
        
        // if zooming to mouse then position viewport so
        // same point on board under is still under mouse
        if (bZoomToMouse)
        {
            newx -= pBefore.x;
            newy -= pBefore.y;
        }
        // otherwise, zooming in/out so center remains same
        else
        {
            newx -= (pdimwidth / 2);
            newy -= (pdimheight / 2);
        }
        
        // adjust for too close to top/left edge 
        // (viewport handles too far to bottom/right edge)
        if (newx < 0) newx = 0;
        if (newy < 0) newy = 0;
        
        // set new position
        ptopleft.x = newx;
        ptopleft.y = newy;
        setViewPosition(ptopleft);
        
        // After resize/move, have board recalc mouse-related stuff
        Point pAfter = SwingUtilities.convertPoint(this, pBefore, board_);
        board_.setLastMousePoint(pAfter);
    }
    
    ////
    //// Component Listener methods
    ////
    public void componentResized(ComponentEvent e)
    {
        int nWidth = getWidth();
        int nHeight = getHeight();
        
        // don't go smaller than smallest width/height given
        if (nWidth < nSmallWidth_) nWidth = nSmallWidth_;
        if (nHeight < nSmallHeight_) nHeight = nSmallHeight_;
        
        // setup new sizes
        setupSize(nWidth, nHeight);
    }

    /** EMPTY **/
    public void componentMoved(ComponentEvent e) {}

    /** EMPTY **/
    public void componentShown(ComponentEvent e) {}

    /** EMPTY **/
    public void componentHidden(ComponentEvent e) {}    
    
    //////
    ////// AUTO SCROLLING logic
    //////
    
    private Thread SCROLL_THREAD = null; 
    private int adjustx_;
    private int adjusty_;
    private boolean bSCROLL = false;
    private boolean bMOUSEDOWN = false;
    private boolean CLICKTOSCROLL = false;
    private static final int AUTO_SCROLL_BUFFER = 15;
    private static final int AUTO_SCROLL_CORNER_BUFFER = 50;
    private static final int SCROLL_DELAY_MILLIS = 75;
    private static final int SCROLL_DELAY_MILLIS_AUTO = 100;
    private static final int SCROLL_AMOUNT = 50;

    public void getClickToScroll()
    {
        if (engine_ == null) return;
        EnginePrefs prefs = engine_.getPrefsNode();
        CLICKTOSCROLL = prefs.getBoolean(EngineConstants.PREF_SCROLL, true);
    }
    
    /**
     * check to see if we should be scrolling
     */
    private void checkMouseScroll(MouseEvent e) 
    {   
        if (!bAllowScroll_) return;
        
        //Point viewpoint = getViewPosition();
        // do next two instead of above to avoid creating objects
        int viewpointx = -getView().getX();
        int viewpointy = -getView().getY();
        int width = getWidth();
        int height = getHeight();
        
        //Point mousepoint = e.getPoint();
        // do this so we don't create objects
        int mousepointx = e.getX();
        int mousepointy = e.getY();
        
        int x = mousepointx - viewpointx;
        int y = mousepointy - viewpointy;
        
        int minx = AUTO_SCROLL_BUFFER;
        int miny = AUTO_SCROLL_BUFFER;
        int maxx = width - AUTO_SCROLL_BUFFER;
        int maxy = height -AUTO_SCROLL_BUFFER;
        
        if (x < minx || x > maxx ||
            y < miny || y > maxy)
        {
            
            adjustx_ = 0;
            adjusty_ = 0;
            
            // left side
            if (x < minx) {
                adjustx_ = -1;
                if (y < (miny + AUTO_SCROLL_CORNER_BUFFER)) adjusty_ = -1;
                if (y > (maxy - AUTO_SCROLL_CORNER_BUFFER)) adjusty_ = +1;
            }
            
            // right side
            if (x > maxx) {
                adjustx_ = +1;
                if (y < (miny + AUTO_SCROLL_CORNER_BUFFER)) adjusty_ = -1;
                if (y > (maxy - AUTO_SCROLL_CORNER_BUFFER)) adjusty_ = +1;
            }
            
            // top side
            if (y < miny) {
                adjusty_ = -1;
                if (x < (minx + AUTO_SCROLL_CORNER_BUFFER)) adjustx_ = -1;
                if (x > (maxx - AUTO_SCROLL_CORNER_BUFFER)) adjustx_ = +1;
            }
            
            // bottom side
            if (y > maxy) {
                adjusty_ = +1;
                if (x < (minx + AUTO_SCROLL_CORNER_BUFFER)) adjustx_ = -1;
                if (x > (maxx - AUTO_SCROLL_CORNER_BUFFER)) adjustx_ = +1;
            }
            
            // move farther when mouse scrolling
            if (CLICKTOSCROLL)
            {
                adjusty_ *= 2;
                adjustx_ *= 2;
            }
            else
            {
                adjusty_ *= 1.5;
                adjustx_ *= 1.5;
            }
            startScrolling();
        }
        else
        {
            if (bSCROLL) 
            {
                stopScrolling();
            }
        }
    }
    
    /**
     * Create scroller thread
     */
    private void startScrolling()
    {
        if (!bAllowScroll_) return;
        
        bSCROLL = true;
        setScrollCursor();
        board_.setScrolling(true);
        if (SCROLL_THREAD == null)
        {
            SCROLL_THREAD = new Thread(new Runnable() {
                    public void run() {
                            while (bSCROLL) {
                                scrollBoardAndWait();
                            }
                    }
                }, "ScrollThread");
            SCROLL_THREAD.start();
        }
    }
    
    /**
     * Reset cursor, stop thread
     */
    private void stopScrolling()
    {  
        if (!bAllowScroll_) return;
        
        setOverrideCursor(null);
        board_.setScrolling(false);
        
        bSCROLL = false;
        SCROLL_THREAD = null;
    }
    
    private void setScrollCursor()
    {        
        if (adjustx_ < 0 && adjusty_ == 0) setOverrideCursor(Cursors.SCROLLLEFT);
        if (adjustx_ < 0 && adjusty_ <  0) setOverrideCursor(Cursors.SCROLLUPLEFT);
        if (adjustx_ < 0 && adjusty_ >  0) setOverrideCursor(Cursors.SCROLLDOWNLEFT);
        
        if (adjustx_ == 0 && adjusty_ < 0) setOverrideCursor(Cursors.SCROLLUP);
        if (adjustx_ == 0 && adjusty_ > 0) setOverrideCursor(Cursors.SCROLLDOWN);
        
        if (adjustx_ > 0 && adjusty_ == 0) setOverrideCursor(Cursors.SCROLLRIGHT);
        if (adjustx_ > 0 && adjusty_ <  0) setOverrideCursor(Cursors.SCROLLUPRIGHT);
        if (adjustx_ > 0 && adjusty_ >  0) setOverrideCursor(Cursors.SCROLLDOWNRIGHT);
        
    }
    
    /**
     * called from SCROLL_THREAD thread
     */
    private void scrollBoardAndWait()
    {
        if (bSCROLL)
        {
            Utils.sleepMillis(CLICKTOSCROLL ? SCROLL_DELAY_MILLIS : SCROLL_DELAY_MILLIS_AUTO);
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {     
                            if (bSCROLL && (bMOUSEDOWN || !CLICKTOSCROLL)) scrollBoard();
                    }
                }
            );
        }
    }
   
    /**
     * Make given territory visible
     */
    public void centerTerritory(Territory t)
    {
        Rectangle bounds = t.getScaledPathBounds();
        Point p = SwingUtilities.convertPoint(board_, bounds.x + bounds.width/2, bounds.y + bounds.height/2, this);
        center(p.x, p.y);
    }
    
    /**
     * Center top on given territory point
     */
    public void centerTerritoryPointTop(int x, int y)
    {
        Point p = SwingUtilities.convertPoint(board_, x, y, this);
        centerTop(p.x, p.y);
    }
    
    /**
     * Center board on given x,y
     */
    public void center(int x, int y)
    {
        int xcenter = this.getWidth() / 2;
        int ycenter = this.getHeight() / 2;
        scrollBoard(x - xcenter, y - ycenter);
    }
    
    /**
     * Center board at top on given x,y
     */
    public void centerTop(int x, int y)
    {
        int xcenter = this.getWidth() / 2;
        scrollBoard(x - xcenter, y);
    }
    
    // used to avoid creating lots of objects
    private Point viewpoint = new Point();

    /**
     * Scrolls board by current adjustx_ and adjusty_
     * values (* SCROLL_AMOUNT)
     */
    private void scrollBoard()
    {
        if (adjustx_ == 0 && adjusty_ == 0) return;
        int myadjustx = adjustx_ * SCROLL_AMOUNT;
        int myadjusty = adjusty_ * SCROLL_AMOUNT;
        
        scrollBoard(myadjustx, myadjusty);
    }
    
    /**
     * Scroll given x,y amount
     */
    private void scrollBoard(int myadjustx, int myadjusty)
    {
        int origx, origy;
        int viewpointx = origx = -getView().getX();
        int viewpointy = origy = -getView().getY();
        int viewportsizewidth = getWidth();
        int viewportsizeheight =getHeight();
        int viewsizewidth = board_.getWidth();
        int viewsizeheight= board_.getHeight();

        viewpointx += myadjustx;
        viewpointy += myadjusty;
        int maxx = viewsizewidth - viewportsizewidth;
        int maxy = viewsizeheight -viewportsizeheight;

        if (viewpointx < 0) viewpointx = 0;
        if (viewpointy < 0) viewpointy = 0;
        if (viewpointx > maxx) viewpointx = maxx;
        if (viewpointy > maxy) viewpointy = maxy;

        // skip if same
        if (viewpointx == origx &&
            viewpointy == origy)
        {
            return;
        }
        // get current mouse point
        Point pNewLast = board_.getLastMousePoint();
        pNewLast.x += (viewpointx - origx);
        pNewLast.y += (viewpointy - origy);
        
        // Get current piece attached to mouse
        EngineGamePiece paintAtMouse = board_.getPaintAtMouse();
        board_.setPaintAtMouse(null, true); // repaint before scroll
        
        // scroll to new position
        viewpoint.x = viewpointx;
        viewpoint.y = viewpointy;
        setViewPosition(viewpoint);
        
        // set new mouse point
        // reset what was painting at mouse
        board_.setLastMousePoint(pNewLast);
        board_.setPaintAtMouse(paintAtMouse, true);
    }
    
    ///
    /// Mouse Motion Listener
    ///
    
    /** 
     * calls mouseMoved, since logic is same
     */
    public void mouseDragged(MouseEvent e) 
    {
        if (bMoveMapMode_) 
        { 
            moveMap(e, true);
        }
        else
        {
            checkMouseScroll(e);
        }
        board_.mouseDragged(e);
    }
  
    /** 
     * If mouse moved within AUTO_SCROLL_BUFFER pixels
     * of edge, start scrolling
     */
    public void mouseMoved(MouseEvent e) 
    {
        if (bMoveMapMode_) 
        { 
            moveMap(e, false);
        }
        else
        {
            checkMouseScroll(e);
        }
        board_.mouseMoved(e);
    }
    
    ///
    /// Mouse Listener
    ///
    
    /**
     * propagate to board
     */
    public void mouseExited(MouseEvent e) 
    {
        if (bMoveMapMode_) 
        {
            // do nothing
        }
        else if (!bMOUSEDOWN || !CLICKTOSCROLL)
        {
            stopScrolling();
        }    
        board_.mouseExited(e);

    }
    
    /** 
     * call checkMouseScroll, propagate to board
     */
    public void mouseEntered(MouseEvent e) 
    {
        if (SHIFTDOWN) {
            setMapMode(true);
        } else {
            checkMouseScroll(e);
        }
        board_.mouseEntered(e);
    }

    /** 
     * track mouse down, propagate to board
     */
    public void mousePressed(MouseEvent e) 
    {
        if (bMoveMapMode_)
        {
            // right click center
            if (e.getButton() != MouseEvent.BUTTON1)
            {
                MouseEvent here = SwingUtilities.convertMouseEvent(board_, e, this);
                center(here.getX(), here.getY());
            }
        }
        bMOUSEDOWN = true;
        if (!isModalMode()) board_.mousePressed(e);
    }
    
    /** 
     * track mouse up, propagate to board
     */
    public void mouseReleased(MouseEvent e) {
        bMOUSEDOWN = false;
        if (!isModalMode()) board_.mouseReleased(e);
    }
    
    /** 
     * propagate to board
     */
    public void mouseClicked(MouseEvent e) 
    {
        if (bMoveMapMode_) {
            return;
        }
        if (!isModalMode()) board_.mouseClicked(e);
    }
    
    ///
    /// Mouse Wheel Listener
    ///
    /** 
     * Scroll/Zoom if mouse wheel moved
     */
    public void mouseWheelMoved(MouseWheelEvent e) 
    {
        if (e.isConsumed()) return;
        boolean bScroll = (e.isShiftDown());
        
        if (bScroll)
        {
            int adjust = e.getWheelRotation() * 2;
            adjusty_ = 0;
            adjustx_ = 0;
        
            // if ctrl down scroll left-right
            // else scroll up-down
            if (e.isControlDown()) adjustx_ = adjust;
            else adjusty_ = adjust;
        
            scrollBoard();
        }
        else
        {
            int nRotate = e.getWheelRotation();
            if (nRotate < 0)
            {
                changeGameboardSize(INCREASE, false, true);
            }
            else
            {
                changeGameboardSize(DECREASE, false, true);
            }
        }
        
        e.consume();
    }
    
    ///
    /// key actions
    ///
    private void scrollLeft()
    {
        scroll(-1, 0);
    }
    private void scrollRight()
    {
        scroll(1, 0);
    }
    private void scrollUp()
    {
        scroll(0, -1);
    }
    private void scrollDown()
    {
        scroll(0, 1);
    }
    
    private void scrollLeftMax()
    {
        scroll(-10000, 0);
    }
    
    private void scrollRightMax()
    {
        scroll(10000, 0);
    }
    private void scrollUpMax()
    {
        scroll(0, -10000);
    }
    private void scrollDownMax()
    {
        scroll(0, 10000);
    }
    
    ///
    /// helper
    ///
    private void scroll(int adjustx, int adjusty)
    {
        adjustx_ = adjustx;
        adjusty_ = adjusty;
        
        scrollBoard();
    }

    ///
    /// AWTListener/Focus methods for mouse scrolling
    ///
    
    /** 
     * Invoked when an event is dispatched in the AWT.
     */
    public void eventDispatched(AWTEvent event) 
    {
        // if key event and the mouse is over the board, process it
        if (event instanceof KeyEvent && board_.getTerritoryUnderMouse() != null)
        {
            KeyEvent k = (KeyEvent) event;
            if (k.getKeyCode() == KeyEvent.VK_SHIFT)
            {
                
                if (k.getID() == KeyEvent.KEY_PRESSED)
                {
                    SHIFTDOWN = true;
                    setMapMode(true);
                }
                else if (k.getID() == KeyEvent.KEY_RELEASED)
                {
                    SHIFTDOWN = false;
                    setMapMode(false);
                }
                return;
            }
        }
        
        // +/- for resize, arrow keys
        if (event instanceof KeyEvent)
        {
            KeyEvent k = (KeyEvent) event;
            
            // if not pressed or source is a text component, ignore
            if (k.getID() != KeyEvent.KEY_PRESSED || k.getSource() instanceof javax.swing.text.JTextComponent
                    || k.getSource() instanceof javax.swing.JTabbedPane) return;
            
            switch (k.getKeyCode())
            {
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                    if (k.isControlDown()) largestSize();
                    else increaseSize();
                    break;
                    
                case KeyEvent.VK_MINUS:
                case KeyEvent.VK_UNDERSCORE:
                    if (k.isControlDown()) smallestSize();
                    else decreaseSize();
                    break;
                    
                case KeyEvent.VK_LEFT:
                    if (k.isControlDown()) scrollLeftMax();
                    else scrollLeft();
                    break;
                    
                case KeyEvent.VK_RIGHT:
                    if (k.isControlDown()) scrollRightMax();
                    else scrollRight();
                    break;
                    
                case KeyEvent.VK_UP:
                    if (k.isControlDown()) scrollUpMax();
                    else scrollUp();
                    break;
                    
               case KeyEvent.VK_DOWN:
                    if (k.isControlDown()) scrollDownMax();
                    else scrollDown();
                    break;
                    
               // C key - toggle fill color
               case KeyEvent.VK_C:  
                   toggleColor();
                   break;
            }
        }
        
        // BUG 181 - need to keep track of mouse for Mac
        if (event instanceof MouseEvent)
        {
            MouseEvent e = (MouseEvent) event;
            if (e.getSource() instanceof Component)
            {
                board_.lastPointFromAWT_ = SwingUtilities.convertPoint((Component)e.getSource(), e.getX(), e.getY(), board_);
            }
        }
    }


    public void focusGained(FocusEvent e) {
    }
    
    public void focusLost(FocusEvent e) {
        SHIFTDOWN = false;
        setMapMode(false);
    }
    
    boolean SHIFTDOWN = false;
    boolean bMoveMapMode_ = false;
    Cursor HAND = Cursors.HAND;
    
    private void setMapMode(boolean shiftdown)
    {
        if (!bAllowScroll_) return;
        
        if (bSCROLL) return;
        
        if (bMoveMapMode_ && !shiftdown)
        {
            bMoveMapMode_ = false;
            
            // set cursor back
            setOverrideCursor(null);
            board_.setScrolling(false);
        }
        // ENTER MOVE MAP MODE
        else if (!bMoveMapMode_ && shiftdown)
        {
            bMoveMapMode_ = true;
            lastMouse_ = null;
            
            // set cursor to hand
            setOverrideCursor(HAND);
            board_.setScrolling(true);
        }
    }
    
    private Point lastMouse_ = null;
    
    private void moveMap(MouseEvent e, boolean bScroll)
    {
        if (bSCROLL) return;
        e = SwingUtilities.convertMouseEvent(board_, e, this);
        if (lastMouse_ == null) {
            lastMouse_ = e.getPoint();
            return;
        }

        if (bScroll) scrollBoard(lastMouse_.x - e.getX(), lastMouse_.y - e.getY());
        lastMouse_.x = e.getX();
        lastMouse_.y = e.getY();
    }

    /////
    ///// modal handler
    /////
    
    // handler for modal panels
    ModalHandler handler_ = new ModalHandler();
    private Stack<JPanel> topPanels_ = new Stack<JPanel>();
    private JPanel topPanel_ = null; // last modal panel
    
    /**
     * Modal window started - allow us to process the panel
     */
    public void blockerCreated(JPanel panel) {
        panel.setCursor(Cursors.PROHIBITED);
        panel.addMouseListener(handler_);
        panel.addMouseMotionListener(handler_);
        panel.addMouseWheelListener(handler_);
        handler_.init();
        topPanels_.push(panel);
        topPanel_ = panel;
    }
    
    /**
     * modal window done - allow us to clean up
     */
    public void blockerFinished(JPanel panel) {
        panel.setCursor(null);
        panel.removeMouseListener(handler_);
        panel.removeMouseMotionListener(handler_);
        panel.removeMouseWheelListener(handler_);
        handler_.init();
        topPanels_.pop();
        if (topPanels_.size() > 0)
        {
            topPanel_ = topPanels_.peek();
        }
        bMOUSEDOWN = false;
        bMoveMapMode_ = false;
        mouseExited(null);
    }
    
    /**
     * Return true if we are in a modal mode
     */
    public boolean isModalMode()
    {
        return topPanels_.size() > 0;
    }
    
    /**
     * Set override cursor on board, unless modal mode, then
     * set on the topmost panel
     */
    private void setOverrideCursor(Cursor c)
    {
        if (!isModalMode())
        {
            board_.setOverrideCursor(c);
        }
        else
        {
            if (c == null) c = Cursors.PROHIBITED;
            topPanel_.setCursor(c);
        }
    }

    /**
     * Class to pass mouse events from modal blocker to board so that it
     * can still be scrolled
     */
    public class ModalHandler implements MouseListener, MouseMotionListener, MouseWheelListener
    {
        boolean bPreviousPointInBoard = false;
        
        private void init()
        {
            bPreviousPointInBoard = false;
        }
        
        private MouseEvent convertToBoard(MouseEvent e)
        {
            ScrollGameboard scroll = ScrollGameboard.this;
            
            // convert mouse event to this component
            Point p = SwingUtilities.convertPoint((JPanel)e.getSource(), e.getX(), e.getY(), scroll);
            
            // if mouse event is in bounds of this component, continue
            if (p.getX() >= 0 && p.getX() < scroll.getWidth() &&
                p.getY() >= 0 && p.getY() < scroll.getHeight())
            {
                // convert point to the game board
                p = SwingUtilities.convertPoint(scroll, p.x, p.y, board_);
                
                // change event to match new point
                e.translatePoint(-e.getX(), -e.getY());
                e.translatePoint(p.x, p.y);
                
                // if the previous mouse event was not in the board, 
                // simulate mouse entered
                if (!bPreviousPointInBoard)
                {
                    scroll.mouseEntered(e);
                    bPreviousPointInBoard = true;
                }
                
                return e;
            }
            
            // if previous point was in the board, 
            // simulate mouse exited
            if (bPreviousPointInBoard)
            {
                scroll.mouseExited(null);
                bPreviousPointInBoard = false;
            }
            return null;
        }

        public void mouseClicked(MouseEvent e) {
            e = convertToBoard(e);
            if (e != null) ScrollGameboard.this.mouseClicked(e);
        }
        
        public void mouseDragged(MouseEvent e) {
            e = convertToBoard(e);
            if (e != null) ScrollGameboard.this.mouseDragged(e);
        }

        public void mouseEntered(MouseEvent e) {
        }
        
        public void mouseExited(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            e = convertToBoard(e);
            if (e != null) ScrollGameboard.this.mouseMoved(e);
        }

        public void mousePressed(MouseEvent e) {
            e = convertToBoard(e);
            if (e != null) ScrollGameboard.this.mousePressed(e);
        }

        public void mouseReleased(MouseEvent e) {
            e = convertToBoard(e);
            if (e != null) ScrollGameboard.this.mouseReleased(e);
        }
        
        public void mouseWheelMoved(MouseWheelEvent e) {
            e = (MouseWheelEvent) convertToBoard(e);
            if (e != null) ScrollGameboard.this.mouseWheelMoved(e);
        }
        
    }
}
