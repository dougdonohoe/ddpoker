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
 * Areas.java
 *
 * Created on October 28, 2002, 4:48 PM
 */

package com.donohoedigital.games.config;

import java.util.*;
import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.config.*;

import org.jdom2.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Areas extends TreeMap {
    
    //static Logger logger = LogManager.getLogger(Areas.class);
    
    /** 
     * Creates a new Areas from XML data 
     */
    public Areas(Element root, Namespace ns) 
                    throws ApplicationError
    {
        
        List children = XMLConfigFileLoader.getChildren(root, "area", ns, false, null);
        int nSize = children.size();
        if (nSize != 0) 
        {
            String sAttrErrorDesc;
            Element area;
            for (int i = 0; i < nSize; i++)
            {
                sAttrErrorDesc = "Area #" +(i+1)+" in " + GameboardConfig.GAMEBOARD_CONFIG;
                area = (Element)children.get(i);
                addArea(new Area(area, sAttrErrorDesc));
            }
        }
    }
    
    /**
     * Add Area to list
     */
    public void addArea(Area t)
    {
        put(t.getName(), t);
    }
    
    /**
     * Get area with given name
     */
    public Area getArea(String sName)
    {
        return (Area) get(sName);
    }
    
    /**
     * Print XML representation of Areas to given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        Set areas = this.keySet();
        Iterator iter = areas.iterator();
        String sAreaName;
        while (iter.hasNext())
        {
            sAreaName = (String) iter.next();
            getArea(sAreaName).printXML(writer, nIndent);
        }   
    }
    
    ////
    //// GAME MODE FUNCTIONALITY
    ////
        
    /**
     * Get array of areas to avoid using iters.
     * Note: this array is not updated if an Area
     * is added - but that shouldn't be the case in
     * GAME MODE.  Returns new array each time.  
     *
     * @see getAreaArrayCached
     */
    public Area[] getAreaArray()
    {
        Area array[] = new Area[size()];
        Set areas = this.keySet();
        Iterator iter = areas.iterator();
        String sAreaName;
        int nCnt = 0;
        while (iter.hasNext())
        {
            sAreaName = (String) iter.next();
            array[nCnt++] = getArea(sAreaName);
        }   
        return array;
    }
    
    Area cached_[];
    
    /**
     * Get cached copy of areas array
     */
    public Area[] getAreaArrayCached()
    {
        if (cached_ == null)
        {
            cached_ = getAreaArray();
        }
        
        return cached_;
    }
    
    /**
     * Have each area calculate information about the territories it contiains
     */
    public void calculateStats()
    {
        Area areas[] = getAreaArrayCached();
        for (int i = 0; i < areas.length; i++)
        {
            areas[i].calculateStats();
        }
    }
}
