/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * MapPoints.java
 *
 * Created on November 10, 2002, 5:29 PM
 */

package com.donohoedigital.games.config;

import java.util.*;

/**
 * ArrayList subclass used for MapPoints
 * @author  Doug Donohoe
 */
public class MapPoints extends ArrayList {
    
    /** 
     * Creates a new instance of MapPoints 
     */
    public MapPoints() {
        super();
    }
       
    /** 
     * Creates a new instance of MapPoints 
     */
    public MapPoints(int nSize) {
        super(nSize);
    }
    
    /**
     * Add MapPoint (if not in array).  If point
     * equal to bp is in array,
     * return point in array.  Otherwise add and return bp.
     */
    public MapPoint addMapPoint(MapPoint bp)
    {
        return addMapPoint(bp, -1);

    }
    
    /**
     * Add MapPoint (if not in array).  If point
     * equal to bp is in array,
     * return point in array.  Otherwise add and return bp.
     */
    public MapPoint addMapPoint(MapPoint bp, int nIndex)
    {
        if (contains(bp)) return getMapPoint(indexOf(bp));
        if (nIndex < 0 || nIndex > size())
        {
            nIndex = size();
        }
        add(nIndex, bp);
        return bp;
    }
    
    /**
     * Get mappoint at given index
     */
    public MapPoint getMapPoint(int i)
    {
        return (MapPoint) get(i);
    }
    
    /**
     * Remove mappoint
     */
    public void removeMapPoint(MapPoint bp)
    {
        remove(bp);
    }
}
