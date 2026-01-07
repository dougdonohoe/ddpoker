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
 * EngineGamePiece.java
 *
 * Created on December 9, 2002, 2:55 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import java.awt.*;
import java.awt.geom.*;
/**
 *
 * @author  Doug Donohoe
 */
public abstract class EngineGamePiece extends GamePiece {
    static Logger logger = LogManager.getLogger(EngineGamePiece.class);
    
    protected String tpName_;
    
    protected boolean bNotDrawn_ = false;
    protected boolean bDrawHighlighted_ = false;
    protected int nMovingQuantity_ = 0;
    protected boolean bEnabled_ = true;
    
    protected static Font quantityFont_ = StylesConfig.getFont("gamepiece.quantity");
    protected static Color quantityBG_ =  StylesConfig.getColor("gamepiece.quantity.bg");
    protected static Color quantityHighlightBG_ =  StylesConfig.getColor("gamepiece.quantity.hilite.bg");
    protected static Color quantityFG_ =  StylesConfig.getColor("gamepiece.quantity.fg");
    protected static Color quantityShadow_ =  StylesConfig.getColor("gamepiece.quantity.shadow");
    protected static Color quantityBorder_ =  StylesConfig.getColor("gamepiece.quantity.border");
    
    /**
     * Empty constructor needed for demarshalling
     */
    public EngineGamePiece() {}
    
    /**
     * Creates a new instance of EngineGamePiece
     */
    public EngineGamePiece(int nType, GamePlayer player, String tpName, String sName) {
        super(nType, player, sName);
        tpName_ = tpName;
    }
    
    /**
     * Set name used for territory point
     */
    public String getTerritoryPointName() {
        return tpName_;
    }
    
    /**
     * Get name used to look up territory point
     */
    public void setTerritoryPointName(String tpName) {
        tpName_ = tpName;
    }
    
    /**
     * Set whether this piece is drawn
     */
    public void setNotDrawn(boolean b) {
        bNotDrawn_ = b;
    }
    
    /**
     *  Is this piece drawn?
     */
    public boolean isNotDrawn() {
        return bNotDrawn_;
    }
    
    /**
     * Set whether this piece is enabled
     */
    public void setEnabled(boolean b) {
        bEnabled_ = b;
    }
    
    /**
     *  Is this piece enabled?
     */
    public boolean isEnabled() {
        return bEnabled_;
    }
    
    /**
     * set this piece to draw highlighted
     */
    public void setDrawHighlighted(boolean b) {
        bDrawHighlighted_ = b;
    }
    
    /**
     * Is this piece drawing highlighted?
     */
    public boolean isDrawHighlighted() {
        return bDrawHighlighted_;
    }
    
    /**
     * Is this piece visible on the gameboard.  It might not
     * be if in the middle of a move, or set to not drawn.
     */
    public boolean isVisible() {
        if (isNotDrawn()) return false;

        return (getDrawingQuantity() + getDrawingHiddenQuantity()) > 0;

    }
    
    /**
     * Is this piece mouseable?  Default is true
     */
    public boolean allowMouseOver()
    {
        return true;
    }
    
    /**
     * Set quantity being moved (-1 is bulk move)
     */
    public void setMovingQuantity(int n) 
    {
        ApplicationError.assertTrue(n <= getQuantity(), "Moving quantity greater than quantity");
        nMovingQuantity_ = n;
    }
    
    /**
     * Get quantity being moved
     */
    public int getMovingQuantity() {
        return nMovingQuantity_;
    }
    
    /**
     * Get quantity being drawn (getQuantity - getMovingQuantity)
     */
    public int getDrawingQuantity() {
        return getQuantity() - getMovingQuantity();
    }
    
    /**
     * Get hidden quantity being drawn (getHiddenQuantity if owner is current)
     */
    public int getDrawingHiddenQuantity() {
        int nHiddenNum = 0;
        // only if owner is current player, hidden quantity used (sounds like yoda)
        // BUG 176 - added isComputer() to prevent drawing hidden quantity when computer player
        // is working on purchases
        if (getGamePlayer() != null && getGamePlayer().isCurrentGamePlayer() && 
                (!getGamePlayer().isComputer() || TESTING(EngineConstants.TESTING_AI_DEBUG))) {
            nHiddenNum = getHiddenQuantity();
        }
        
        return nHiddenNum;
    }
    
    /**
     * get scale to draw items in this territory.  Returns
     * getTerritory().getScaleImagesAsDouble().  Can be overridden
     * to stop this behavoir
     */
    public double getScale() {
        if (container_ instanceof Territory) {
            return ((Territory)container_).getScaleImagesAsDouble();
        }
        
        return 1.0d;
    }
    
    /**
     * Called to draw this piece
     */
    public void drawPiece(Gameboard board, Graphics2D g, Territory t,
                        GeneralPath path, Rectangle territoryBounds, int iPart,
                        int x_adjust, int y_adjust) 
    {
        if (!isVisible()) return;

        // get point at which we draw this piece
        TerritoryPoint tp = t.getTerritoryPoint(tpName_);
        if (tp == null) {
            logger.warn("No '" + tpName_ + "' territory point defined for " + t.getName());
            return;
        }
        
        drawPiece(board, g, t, path, territoryBounds, iPart, x_adjust, y_adjust, tp);
    }

    // used for performance so new rect isn't needed everytime we repaint
    // BUG 133 - only need one because we never draw two pieces at
    // same time
    private static Rectangle bounds_ = new Rectangle();
    
    /**
     * Called to draw this piece at given territory point
     */
    public void drawPiece(Gameboard board, Graphics2D g, Territory t,
                        GeneralPath path, Rectangle territoryBounds, int iPart,
                        int x_adjust, int y_adjust, TerritoryPoint tp) 
    {
        // we use isVisible above, which basically calculates if nTotalNum
        // is > 0 and notDrawn is false.
        int nNum = getDrawingQuantity();
        int nHiddenNum = getDrawingHiddenQuantity();
        
        // get image and figure out where to draw it and how big
        ImageComponent ic = getImageComponent();
        int sizewidth = ic.getWidth();
        int sizeheight = ic.getHeight();
        
        // calc pos and size
        double width = board.scaleToCurrentSpace(sizewidth) * getScale();
        double height = board.scaleToCurrentSpace(sizeheight) * getScale();
        double x = board.scaleToCurrentSpace(tp.x_ + x_adjust) - (width/2);// center left-right
        double y;
        
        if (isPositionedAtBottom())
        {
            y = board.scaleToCurrentSpace(tp.y_ + y_adjust) - (height - 2); // position bottom of image at point
        }
        else
        {
            y = board.scaleToCurrentSpace(tp.y_ + y_adjust) - (height/2); // center top-bottom
        }
        
        // store for use later on
        setLastDrawnBounds((int) x, (int) y, (int) width, (int) height);
        
        // get region we are painting (stored for use by all paint methods this call)
        g.getClipBounds(bounds_); 
        
        // don't paint if out of clip area
        if (!bounds_.intersects(boundsLastDrawn_)) return;
        
        if (TESTING(EngineConstants.TESTING_DEBUG_REPAINT_DETAILS))
        {
            logger.debug("Drawing " + getTerritory().getName() + ":" + getName());
        }
        drawImageAt(g, ic, nNum, nHiddenNum, 0, x, y, width, height, board.dScale_);
    }
    
    /**
     * Set last drawn bounds
     */
    protected void setLastDrawnBounds(int x, int y, int width, int height)
    {
        boundsLastDrawn_.width = width;
        boundsLastDrawn_.height = height;
        boundsLastDrawn_.x = x;
        boundsLastDrawn_.y = y;
    }
    
    /**
     * Default is to position bottom of image at territory point.  Override
     * and return false to position at center
     */
    protected boolean isPositionedAtBottom()
    {
        return true;
    }
    
    // used to save object creation
    private Rectangle boundsLastDrawn_ = new Rectangle();
    private Dimension drawingSize_ = new Dimension();
    
    /**
     * Return bounds of image as it was last drawn.  A new Rectangle is
     * not created each time, so consider this read-only.
     */
    public Rectangle getLastDrawnBounds() {
        return boundsLastDrawn_;
    }
    
    /**
     * Get width/height to draw this at current board dimensions.
     * Returns same object each time, so consider it read-only
     */
    public Dimension getDrawingSize(Gameboard board, int nMin) {
        ImageComponent ic = getImageComponent();
        int sizewidth = ic.getWidth();
        int sizeheight = ic.getHeight();
        double width = board.scaleToCurrentSpace(sizewidth) * getScale();
        double height = board.scaleToCurrentSpace(sizeheight) * getScale();
        drawingSize_.width = (int) width;
        drawingSize_.height = (int) height;
        
        if (drawingSize_.height < nMin) {
            drawingSize_.height = nMin;
            double scale = nMin / height;
            drawingSize_.width = (int) (width * scale);
        }
        
        return drawingSize_;
    }
    
    /**
     * code to draw the image and the quantity in a circle (if greater than one)
     */
    public void drawImageAt(Graphics2D g, ImageComponent ic,
                        int nNum, int nHiddenNum, int nMovingNum,
                        double x, double y,
                        double width, double height,
                        double dScale) 
    {
        // get current setting just to be safe
        boolean bOldEnabled = ic.isEnabled();
        boolean bOld = ic.isHighlighted();
        ic.setHighlighted(bDrawHighlighted_);
        ic.setEnabled(bEnabled_);
        
        // draw image
        ic.drawImageAt(g, (int)x, (int)y, (int)width, (int)height);
        ic.setHighlighted(bOld);
        ic.setEnabled(bOldEnabled);
        
        ////
        //// draw quantity
        ////
        
        // nothing possible to draw
        if (nNum == 0 && nHiddenNum == 0 && nMovingNum == 0) return;
        
        // see if we draw the quantity circle
        if (!isQuantityDrawn(nNum, nHiddenNum)) return;
        
        // figure out string to draw
        boolean bDrawEllipse = false;
        String sNum = getQuantityString(nNum, nHiddenNum, nMovingNum);
        if (sNum == null || sNum.length() == 0) return;
        bDrawEllipse = (sNum.length() > 2);
        
        // adjust out x,y to draw num at center of image
        x += (width/2);
        y += (height/2);
        
        // prepare to draw string
        TextUtil tu = new TextUtil(g, quantityFont_, sNum);
        tu.prepareDraw(x, y, null, dScale, true);
        
        // background circle coordinates
        int PAD = 5;
        double dCircleWidth = 0, dCircleHeight=0;
        
        if (bDrawEllipse) {
            dCircleWidth = tu.width + PAD;
            dCircleHeight = tu.lineHeight + PAD + 2;
        } else {
            dCircleWidth = PAD + Math.max(tu.lineHeight, tu.width);
            dCircleHeight = dCircleWidth;
        }
        
        double circx = (x - (dCircleWidth/2));
        double circy = (y - (dCircleHeight/2));
        
        // draw background circle
        Ellipse2D.Double circle = new Ellipse2D.Double(circx, circy, dCircleWidth, dCircleHeight);
        Color cBack = getCustomQuantityBackground(nNum, nHiddenNum, nMovingNum);
        if (cBack == null){
            cBack = ((nHiddenNum > 0 || hasMovedTokens()) && nMovingNum == 0) ? quantityHighlightBG_ : quantityBG_;
        }
        g.setColor(cBack);
        g.fill(circle);
        
        // border of circle
        Color cBorder = getCustomQuantityBorder(nNum, nHiddenNum, nMovingNum);
        if (cBorder == null) cBorder = quantityBorder_;
        g.setColor(cBorder);
        g.draw(circle);
        
        // text w/ shadow
        Color cText = getCustomQuantityForeground(nNum, nHiddenNum, nMovingNum);
        if (cText == null) cText = quantityFG_;
        Color cShadow = getCustomQuantityShadow(nNum, nHiddenNum, nMovingNum);
        if (cShadow == null) cShadow = quantityShadow_;
        tu.drawString(cText, cShadow);
        tu.finishDraw();
    }
    
    /**
     * Used by subclass to override quantity bg color in specific circumstances.
     * Return null to indicate default behavoir
     */
    protected Color getCustomQuantityBackground(int nNum, int nHiddenNum, int nMovingNum)
    {
        return null;
    }
    
    /**
     * Used by subclass to override quantity fg color in specific circumstances.
     * Return null to indicate default behavoir
     */
    protected Color getCustomQuantityForeground(int nNum, int nHiddenNum, int nMovingNum)
    {
        return null;
    }
    
    /**
     * Used by subclass to override quantity border color in specific circumstances.
     * Return null to indicate default behavoir
     */
    protected Color getCustomQuantityBorder(int nNum, int nHiddenNum, int nMovingNum)
    {
        return null;
    }
    
    /**
     * Used by subclass to override quantity text shadow color in specific circumstances.
     * Return null to indicate default behavoir
     */
    protected Color getCustomQuantityShadow(int nNum, int nHiddenNum, int nMovingNum)
    {
        return null;
    }
    
    /**
     * Return string drawn for quantity, can be overriden by subclass for
     * special display
     */
    protected String getQuantityString(int nNum, int nHiddenNum, int nMovingNum) 
    {
        String sNum = null;
        if (nHiddenNum > 0) {
            if (nNum == 0) sNum = "+" + nHiddenNum;
            else {
                sNum = "" + nNum +"+" + nHiddenNum;
            }
        }
        else {
            sNum = "" + nNum;
        }
        
        return sNum;
    }
    
    /**
     *  Should the quantity be drawn over piece? Default is to return true.
     */
    protected boolean isQuantityDrawn(int nNum, int nHiddenNum) {
        return true;
    }
    
    /**
     * Subclass must define this and return an image to draw
     */
    public abstract ImageComponent getImageComponent();
    
    /**
     * Used to return image component used for small icons - default
     * is to return getImageComponent()
     */
    public ImageComponent getImageComponentForIcon()
    {
        return getImageComponent();
    }
    
    ////
    //// Game save logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state, boolean bAdd)
    {
        GameStateEntry entry = super.addGameStateEntry(state, bAdd);
        entry.addToken(tpName_);
        entry.addToken(bNotDrawn_);
        entry.addToken(bDrawHighlighted_);
        // don't save moving quantity
        // don't save enabled
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        super.loadFromGameStateEntry(state, entry);
        tpName_ = entry.removeStringToken();
        bNotDrawn_ = entry.removeBooleanToken();
        bDrawHighlighted_ = entry.removeBooleanToken();
        nMovingQuantity_ = 0;
        bEnabled_ = true; // BUG 200 - don't see reason to disable this on load
    }
}
