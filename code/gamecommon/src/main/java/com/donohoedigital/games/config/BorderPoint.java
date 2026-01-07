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
 * BorderPoint.java
 *
 * Created on October 28, 2002, 4:49 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.config.*;

import org.jdom2.*;
import java.util.*;


/**
 *
 * @author  Doug Donohoe
 */
public class BorderPoint extends MapPoint
{
    static Logger logger = LogManager.getLogger(BorderPoint.class);
    
    private BorderArrayList myBorders_ = new BorderArrayList(1);

    /** 
     * New BorderPoint from x,y
     */
    public BorderPoint(int x, int y) {
        super(x,y);
    }
    
    /**
     * New BorderPoint from an XML Element
     */
    public BorderPoint(Element point, Namespace ns, String sAttrErrorDesc)
                throws ApplicationError
    {
        super(point, ns, sAttrErrorDesc);
    }
    
    /**
     * Get borders this point belongs to
     */
    public BorderArrayList getBorders()
    {
        return myBorders_;
    }
    
    /**
     * is anchor point?
     */
    public boolean isAnchor()
    {
        return (myBorders_.size() > 1);
    }
    
    /**
     * adds a border that this point
     * belongs to (called from Border)
     */
    public void addBorder(Border border)
    {
        myBorders_.addBorder(border);
    }
    
    /**
     * removes a border that this point
     * belongs to (called from Border)
     */
    void removeBorder(Border border)
    {
        myBorders_.removeBorder(border);
        if (this.bCurrentBorder_ == border) bCurrentBorder_ = null;
    }
   
    /**
     * removes this point from list of borders
     */
    public void removeFromAllBorders()
    {
        BorderArrayList local = (BorderArrayList) myBorders_.clone();
        for (int i = 0; i < local.size(); i++)
        {
            local.getBorder(i).removeBorderPoint(this);
        }
    }
    
    /**
     * removes this point from given border
     */
    public void removeFromBorder(Border border)
    {
        border.removeBorderPoint(this);
    }
    
    /**
     * Shift this point in the border array up (+) / down(-).
     * Wraps around if reaches end/beginning
     */
    public void shift(boolean bUp)
    {       
        Border border = bCurrentBorder_;
        if (border == null) return;
        int nThisPoint = border.getPointIndex(this);
        if (bUp) nThisPoint++;
        else     nThisPoint--;
        border.removeBorderPoint(this);
         
        int nSize = border.size();
        if (nThisPoint > nSize) nThisPoint = 0;
        if (nThisPoint < 0) nThisPoint = nSize;
        border.addBorderPoint(this, nThisPoint);
    }        

    /**
     * String representation of point for debugging
     */
    public String toString()
    {
        return longDesc(null);
    }
    
    /**
     * String representation used for UI display
     */
    public String longDesc(Border b)
    {
        StringBuilder msg = new StringBuilder("(" + x_ + "," + y_ + ")");
        boolean bHeader = false;
        int nSize = myBorders_.size();
        for (int i = 0; nSize > 1 && i< nSize; i++)
        {
            if (myBorders_.getBorder(i) != b)
            {
                if (!bHeader) {
                    if (b == null) msg.append(" used in: ");
                    else msg.append("\nShared with: ");
                    bHeader = true;
                }
                else
                {
                    msg.append(", ");
                }
                msg.append(myBorders_.getBorder(i).shortDesc());
            }
        }
        return msg.toString();
    }
    
    ///
    /// UI helper methods
    ///
    
    // current (active) border in UI
    private Border bCurrentBorder_;
    
    /**
     * set the border this point is representing in the UI
     */
    public void setCurrentBorder(Border border)
    {
        for (int i = 0; i <myBorders_.size(); i++)
        {
            if (myBorders_.getBorder(i) == border)
            {
                bCurrentBorder_ = border;
                return;
            }
        }
        bCurrentBorder_ = null;
        logger.warn("setCurrentBorder to border: " + border.shortDesc() + 
                            " - is not a border in point " + longDesc(null));
    }
    
    /**
     * get the border this is representing in the UI
     */
    public Border getCurrentBorder()
    {
        // if no border set, but we have borders, return 1st border
        if (bCurrentBorder_ == null && myBorders_.size() > 0)
        {
            bCurrentBorder_ = myBorders_.getBorder(0);
        }
        return bCurrentBorder_;
    }
    
    /**
     * Get next border in array list after the current one
     */
    public Border nextBorder()
    {
        for (int i = 0; i <myBorders_.size(); i++)
        {
            if (myBorders_.getBorder(i) == bCurrentBorder_)
            {
                int nIndex = i+1;
                if (nIndex >= myBorders_.size()) nIndex = 0;
                return myBorders_.getBorder(nIndex);
            }
        }
        logger.warn("nextBorder() not found for " + bCurrentBorder_.shortDesc() +
                                    " in point " + longDesc(null));
        return null;
    }
    
    /**
     * Return nearest point to this one.  Used by GUIs
     * about to remove this point from the border to determine
     * what should become active.
     */
    public BorderPoint getNearestPoint()
    {
        return getNavPoint(POINT_DELETE, bCurrentBorder_);     
    }
    
    /**
     * Return next point in border.  Used for tabbing through points
     */
    public BorderPoint getNextPoint()
    {
        return getNavPoint(POINT_NEXT, bCurrentBorder_);
    }
    
    /**
     * Return previous point in the border.  Used for shift-tabbing through points
     */
    public BorderPoint getPrevPoint()
    {
        return getNavPoint(POINT_PREV, bCurrentBorder_);
    }
    
    private static final int POINT_NEXT = 1;
    private static final int POINT_PREV = 2;
    private static final int POINT_DELETE = 3;
    
    /**
     * Gets related point based on navigation type
     */
    private BorderPoint getNavPoint(int nType, Border border)
    {
         if (border == null) {
            logger.warn("myBorder is null in BorderPoint.getNavXPoint() " + toString());
            return null;
        }
        int nSize = border.size();
        if (nSize == 1) 
        {
            // if delete nav point, don't return point being deleted
            if (nType == POINT_DELETE) return null;
            // if tab, return only point
            return border.getBorderPoint(0);
        }
        
        int nIndex = border.getPointIndex(this);
        if (nIndex == -1) return null;
        
        switch (nType) {
            case POINT_NEXT:
                if (nIndex == (nSize - 1)) return border.getBorderPoint(0);
                else return border.getBorderPoint(nIndex + 1);
               
            case POINT_PREV:
                if (nIndex == 0) return border.getBorderPoint(nSize - 1);
                else return border.getBorderPoint(nIndex - 1);
                
            case POINT_DELETE:
                if (nIndex == 0) return border.getBorderPoint(1);
                else return border.getBorderPoint(nIndex - 1);
                
            default:
                return null;
        }
    }
}
