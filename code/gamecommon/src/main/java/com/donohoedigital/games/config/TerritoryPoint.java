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
 * TerritoryPoint.java
 *
 * Created on November 10, 2002, 4:49 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.ApplicationError;
import org.apache.logging.log4j.*;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 *
 * @author  Doug Donohoe
 */
public class TerritoryPoint extends MapPoint
{
    static Logger logger = LogManager.getLogger(TerritoryPoint.class);
    
    Territory myTerritory_;

    /** 
     * New TerritoryPoint from x,y
     */
    public TerritoryPoint(int x, int y, String sType) {
        super(x,y,sType);
    }
    
    /**
     * New TerritoryPoint from an XML Element
     */
    public TerritoryPoint(Element point, Namespace ns, String sAttrErrorDesc)
                throws ApplicationError
    {
        super(point, ns, sAttrErrorDesc);
    }
    
    /**
     * Set territory this point belongs to
     */
    public void setTerritory(Territory t)
    {
        myTerritory_ = t;
    }
    
    /**
     * get territory this point belongs to
     */
    public Territory getTerritory()
    {
        return myTerritory_;
    }
    
    //
    // UI helper methods
    //
    
    /**
     * Return nearest point to this one.  Used by GUIs
     * about to remove this point from the territory to determine
     * what should become active.
     */
    public TerritoryPoint getNearestPoint()
    {
        return getNavPoint(POINT_DELETE);     
    }
    
    /**
     * Return next point in territory.  Used for tabbing through points
     */
    public TerritoryPoint getNextPoint()
    {
        return getNavPoint(POINT_NEXT);
    }
    
    /**
     * Return previous point in the territory.  Used for shift-tabbing through points
     */
    public TerritoryPoint getPrevPoint()
    {
        return getNavPoint(POINT_PREV);
    }
    
    private static final int POINT_NEXT = 1;
    private static final int POINT_PREV = 2;
    private static final int POINT_DELETE = 3;
    
    /**
     * Gets related point based on navigation type
     */
    private TerritoryPoint getNavPoint(int nType)
    {
        if (myTerritory_ == null) {
            logger.warn("myTerritory_ is null in TerritoryPoint.getNavXPoint() " + this);
            return null;
        }
        int nSize = myTerritory_.size();
        if (nSize == 1) 
        {
            // if delete nav point, don't return point being deleted
            if (nType == POINT_DELETE) return null;
            // if tab, return only point
            return myTerritory_.getTerritoryPoint(0);
        }
        
        int nIndex = myTerritory_.getPointIndex(this);
        if (nIndex == -1) return null;
        
        switch (nType) {
            case POINT_NEXT:
                if (nIndex == (nSize - 1)) return myTerritory_.getTerritoryPoint(0);
                else return myTerritory_.getTerritoryPoint(nIndex + 1);
               
            case POINT_PREV:
                if (nIndex == 0) return myTerritory_.getTerritoryPoint(nSize - 1);
                else return myTerritory_.getTerritoryPoint(nIndex - 1);
                
            case POINT_DELETE:
                if (nIndex == 0) return myTerritory_.getTerritoryPoint(1);
                else return myTerritory_.getTerritoryPoint(nIndex - 1);
                
            default:
                return null;
        }
    }
}
