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
 * Territories.java
 *
 * Created on October 28, 2002, 4:48 PM
 */

package com.donohoedigital.games.config;

import java.util.*;
import com.donohoedigital.base.*;
import org.apache.log4j.*;
import com.donohoedigital.config.*;

import org.jdom.*;
import java.awt.geom.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Territories extends TreeMap {
    
    //static Logger logger = Logger.getLogger(Territories.class);
    
    /** 
     * Creates a new Territories from XML element
     */
    public Territories(Areas areas, MapPoints allPoints, Element root, Namespace ns) 
                    throws ApplicationError
    {
        Territory.setAreas(areas);
        Territory.setMapPoints(allPoints);
        Territory.setTerritories(this);
        List children = XMLConfigFileLoader.getChildren(root, "territory", ns, false, null);
        int nSize = children.size();
        if (nSize != 0) 
        {
            String sAttrErrorDesc;
            Element territory;
            for (int i = 0; i < nSize; i++)
            {
                sAttrErrorDesc = "Territory #" +(i+1)+" in " + GameboardConfig.GAMEBOARD_CONFIG;
                territory = (Element)children.get(i);
                addTerritory(new Territory(territory, ns, sAttrErrorDesc));
            }
        }
    }
    
    /**
     * Add territory to list
     */
    public void addTerritory(Territory t)
    {
        put(t.getName(), t);
    }
    
    /**
     * Get territory with given name
     */
    public Territory getTerritory(String sName)
    {
        return (Territory) get(sName);
    }
    
    /**
     * Print XML representation of Territories to given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        Set territories = this.keySet();
        Iterator iter = territories.iterator();
        String sTerritoryName;
        while (iter.hasNext())
        {
            sTerritoryName = (String) iter.next();
            getTerritory(sTerritoryName).printXML(writer, nIndent);
        }   
    }
    
    ////
    //// GAME MODE FUNCTIONALITY
    ////
    
    /**
     * Get array of territories to avoid using iters.
     * Note: this array is not updated if a Territory
     * is added - but that shouldn't be the case in
     * GAME MODE.  Returns new array each time.  
     *
     * @see getTerritoryArrayCached
     */
    public Territory[] getTerritoryArray()
    {
        Territory array[] = new Territory[size()];
        Set territories = this.keySet();
        Iterator iter = territories.iterator();
        String sTerritoryName;
        int nCnt = 0;
        while (iter.hasNext())
        {
            sTerritoryName = (String) iter.next();
            array[nCnt++] = getTerritory(sTerritoryName);
        }   
        return array;
    }
    
    Territory cached_[];
    /**
     * Get cached copy of territories array
     */
    public Territory[] getTerritoryArrayCached()
    {
        if (cached_ == null)
        {
            cached_ = getTerritoryArray();
        }
        
        return cached_;
    }
        
    /**
     * Init all territories for a new game
     */
    public void initForGame()
    {   
        // Create path in each territory
        Territory ts[] = getTerritoryArrayCached();
        for (int i = 0; i < ts.length; i++)
        {
            ts[i].initForGame();
        }   
    }
    
    /**
     * Used in game mode - creates paths which represent
     * territory - used for filling.
     */
    public void createPaths()
    {   
        // Create path in each territory
        Territory ts[] = getTerritoryArrayCached();
        for (int i = 0; i < ts.length; i++)
        {
            ts[i].createPath();
        }   
    }
    
    /**
     * Used in game mode - creates array of adjacent borders for 
     * each territory.
     */
    public void determineAdjacentTerritories(boolean bClearBorders)
    {
        // figure adjacent territories
        Territory ts[] = getTerritoryArrayCached();
        for (int i = 0; i < ts.length; i++)
        {
            ts[i].determineAdjacentTerritories(bClearBorders);
        }   
    }
}
