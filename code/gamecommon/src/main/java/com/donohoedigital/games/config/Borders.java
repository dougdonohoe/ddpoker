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
 * Borders.java
 *
 * Created on October 28, 2002, 4:48 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.config.*;
import org.apache.log4j.*;
import org.jdom.*;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Borders extends TreeSet<Border> {
    
    private static Logger logger = Logger.getLogger(Borders.class);
    
    private MapPoints allPoints_;
    private static final String BORDER_TAG = "BORDER";
    private static final String BORDER_DEF_TAG = "border";
    private static final String BORDER_XSD = "border.xsd";
    private static final String BORDERDIR_PREFIX = "borders/border";
    private static final String NUMBORDERFILES = "numborderfiles";
    
    /** 
     * Creates a new instance of Borders 
     */
    public Borders(Territories territories, MapPoints points, Element root, Namespace ns, String module)
    {
        super(Border.COMPARATOR);
        allPoints_ = points;
        Border.setTerritories(territories);
        Border.setMapPoints(allPoints_);
        
        Integer nNum = XMLConfigFileLoader.getChildIntegerValue(root, NUMBORDERFILES, ns, 
                                false, GameboardConfig.GAMEBOARD_CONFIG, 0);
        XMLConfigFileLoader loader = new XMLConfigFileLoader();
        
        for (int j = 0; j < nNum; j++)
        {
            URL url = new MatchingResources("classpath*:config/" + module + "/" + BORDERDIR_PREFIX + j + ".xml").getSingleRequiredResourceURL();
            Document doc = loader.loadXMLUrl(url, BORDER_XSD);
            root = doc.getRootElement();

            // get <border> children
            List<Element> children = XMLConfigFileLoader.getChildren(root, BORDER_DEF_TAG, ns, false, null);
            int nSize = children.size();

            if (nSize != 0) 
            {
                String sAttrErrorDesc;
                Element border;
                Border newBorder;
                for (int i = 0; i < nSize; i++)
                {
                    sAttrErrorDesc = "Border #" +(i+1)+" in " + GameboardConfig.GAMEBOARD_CONFIG;
                    border = children.get(i);
                    newBorder = new Border(border, ns, sAttrErrorDesc);
                    addBorder(newBorder);
                }
            }    
        }
    }
    
    /**
     * Add a border to this.  Returns t if a new border.  If border
     * exists, returns existing border (and updates bEnclosed).
     * When a border is added, the territories that make up the border
     * are notified of the new border.
     */
    public Border addBorder(Border b)
    {
        if (this.contains(b)) 
        {
            Border bExist = getBorder(b.getTerritory1(), b.getTerritory2(), b.getNumber());
            bExist.setEnclosed(b.isEnclosed());
            return bExist;
        }
        add(b);
        
        // let territories know of new border
        b.getTerritory1().addBorder(b);
        b.getTerritory2().addBorder(b);
        return b;
    }
    
    /**
     * Remove given border from this
     * When a border is removed, the territories that make up the border
     * are notified of the old border.
     */
    public void removeBorder(Border b)
    {
        remove(b);
                
        // let territories know of removed border
        b.getTerritory1().removeBorder(b);
        b.getTerritory2().removeBorder(b);
    }
    
    /**
     * Return border which contains t1 and t2
     */
    public Border getBorder(Territory t1, Territory t2, int nNum)
    {
        Iterator<Border> iter = iterator();
        Border border;
        while (iter.hasNext())
        {
            border = iter.next();
            if (border.contains(t1, t2, nNum))
            {
                return border;
            }
        }   
        return null;
    }
    
    /**
     * Print XML representation of this borders to given writer
     */
    public void printXML(XMLWriter writer, File fConfigDir, int nIndent)
    {
        int BORDERS_PER_FILE = 20;
        
        Iterator<Border> iter = iterator();
        Border border;
        int id = 0;
        int nCnt = 0;
        int nBorderIndent = 0;
        XMLWriter borderwriter = null;
        File fConfigFile;

        while (iter.hasNext())
        {
            border = iter.next();
            // don't bother writing out if not 1 point in the border
            if (border.getBorderPoints().size() > 0)
            {
                //sName = border.getTerritory1().getName() + "." + border.getTerritory2().getName() + ".xml";
                //sName = sName.replace(' ', '_');
                if (nCnt++ % BORDERS_PER_FILE == 0)
                {
                    if (borderwriter != null)
                    {
                        // close previous file
                        borderwriter.printElementEndLine(BORDER_TAG, nBorderIndent);
                        borderwriter.close();
                    }
                    fConfigFile = new File(fConfigDir, BORDERDIR_PREFIX + (id++) + ".xml");
                    borderwriter = XMLWriter.CreateXMLWriter(fConfigFile, false);
                    borderwriter.printXMLHeaderLine();
                    borderwriter.printRootElementStartLine(BORDER_TAG, XMLConfigFileLoader.DDNAMESPACE, 
                                    "../../xml-schema/" + BORDER_XSD, nBorderIndent); // TODO: determine path programmatically?
                    borderwriter.printNewLine();
                }
                
                // print border
                border.printXML(borderwriter, nBorderIndent+1);
            }
        }   
        
        if (borderwriter != null)
        {
            // close last file
            borderwriter.printElementEndLine(BORDER_TAG, nBorderIndent);
            borderwriter.close();
        }
        
        logger.info("Saved " + id + " border files");
        
        writer.printElementLine(NUMBORDERFILES, id, nIndent);
        writer.printNewLine();
    }
    
    ////
    //// GAME MODE FUNCTIONALITY
    ////
    
//  NOT CURRENTLY USED  
//    /**
//     * Used in game mode - creates a path which represent 
//     * all border lines
//     */
//    public GeneralPath createPath()
//    {
//        GeneralPath path;
//        Border border;
//        BorderPoints points;
//        BorderPoint point;
//        int nNumPoints = allPoints_.size();
//        int nNumBorders = size();
//        
//        // estimate size of path as number of points +
//        // number of borders
//        path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, nNumPoints + nNumBorders);
//        
//        // loop through each border
//        Iterator iter = iterator();
//        while (iter.hasNext())
//        {
//            border = (Border) iter.next();
//            nNumPoints = border.size();
//            
//            // add each point to path - moving to
//            // start of border at first point (0)
//            for (int i = 0; i < nNumPoints; i++)
//            {
//                point = border.getBorderPoint(i);
//                if (i == 0)
//                {
//                    path.moveTo(point.x_, point.y_);
//                } else {
//                    path.lineTo(point.x_, point.y_);
//                }
//            }
//            if (border.isEnclosed())
//            {
//                path.closePath();
//            }
//        }
//        
//        return path;
//    }
    
    /**
     * Used to free up memory - removes all points from each border
     */
    void clearPoints()
    {
        allPoints_ = null;
        Iterator<Border> iter = iterator();
        Border border;
        while (iter.hasNext())
        {
            border = iter.next();
            border.clearPoints();    
        }      
    }
}
