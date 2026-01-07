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
 * Gameboard.java
 *
 * Created on November 7, 2002, 7:46 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.Perf;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.GuiUtils;
import com.donohoedigital.gui.ImageComponent;
import com.donohoedigital.gui.TextUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 *
 * @author  Doug Donohoe
 */
public class Gameboard extends ImageComponent implements Scrollable, 
                            MouseMotionListener, MouseListener
{
    static Logger logger = LogManager.getLogger(Gameboard.class);
    
    protected GameboardConfig gameconfig_;
    protected Territory[] territories_;
    
    protected Dimension mapDefaultSize_;
    
    protected Territory territoryAtMouse_;
    protected Territory territoryAtMouseWhenPressed_;
    protected Territory territorySelected_;
    
    protected EngineGamePiece pieceAtMouse_;
    protected EngineGamePiece pieceAtMouseWhenPressed_;
    protected EngineGamePiece pieceSelected_;
    protected javax.swing.FocusManager focusMgr_;
    protected JComponent scroll_ = null;
    protected GameContext context_;

    protected boolean bScaleBig_ = false;
    // not currently used
    //private GeneralPath bordersPath_;
    //private GeneralPath borderScaledPath_;
    
    protected boolean bUseImage_ = true;
    protected GameEngine engine_;

    public void cleanup()
    {
        tlisteners_.clear();
        elisteners_.clear();
        customDrawer_ = null;
        lastPointFromAWT_ = null;
    }

    /**
     * if false, gameboard image is not used
     */
    public void setUseImage(boolean b)
    {
        bUseImage_ = b;
    }

    /**
     * Is image used?
     */
    public boolean isUseImage()
    {
        return bUseImage_;
    }

    /**
     * Tools useage
     */
    public Gameboard(GameboardConfig gameconfig, boolean bGameMode)
    {
        this(null, null, gameconfig, bGameMode, null, true);
    }

    /**
     * Creates a new Gameboard 
     */
    public Gameboard(GameEngine engine, GameContext context, GameboardConfig gameconfig, boolean bGameMode,
                     Territory tUpperLeft, boolean bScaleBig)
    {
        super(gameconfig.getImage(), 1.0);
        Perf.construct(this, null);
        bScaleBig_ = bScaleBig;
        
        // SETUP
        focusMgr_ = javax.swing.FocusManager.getCurrentManager();
        setName("Gameboard");
        setOpaque(true);
        gameconfig_ = gameconfig;
        engine_ = engine;
        context_ = context;

        // store territories in array for fast access
        territories_ = Territory.getTerritoryArrayCached();
        
        // transform all points so map starts at (0,0)
        // and height/width are correct
        if (bGameMode)
        {
            getFillSetting();
            gameconfig_.transformAllPoints();
        }
        
        // create paths for territories from borders
        gameconfig_.createPaths(bGameMode);
        
        // after creating path, if we have bounds, set image bounds and
        // adjust if new upper left corner specified

        // if different territory specified for upper left,
        // adjust paths/points. 
        if (bGameMode)
        {
            Rectangle bounds = gameconfig_.adjustPaths(tUpperLeft);
            // finally set the bounds
            setImageBounds(bounds);
        }
        
        // determine adjacent territories, etc.
        gameconfig_.determineTerritoryInfo(bGameMode);
        
        // store border path - not currently used
        //bordersPath_ = gameconfig_.getBorderPath();
        //borderScaledPath_ = bordersPath_;

        // set size of this map
        mapDefaultSize_ = new Dimension(gameconfig_.getWidth(), gameconfig_.getHeight());

        // deal with focus
        setFocusable(true);
        
        // listeners
        addMouseMotionListener(this);
        addMouseListener(this);

        // add action for showing AI debug info        
        if (TESTING(EngineConstants.TESTING_AI_DEBUG))
        {
            GuiUtils.addKeyAction(this, JComponent.WHEN_IN_FOCUSED_WINDOW, 
                                "debugai", new DebugAI(), KeyEvent.VK_X, 0);
            GuiUtils.addKeyAction(this, JComponent.WHEN_IN_FOCUSED_WINDOW, 
                                "debugai2", new DebugAI2(), KeyEvent.VK_V, 0);
            GuiUtils.addKeyAction(this, JComponent.WHEN_IN_FOCUSED_WINDOW, 
                                "debugai3", new DebugAI3(), KeyEvent.VK_S, 0);
        }
    }

    /**
     * context
     */
    public GameContext getGameContext()
    {
        return context_;
    }

    /**
     * Clear mouse listeners - assumes scrollgameboard will propogate them
     * Done because scrollgameboard needs to get them first.  This ensures
     * the order.
     */
    void clearMouseListeners()
    {
        removeMouseMotionListener(this);
        removeMouseListener(this);
    }
    
    /**
     * set scrollgame board used
     */
    void setScrollGameboard(JComponent s)
    {
        scroll_ = s;
    }
    
    /**
     * Return scrollgame board used
     */
    public JComponent getScrollGameboard()
    {
        return scroll_;
    }
    
    /**
     * Return gameboard config
     */
    public GameboardConfig getGameboardConfig()
    {
        return gameconfig_;
    }
    
    /**
     * override to set preferred size based on parent's size -
     * we scale so that the map is as big as possible w/out having
     * to scroll in two dimensions (we scroll in dimension with least room)
     */
    @Override
    public Dimension getPreferredSize()
    {
        if (!bResizeToParent_) return super.getPreferredSize();

        if (getParent() == null) return mapDefaultSize_;
        
        Dimension pSize = getParent().getSize();
        return getPreferredSize(pSize);
    }
    // old - used to resize board to parent's size when board resized.
    // not sure if this works anymore
    boolean bResizeToParent_ = false;
    
    /**
     * like getPreferredSize, except based on given dimension instead
     * of parent's size
     */
    public Dimension getPreferredSize(Dimension dSize)
    {
        double dScaleFactor = getScaleFactor(dSize);
        Dimension d = new Dimension();
        
        // cast to int to get exact integers for scrollable benefit
        d.setSize((int)((mapDefaultSize_.getWidth() * dScaleFactor)), 
                  (int)((mapDefaultSize_.getHeight() * dScaleFactor)));
        return d;        
    }

    /**
     * calls setNewSize(size, true)
     */
    public void setNewSize(Dimension size)
    {
        setNewSize(size, true);
    }
    
    private Dimension dLastSizeSet_;
    
    /**
     * Set new size for this widget.  Sets preferred size, adjusts paths
     * and repaints
     */
    public void setNewSize(Dimension size, boolean bRepaint)
    {   
        // get preferred size of this image based on new size requested
        Dimension d = getPreferredSize(size);
        
        // if desired size already equals preferred size, do nothing
        if (size.equals(dLastSizeSet_)) return;
        dLastSizeSet_ = size;
        
        // set preferred size & size
        setPreferredSize(d);
        setSize(d);
        
        // adjust paths to reflect new size
        GeneralPath scaledPath;
        
        AffineTransform tx = new AffineTransform();
        dScale_ =  getScaleFactor(d);
        tx.setToScale(dScale_, dScale_);
        
        // adjust total path (NOT USED CURRENTLY)
        //borderScaledPath_ = (GeneralPath) bordersPath_.clone();
        //borderScaledPath_.transform(tx);        
        
        // adjust territories
        for (Territory aTerritories_ : territories_)
        {
            scaledPath = (GeneralPath) aTerritories_.getPath().clone();
            scaledPath.transform(tx);
            aTerritories_.setScaledPath(scaledPath);
        }
        
        if (bRepaint) context_.getFrame().repaint();
    }
    
    /**
     * Repaint visible portion of gameboard (typically
     * that inside the GameScrollboard viewport
     */
    public void repaintVisible(boolean bImmediate)
    {
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) logger.debug("REPAINT VISIBLE " + (CNT++) + " immediate: " + bImmediate);
        if (scroll_ == null)
        {
            if (bImmediate){
                this.paintImmediately(0,0,getWidth(),getHeight());
            } else {
                repaint();
            }
            return;
        }
     
        if (bImmediate){
            scroll_.paintImmediately(0,0,scroll_.getWidth(),scroll_.getHeight());
        } else {
            scroll_.repaint();
        }
    }
    
    double dScale_;
    
    /**
     * Return scale last used to convert
     * points in gameboard space to current dimensions
     * <BR>
     * gameboard dimension * getLastScale = current dimension<BR>
     * current dimension / getLastScale = gameboard dimension
     */
    public double getLastScale()
    {
        return dScale_;
    }
    
    /**
     * Return scale factor that one would multiply the getDefaultMapSize() 
     * width and height by so that the one of the dimensions is exactly
     * same as dSize and the other is equal to or greater.
     */
    public double getScaleFactor(Dimension dSize)
    {
        // getting min keeps figure entirely in screen
        // getting max scales image as big as it can go (so one dimension equals width/height)
        
        if (bScaleBig_)
        {
            return Math.max(dSize.getWidth() / mapDefaultSize_.getWidth(),
                        dSize.getHeight() / mapDefaultSize_.getHeight());      
        }
        else
        {
            return Math.min(dSize.getWidth() / mapDefaultSize_.getWidth(),
                        dSize.getHeight() / mapDefaultSize_.getHeight());
        }
    }
    
    /**
     * scale value from current space to map space
     */
    public int scaleToMapSpace(int nValue)
    {
        return (int) (nValue / dScale_);
    }
    
    /**
     * scale value from current space to map space
     */
    public double scaleToMapSpace(double dValue)
    {
        return dValue / dScale_;
    }
    
    /**
     * Scale value from map space to current space
     */
    public int scaleToCurrentSpace(int nValue)
    {
        return (int) (nValue * dScale_);
    }
    
    /**
     * Scale value from map space to current space
     */
    public double scaleToCurrentSpace(double dValue)
    {
        return dValue * dScale_;
    }
    
    /**
     * Change size 5% * nChange
     */
    public void changeSize(int nChange, boolean bRepaint)
    {
        double nPercent = .05 * nChange;
        Dimension size = getPreferredSize();
        size.width += (int)(size.width * nPercent);
        size.height += (int)(size.height * nPercent);
        setNewSize(size, bRepaint);
    }
    
    /**
     * calls changeSize(nChange, true)
     */
    public void changeSize(int nChange)
    {
        changeSize(nChange, true);
    }
    
    private Dimension[] resizeDimensions_;
    private int nCurrentDimension_ = 0;
    
    /**
     * Calculates the resize dimensions based on the starting smallest
     * size.  Calcs 150%, 200%, 300% and 400%.  Resizes board to smallest size
     */
    public void setResizeDimensions(Dimension dSmall)
    {
        // if already this size, don't recalc
        if (resizeDimensions_ != null && resizeDimensions_[0].equals(dSmall))
        {
            return;
        }
        
        resizeDimensions_ = new Dimension[5];
        int i = 0;
        resizeDimensions_[i++] = dSmall;
        resizeDimensions_[i++] = calcResizeDimension(dSmall, 1.5d); // 50% bigger
        resizeDimensions_[i++] = calcResizeDimension(dSmall, 2.0d); // 100% bigger
        resizeDimensions_[i++] = calcResizeDimension(dSmall, 3.0d); // 150% bigger
        resizeDimensions_[i] = calcResizeDimension(dSmall, 4.0d); // 200% bigger
        //resizeDimensions_[i++] = calcResizeDimension(dSmall, 5.0d); // 250% bigger
        setNewSize(resizeDimensions_[nCurrentDimension_], false);
    }
    
    /**
     * Return dimension multiplied by dScale
     */
    private Dimension calcResizeDimension(Dimension dBase, double dScale)
    {
        Dimension d = new Dimension(dBase);
        d.width *= dScale;
        d.height *= dScale;
        return d;
    }
    
    /**
     * Get current index into resize dimension
     */
    public int getResizeDimensionIndex()
    {
        return nCurrentDimension_;
    }
    
    /**
     * Get resize dimensions
     */
    public Dimension[] getResizeDimensions()
    {
        return resizeDimensions_;
    }
    
    public boolean canIncreaseSize()
    {
        return nCurrentDimension_ != (resizeDimensions_.length - 1);

    }
    
    public boolean canDecreaseSize()
    {
        return nCurrentDimension_ != 0;

    }
    
    /**
     * Increase size to next resize dimension
     */
    public boolean increaseSize(boolean bAllTheWay)
    {
        if (!canIncreaseSize()) return false;
        
        if (bAllTheWay) {
            nCurrentDimension_ = resizeDimensions_.length - 1;
        } else {
            nCurrentDimension_++;
        }
        setNewSize(resizeDimensions_[nCurrentDimension_], false);
        return true;
    }
    
    /**
     * Decrease size to next resize dimension
     */
    public boolean decreaseSize(boolean bAllTheWay)
    {
        if (!canDecreaseSize()) return false;
        
        if (bAllTheWay) {
            nCurrentDimension_ = 0;
        } else {
            nCurrentDimension_--;
        }
        setNewSize(resizeDimensions_[nCurrentDimension_], false);
        return true;
    }
    
    /**
     * Return default map size (as calc'd from gameconfig file)
     */
    public Dimension getDefaultMapSize()
    {
        return mapDefaultSize_;
    }
    
    ///
    /// Drawing methods
    /// 
 
    
    private int CNT = 0;
    
    @Override
    public void repaint()
    {
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT))
        {
            logger.debug("REPAINT "+(CNT++)+" ALL");
            //logger.debug(Utils.formatExceptionText(new Throwable()));
        }
        super.repaint();
    }
        
    /**
     * Overriden for debugging
     */
    @Override
    public void repaint(int x, int y, int width, int height)
    {
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT))
        {
            logger.debug("REPAINT "+(CNT++)+" portion " + x +","+y+" " +width+"x"+height);   
            //logger.debug(Utils.formatExceptionText(new Throwable()));
        }
        super.repaint(x,y,width,height);
    }
    
    /**
     * Overridden for debugging
     */
    @Override
    public void paintImmediately(int x,int y,int width, int height)
    {
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT))
        {
            logger.debug("REPAINT IMMEDIATELY "+(CNT++)+" portion " + x +","+y+" " +width+"x"+height);   
            //logger.debug(Utils.formatExceptionText(new Throwable()));
        }
        super.paintImmediately(x,y,width,height);
    }
    
    // used for performance so new rect isn't needed everytime we repaint
    private Rectangle bounds_ = new Rectangle();
 
    // used so we create once per paint
    private java.awt.geom.Area cliparea_;
    
    boolean bFill_ = true;
    
    /**
     * Get fill setting from prefs, repaint if it changed
     */
    public void getFillSetting()
    {
        if (engine_ == null) return;
        EnginePrefs prefs = engine_.getPrefsNode();
        boolean bFillOld = bFill_;
        bFill_ = prefs.getBoolean(EngineConstants.PREF_FILL, true);
        if (bFillOld != bFill_)
        {
            repaintVisible(false);
        }
    }
    
    /**
     * Paint this component
     */
    @Override
    protected void paintComponent(Graphics g1)
    {
        if (TESTING(EngineConstants.TESTING_PERFORMANCE))
        {
            //Perf.start();
        }
        
        Graphics2D g = (Graphics2D) g1;
        if (bUseImage_)
        {
            super.paintComponent(g);
        }
        else
        {
            super.paintParentTile(g);
        }

        // get region we are painting (stored for use by all paint methods this call)
        g.getClipBounds(bounds_);
        cliparea_ = new java.awt.geom.Area(bounds_);
        
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT))
        {
            logger.debug("REPAINT COMPONENT "+(CNT++)+" ("+getDebugColorName()+") portion " + bounds_.x +","+bounds_.y+" " +bounds_.width+"x"+bounds_.height);   
            //logger.debug(Utils.formatExceptionText(new Throwable()));
        }
        
        // draw fills first
        drawTerritories(g, PART_FILL);
        
        // draw borders next
        if (bAntiAlias_)
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        
        drawTerritories(g, PART_BORDERS);
        
        if (bAntiAlias_)
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                        RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        
        // draw label
        drawTerritories(g, PART_LABEL);
        
        // draw custom
        drawTerritories(g, PART_CUSTOM);
        
        // draw label for AI
        if (DEBUG_AI) drawTerritories(g, PART_LABEL_AI);
        
        // draw mouse related items
        drawMouseItems(g);
        
        // Use this code to paint red line around box we are repainting - useful
        // for debugging.
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT)) {
            g.setColor(getDebugColor());
            g.drawRect(bounds_.x, bounds_.y, bounds_.width-1, bounds_.height-1);
        }
        
        if (TESTING(EngineConstants.TESTING_PERFORMANCE))
        {
            //Perf.stop();
        }

        // JDD 2019
        // disabled in DD Poker 3.0p2 - realized not really used (probably since 1.0 actually)
        // Keeping code around in case I want to add something like this back.
        // paint corner on mac
        if (EngineBasePanel.PAINT_GROW_BOX && Utils.ISMAC && engine_ != null)
        {
            EngineWindow win = context_.getFrame();
            int w = win.getWidth() - 51;
            int h = win.getHeight() - 52;
            EngineBasePanel p = win.getBasePanel();
            Point origin = SwingUtilities.convertPoint(this, OO, p);
            
            // if this paint didn't originate from the base panel,
            // and the area we just painted covers the grow box,
            // then repaint it.  The 51/52 check is to prevent
            // an infinited loop
            if (!p.bPainting_ &&
                origin.x + bounds_.x + bounds_.width > w &&
                origin.y + bounds_.y + bounds_.height > h &&
                bounds_.width != 51 &&
                bounds_.height != 52)
            {
                p.paintImmediately(w,h,51,52);
            }
        }
    }
    
    // only need one of these for above
    private Point OO = new Point(0,0);

    // used to control drawing features
    protected boolean bAntiAlias_ = true;
    
    // types of things drawTerritories can draw
    protected static final int PART_BORDERS = 1;
    protected static final int PART_FILL = 2;
    protected static final int PART_LABEL = 3;
    protected static final int PART_CUSTOM = 4;
    protected static final int PART_LABEL_AI = 50;
    
    /**
     * Logic to draw territory parts
     */
    protected void drawTerritories(Graphics2D g, int iPart)
    {
        Territory tSelected = null;
        
        // Loop through territories and paint each
        for (Territory t : territories_)
        {
            // don't paint if edge
            if (t.isEdge()) continue;

            if (t.isSelected())
            {
                tSelected = t;
                continue;
            }

            drawTerritory(g, t, iPart);
        }     
        
        if (tSelected != null)
        {
            drawTerritory(g, tSelected, iPart);
        }
    }
    
    /**
     * Get path and draw territory part
     */
    protected void drawTerritory(Graphics2D g, Territory t, int iPart)
    {
        Rectangle territoryBounds = t.getScaledPathBounds();
        
        // don't paint if not in repaint area
        if (!bounds_.intersects(territoryBounds)) return;
        
        drawTerritoryPart(g, t, t.getScaledPath(), territoryBounds, iPart);
    }
    
    // store last stroke to avoid recreating them
    private BasicStroke lastStroke_ = null;
    
    /** 
     * Draw specific part.  This class reserves part number 1-100 (see PART_XXX)
     */
    protected void drawTerritoryPart(Graphics2D g, Territory t, GeneralPath path,
                                    Rectangle territoryBounds, int iPart)
    {
        switch(iPart)
        {
            case PART_FILL:
                
                if (!bFill_) return;
                
                // draw fill     
                Color tColor = fireGetTerritoryColor(t);
                if (tColor == null) return;
                
                g.setColor(tColor);
                g.fill(path);
                
                break;

            case PART_BORDERS:
                
                boolean bFill = bFill_;
                Color cBorder = fireGetTerritoryBorderColor(t);
                if (cBorder == null) return;
                
                BasicStroke stroke = fireGetTerritoryBorderStroke(t);
                if (stroke != null)
                {
                    bFill = true;
                    
                    java.awt.geom.Area area = t.getScaledPathArea();
                    float nOrigStrokeWidth = stroke.getLineWidth();
                    float nNewStrokeWidth = (float) scaleToCurrentSpace(nOrigStrokeWidth);

                    // create new stroke if diff from last one
                    if (lastStroke_ == null || 
                            (lastStroke_.getLineWidth() != nNewStrokeWidth ||
                             lastStroke_.getEndCap() != stroke.getEndCap() ||
                             lastStroke_.getLineJoin() != stroke.getLineJoin()))
                    {
                        lastStroke_ = new BasicStroke(nNewStrokeWidth, stroke.getEndCap(), stroke.getLineJoin());
                    }

                    // need to set clip to intersection of current clip and the
                    // territory bounds (so stroke doesn't go outside border
                    // of territory
                    // cloning an area is faster than recreating it every time
                    java.awt.geom.Area areaold = new java.awt.geom.Area(cliparea_);
                    areaold.intersect(area);
                    Shape oldClip = g.getClip(); // to restore later
                    g.setClip(areaold);
                    // draw the border color
                    g.setColor(cBorder);
                    g.setStroke(lastStroke_);
                    g.draw(path);

                    // reset the clip
                    g.setClip(oldClip);
                }
                
                // draw a thin black line around border
                if (bFill)
                {
                    g.setColor(Color.black);
                    g.setStroke(borderStroke_);
                    g.draw(path);
                }
                break;

            case PART_LABEL:
            case PART_LABEL_AI:
                
                Font font = fireGetTerritoryLabelFont(t);
                if (font == null) return; // don't draw label if we don't know font
                
                Color shadowColor = fireGetTerritoryLabelShadowColor(t);
                if (shadowColor == null) shadowColor = Color.white;
                
                Color textColor = fireGetTerritoryLabelColor(t);
                if (textColor == null) textColor = Color.black;
                
                TerritoryPoint tp = t.getTerritoryPoint(Territory.LABEL_LOCATION);
                if (tp == null) return;

                // need to adjust points for current scale
                double x = scaleToCurrentSpace((double)tp.getX());
                double y = scaleToCurrentSpace((double)tp.getY());
                
                // draw name   
                TextUtil tu = new TextUtil(g, font, getTerritoryDisplay(t), getTerritoryDisplayLineSpacing());                
                if (iPart == PART_LABEL)
                {    
                    tu.prepareDraw(x, y, tp.getAngle(), dScale_, getTerritoryLabelAntiAliased(t));
                    tu.drawString(textColor, shadowColor);
                    tu.finishDraw();
                }
                // PART_LABEL_AI
                else if (DEBUG_AI && aidebug_ != null)
                {
                    String s = aidebug_.getDebugDisplay(t);
                    if (s != null)
                    {
                        TextUtil tu2 = new TextUtil(g, font, s);
                        tu2.prepareDraw(x,y, tp.getAngle(), dScale_ * aidebug_.getScale(), getTerritoryLabelAntiAliased(t));
                        tu2.yadjust = aidebug_.getYAdjust(t, tu, tu2);
                        tu2.drawString(aidebug_.getTextColor(),  shadowColor);
                        tu2.finishDraw();
                    }
                }
                break;

            case PART_CUSTOM:
                if (customDrawer_ != null)
                {
                    customDrawer_.drawTerritoryPart(this, g, t, path, territoryBounds, iPart);
                }
                break;
        }
    }
    
    /**
     * Get whether labels are antialiased
     */
    protected boolean getTerritoryLabelAntiAliased(Territory t)
    {
        return true;
    }
    
    /**
     * Get line spacing for territory labels
     */
    protected float getTerritoryDisplayLineSpacing()
    {
        return 0.0f;
    }
    
    /**
     * Get display string for territory.  Defaults to t.getMapDisplayName()
     */
    protected String getTerritoryDisplay(Territory t)
    {
        return t.getMapDisplayName();
    }
    
    // used for debugging
    boolean DEBUG_AI = TESTING(EngineConstants.TESTING_AI_DEBUG);
    public int CURRENT_INDEX = 0;
    public boolean DEBUG_OPTION_1 = false;
    
    /**
     * Called when 'x' pressed - toggles AI Debugging on/off
     */
    private class DebugAI extends AbstractAction
    {
        public void actionPerformed(ActionEvent e) 
        {
            DEBUG_AI = !DEBUG_AI;
            Gameboard.this.repaintVisible(false);
        }
    }
    
    /**
     * Called when 'v' pressed - toggles AI current player
     */
    private class DebugAI2 extends AbstractAction
    {
        public void actionPerformed(ActionEvent e) 
        {
            int nNum = context_.getGame().getNumPlayers();
            CURRENT_INDEX++;
            if (CURRENT_INDEX == nNum) CURRENT_INDEX = 0;
            Gameboard.this.repaintVisible(false);
        }
    }
    
    /**
     * Called when 's' pressed - toggles AI OPTION 1 on/off
     */
    private class DebugAI3 extends AbstractAction
    {
        public void actionPerformed(ActionEvent e) 
        {
            DEBUG_OPTION_1 = !DEBUG_OPTION_1;
            Gameboard.this.repaintVisible(false);
        }
    }
    
    /**
     * Interface for AI debugging
     */
    public static interface AIDebug
    {
        public String getDebugDisplay(Territory t);
        public Color getTextColor();
        public double getScale();
        public double getYAdjust(Territory t, TextUtil tuName, TextUtil tuDebug);
    }
    
    private AIDebug aidebug_;
    
    public void setAIDebug(AIDebug ai)
    {
        aidebug_ = ai;
    }
    
    // stroke used in borders
    private BasicStroke borderStroke_ = new BasicStroke((float)1.0, BasicStroke.CAP_BUTT,
                                                BasicStroke.JOIN_ROUND);
    
    CustomTerritoryDrawer customDrawer_ = null;
    
    /** 
     * Set custom territory drawer
     */
    public void setCustomTerritoryDrawer(CustomTerritoryDrawer custom)
    {
        customDrawer_ = custom;
    }
    
    /**
     * Get custom territory drawer
     */
    public CustomTerritoryDrawer getCustomTerritoryDrawer()
    {
        return customDrawer_;
    }
    
    /**
     * Repaint a game piece container by figuring out
     * what the actual territory is     
     */
    public void repaintContainer(GamePieceContainer container)
    {
        if (container instanceof Territory)
        {
            repaintTerritory((Territory) container);
        }
        else if (container instanceof Token)
        {
            repaintTerritory(((Token) container).getGamePiece().getTerritory());
        }
    }
    
    /**
     * Repaint territory, bImmediate = false
     */
    public void repaintTerritory(Territory t)
    {
        repaintTerritory(t, false);
    }
    
    /**
     * repaint given territory by repainting rectangle that contains it.
     * Multiple calls to this in a row tend to be coallesced by AWT.
     */
    public void repaintTerritory(Territory t, boolean bImmediate)
    {
        if (t == null) return;
        Rectangle r = t.getScaledPathBounds();
        
        // add one to width - needed to ensure full width and height are painted
        // (without it, pixels on east/south side of territory don't get repainted)
        // I think this is needed due to floating point math
        
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT_DETAILS)) {
            logger.debug("REPAINT TERRITORY " + t.getName() + ": " + r);
        }
        
        if (bImmediate)
        {
            paintImmediately(r.x, r.y, r.width+1, r.height+1);
        }
        else
        {
            repaint(r.x, r.y, r.width+1, r.height+1);
        }
    }
    
    // kind of a hack, but i don't want to pass bMouseAction 
    // into gamepiece listeners in findPieceAt
    private boolean bMouseMovementTriggeredPieceFound_ = false;
    
    /**
     * Repaint gamepiece, passing bImmediate based on whether the
     * use triggered the mouse movement or not (can enter through
     * scrolling
     */
    public void repaintGamePiece(EngineGamePiece gp)
    {
        repaintGamePiece(gp, bMouseMovementTriggeredPieceFound_);
    }
    
    /**
     * Repaint gamepiece
     */
    public void repaintGamePiece(EngineGamePiece e, boolean bImmediate)
    {
        if (e == null) return;

        Rectangle r = e.getLastDrawnBounds();
        
        // if width is null, this piece hasn't been drawn yet, so
        // repaint entire territory
        if (r.width == 0)
        {
            if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT_DETAILS)) {
                logger.debug("REPAINT GAMEPIECE " + e.getName() + ": delegating to Territory");
            }
            repaintTerritory(e.getTerritory());
            return;
        }
        
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT_DETAILS)) {
            logger.debug("REPAINT GAMEPIECE " + e.getName() + ": " + r);
        }
        
        if (bImmediate)
        {
            paintImmediately(r.x, r.y, r.width, r.height);
        }
        else
        {
            repaint(r.x, r.y, r.width, r.height);
        }
    }
    
    private static final int MIN_MOUSEPIECE_WIDTH = 20;//Cursors.CURSOR_WIDTH + 5;
    
    private static int YSHIFT = 10;
    private static int XSHIFT = -10;
    
    /**
     * Draw items at mouse
     */
    protected void drawMouseItems(Graphics2D g)
    {
        if (paintAtMouse_ == null) return;
        
        Dimension r = paintAtMouse_.getDrawingSize(this, MIN_MOUSEPIECE_WIDTH);
        
        int x = lastMouseX_ - (r.width)/2;
        int y = lastMouseY_ - (r.height)/2;
        
        // shift so numbers are visible
        y += scaleToCurrentSpace(YSHIFT);
        x += scaleToCurrentSpace(XSHIFT);
        
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT_DETAILS)) {
            logger.debug(paintAtMouse_.getName() + " calling drawImage: " + x+","+y+ " " +r.width+"x"+r.height+ " scale: " + dScale_);
        }
        
        int nMoving = paintAtMouse_.getMovingQuantity();
        paintAtMouse_.drawImageAt(g, paintAtMouse_.getImageComponent(), 
                                  nMoving, 0, nMoving,
                                  x, y, r.width, r.height, 
                                  dScale_);
    }
    
    /**
     * Repaint current paintAtMouse
     */
    public void repaintMouseItems(boolean bImmediate)
    {
        repaintMouseItems(paintAtMouse_, bImmediate);
    }
    
    /**
     * repaint area around mouse equal to this gamepiece
     * (we repaint the union of the piece at the current and 
     *  previous mouse positions, so old one is painted over)
     */
    private void repaintMouseItems(EngineGamePiece paintThis, boolean bImmediate)
    {
        if (paintThis == null) return;

        Dimension r = paintThis.getDrawingSize(this, MIN_MOUSEPIECE_WIDTH);
        
        int x = Math.min(prevMouseX_, lastMouseX_);
        int y = Math.min(prevMouseY_, lastMouseY_);
        int xmax = Math.max(prevMouseX_, lastMouseX_);
        int ymax = Math.max(prevMouseY_, lastMouseY_);
        
        // if x == -1, then just repainting old mouse
        if (x == -1 || y == -1)
        {
            x = Math.max(prevMouseX_, lastMouseX_);
            y = Math.max(prevMouseY_, lastMouseY_);
        }
        
        int drawwidth = (xmax - x) + r.width + 1; // extra 1 to ensure repaint clean
        int drawheight = (ymax - y) + r.height + 1;
        
        x -= (r.width/2);
        y -= (r.height/2);
        
        // shift so numbers are visible
        y += scaleToCurrentSpace(YSHIFT);
        x += scaleToCurrentSpace(XSHIFT);
        
        // 6/25/03 - now using paint immediately so it works with
        // the blit scrolling
        if (bImmediate) {
            paintImmediately(x, y,drawwidth,drawheight);
        }
        // paintImmediately isn't discernably faster - and looks bad when
        // moving mouse and using wheel at same time (paints before scroll repaint)
        // so this is used when moving mouse
        else {
            repaint(x,y,drawwidth,drawheight);
        }
        
    }

    // item painted at mouse
    EngineGamePiece paintAtMouse_ = null;

    /**
     * Set gamepiece we are painting at mouse (bImmediate == false)
     */
    public void setPaintAtMouse(EngineGamePiece piece)
    {
        setPaintAtMouse(piece, false);
    }
    
    /**
     * Set gamepiece we are painting at mouse
     */
    public void setPaintAtMouse(EngineGamePiece piece, boolean bImmediate)
    {
        if (piece == null && paintAtMouse_ != null)
        {
            // repaint area of old item w/out painting the item itself
            EngineGamePiece old = paintAtMouse_;
            paintAtMouse_ = null;
            repaintMouseItems(old, bImmediate);
        }
        
        paintAtMouse_ = piece;
        if (paintAtMouse_ != null)
        {
            repaintMouseItems(bImmediate);
        }
    }
    
    /**
     * Get game piece we are painting at mouse
     */
    public EngineGamePiece getPaintAtMouse()
    {
        return paintAtMouse_;
    }
    
    ///
    /// convienence methods
    ///
    
    Point lastPointFromAWT_ = null;
    private int prevMouseX_ = 0;
    private int prevMouseY_ = 0;
    private int lastMouseX_ = 0;
    private int lastMouseY_ = 0;
    
    /**
     * set x/y, store old one
     */
    private void setMouseXY(int x, int y)
    {
        prevMouseX_ = lastMouseX_;
        prevMouseY_ = lastMouseY_;
        lastMouseX_ = x;
        lastMouseY_ = y;
    }
    
    /**
     * Return Point indicating last mouse point
     */
    public Point getLastMousePoint()
    {
        return new Point(lastMouseX_, lastMouseY_);
    }
    
    /**
     * Set board's mouse point to last AWT point
     * (needed for case where last point not known)
     */
    public void setLastMousePointFromAWT()
    {
        setLastMousePoint(lastPointFromAWT_);
    }

    /**
     * Set new "last mouse point".  Used by ScrollGameboard
     * after resize & move.  Calls setLastMousePoint()
     */
    void setLastMousePoint(Point p)
    {
        setMouseXY(p.x, p.y);
        setLastMousePoint();
    }
    
    /**
     * Find items at last mouse point stored by gameboard
     * (or set previously sith setLastMousePoint)
     */
    private void setLastMousePoint()
    {        
        findItemsAt(lastMouseX_, lastMouseY_, false);
    }
    
    /**
     * Find territory/piece at current mouse location and store it.
     */
    private void findItemsUnderMouse(MouseEvent e)
    {
        if (e == null) return;

        setMouseXY(e.getX(), e.getY());
        findItemsAt(e.getX(), e.getY(), true);
    }
    
    /**
     * Find items at the given x,y coordinate
     */
    private void findItemsAt(double x, double y, boolean bMouseAction)
    {
        findTerritoryAt(x,y,bMouseAction);
        findPieceAt(x,y,bMouseAction);
    }
    
    /**
     * Finds territory at given x,y coordinates (in current space)
     */
    private void findTerritoryAt(double x, double y, boolean bMouseAction)
    {
        Territory newTerritory = null;

        for (int i = 0; i < territories_.length && x >= 0 && y >= 0; i++)
        {
            if (territories_[i].isEdge()) continue;
            //BUG 236 - undo (need this to find pieces that overlap decoration regions) 
            //if (territories_[i].isDecoration()) continue; // BUG 133 - skip decorations
            if (territories_[i].scaledPathContains(x, y))
            {
                newTerritory = territories_[i];
                break;
            }
        }
        
        Territory old = territoryAtMouse_;
        territoryAtMouse_ = newTerritory;

        if (old != null) {
            old.setUnderMouse(false);
        }

        if (territoryAtMouse_ != null) {
            territoryAtMouse_.setUnderMouse(true);
        }
        
        // if we have a new territory, fire exit/enter events
        // Always fire if this wasn't from a mouse action (useful
        // to ensure correct behavoir when scrolling)
        if (old != territoryAtMouse_ || !bMouseAction)
        {
            if (old != null) fireMouseExited(old);
            if (territoryAtMouse_ != null) fireMouseEntered(territoryAtMouse_);
        }
    }
    
    /**
     * Find if any piece is under mouse in territory
     */
    private void findPieceAt(double x, double y, boolean bMouseAction)
    {
        bMouseMovementTriggeredPieceFound_ = bMouseAction;
        EngineGamePiece newPiece = null;
        
        //logger.debug("findPieceAt " + x +","+y);
        //if (x < 0 || y < 0) logger.debug(Utils.formatExceptionText(new Throwable()));
        
        // must be over a territory and x/y not negative
        if (territoryAtMouse_ != null && x >= 0 && y >= 0)
        {
            // look at territory under mouse first
            newPiece = findPieceInTerritory(territoryAtMouse_, x, y);
            
            // if nothing there, check adjacent territories
            if (newPiece == null)
            {
                Territory[] adjacent = territoryAtMouse_.getAdjacentTerritories();
                for (int i = 0; newPiece == null && i < adjacent.length; i++)
                {
                    newPiece = findPieceInTerritory(adjacent[i], x, y);
                }
            }
        }
        
        EngineGamePiece old = pieceAtMouse_;
        pieceAtMouse_ = newPiece;

        if (old != null) {
            old.setUnderMouse(false);
        }

        if (pieceAtMouse_ != null) {
            pieceAtMouse_.setUnderMouse(true);
        }
        
        // if we have a new territoyr, fire exit/enter events
        // Always fire if this wasn't from a mouse action (useful
        // to ensure correct behavoir when scrolling)
        if (old != pieceAtMouse_ || !bMouseAction)
        {
            if (old != null) fireMouseExited(old);
            if (pieceAtMouse_ != null) fireMouseEntered(pieceAtMouse_);
        }
    }
    
    /**
     * See if given territory has a piece at the x,y coordinates
     */
    private EngineGamePiece findPieceInTerritory(Territory t, double x, double y)
    {
	 	if (t.isDecoration()) return null;

        EngineGamePiece foundPiece = null;
        EngineGamePiece piece;
        Rectangle bounds;
        ImageComponent image;
        int imagex, imagey;
        double dTerritoryScale;

        synchronized (t.getMap())
        {
            Iterator pieces = t.getGamePieces();
            while (pieces.hasNext())
            {
                piece = (EngineGamePiece) pieces.next();
                if (!piece.isVisible() || !piece.allowMouseOver()) continue;
                
                bounds = piece.getLastDrawnBounds();
                dTerritoryScale = piece.getScale();

                if (x >= bounds.x && x < (bounds.x + bounds.width) &&
                    y >= bounds.y && y < (bounds.y + bounds.height))
                {
                    // figure out x,y in image
                    imagex = (int)(scaleToMapSpace(x - bounds.x) / dTerritoryScale);
                    imagey = (int)(scaleToMapSpace(y - bounds.y) / dTerritoryScale);
                    
                    // get image
                    image = piece.getImageComponent();
                    
                    // if non transparent, piece is found
                    if (image.isNonTransparent(imagex, imagey))
                    {
                        foundPiece = piece;
                    }
                    //we want piece nearest end of iter, so keep looking
                    //break;
                }
            }
        }
        return foundPiece;
    }
    
    /**
     * Return territory currently under the mouse
     */
    public Territory getTerritoryUnderMouse()
    {
        return territoryAtMouse_;
    }
    
    /**
     * Return piece currently under the mouse
     */
    public EngineGamePiece getGamePieceUnderMouse()
    {
        return pieceAtMouse_;
    }
    
    /**
     * Return currently selected territory
     */
    public Territory getSelectedTerritory()
    {
        return territorySelected_;
    }
    
    ///
    /// Territory listeners
    ///
    
    private List<TerritorySelectionListener> tlisteners_ = new ArrayList<TerritorySelectionListener>();
    private List<GamePieceSelectionListener> elisteners_ = new ArrayList<GamePieceSelectionListener>();
    
    /**
     * Add a territory selection listener
     */
    public synchronized void addTerritorySelectionListener(TerritorySelectionListener t)
    {
        if (!tlisteners_.contains(t))
        {
            tlisteners_.add(t);
        }
    }
    
    /**
     * Remove a territory selection listener
     */
    public synchronized void removeTerritorySelectionListener(TerritorySelectionListener t)
    {
        tlisteners_.remove(t);
    }
    
    /**
     * Add a game piece selection listener
     */
    public synchronized void addGamePieceSelectionListener(GamePieceSelectionListener t)
    {
        if (!elisteners_.contains(t))
        {
            elisteners_.add(t);
        }
    }
    
    /**
     * Remove a territory selection listener
     */
    public synchronized void removeGamePieceSelectionListener(GamePieceSelectionListener t)
    {
        elisteners_.remove(t);
    }
    
    private TerritoryDisplayListener tdisplaylistener_ = null;
    
    /**
     * Set the territory display listener
     */
    public void setTerritoryDisplayListener(TerritoryDisplayListener t)
    {
        tdisplaylistener_ = t;
    }
    
    /**
     * Get the territory display listener
     */
    public TerritoryDisplayListener getTerritoryDisplayListener()
    {
        return tdisplaylistener_;
    }
    
    /**
     * Notify all TerritorySelectionListeners of a selected territory
     */
    public synchronized void fireTerritorySelected(Territory t, MouseEvent e)
    {
        for (TerritorySelectionListener aTlisteners_ : tlisteners_)
        {
            aTlisteners_.territorySelected(t, e);
        }
    }
    
    /**
     * Notify all GamePieceSelectionListeners of a deselected game piece
     */
    public synchronized void fireGamePieceDeselected(EngineGamePiece gp, MouseEvent e)
    {
        for (GamePieceSelectionListener anElisteners_ : elisteners_)
        {
            anElisteners_.gamePieceDeselected(gp, e);
        }
    }

    /**
     * Notify all GamePieceSelectionListeners of a selected game piece
     */
    public synchronized void fireGamePieceSelected(EngineGamePiece gp, MouseEvent e)
    {
        for (GamePieceSelectionListener anElisteners_ : elisteners_)
        {
            anElisteners_.gamePieceSelected(gp, e);
        }
    }

    /**
     * Notify all TerritorySelectionListeners of a mouseEntered on a territory
     */
    public synchronized void fireMouseEntered(Territory t)
    {
        for (TerritorySelectionListener aTlisteners_ : tlisteners_)
        {
            aTlisteners_.mouseEntered(this, t);
        }
    }  
    
    /**
     * Notify all TerritorySelectionListeners of a mouseExited on a territory
     */
    public synchronized void fireMouseExited(Territory t)
    {
        for (TerritorySelectionListener aTlisteners_ : tlisteners_)
        {
            aTlisteners_.mouseExited(this, t);
        }
    }    
    
    /**
     * Notify all GamePieceSelectionListeners of a mouseEntered on a game piece
     */
    public synchronized void fireMouseEntered(EngineGamePiece e)
    {
        for (GamePieceSelectionListener anElisteners_ : elisteners_)
        {
            anElisteners_.mouseEntered(this, e);
        }
    }  
    
    /**
     * Notify all GamePieceSelectionListeners of a mouseExited on a game piece
     */
    public synchronized void fireMouseExited(EngineGamePiece e)
    {
        for (GamePieceSelectionListener anElisteners_ : elisteners_)
        {
            anElisteners_.mouseExited(this, e);
        }
    }   
    
    /**
     * Call allowTerritorySelection on all TerritorySelectionListeners.  
     * If any return true, this returns true.
     */
    public synchronized  boolean fireAllowTerritorySelection(Territory t, MouseEvent e)
    {
        if (t.isDecoration()) return false;

        for (TerritorySelectionListener aTlisteners_ : tlisteners_)
        {
            if (aTlisteners_.allowTerritorySelection(t, e))
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Call allowGamePieceSelection on all GamePieceSelectionListeners.  
     * If any return true, this returns true 
     */
    public synchronized boolean fireAllowGamePieceSelection(EngineGamePiece gp, MouseEvent e)
    {
        for (GamePieceSelectionListener anElisteners_ : elisteners_)
        {
            if (anElisteners_.allowGamePieceSelection(gp, e))
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get territory color from TerritoryDisplayListener
     */
    public Color fireGetTerritoryColor(Territory t)
    {
        if (tdisplaylistener_ == null) return null;
        return tdisplaylistener_.getTerritoryColor(t);
    }
    
    /**
     * Get territory border color from TerritoryDisplayListener
     */
    public BasicStroke fireGetTerritoryBorderStroke(Territory t)
    {
        if (tdisplaylistener_ == null) return null;
        return tdisplaylistener_.getTerritoryBorderStroke(t);
    }
    
    /**
     * Get territory border color from TerritoryDisplayListener
     */
    public Color fireGetTerritoryBorderColor(Territory t)
    {
        if (tdisplaylistener_ == null) return null;
        return tdisplaylistener_.getTerritoryBorderColor(t);
    }
    
    /**
     * Get territory label font from TerritoryDisplayListener
     */
    public Font fireGetTerritoryLabelFont(Territory t)
    {
        if (tdisplaylistener_ == null) return null;
        return tdisplaylistener_.getTerritoryLabelFont(context_, t);
    }
    
    /**
     * Get territory label color from TerritoryDisplayListener
     */
    public Color fireGetTerritoryLabelColor(Territory t)
    {
        if (tdisplaylistener_ == null) return null;
        return tdisplaylistener_.getTerritoryLabelColor(context_, t);
    }
    
    /**
     * Get territory label shadow color from TerritoryDisplayListener
     */
    public Color fireGetTerritoryLabelShadowColor(Territory t)
    {
        if (tdisplaylistener_ == null) return null;
        return tdisplaylistener_.getTerritoryLabelShadowColor(t);
    }
    
    /**
     * Select a single territory
     */
    public static final int SELECTION_MODE_SINGLE = 1;
    
    /**
     * Select territories until turned off
     */
    public static final int SELECTION_MODE_MULTIPLE = 2;
    
    /**
     * No selections allowed (default)
     */
    public static final int SELECTION_MODE_OFF = 10;
    private int nSelectionMode_ = SELECTION_MODE_OFF;
    private int nPieceSelectionMode_ = SELECTION_MODE_OFF;
    
    // mode stuff
    
    /**
     * Set selection mode for territories
     */
    public void setTerritorySelectionMode(int nMode)
    {
        nSelectionMode_ = nMode;
    }
    
    /**
     * Get selection mode for territories
     */
    public int getTerritorySelectionMode()
    {
        return nSelectionMode_;
    }
    
    /**
     * Set selection mode for game pieces
     */
    public void setGamePieceSelectionMode(int nMode)
    {
        nPieceSelectionMode_ = nMode;
    }
    
    /**
     * Get selection mode for territories
     */
    public int getGamePieceSelectionMode()
    {
        return nPieceSelectionMode_;
    }
 
    /**
     * Handles a mouse click on a territory
     */
    private void processTerritorySelection(MouseEvent e)
    {
        if (nSelectionMode_ == SELECTION_MODE_OFF) return;
        
        if (territoryAtMouse_ == null) return;
        
        if (isScrolling()) return;
        
        // can't think of case where we wouldn't always
        // want to do this, so put here in gameboard 
        // so each listener doesn't have to check
        if (territoryAtMouse_.isEdge()) return;
        if (territoryAtMouse_.isDecoration()) return;
        
        if (!fireAllowTerritorySelection(territoryAtMouse_, e))
        {
            return;
        }
        
        setSelectedTerritory(territoryAtMouse_, e);
    }

    /**
     * Set selected territory (fires any registered listeners)
     */
    public void setSelectedTerritory(Territory t)
    {
        setSelectedTerritory(t, null);
    }
    
    /**
     * Set selected territory (fires any registered listeners)
     */
    private void setSelectedTerritory(Territory t, MouseEvent e)
    {
        
        Territory old = territorySelected_;
        territorySelected_ = t;
        
        if (old != territorySelected_)
        {
            // if in single selection mode, done, so turn off
            if (nSelectionMode_ == SELECTION_MODE_SINGLE)
            {
                nSelectionMode_ = SELECTION_MODE_OFF;
            }
            
            if (old != null) {
                old.setSelected(false);
            }
            if (territorySelected_ != null)
            {
                territorySelected_.setSelected(true);
                fireTerritorySelected(territorySelected_, e);
            }
 
            repaintTerritory(old, false);
            repaintTerritory(territorySelected_, false);
        }
    }
    
    /**
     * Set selected game piece (fires any registered listeners)
     */
    private void processGamePieceSelection(MouseEvent e)
    {
        if (nPieceSelectionMode_ == SELECTION_MODE_OFF) return;
        
        if (pieceAtMouse_ == null) return;
        
        if (isScrolling()) return;
        
        if (!fireAllowGamePieceSelection(pieceAtMouse_, e))
        {
            return;
        }
        
        setSelectedGamePiece(pieceAtMouse_, e);
    }
 
    /**
     * Set selected territory (fires any registered listeners)
     */
    public void setSelectedGamePiece(EngineGamePiece gp)
    {
        setSelectedGamePiece(gp, null);
    }
    
    /**
     * Set selected territory (fires any registered listeners)
     */
    private void setSelectedGamePiece(EngineGamePiece gp, MouseEvent e)
    {
        EngineGamePiece old = pieceSelected_;
        pieceSelected_ = gp;
        
        if (old != pieceSelected_)
        {
            // if in single selection mode, done, so turn off
            if (nPieceSelectionMode_ == SELECTION_MODE_SINGLE)
            {
                nPieceSelectionMode_ = SELECTION_MODE_OFF;
            }
            
            if (old != null)
            {
                old.setSelected(false);
                repaintGamePiece(old, false);
                fireGamePieceDeselected(old, e);
            }
            if (pieceSelected_ != null)
            {
                pieceSelected_.setSelected(true);
                repaintGamePiece(pieceSelected_, false);
                fireGamePieceSelected(pieceSelected_, e);
            }
        }
    }
    
    ////
    //// Scrollable interface methods (and other scroll stuff)
    ////
    
    private boolean bScrolling_ = false;
    
    /**
     * Set whether this is being scrolled by ScrollGameboard
     */
    void setScrolling(boolean bScrolling)
    {
        //if (bScrolling_ == bScrolling) return;
        bScrolling_ = bScrolling;
        if (bScrolling)
        {
            // set this to null to prevent processing mouse clicks
            territoryAtMouseWhenPressed_ = null;
            pieceAtMouseWhenPressed_ = null;
        }
        else
        {
            // reset territory and piece under mouse
            setLastMousePoint(); 
        }
    }
    
    /**
     * Is this board currently being scrolled?
     */
    public boolean isScrolling()
    {
        return bScrolling_;
    }
    
    /**
     * returns getPreferredSize()
     */
    public Dimension getPreferredScrollableViewportSize() {
        return this.getPreferredSize();
    }
    
    /**
     * returns 250
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 250;
    }
    
    /**
     * returns false
     */
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
    
    /**
     * returns false
     */
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }
    
    /**
     * Returns 25
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 25;
    }

    ////
    //// Mouse Motion Listener methods
    ////
    
    /**
     * Finds items under mouse
     */
    public void mouseDragged(MouseEvent e) {
        findItemsUnderMouse(e);
        repaintMouseItems(false);
    }
    
    /**
     * Finds items under mouse
     */
    public void mouseMoved(MouseEvent e) {
        findItemsUnderMouse(e);
        repaintMouseItems(false);
    }
    
    ////
    //// Mouse Motion Listener methods
    ////
    
    /**
     * Empty
     */
    public void mouseClicked(MouseEvent e) {
    }
    
    /**
     * Finds items under mouse
     */
    public void mouseEntered(MouseEvent e) {
        findItemsUnderMouse(e);
        repaintMouseItems(false);
    }
    
    /**
     * Finds items under mouse
     */
    public void mouseExited(MouseEvent e) {
        // ignore mouse exited events internal to the gameboard - 
        // can happen when components are used in board like in DD Poker
        if (e != null && contains(e.getX(), e.getY())) return;
        setMouseXY(-1,-1);
        setLastMousePoint();
        territoryAtMouseWhenPressed_ = null;
        pieceAtMouseWhenPressed_ = null;
        repaintMouseItems(false);
    }
    
    /**
     * Finds items under mouse, stores them
     */
    public void mousePressed(MouseEvent e) {
        findItemsUnderMouse(e);

        // for poker, we have explicit keyboard focus
        // indicator and requesting focus when clicked
        // doesn't work with pop-up menus.  For now, leave
        // this off.  I added an explicit call to requestFocus
        // when the popup goes away and when no popup shown - Doug

//        // BUG 247 - change focus to board when a click happens
//        // and unselect any dialogs to give visible confirmation
//        if (focusMgr_.getFocusOwner() != this)
//        {
//            //logger.debug("Changing focus to gameboard");
//            InternalDialog.unselectAll();
//            this.requestFocus();
//        }
        
        // if scrolling, don't record territory at mouse 
        if (!isScrolling())
        {
            territoryAtMouseWhenPressed_ = territoryAtMouse_;
            pieceAtMouseWhenPressed_ = pieceAtMouse_;
        }
    }
    
    /**
     * finds items under mouse and processes them (if they 
     * were under mouse when it was pressed)
     */
    public void mouseReleased(MouseEvent e) {
        findItemsUnderMouse(e);
        
        // right click when drawing piece at mouse is a cancel, so return
        if (e.getButton() != MouseEvent.BUTTON1 && paintAtMouse_ != null) return;
        
        if (territoryAtMouse_ == territoryAtMouseWhenPressed_)
        {
            processTerritorySelection(e);
        }
        
        if (pieceAtMouse_ == pieceAtMouseWhenPressed_)
        {
            processGamePieceSelection(e);
        }
    }
    
    ///
    /// Cursor
    ///
    
    Cursor cursor_ = getCursor();
    Cursor overrideCursor_ = null;
    
    @Override
    public void setCursor(Cursor c)
    {
        cursor_ = c;
        if (overrideCursor_ == null) super.setCursor(cursor_);
    }
    
    public void setOverrideCursor(Cursor c)
    {
        if (overrideCursor_ == c) return;
        overrideCursor_ = c;
        if (overrideCursor_ != null) super.setCursor(overrideCursor_);
        else super.setCursor(cursor_);
    }
}
