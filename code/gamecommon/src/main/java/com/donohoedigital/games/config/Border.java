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
 * Border.java
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
public class Border 
{
    static Logger logger = LogManager.getLogger(Border.class);
    
    private static Territories t_ = null;
    private static MapPoints allPoints_ = null;
    BorderPoints myPoints_ = new BorderPoints();
    
    private Territory t1_;
    private Territory t2_;
    private boolean bEnclosed_;
    private boolean bWrap_;
    private Integer nNum_;
    public static final Integer DEFAULT_NUM = 1;
    
    /**
     * Initialize Border with list of Territories
     */
    static void setTerritories(Territories t)
    {
        t_ = t;
    }
    
    /**
     * Initialize Border with BorderPoints array
     */
    static void setMapPoints(MapPoints points)
    {
        allPoints_ = points;
    }
    
    /** 
     * Creates a new instance of Border 
     */
    public Border(String s1, String s2, boolean bEnclosed)
                throws ApplicationError
    {
        init(s1, s2, bEnclosed, false, DEFAULT_NUM);
    }
    
    /**
     * Create new border
     */
    public Border(Territory t1, Territory t2, boolean bEnclosed)
    {
        t1_ = t1;
        t2_ = t2;
        bEnclosed_ = bEnclosed;
        bWrap_ = false;
        nNum_ = DEFAULT_NUM;
        orderTerritories();
    }
    
    /**
     * Create new border
     */
    public Border(Territory t1, Territory t2, boolean bEnclosed, Integer nNum)
    {
        t1_ = t1;
        t2_ = t2;
        bEnclosed_ = bEnclosed;
        bWrap_ = false;
        nNum_ = nNum;
        orderTerritories();
    }
    
    /**
     * New Border from XML element
     */
    public Border(Element border, Namespace ns, String sAttrErrorDesc)
    {
        String sT1, sT2;
        Boolean bEnclosed, bWrap;
        Integer nNum;
        
        sT1 = XMLConfigFileLoader.getStringAttributeValue(border, "t1", true, sAttrErrorDesc);
        sT2 = XMLConfigFileLoader.getStringAttributeValue(border, "t2", true, sAttrErrorDesc);
        bEnclosed = XMLConfigFileLoader.getBooleanAttributeValue(border, "enclosed", false, sAttrErrorDesc, Boolean.FALSE);
        bWrap = XMLConfigFileLoader.getBooleanAttributeValue(border, "wrap", false, sAttrErrorDesc, Boolean.FALSE);
        nNum = XMLConfigFileLoader.getIntegerAttributeValue(border, "num", false, sAttrErrorDesc, DEFAULT_NUM);
       
        init(sT1, sT2, bEnclosed, bWrap, nNum);
        
        // points in border
        List<Element> points = XMLConfigFileLoader.getChildren(border, "point", ns, true, GameboardConfig.GAMEBOARD_CONFIG);
        Element point;
        int nNumBorderPoints = points.size();
        BorderPoint newPoint;
        for (int j = 0; j < nNumBorderPoints; j++)
        {
            sAttrErrorDesc = "BorderPoint #" + (j+1)+ " of border (" +
                                sT1 + "," + sT2 + ") in " + GameboardConfig.GAMEBOARD_CONFIG;
            point = points.get(j);
            
            newPoint = new BorderPoint(point, ns, sAttrErrorDesc);
            
            // add point to list of all points (gets existing point if duplicate)
            try {
                newPoint = (BorderPoint) allPoints_.addMapPoint(newPoint);
            } catch (ClassCastException cce)
            {
                logger.fatal("BorderPoint " + newPoint + " already exists as another type " + cce);
                throw new ApplicationError(cce);
            }
            addBorderPoint(newPoint);
        }   
    }
    
    /**
     * Init from strings - looks up territories
     */
    private void init(String s1, String s2, boolean bEnclosed, boolean bWrap, Integer nNum)
    {
        ApplicationError.assertNotNull(t_, "Territories has not been initialized");
        
        t1_ = t_.getTerritory(s1);
        ApplicationError.assertNotNull(t1_, "Territory not found", s1);
        
        t2_ = t_.getTerritory(s2);
        ApplicationError.assertNotNull(t2_, "Territory not found", s2);
        
        bEnclosed_ = bEnclosed;
        bWrap_ = bWrap;
        nNum_ = nNum;
        orderTerritories();
    }
    
    /**
     * order t1,t2 alphabetically so Borders treeset works
     */
    private void orderTerritories()
    {
        Territory t1 = t1_;
        Territory t2 = t2_;
        
        if (Territory.compare(t1, t2) > 0)
        {
            t1_ = t2;
            t2_ = t1;
        }
    }
    
    /**
     * Return 1st territory in border
     */
    public Territory getTerritory1()
    {
        return t1_;
    }
    
    /**
     * Set 1st territory in border
     */
    public void setTerritory1(Territory t1)
    {
        t1_ = t1;
    }
    
    /**
     * Return 2nd territory in border
     */
    public Territory getTerritory2()
    {
        return t2_;
    }
    
    /**
     * Set 2nd territory in border
     */
    public void setTerritory2(Territory t2)
    {
        t2_ = t2;
    }
    
    /**
     * Return whether border is enclosed (end point connects to start point)
     */
    public boolean isEnclosed()
    {
        return bEnclosed_;
    }
    
    /**
     * Set whether border is enclosed
     */
    public void setEnclosed(boolean b)
    {
        bEnclosed_ = b;
    }
    
    /**
     * Return whether border is a wrap around border (left side of board touches right)
     */
    public boolean isWrapAround()
    {
        return bWrap_;
    }
    
    /**
     * Set whether border is wrap
     */
    public void setWrapAround(boolean b)
    {
        bWrap_ = b;
    }
    
    /**
     * Return number of this border
     */
    public int getNumber()
    {
        return nNum_;
    }
    
    /**
     * Sets the number of this border
     */
    public void setNumber(int nNum)
    {
        nNum_ = nNum;
    }
    
    /**
     * Return list of points in border
     */
    public BorderPoints getBorderPoints()
    {
        return myPoints_;
    }
    
    /**
     * Number of points in this border
     */
    public int size()
    {
        return myPoints_.size();
    }
    
    /**
     * Returns point at given index
     */
    public BorderPoint getBorderPoint(int i)
    {
        if (myPoints_ == null) return null;
        if (i < 0 || i >= myPoints_.size()) return null;
        return myPoints_.getBorderPoint(i);
    }
    
    /**
     * add a new border point at end of list
     */
    public void addBorderPoint(BorderPoint p)
    {
        addBorderPoint(p, -1);
    }
    
    /**
     * add a new border point at given index
     */
    public void addBorderPoint(BorderPoint p, int nIndex)
    {
        if (myPoints_ == null) return;
        p.addBorder(this); // tell p this border is using it
        myPoints_.addBorderPoint(p, nIndex); // store this point locally
    }
    
    /**
     * remove given point from list
     */
    public void removeBorderPoint(BorderPoint p)
    {
        if (myPoints_ == null) return;
        myPoints_.removeBorderPoint(p); // remove from local list
        p.removeBorder(this); // tell p this border no longer using it
    }
    
    /**
     * Get index of this borderpoint.  Returns -1 if not found
     */
    public int getPointIndex(BorderPoint p)
    {
        if (myPoints_ == null) return -1;
        int nSize = myPoints_.size();
        BorderPoint point;
        for (int i = 0; i < nSize; i++)
        {
            point = getBorderPoint(i);
            if (point.equals(p)) return i;
        }
        
        return -1;
    }
      
    /**
     * return true if this border contains t1 and t2 and the given num
     */
    public boolean contains(Territory t1, Territory t2, int nNum)
    {    
        if (nNum != getNumber()) return false;
        if (t1_ == t1 && t2_ == t2) return true;
        if (t1_ == t2 && t2_ == t1) return true;
        // bEnclosed doesn't factor here 
        
        return false;
    }
    
    /**
     * return true if the passed in object equals this object.
     * Border equality is determined if both contain the
     * same two territories (order is not important)
     */
    public boolean equals(Object obj)
    {
	    if (this == obj) return true;
        
        if (obj instanceof Border)
        {
            Border b = (Border) obj;
            // borders are equivalent if both contain the same territories in same order
            // territories are ordered when the border is created
            // and num is same
            if (b.getNumber() == getNumber() && b.t1_ == t1_ && b.t2_ == t2_) return true;
        }
        
        return false;
    }

    public int hashCode()
    {
        return 31*31*getNumber() + 31 * t1_.hashCode() + t2_.hashCode();
    }
    
    /**
     * Comparator that can be used for borders
     */
    public static final Comparator<Border> COMPARATOR = new BorderComparator();
    
    /**
     * Used to compare two borders
     */
    public static int compare(Border b1, Border b2)
    {
        return COMPARATOR.compare(b1, b2);
    }
    
    /**
     * actual comparator
     */
    private static class BorderComparator implements Comparator<Border>
    {
        public int compare(Border b1, Border b2)
        {
            if (b1.equals(b2)) return 0;
            
            // if first territories are not equal, return comparison between
            // them
            if (b1.t1_ != b2.t1_)
            {
                return Territory.compare(b1.t1_, b2.t1_);
            }
            // else return comparison between second territories
            else if (b1.t2_ != b2.t2_)
            {
                return Territory.compare(b1.t2_, b2.t2_);
            }
            // else same, but num is different
            else
            {
                return b1.getNumber() - b2.getNumber();
            }
        }
    }
    
    /**
     * Write XML which represents this border to the given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        writer.printIndent(nIndent);
        writer.printElementStartOpen("border");
        writer.printAttribute("t1", getTerritory1().getName());
        writer.printAttribute("t2", getTerritory2().getName());
        // save space in file since this is optional and defaults to false
        if (isEnclosed()) writer.printAttribute("enclosed", isEnclosed() ? Boolean.TRUE : Boolean.FALSE);
        if (isWrapAround()) writer.printAttribute("wrap", isWrapAround() ? Boolean.TRUE : Boolean.FALSE);
        // default territory num is 1, so don't write unless greater than 1
        if (getNumber() > 1) writer.printAttribute("num", nNum_);
        writer.printElementCloseLine();
        
        // points
        if (myPoints_ != null)
        {
            int nSize = myPoints_.size();
            BorderPoint point;
            for (int i = 0; i < nSize; i++)
            {
                point = getBorderPoint(i);
                point.printXML(writer, nIndent+1);
            }
        }
        
        writer.printElementEndLine("border", nIndent);
        writer.printNewLine();
    }
    
    /**
     * String representation of border for debugging
     */
    public String toString()
    {
        //noinspection ConstantIfStatement
        if (true) return shortDesc(); // DEBUG
        
        String sNum = "";
        if (getNumber() > 1) sNum = " (#" + getNumber() + ")";
        StringBuilder sbBuffer = new StringBuilder();
        sbBuffer.append("[")
                .append(getTerritory1().toString()).append(" - ").append(getTerritory2().toString()).append(sNum)
                .append(" : enclosed=").append(isEnclosed())
                .append(", wrapped=").append(isWrapAround())
                .append("] points:");
                int nSize = size();
        BorderPoint point;
        for (int i = 0; i < nSize; i++)
        {
            point = getBorderPoint(i);
            sbBuffer.append(" ").append(point.toString());
        }
        return sbBuffer.toString();
    }
    
    /**
     * Display string
     */
    public String shortDesc()
    {
        String sNum = "";
        if (getNumber() > 1) sNum = " (#" + getNumber() + ")";
        return "[" + getTerritory1().getName() + " - " + 
                     getTerritory2().getName() + sNum + 
                     (isWrapAround() ? " (wrap)" : "") +
                     (isEnclosed() ? " (enclosed)" : "") +
                     "]";
    }
    
    ///
    /// GAME MODE FUNCTIONALITY
    ///
    
    boolean bPathStartsAtBeginning_ = true;
    
    /**
     * return whether path starts at beginning
     */
    public void setPathStartsAtBeginning(boolean b)
    {
        bPathStartsAtBeginning_ = b;
    }
    
    /**
     * set whether path starts at beginning
     */
    public boolean getPathStartsAtBeginning()
    {
        return bPathStartsAtBeginning_;
    }
    
    /**
     * based on where path starts, return start point
     */
    public BorderPoint getStartPoint()
    {
        if (bPathStartsAtBeginning_)
        {
            return getBorderPoint(0);
        }
        else
        {
            return getBorderPoint(size() - 1);
        }
    }
    
    /**
     * based on where path starts, get end point
     */
    public BorderPoint getEndPoint()
    {
        if (bPathStartsAtBeginning_)
        {
            return getBorderPoint(size() - 1);
        }
        else
        {
            return getBorderPoint(0);
        }
    }
    
    /**
     * clear all points (called after paths are constructed to free memory)
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    void clearPoints()
    {
        allPoints_ = null;
        myPoints_.clear();
        myPoints_ = null;
    }
}
