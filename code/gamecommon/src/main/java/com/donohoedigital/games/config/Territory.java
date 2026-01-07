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
 * Territory.java
 *
 * Created on October 28, 2002, 4:49 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import org.jdom2.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author  Doug Donohoe
 */
public class Territory implements GamePieceContainer, ObjectID, Comparable
{ 
    static Logger logger = LogManager.getLogger(Territory.class);
    
    // helper for game piece container logic
    GamePieceContainerImpl impl_;
    private GamePlayer previousOwner_ = null;
    
    /**
     * Default territory point names
     */
    public static final String LABEL_LOCATION = PropertyConfig.getRequiredStringProperty("define.territoryPointType.label");
        
    /**
     * name of type representing the EDGE - we don't draw this territory
     */
    public static final String EDGE = PropertyConfig.getRequiredStringProperty("define.territoryType.edge");
    
    /**
     * name of type representing a decorative territory - we don't allow
     * these to be selected or highlighted
     */
    public static final String DECORATION = PropertyConfig.getRequiredStringProperty("define.territoryType.decoration");
    
    /**
     * name of type representing water
     */
    public static final String WATER = PropertyConfig.getRequiredStringProperty("define.territoryType.water");
    
    /**
     * name of type representing land
     */
    public static final String LAND = PropertyConfig.getRequiredStringProperty("define.territoryType.land");
    
    /**
     * List of all areas
     */
    private static Areas a_;
    
    // stuff
    Integer id_;
    String sName_;
    String sMapName_;
    String sMapNameNoReturn_;
    String sType_;
    String sTypeOrig_;
    Integer nCat_;
    Area area_;
    Integer nScaleImages_;
    public static final Integer FULL_SCALE = 100;
    TerritoryPoints myPoints_ = new TerritoryPoints();
    boolean bEdge_ = false;
    boolean bDecoration_ = false;
    boolean bWater_ = false;
    boolean bLand_ = false;
    static MapPoints allPoints_;   
    BorderArrayList myBorders_ = new BorderArrayList(2);
    static Territories myTerritories_;
    
    /**
     * Pointer to list of all territories
     */
    static void setTerritories(Territories t)
    {
        myTerritories_ = t;
    }
    
    /**
     * Initialize Territory with list of Areas
     */
    static void setAreas(Areas a)
    {
        a_ = a;
    }
    
    /**
     * Initialize Border with BorderPoints array
     */
    static void setMapPoints(MapPoints points)
    {
        allPoints_ = points;
    }
    
    /** 
     * Creates a new Territory from String data
     */
    public Territory(Integer id, String sName, String sArea, String sType) 
                throws ApplicationError
    {
        init(id, sName, sArea, sType, null, null);
    }
    
    /**
     * Create a new Territory from XML element
     */
    public Territory(Element territory, Namespace ns, String sAttrErrorDesc)
                throws ApplicationError
    {
        Integer id = XMLConfigFileLoader.getIntegerAttributeValue(territory, "id", true, sAttrErrorDesc);
        String sName = XMLConfigFileLoader.getStringAttributeValue(territory, "name", true, sAttrErrorDesc);
        String sType = XMLConfigFileLoader.getStringAttributeValue(territory, "type", true, sAttrErrorDesc);
        String sArea = XMLConfigFileLoader.getStringAttributeValue(territory, "area", true, sAttrErrorDesc);
        Integer cat = XMLConfigFileLoader.getIntegerAttributeValue(territory, "cat", false, sAttrErrorDesc);
        Integer nScaleImages = XMLConfigFileLoader.getIntegerAttributeValue(territory, "scaleimages", false, 
                                                        sAttrErrorDesc, FULL_SCALE);

        //logger.debug("Adding territory " + sName + " in " + sArea + " ("+ sType+")");
        init(id, sName, sArea, sType, cat, nScaleImages);
        
        // points in territory
        List points = XMLConfigFileLoader.getChildren(territory, "point", ns, false, GameboardConfig.GAMEBOARD_CONFIG);
        Element point;
        int nNumTerriotryPoints = points.size();
        TerritoryPoint newPoint;
        for (int j = 0; j < nNumTerriotryPoints; j++)
        {
            sAttrErrorDesc = "TerritoryPoint #" + (j+1)+ " of territory " +
                                sName + " in " + GameboardConfig.GAMEBOARD_CONFIG;
            point = (Element)points.get(j);
            
            newPoint = new TerritoryPoint(point, ns, sAttrErrorDesc);
            
            // add point to list of all points (gets existing point if duplicate)
            try {
                newPoint = (TerritoryPoint) allPoints_.addMapPoint(newPoint);
            } catch (ClassCastException cce)
            {
                logger.fatal("TerritoryPoint " + newPoint + " already exists as another type " + cce);
                throw new ApplicationError(cce);
            }
            addTerritoryPoint(newPoint);
        }   
    }
    
    /**
     * Init territory from string data
     */
    private void init(Integer id, String sName, String sArea, String sType, Integer nCat, Integer nScaleImages)
                    throws ApplicationError
    {
        ApplicationError.assertNotNull(a_, "Areas has not been initialized");
        
        area_ = a_.getArea(sArea);
        ApplicationError.assertNotNull(area_, "Area not found", sArea);
        
        id_ = id;
        sName_ = sName;
        String sLookupName = sName.replace(' ', '_');
        sMapName_ = PropertyConfig.getStringProperty("territory." + sLookupName, sName, false);
        sMapNameNoReturn_ = sMapName_.replace('\n',' ');
        setType(sType);
        sTypeOrig_ = sType;
        nCat_ = nCat;
        nScaleImages_ = nScaleImages;
        
        impl_ = new GamePieceContainerImpl(this);
    }

    /**
     * Get id, returns -1 if not set
     */
    public int getID()
    {
        if (id_ == null) return -1;
        return id_;
    }
    
    /**
     * Set id
     */
    public void setID(int id)
    {
        id_ = id;
    }
    
    /**
     * Return territory name
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Return territory name for display on map (may contain returns)
     */
    public String getMapDisplayName()
    {
        return sMapName_;
    }
    
    /**
     * Return territory name for display with no returns in name
     */
    public String getDisplayName()
    {
        return sMapNameNoReturn_;
    }
    
    /**
     * Set territory name
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }
    
    /**
     * Get territory type
     */
    public String getType()
    {
        return sType_;
    }
    
    /**
     * Set territory type
     */
    public void setType(String sType)
    {
        // TODO: validate valid type
        sType_ = sType;
        
        bEdge_ = sType_.equals(EDGE);
        bDecoration_ = sType_.equals(DECORATION);
        bWater_ = sType_.equals(WATER);
        bLand_ = sType_.equals(LAND);
    }
    
    /**
     * set back to original type
     */
    public void resetType()
    {
        setType(sTypeOrig_);
    }
    
    /**
     * Get cat, returns -1 if not set
     */
    public int getCategory()
    {
        if (nCat_ == null) return -1;
        return nCat_;
    }
    
    /**
     * Return the area this territory is in
     */
    public Area getArea()
    {
        return area_;
    }
    
    /**
     * Return % to scale images
     */
    public int getScaleImages()
    {
        return nScaleImages_;
    }
    
    /**
     * Return % to scale images as a double value.  For example,
     * 25% is returned as .25
     */
    public double getScaleImagesAsDouble()
    {
        return ((double) nScaleImages_.intValue()) / 100.0d;
    }
    
    /**
     * Set % to scale images
     */
    public void setScaleImages(int nScale)
    {
        nScaleImages_ = nScale;
    }
    
    /**
     * Return whether this is the territory representing the EDGE of the
     * game board
     */
    public boolean isEdge()
    {
        return bEdge_;
    }
    
    /**
     * Return whether this is a decorative territory (just there to look
     * nice, but not used in the game)
     */
    public boolean isDecoration()
    {
        return bDecoration_;
    }
 
    /**
     * Set this as a decoration (used to alter map at runtime)
     */
    public void setDecoration(boolean b)
    {
        bDecoration_ = b;
    }
    
    /**
     * Return whether this is a water territory
     */
    public boolean isWater()
    {
        return bWater_;
    }

    /**
     * Set this as a water (used to alter map at runtime)
     */
    public void setWater(boolean b)
    {
        bWater_ = b;
    }
    
    /**
     * Return whether this is a land territory
     */
    public boolean isLand()
    {
        return bLand_;
    }
    
    /**
     * Set this as a lasnd (used to alter map at runtime)
     */
    public void setLand(boolean b)
    {
        bLand_ = b;
    }
    
    /**
     * Return true if this is an island (is land and the
     * number of adjacent land regions is 0)
     */
    public boolean isIsland()
    {
        return isLand() && nNumAdjLand_ == 0;
    }
    
    /**
     * Add a border to this territories list of borders
     */
    void addBorder(Border b)
    {
        myBorders_.addBorder(b);
    }
    
    /**
     * Remove a border from this territories list of borders
     */
    void removeBorder(Border b)
    {
        myBorders_.removeBorder(b);
    }
    
    /**
     * Get list of borders this territory has
     */
    public BorderArrayList getBorders()
    {
        return myBorders_;
    }
    
    /**
     * Set the area this territory is in
     */
    public void setArea(Area area)
    {
        area_ = area;
    }
    
    /**
     * Return list of points in territory
     */
    public TerritoryPoints getTerritoryPoints()
    {
        return myPoints_;
    }
    
    /**
     * Number of points in this territory
     */
    public int size()
    {
        return myPoints_.size();
    }
    
    /**
     * Returns point at given index
     */
    public TerritoryPoint getTerritoryPoint(int i)
    {
        if (myPoints_ == null) return null;
        if (i < 0 || i >= myPoints_.size()) return null;
        return myPoints_.getTerritoryPoint(i);
    }
    
    /**
     * Return point with given type // TODO: faster way to do getTerritoryPoint(string)
     */
    public TerritoryPoint getTerritoryPoint(String sType)
    {
        int nSize = myPoints_.size();
        TerritoryPoint point;
        for (int i = 0; i < nSize; i++)
        {
            point = myPoints_.getTerritoryPoint(i);
            if (point.sType_.equals(sType))
            {
                return point;
            }
        }
        return null;
    }
    
    /**
     * add a new territory point at end of list
     */
    public void addTerritoryPoint(TerritoryPoint p)
    {
        addTerritoryPoint(p, -1);
    }
    
    /**
     * add a new territory point at given index
     */
    public void addTerritoryPoint(TerritoryPoint p, int nIndex)
    {
        if (myPoints_ == null) return;
        myPoints_.addTerritoryPoint(p, nIndex); // store this point locally
        p.setTerritory(this);
    }
    
    /**
     * remove given point from list
     */
    public void removeTerritoryPoint(TerritoryPoint p)
    {
        if (myPoints_ == null) return;
        myPoints_.removeTerritoryPoint(p); // remove from local list
        p.setTerritory(null);
    }
    
    /**
     * Get index of this territorypoint.  Returns -1 if not found
     */
    public int getPointIndex(TerritoryPoint p)
    {
        if (myPoints_ == null) return -1;
        int nSize = myPoints_.size();
        TerritoryPoint point;
        for (int i = 0; i < nSize; i++)
        {
            point = getTerritoryPoint(i);
            if (point.equals(p)) return i;
        }
        
        return -1;
    }
    
    /**
     * Return whether the given obj matches this territory.  Names
     * must match.  Type and Area don't factor in equality (all
     * territory names must be unique)
     */
    public boolean equals(Object obj) {
	if (this == obj) return true;
        
        if (obj instanceof Territory)
        {
            Territory t = (Territory) obj;
            if (t.sName_.equals(sName_)) return true;
            // type & area don't factor in equality
        }
        
        return false;
    }
    
    /**
     * Comparator to compare territories
     */
    public static final TerritoryComparator COMPARATOR = new TerritoryComparator();
    
    /**
     * Used to compare two territories
     */
    public static int compare(Territory t1, Territory t2)
    {
        return COMPARATOR.compare(t1, t2);
    }
    
    /**
     * Comparable interface
     */
    public int compareTo(Object o) {
        return COMPARATOR.compare(this, (Territory) o);
    }
    
    /**
     * Comparator for territories
     */
    private static class TerritoryComparator implements Comparator<Territory>
    {
        public int compare(Territory t1, Territory t2)
        {
            if (t1.equals(t2)) return 0;
            
            return t1.sName_.compareTo(t2.sName_);
        }
        
        public boolean equals(Object obj)
        {
            return obj instanceof TerritoryComparator;
        }
    }
    
    /**
     * Print XML representation of this territory to the given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        writer.printIndent(nIndent);
        writer.printElementStartOpen("territory");
        writer.printAttribute("id", id_);
        writer.printAttribute("name", sName_);
        writer.printAttribute("area", area_.getName());
        writer.printAttribute("type", sType_);
        if (nCat_ != null) writer.printAttribute("cat", nCat_);
        if (nScaleImages_ != null && !nScaleImages_.equals(FULL_SCALE)) writer.printAttribute("scaleimages", nScaleImages_);

        
        // points
        if (myPoints_ != null && myPoints_.size() > 0)
        {
            writer.printElementCloseLine();
            int nSize = myPoints_.size();
            TerritoryPoint point;
            for (int i = 0; i < nSize; i++)
            {
                point = getTerritoryPoint(i);
                point.printXML(writer, nIndent+1);
            }
            writer.printElementEndLine("territory", nIndent);
            writer.printNewLine();
        }
        else
        {
            writer.printElementEndLine();
        }
    }
    
    /**
     * Print Stirng representation of territory for debugging 
     */
    public String toString()
    {
        String s = area_ + ":" + sName_ + "(" + sType_ + ")";
        if (nCat_ != null) s += " " + "[" + nCat_ + "]";
        if (nScaleImages_ != null) s += " " + nScaleImages_ + "%";
        
        return s;
    }
    
    ////
    //// GAME MODE FUNCTIONALITY
    ////
    
    private Territory[] adjacentTerritories_;
    private int nNumAdjLand_ = 0;
    private int nNumAdjWater_ = 0;
    private int nNumAdjLandArea_ = 0;
    
    /**
     * Count number of land/water territories adjacent to this one
     */
    private void countAdjacent()
    {
       nNumAdjLand_ = 0;
       nNumAdjWater_ = 0;
       nNumAdjLandArea_ = 0;
        for (Territory t : adjacentTerritories_)
        {
            if (t.isDecoration()) continue;//just to be safe
            if (t.isWater()) nNumAdjWater_++;
            if (t.isLand()) nNumAdjLand_++;
            if (t.isLand() &&
                t.getArea() == getArea()) nNumAdjLandArea_++;
        }
    }
       
    /**
     * Return number of adjacent water territories
     */
    public int getNumAdjacentWater()
    {
        return nNumAdjWater_;
    }
    
    /**
     * Return number of adjacent land territories
     */
    public int getNumAdjacentLand()
    {
        return nNumAdjLand_;
    }
    
    /**
     * Return number of adjacent land territories that are part of the same area
     */
    public int getNumAdjacentLandSameArea()
    {
        return nNumAdjLandArea_;
    }
    
    /**
     * creates array of adjacent territories if not already
     * created.  Then calculates number of water/land adjacent
     * as this can change by manipulation of territory types.
     * If bClearBorders is true, the border data in this territory
     * is removed as it is no longer needed after the adjacent list
     * is built
     */
    public void determineAdjacentTerritories(boolean bClearBorders)
    {
        // skip if already created
        if (adjacentTerritories_ == null)
        {
            _determineAdjacentTerritories(bClearBorders);
        }
        
        countAdjacent();
    }
    
    /**
     * Create adjacent territories list
     */
    private void _determineAdjacentTerritories(boolean bClearBorders)
    {
        int nNumBorders = myBorders_.size();
        
        TerritoryArrayList adjacentTerritories = new TerritoryArrayList();
        Border b;
        Territory adj;
        
        for (int i = 0; i < nNumBorders; i++)
        {
            b = myBorders_.getBorder(i);
            
            // get "other" territory from border
            adj = b.getTerritory1();
            if (adj == this)
            {
                adj = b.getTerritory2();
            }
            
            // skip edge territories
            if (adj.isEdge()) continue;
            
            if (!adjacentTerritories.contains(adj))
            {
                adjacentTerritories.add(adj);
            }
        }
        
        // convert to territory array
        adjacentTerritories_ = new Territory[adjacentTerritories.size()];
        for (int i = 0; i < adjacentTerritories.size(); i++)
        {
            adjacentTerritories_[i] = adjacentTerritories.get(i);
        }
        
        if (bClearBorders)
        {
            myBorders_.clear();
            myBorders_ = null;
        }
    }
    
    /**
     * Return list of adjacent territories to this one
     */
    public Territory[] getAdjacentTerritories()
    {
        return adjacentTerritories_;
    }
       
    /////
    ///// Path/drawing stuff related to territories
    /////
    GeneralPath path_;      // path as defined in config file
    Rectangle pathBounds_;  // bounds of this path
     
    GeneralPath scaledPath_;  // path set to current scale of gameboard
    Rectangle scaledPathBounds_;           // reset when scaled path changes
    java.awt.geom.Area scaledArea_;        // ditto
    BasicStroke lastStroke_ = null;        // ditto
    java.awt.geom.Area borderArea_ = null; // ditto
    
    // used within createPath/getNextBorder (means we are not thread safe here)
    private boolean bNewEnclosedArea_;
    
    /**
     * Used in game mode - creates path from borders so
     * this territory can be drawn
     */    
    public void createPath()
    {
        // skip if already created
        if (path_ != null) return; 
        
        int nNumBorders = myBorders_.size();
        
        // figure number of points we are dealing with
        int nNumPoints = 0;
        for (int i = 0; i < nNumBorders; i++)
        {
            nNumPoints += myBorders_.getBorder(i).getBorderPoints().size();
        }
        
        // create path with default size to accomodate # of points
        path_ = new GeneralPath(GeneralPath.WIND_EVEN_ODD, nNumPoints + nNumBorders);
        
        // clone and remove wrap borders (redundant for path)
        BorderArrayList borders = (BorderArrayList) myBorders_.clone();
        for (int i = borders.size()-1; i >= 0; i--)
        {
            if (borders.getBorder(i).isWrapAround()) borders.remove(i);
        }
        Border border = null;
        BorderPoints points;
        BorderPoint point;
        int i;
        int nIndex;

        while ((border = getNextBorder(borders, border)) != null)
        {
            points = border.getBorderPoints();
            nNumPoints = points.size();
            
            for (i = 0; i < nNumPoints; i++)
            {
                // depending on order of points, we determine index
                if (border.getPathStartsAtBeginning()) {
                    nIndex = i;
                } else {
                    nIndex = (nNumPoints - 1) - i;
                }
                point = points.getBorderPoint(nIndex);
                
                // if new enclosed area and a new point is being drawn, move 
                // path to that point
                if (bNewEnclosedArea_ && i == 0)
                {
                    if (path_.getCurrentPoint() != null) { path_.closePath(); }
                    path_.moveTo(point.x_, point.y_);
                    continue;
                }
                path_.lineTo(point.x_, point.y_);
            }
            
            if (border.isEnclosed())
            {
                path_.closePath();
            }
        }
        if (path_.getCurrentPoint() != null) { path_.closePath(); }
        
        pathBounds_ = path_.getBounds();
        setScaledPath(path_);
    }
    
    /**
     * Get next border to process for path
     */
    private Border getNextBorder(BorderArrayList borders, Border currentBorder)
    {
        Border nextBorder = null;
        int nSize = borders.size();
        
        // if no more borders left to process, return null
        if (nSize == 0) return null;
        
        // if current border is null, return first item in array
        if (currentBorder == null || currentBorder.isEnclosed())
        {
            nextBorder = borders.getBorder(0);
            
            nextBorder.setPathStartsAtBeginning(true);
            bNewEnclosedArea_ = true;
        }
        // find border where the end point of the last border matches
        // a start/end point of another border
        else
        {
            BorderPoint currentEndPoint = currentBorder.getEndPoint();
            BorderPoint startPoint, endPoint;
            Border border;
            for (int i = 0; i < nSize; i++)
            {
                border = borders.getBorder(i);
                border.setPathStartsAtBeginning(true); // reset (border may have been used previously)
                startPoint = border.getStartPoint();
                endPoint = border.getEndPoint();
                if (startPoint.equals(currentEndPoint))
                {
                    nextBorder = border;
                    nextBorder.setPathStartsAtBeginning(true);
                    bNewEnclosedArea_ = false;
                    break;
                }
                else if (endPoint.equals(currentEndPoint))
                {
                    nextBorder = border;
                    nextBorder.setPathStartsAtBeginning(false);
                    bNewEnclosedArea_ = false;
                    break;
                }
            }
            
            // if no next border found but we still have borders
            // left to process it must mean there are two or more
            // disjoint areas in this territory, so start with
            // next border
            if (nextBorder == null)
            {
                nextBorder = borders.getBorder(0);
                nextBorder.setPathStartsAtBeginning(true);
                bNewEnclosedArea_ = true;
            }
        }

        borders.removeBorder(nextBorder);

        if (nextBorder == currentBorder) { 
            logger.fatal("Exit - return same border will cause infinite loop"); 
            System.exit(-1);
        }
        return nextBorder;
    }
    
    /**
     * Return original path which describes this territory
     */
    public GeneralPath getPath()
    {
        return path_;
    }
    
    /**
     * Return bounds of path (instead of computing
     * it every time)
     */
    public Rectangle getPathBounds()
    {
        return pathBounds_;
    }
    
    /**
     * recalculate path bounds
     */
    public void recalcPathBounds()
    {
        pathBounds_ = path_.getBounds();
    }
    
    /**
     * Return scaled path which represents territory adjusted to
     * current window size
     */
    public GeneralPath getScaledPath()
    {
        return scaledPath_;
    }
    
    /**
     * Set the scaled path (and store bounds,
     * create the resuable path iter)
     */
    public void setScaledPath(GeneralPath path)
    {
        scaledPath_ = path;
        scaledPathBounds_ = null;
        scaledArea_ = null;
        lastStroke_ = null;
        borderArea_ = null;
    }

    /**
     * Return bounds of path (instead of computing
     * it every time)
     */
    public Rectangle getScaledPathBounds()
    {
        if (scaledPathBounds_ == null)
        {
            scaledPathBounds_ = scaledPath_.getBounds();
        }
        return scaledPathBounds_;
    }

    /**
     * Return area of path (instead of computing
     * it every time)
     */
    public java.awt.geom.Area getScaledPathArea()
    {
        if (scaledArea_ == null)
        {
            scaledArea_ = new java.awt.geom.Area(scaledPath_);
        }
        return scaledArea_;
    }
    
    /**
     * Copy contains() from GeneralPath, but with reusable
     * path iter, to avoid so many creations
     */
    public boolean scaledPathContains(double x, double y) 
    {
        // added this to quickly eliminate by bounds box to
        // avoid expensive crossingsForPath below

        // changed to this for Java 1.6
        return getScaledPathBounds().contains(x, y) && scaledPath_.contains(x, y);

        //        // This check is not needed (we can't get at numTypes
//        // directly, so leaving it out)
//        //        if (numTypes < 2) {
//        //            return false;
//        //        }
//        if (scaledPathIter_ == null)
//        {
//            scaledPathIter_ = new DDGeneralPathIterator(scaledPath_);
//        }
//        else
//        {
//            scaledPathIter_.reset();
//        }
//
//        int cross = sun.awt.geom.Curve.crossingsForPath(scaledPathIter_, x, y);
//        if (scaledPath_.getWindingRule() == GeneralPath.WIND_NON_ZERO) {
//            return (cross != 0);
//        } else {
//            return ((cross & 1) != 0);
//        }
    }
    
    /**
     * Return area representing a stroke around the area
     * using the template as the basis for a new stoke
     * whos width is nNewStokeWidth
     */
    
    // NOTE: not currently used because not any faster than stoke method
    
//    public java.awt.geom.Area getScaledPathBorderArea(BasicStroke template, float nNewStrokeWidth)
//    {
//        boolean bNew = false;      
//        // create new stroke if diff from last one
//        if (lastStroke_ == null || 
//                (lastStroke_.getLineWidth() != nNewStrokeWidth ||
//                 lastStroke_.getEndCap() != template.getEndCap() ||
//                 lastStroke_.getLineJoin() != template.getLineJoin()))
//        {
//            lastStroke_ = new BasicStroke(nNewStrokeWidth, template.getEndCap(), template.getLineJoin());
//            bNew = true;
//        }
//                
//        if (bNew || borderArea_ == null)
//        {
//            java.awt.geom.Area area = getScaledPathArea();
//            borderArea_ = new java.awt.geom.Area(area);
//            borderArea_.intersect(new java.awt.geom.Area(lastStroke_.createStrokedShape(area)));
//        }
//        return borderArea_;
//    }
//    
//    /**
//     * Get stroke created by last getScaledPathBorderArea call
//     */
//    public BasicStroke getScaledBorderStroke()
//    {
//        return lastStroke_;
//    }
    
    /**
     * Init territory for a new game
     */
    public void initForGame()
    {
        setDirty(false);
        setSelected(false);
        setUnderMouse(false);
        setUserData(null);
        setUserFlag(false);
        setUserInt(0);
        userTerritory_ = new DMArrayList();
        // get new impl so any pieces in territory are cleared and owner
        impl_ = new GamePieceContainerImpl(this);
    }
    
    ////
    //// GUI helpers for selection/mouse
    ////
    protected boolean bSelected_ = false;
    protected boolean bUnderMouse_ = false;
    
    /**
     * Set this territory as selected
     */
    public void setSelected(boolean b)
    {
        bSelected_ = b;
    }
    
    /**
     * Is this territory selected?
     */
    public boolean isSelected()
    {
        return bSelected_;
    }
    
    /**
     * Set this territory as under mouse
     */
    public void setUnderMouse(boolean b)
    {
        bUnderMouse_ = b;
    }
    
    /**
     * Is this territory under mouse?
     */
    public boolean isUnderMouse()
    {
        return bUnderMouse_;
    }

    ///// Related to below
    
    public GamePlayer getGamePlayerPrevious()
    {
        return previousOwner_;
    }
    
    /////
    ///// GamePieceContainer methods
    /////
    
    public void setGamePlayer(GamePlayer player) {
        GamePlayer old = impl_.getGamePlayer();
        if (player != old)
        {
             previousOwner_ = old;
        }
        impl_.setGamePlayer(player);
    }
    
    public GamePlayer getGamePlayer() {
        return impl_.getGamePlayer();
    }

    public GamePiece addGamePiece(GamePiece gp) {
        return impl_.addGamePiece(gp);
    }
    
    public void removeGamePiece(GamePiece gp) {
        impl_.removeGamePiece(gp);
    }
    
    public int getNumPieces() {
        return impl_.getNumPieces();
    }
    
    public Iterator getGamePieces() {
        return impl_.getGamePieces();
    }

    public GamePiece getGamePiece(int nType, GamePlayer owner) {
        return impl_.getGamePiece(nType, owner);
    }
    
    public boolean hasNonOwnerGamePiece(int nType, GamePlayer owner) {
        return impl_.hasNonOwnerGamePiece(nType, owner);
    }
    
    public boolean equals(GamePieceContainer c) {
        return this.equals((Object) c);
    }
    
    public boolean hasMovedPieces() {
        return impl_.hasMovedPieces();
    }

    public Map getMap() {
        return impl_.getMap();
    }

    ////
    //// Territory helper methods
    ////
    
    /**
     * Return true if this territories owner has a piece of the given type
     */
    public boolean hasOwnerPiece(int nType)
    {   
        GamePiece gp = getGamePiece(nType, getGamePlayer());

        return gp != null;

    }
    
    /**
     * Return true if the given player has a piece of the given type
     * in this territory
     */
    public boolean hasOwnerPiece(int nType, GamePlayer player)
    {   
        GamePiece gp = getGamePiece(nType, player);

        return gp != null;

    }
    
    /**
     * Is this adjacent to given territory?
     */
    public boolean isAdjacent(Territory t)
    {
        for (Territory anAdjacentTerritories_ : adjacentTerritories_)
        {
            if (anAdjacentTerritories_ == t) return true;
        }
        return false;
    }
    
    /**
     * Is this adjacent to a water territory?
     */
    public boolean isWaterAdjacent()
    {
        for (Territory anAdjacentTerritories_ : adjacentTerritories_)
        {
            if (anAdjacentTerritories_.isWater()) return true;
        }
        return false;
    }
    
    /**
     * Is there a land territory owned by given player adjacent
     * to this territory?
     */
    public boolean isLandAdjacent(GamePlayer player)
    {
        for (Territory anAdjacentTerritories_ : adjacentTerritories_)
        {
            if (anAdjacentTerritories_.isLand() &&
                anAdjacentTerritories_.getGamePlayer() == player) return true;
        }
        return false;
    }
    
    /**
     * Is there a territory owned by the given playerOwner adjacent
     * to this territory which contains a piece of the given type
     * (also owner by the playerOwner)
     */
    public boolean isAdjacent(GamePlayer playerOwner, int nType)
    {
        for (Territory anAdjacentTerritories_ : adjacentTerritories_)
        {
            if (anAdjacentTerritories_.getGamePlayer() == playerOwner &&
                anAdjacentTerritories_.hasOwnerPiece(nType, playerOwner)) return true;
        }
        return false;
    }
    
    /**
     * Is there a territory owned by territoryOwner adjacent
     * to this territory which contains a piece of the given type
     * owned by pieceOwner?
     */
    public boolean isAdjacent(GamePlayer territoryOwner, GamePlayer pieceOwner, int nType)
    {
        for (Territory anAdjacentTerritories_ : adjacentTerritories_)
        {
            if (anAdjacentTerritories_.getGamePlayer() == territoryOwner &&
                anAdjacentTerritories_.hasOwnerPiece(nType, pieceOwner)) return true;
        }
        return false;
    }
    
    ////
    //// data/list for storing user data list of territories related to this one
    ////
    
    private DMArrayList userTerritory_ = new DMArrayList();
    
    /**
     * Get generic array for storing list of territory names
     * associated with this territory - used for application-specific
     * functionality
     */
    public DMArrayList getUserTerritoryArray()
    {
        return userTerritory_;
    }
    
    
    private int nUserInt_ = 0;
    
    /**
     * Get user-data integer
     */
    public int getUserInt()
    {
        return nUserInt_;
    }
    
    /**
     * Set user-data integer
     */
    public void setUserInt(int n)
    {
        nUserInt_ = n;
    }
    
    ////
    //// Game save logic
    ////
    
    /**
     * Return this piece encoded as a game state entry
     */
    public GameStateEntry addGameStateEntry(GameState state)
    {
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_TERRITORY);
        state.addEntry(entry);
        
        // territory name
        entry.addToken(sName_); // written out for readability reasons of save file
        
        // owner
        entry.addToken(state.getId(getGamePlayer()));
        
        // territory list
        entry.addToken(userTerritory_);
        
        // number of pieces in territory
        entry.addToken(getNumPieces());
        
        // previous owner
        entry.addToken(state.getId(getGamePlayerPrevious()));
       
        // user int
        entry.addToken(nUserInt_);
        
        // WAR PATCH 2 - save dirty flag
        entry.addToken(bDirty_);
        
        // add each piece
        GamePiece piece;
        synchronized (getMap())
        {
            Iterator iter = getGamePieces();
            while (iter.hasNext())
            {
                piece = (GamePiece) iter.next();
                piece.addGameStateEntry(state, true);
            }
        }
        
        return entry;
    }
    
    /**
     * Load from game state entry
     */
    public void loadFromGameStateEntry(GameState state, GameStateEntry entry)
    {
        SaveDetails details = state.getSaveDetails();
        boolean bDirty = details.getSaveTerritories() == SaveDetails.SAVE_DIRTY;
        int nUpdateType = details.getTerritoriesDirtyType();
        int nUpdateID = details.getTerritoriesUnitOwnerID();
        
        // clean territory according to dirty update type
        if (bDirty)
        {
            switch(nUpdateType)
            {
                // if updating all units, remove all contents
                case SaveDetails.TERRITORY_ALL_UNITS:
                    impl_ = new GamePieceContainerImpl(this);
                    break;
                    
                // if updating only sender units, remove pieces from that player
                case SaveDetails.TERRITORY_OWNER_UNITS:
                    GamePiece piece;
                    synchronized (getMap())
                    {
                        Iterator iter = getGamePieces();
                        while (iter.hasNext())
                        {
                            piece = (GamePiece) iter.next();
                            if (piece.getGamePlayer().getID() == nUpdateID)
                            {
                                iter.remove();
                            }
                        }
                    }
                    break;
                    
                default:
                    throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR,
                                "Dirty load, unsupported update type: " + nUpdateType, null);
                    
            }
        }
        
        // set player id, get num pieces
        entry.removeStringToken(); // get name (do nothing - only written out for debugging)
        setGamePlayer((GamePlayer) state.getObject(entry.removeIntegerToken()));
        
        // BUG 150 - store list of territories for War! requirement of retreat
        //           only from whence you came.  
        // NOTE: the userTerritoryList is replaced during all
        //       update types (the contents are not assumed to
        //       be specific to a player, only to a time in the game
        userTerritory_ = (DMArrayList) entry.removeToken();
        
        // get number of pieces
        int nNum = entry.removeIntToken();
        
        // BUG 88 - get previous owner for AI use.
        previousOwner_ = (GamePlayer) state.getObject(entry.removeIntegerToken());
        
        // BUG 200 - user integer.
        nUserInt_ = entry.removeIntToken();
        
        // WAR PATCH 2 - dirty flag (added 2/16/03)
        if (entry.hasMoreTokens())
        {
            // set dirty value if non-dirty load
            boolean bDirtyValue = entry.removeBooleanToken();
            if (!bDirty) bDirty_ = bDirtyValue;
        }
           
        // load each piece in region (sep game state entries)
        GamePiece piece;
        for (int i = 0; i < nNum; i++)
        {
            entry = state.removeEntry();
            piece = (GamePiece) entry.getObject();
            piece.loadFromGameStateEntry(state, entry);
            
            // if dirty update and updating only specific player units,
            // then continue if this unit doesn't belong to that player
            if (bDirty && nUpdateType == SaveDetails.TERRITORY_OWNER_UNITS)
            {
                if (piece.getGamePlayer().getID() != nUpdateID) continue;
            }
            addGamePiece(piece);
        }
    }
    
    /**
     * Return id of this object
     */
    public int getObjectID()
    {
        return getID();
    }   
    
    ////
    //// More flags for drawing / other (DATA BELOW HERE NOT MARSHALLED/SAVED)
    ////
    
    private boolean bUserFlag_ = false;
    
    /**
     * Set this territory as dirty for online purposes
     */
    public void setUserFlag(boolean b)
    {
        bUserFlag_ = b;
    }
    
    /**
     * Is this dirty?
     */
    public boolean isUserFlag()
    {
        return bUserFlag_;
    }
    
    ////
    //// Online game methods
    ////
    
    private boolean bDirty_ = false;
    
    /**
     * Set this territory as dirty for online purposes
     */
    public void setDirty(boolean b)
    {
        bDirty_ = b;
    }
    
    /**
     * Is this dirty?
     */
    public boolean isDirty()
    {
        return bDirty_;
    }
    
    /**
     * Set all dirty/clean
     */
    public static void setAllDirty(boolean b)
    {
        Territory[] territories_ = getTerritoryArrayCached();
        for (Territory aTerritories_ : territories_)
        {
            aTerritories_.setDirty(b);
        }
    }
    
    /**
     * Get the cached array of all territories
     */
    public static Territory[] getTerritoryArrayCached()
    {
        if (myTerritories_ == null) return null;
        return myTerritories_.getTerritoryArrayCached();
    }
    
    /**
     * Get the cached array of all areas
     */
    public static Area[] getAreaArrayCached()
    {
        return a_.getAreaArrayCached();
    }
    
    ////
    //// AI spurred methods
    ////
    
    private Object oUser_;
    
    /**
     * Set user data (allow applications to store arbitrary info
     * on territory).  This information is not saved/marshalled
     */
    public void setUserData(Object oUser)
    {
        oUser_ = oUser;
    }
    
    /**
     * Get user data
     */
    public Object getUserData()
    {
        return oUser_;
    }

    
//  Written, but not tested (1/10/03).  Not sure if this will be needed.
//    private int nTempDistance = 0;
//    
//    /**
//     * Return distance between this territory and given territory.
//     * Not thread safe.
//     */
//    public int getDistance(Territory tFind, boolean bSameTerritoryType)
//    {
//        int iDistance = 1;
//        Territory search = this;
//        Territory tCheck;
// 
//        Territory all[] = getTerritoryArrayCached();
//        ArrayList distance = new ArrayList(all.length);
//        
//        // reset distance
//        for (int i = 0; i < all.length; i++)
//        {
//            all[i].nTempDistance = 0;
//        }
//        
//        while (true)
//        {
//            for (int i = 0; i < search.adjacentTerritories_.length; i++)
//            {
//                tCheck = search.adjacentTerritories_[i];
//                
//                // if equal, return distance
//                if (tCheck == tFind)
//                {
//                    return iDistance;
//                }
//                // if distance not set, add it to array
//                else if (tCheck.nTempDistance == 0)
//                {
//                    // if must be same territory type, check that it is so
//                    if (bSameTerritoryType &&
//                            !tCheck.getType().equals(tFind.getType()))
//                    {
//                        continue;
//                    }
//                        
//                    distance.add(tCheck);
//                    tCheck.nTempDistance = iDistance;
//                }
//            }
//            
//            search = (Territory) distance.remove(0);
//            iDistance = search.nTempDistance + 1;
//        }
//    }
//    
}
