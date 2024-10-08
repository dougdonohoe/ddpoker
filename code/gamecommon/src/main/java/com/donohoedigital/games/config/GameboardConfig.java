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
 * GameboardConfig.java
 *
 * Created on October 11, 2002, 6:02 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import org.jdom2.*;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author  donohoe
 */
public class GameboardConfig extends XMLConfigFileLoader
{
    static Logger logger = LogManager.getLogger(GameboardConfig.class);
    
    static final String GAMEBOARD_CONFIG = "gameboard.xml";
    private static final String GAMEBOARD_TAG = "GAMEBOARD";
    private static final String GAMENAME = "gamename";
    private static final String IMAGE = "image";
    private static final String SCALE = "scale";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    
    private String sGamename_;
    private String sImage_;
    private Double dScale_;
    private Areas areas_;
    private Territories territories_;
    private Borders borders_;
    private MapPoints allPoints_;
    private Integer nWidth_, nHeight_;
    private File fConfigFile_;
    
    /** 
     * Load GameboardConfig (gameboard.xml) from the given module
     */
    public GameboardConfig(String module) throws ApplicationError
    {
        init(module);
    }
    
    /**
     * Read in config file from given module
     */
    private void init(String module) throws ApplicationError
    {
        // get gamedef url, throws exception if missing
        URL url = new MatchingResources("classpath*:config/" + module + "/" + GAMEBOARD_CONFIG).getSingleRequiredResourceURL();

        // store actual file for use by tools
        if (url.toString().startsWith("file:"))
        {
            fConfigFile_ = ConfigUtils.getFile(url);
        }

        Document doc = this.loadXMLUrl(url, "gameboard.xsd", module);
        init(doc, module);
    }
    
    /**
     * Initialize from JDOM doc
     */
    private void init(Document doc, String module) throws ApplicationError
    {
        Element root = doc.getRootElement();
        
        // gamename
        sGamename_ = getChildStringValueTrimmed(root, GAMENAME, ns_, true, GAMEBOARD_CONFIG);
        
        // image
        sImage_ = getChildStringValueTrimmed(root, IMAGE, ns_, true, GAMEBOARD_CONFIG);
           
        // width
        nWidth_ = getChildIntegerValue(root, WIDTH, ns_, true, GAMEBOARD_CONFIG);
        
        // height
        nHeight_ = getChildIntegerValue(root, HEIGHT, ns_, true, GAMEBOARD_CONFIG);
        
        // scale
        dScale_ = getChildDoubleValue(root, SCALE, ns_, true, GAMEBOARD_CONFIG);
        
        // areas
        areas_ = new Areas(root, ns_);
        
        // storage for all points in territories and borders (no duplicates allowed/needed)
        allPoints_ = new MapPoints(500);
        
        // territories
        territories_ = new Territories(areas_, allPoints_, root, ns_);
        
        // borders
        borders_ = new Borders(territories_, allPoints_, root, ns_, module);
    }
    
    /**
     * Get name of file
     */
    public File getFile()
    {
        return fConfigFile_;
    }
    
    /**
     * Return name of game
     */
    public String getGamename()
    {
        return sGamename_;
    }
    
    /**
     * Set game name
     */
    public void setGamename(String sGamename)
    {
        sGamename_ = sGamename;
    }
    
    /**
     * Get image name
     */
    public String getImage()
    {
        return sImage_;
    }
    
    public void setImage(String sImage)
    {
        sImage_ = sImage;
    }
    
    /**
     * get width
     */
    public Integer getWidth()
    {
        return nWidth_;
    }
    
    /**
     * set width
     */
    public void setWidth(int nWidth)
    {
        nWidth_  = nWidth;
    }
    
    /**
     * get height
     */
    public Integer getHeight()
    {
        return nHeight_;
    }
    
    /**
     * set height
     */
    public void setHeight(int nHeight)
    {
        nHeight_  = nHeight;
    }    
    
    /**
     * Get scale
     */
    public Double getScale()
    {
        return dScale_;
    }
    
    /**
     * Set scale
     */
    public void setScale(double dNewScale)
    {
        double dScale = dScale_;
        
        // if scale is different, adjust points
        if (dNewScale != dScale)
        {
            dScale_ = dScale;
            MapPoints points = getMapPoints();
            int nNum = points.size();
            MapPoint point;
            
            for (int i = 0; i < nNum; i++)
            {
                point = points.getMapPoint(i);
            
                // scale points
                // to adjust scale use this:  
                // (existingvalue / dScale) * dNewScale
                point.setX((int)((point.getX() / dScale) * dNewScale));
                point.setY((int)((point.getY() / dScale) * dNewScale));
            }
            
            nWidth_ = (int) ((nWidth_ / dScale) * dNewScale);
            nHeight_ = (int) ((nHeight_ / dScale) * dNewScale);
        }
    }
    
    /**
     * Return Areas in this gameboard
     */
    public Areas getAreas()
    {
        return areas_;
    }
    
    /**
     * Return territories in this gameboard
     */
    public Territories getTerritories()
    {
        return territories_;
    }
    
    /**
     * Return complete list of points
     */
    public MapPoints getMapPoints()
    {
        return allPoints_;
    }
    
    /**
     * Return borders in this gameboard
     */
    public Borders getBorders()
    {
        return borders_;
    }
    
    /**
     * Save configuration file back to disk - backup existing file to ".old"
     */
    public void save(boolean bBackup) throws ApplicationError
    {
        if (!bSave_)
        {
            logger.warn("Save disabled for " + fConfigFile_.getAbsolutePath());
            return;
        }
        
        XMLWriter writer = XMLWriter.CreateXMLWriter(fConfigFile_, bBackup);
        
        writer.printXMLHeaderLine();
        printXML(writer, 0);
        writer.close();
        
        logger.info("Saved " + fConfigFile_.getAbsolutePath());
    }
    
    /**
     * Print XML representation of the game board config to the given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        writer.printRootElementStartLine(GAMEBOARD_TAG, XMLConfigFileLoader.DDNAMESPACE, 
                                "../xml-schema/" + "gameboard.xsd", nIndent); // TODO: determine path programmatically?
        writer.printNewLine();
        
        // contents
        writer.printElementLine(GAMENAME, getGamename(), nIndent+1);
        writer.printNewLine();
        writer.printElementLine(IMAGE, getImage(), nIndent+1);
        writer.printNewLine();
        writer.printElementLine(WIDTH, getWidth(), nIndent+1);
        writer.printNewLine();
        writer.printElementLine(HEIGHT, getHeight(), nIndent+1);
        writer.printNewLine();
        writer.printElementLine(SCALE, getScale(), nIndent+1);
        writer.printNewLine();

        // areas
        areas_.printXML(writer, nIndent+1);
        writer.printNewLine();
        
        // territories
        territories_.printXML(writer, nIndent+1);
        writer.printNewLine();
        
        // borders
        borders_.printXML(writer, fConfigFile_.getParentFile(), nIndent+1);
        writer.printNewLine();
        
        // clean things up
        writer.printElementEndLine(GAMEBOARD_TAG, nIndent);
    }
    
    ////
    //// GAME MODE FUNCTIONALITY
    ////
    
    boolean bSave_ = true;
    
    /**
     * Clear territories of all pieces, etc.
     */
    public void initTerritories()
    {
        territories_.initForGame();
    }
    
    /**
     * Create adjacent territories and other info - 
     * must call AFTER createPaths.
     * If bClearBorders is true, then all border information is 
     * cleared to free memory.
     */
    public void determineTerritoryInfo(boolean bClearBorders)
    {
        bSave_ = bSave_ && !bClearBorders; // disable save if clearing points
        
        territories_.determineAdjacentTerritories(bClearBorders);
        areas_.calculateStats();
        
        if (bClearBorders && borders_ != null)
        {
            borders_.clear();
            borders_ = null;
        }
    }
    
    
    /**
     * Create territory paths.  If bClearPoints true, then
     * remove all points to free memory (save is 
     * disabled after this is called).  Should be called
     * after transformAllPoints is done
     */
    public void createPaths(boolean bClearPoints)
    {
        bSave_ = bSave_ && !bClearPoints; // disable save if clearing points
        
        // NOT CURRENTLY USED borderPath_ = borders_.createPath();
        territories_.createPaths();

        if (bClearPoints && allPoints_ != null)
        {
            allPoints_.clear();
            allPoints_ = null;
            borders_.clearPoints();
        }
    }
    
//    NOT CURRENTLY USED 
//    GeneralPath borderPath_;
//    /**
//     * Return path representing all borders
//     */
//    public GeneralPath getBorderPath()
//    {
//        return borderPath_;
//    }
    
    /**
     * translate all points so that the top-left most becomes (0,0).  Adjust
     * height and width to refect distance between top-left most and bottom-right 
     * most points.  In other words, eliminate any gap between borders and the
     * edge of the original image.  Returns rectangle that represents x,y,width,height
     * of edge of original image (in original image coordinate space) 
     */
    public Rectangle transformAllPoints()
    {
        if (allPoints_ == null) return transformedSize_;
        
        int minx = Integer.MAX_VALUE;
        int maxx = Integer.MIN_VALUE;
        int miny = Integer.MAX_VALUE;
        int maxy = Integer.MIN_VALUE;
        int nNum;
        // pass 1 - figure out min/max for x,y
        nNum = allPoints_.size();
        MapPoint point;
        for (int i = 0; i < nNum; i++)
        {
            point = allPoints_.getMapPoint(i);
            minx = Math.min(minx, point.x_);
            maxx = Math.max(maxx, point.x_);
            miny = Math.min(miny, point.y_);
            maxy = Math.max(maxy, point.y_);
        }
        
        for (int i = 0; i < nNum; i++)
        {
            point = allPoints_.getMapPoint(i);
            point.x_ -= minx;
            point.y_ -= miny;
        }
        
        int width = (maxx - minx) + 1; 
        int height = (maxy - miny) + 1;
        setWidth(width);
        setHeight(height);
        
        double dScale = dScale_;
        transformedSize_ = new Rectangle((int)(minx/dScale),
                             (int)(miny/dScale), 
                             (int)(width/dScale), 
                             (int)(height/dScale));
        
        adjustedTransformedSize_ = transformedSize_;
        
        return transformedSize_;
    }
    
    // track last territory we adjusted to, so we don't readjust
    private Territory tAdjustTo_;
    private int nAdjustBy_;
    
    /**
     * Adjust paths, points and sizes so that the given Territory
     * is at the upper left (used when multiple map views used).
     * Most be called after transformAllPoints() and createPaths()
     */
    public Rectangle adjustPaths(Territory t)
    {
        if (tAdjustTo_ == t) return adjustedTransformedSize_;
        
        // newx has already been adjusted to be zero-based from the current bounds,
        // but we need to adjust for the scale factor
        double dScale = dScale_;
        AffineTransform af;
        
        /**
         * Undo last one
         */
        if (tAdjustTo_ != null)
        {
            af = AffineTransform.getTranslateInstance(nAdjustBy_, 0);
            adjustPaths(af, nAdjustBy_);
            adjustedTransformedSize_ = transformedSize_;
        }
        
        /**
         * Do this one
         */
        if (t != null)
        {
            nAdjustBy_ = t.getPathBounds().x;
            af = AffineTransform.getTranslateInstance(-nAdjustBy_, 0);
            adjustPaths(af, -nAdjustBy_);
            
            // adjust for new boundary
            int adjx = (int) (nAdjustBy_ / dScale);
            adjustedTransformedSize_ = new Rectangle(transformedSize_);
            adjustedTransformedSize_.x += adjx;
            adjustedTransformedSize_.width -= adjx;
        }
        
        // set width/height of map
        setWidth((int)(adjustedTransformedSize_.width * dScale));
        setHeight((int)(adjustedTransformedSize_.height * dScale));
        
        // track what we adjusted to
        tAdjustTo_ = t;
        
        // return new size
        return adjustedTransformedSize_;
    }
    
    /**
     * Adjust paths by the given transform and the territory points by
     * the given adjx
     */
    private void adjustPaths(AffineTransform af, int adjx)
    {
        TerritoryPoint point;
        TerritoryPoints points;
        Territory ta[] = this.territories_.getTerritoryArrayCached();

        for (Territory adj : ta)
        {
            // fix path
            adj.getPath().transform(af);
            adj.recalcPathBounds();

            // fix territory points
            points = adj.getTerritoryPoints();
            for (int j = 0; j < points.size(); j++)
            {
                point = points.getTerritoryPoint(j);
                point.setX(point.getX() + adjx);
            }
        }
    }

    private Rectangle transformedSize_;
    private Rectangle adjustedTransformedSize_;
    private Rectangle mapBounds_;
    
    /**
     * Return rectangle representing the 
     * area of the map on which territory borders
     * are drawn - returned in same coordinate space
     * as map is defined.  Note: does not account for
     * changes made by adjustPaths (used typically by
     * tools like TerritoryPointManager)
     */
    public Rectangle getMapBounds()
    {
        if (allPoints_ == null) return mapBounds_;
        
        int minx = Integer.MAX_VALUE;
        int maxx = Integer.MIN_VALUE;
        int miny = Integer.MAX_VALUE;
        int maxy = Integer.MIN_VALUE;
        int nNum;
        // pass 1 - figure out min/max for x,y
        nNum = allPoints_.size();
        MapPoint point;
        for (int i = 0; i < nNum; i++)
        {
            point = allPoints_.getMapPoint(i);
            minx = Math.min(minx, point.x_);
            maxx = Math.max(maxx, point.x_);
            miny = Math.min(miny, point.y_);
            maxy = Math.max(maxy, point.y_);
        }
        
        int width = (maxx - minx) + 1; 
        int height = (maxy - miny) + 1;
        
        mapBounds_ = new Rectangle(minx,miny,width,height);
        return mapBounds_;
    }
}
