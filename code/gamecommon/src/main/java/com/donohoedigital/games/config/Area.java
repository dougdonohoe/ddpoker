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
 * Area.java
 *
 * Created on October 28, 2002, 4:49 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import org.apache.log4j.*;
import com.donohoedigital.config.*;

import java.util.*;
import org.jdom.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Area 
{ 
    static Logger logger = Logger.getLogger(Area.class);
    
    /**
     * Name of area representing no area
     */
    public static final String NONE = "NONE";
    
    private String sName_;
    private String sDisplay_;
    private boolean bNone_ = false;
    
    /** 
     ** Creates a new instance of Area 
     */
    public Area(String sName) {
        sName_ = sName;
    }
    
    /**
     * Create new Area from XML element
     */
    public Area(Element area, String sAttrErrorDesc)
                throws ApplicationError
    {
        sName_ = XMLConfigFileLoader.getStringAttributeValue(area, "name", true, sAttrErrorDesc);
        bNone_ = sName_.equals(NONE);
        
        String sLookupName = sName_.replace(' ', '_');
        sDisplay_ = PropertyConfig.getStringProperty("area." + sLookupName, sName_, false);
    }
    
    /**
     * Get name of area
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * Set name of area
     */
    public void setName(String sName)
    {
        sName_ = sName;
    }

    /**
     * Return area name for display
     */
    public String getDisplayName()
    {
        return sDisplay_;
    }
    
    /**
     * Return whether this is the "none" area
     */
    public boolean isNone()
    {
        return bNone_;
    }
    
    /**
     * Return whether given object equals this Area
     */
    public boolean equals(Object obj) {
	if (this == obj) return true;
        
        if (obj instanceof Area)
        {
            Area t = (Area) obj;
            if (t.sName_.equals(sName_)) return true;
        }
        
        return false;
    }
    
    /**
     * Comparator which can be used to compare to areas
     */
    public static final Comparator COMPARATOR = new AreaComparator();
    
    /**
     * Used to compare two areas
     */
    public static int compare(Area t1, Area t2)
    {
        return COMPARATOR.compare(t1, t2);
    }
    
    /**
     * Comparator for areas
     */
    private static class AreaComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            Area a1 = (Area) o1;
            Area a2 = (Area) o2;
            
            if (a1.equals(a2)) return 0;
           
            return a1.sName_.compareTo(a2.sName_);
        }
        
        public boolean equals(Object obj)
        {
            if (obj instanceof AreaComparator) return true;
            return false;
        }
    }
    
    /**
     * Print XML represenation of this area to given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        writer.printIndent(nIndent);
        writer.printElementStartOpen("area");
        writer.printAttribute("name", sName_);
        writer.printElementEnd();
        writer.printNewLine();
    }
    
    /**
     * Return string representation of area for debugging
     */
    public String toString()
    {
        return sName_;
    }
    
    
    ////
    //// GAME MODE FUNCTIONALITY
    ////
    
    Territory ts_[] = null;
    private int nNumRegions_;
    private int nNumIslands_;
    private int nTotalAdjacentLandRegions_;
    private int nTotalAdjacentWaterRegions_;
    private int nTotalAdjacentAreas_;

    /**
     * Get array of territories in this area - this area
     * is created in calculateStats.  In non-game mode,
     * null is returned.   Note:  no decorations are
     * ever in this list
     */
    public Territory[] getTerritories()
    {
        return ts_;
    }
    
    /**
     * Create territory array of area contents
     * out decorations
     */
    private Territory[] createTerritoryArray()
    {
        Territory all[] = Territory.getTerritoryArrayCached();
        int nNum = 0;
        // first get count
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].getArea() == this && !all[i].isDecoration()) nNum++;
        }
        
        // then create array
        int nCnt = 0;
        ts_ = new Territory[nNum];
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].getArea() == this && !all[i].isDecoration())
            {
                ts_[nCnt++] = all[i];
            }
        }
        
        return ts_;
    }
        
    /**
     * Have area calculate information about the territories it contiains
     */
    public void calculateStats()
    {
        nNumRegions_ = 0;
        nNumIslands_ = 0;
        nTotalAdjacentLandRegions_ = 0;
        nTotalAdjacentWaterRegions_ = 0;
        nTotalAdjacentAreas_ = 0;
        
        Territory t, a;
        Territory[] ts = createTerritoryArray();
        nNumRegions_ = ts.length;
        Territory[] as;
        HashMap area = new HashMap();
        HashMap water = new HashMap();
        HashMap land = new HashMap();
        
        for (int i = 0; i < ts.length; i++)
        {
            t = ts[i];
            if (t.isIsland()) nNumIslands_++;
            
            // add adjacent territories to relevant lists
            // dups okay because using hashmap
            as = t.getAdjacentTerritories();
            for (int j = 0; j < as.length; j++)
            {
                a = as[j];
                if (a.isDecoration() || a.getArea() == this) continue;
                
                if (a.isWater())
                {
                    water.put(a,null);
                }

                if (a.isLand())
                {
                    land.put(a,null);
                }
                
                if (!a.getArea().isNone())
                {
                    area.put(a.getArea(), null);
                }
            }
        }
        
        nTotalAdjacentAreas_ = area.size();
        nTotalAdjacentWaterRegions_ = water.size();
        nTotalAdjacentLandRegions_ = land.size();
        
        //logger.debug(toStringDebug());
    }
    
    /**
     * Return string showing area information
     */
    public String toStringDebug()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(sName_);
        sb.append(": num=");
        sb.append(nNumRegions_);
        sb.append(" islands=");
        sb.append(nNumIslands_);
        sb.append(" adjland=");
        sb.append(nTotalAdjacentLandRegions_);
        sb.append(" adjwater=");
        sb.append(nTotalAdjacentWaterRegions_);
        sb.append(" adjarea=");
        sb.append(nTotalAdjacentAreas_);
        
        return sb.toString();
    }
    
    /**
     * Num territories in this area (no decorations ever in array list)
     */
    public int getNumTerritories()
    {
        return nNumRegions_;
    }
    
    /**
     * Num islands in the area
     */
    public int getNumIslands()
    {
        return nNumIslands_;
    }
    
    /**
     * Num of adjacent water regions to the entire area
     */
    public int getNumAdjacentWater()
    {
        return nTotalAdjacentWaterRegions_;
    }
    
    /**
     * Num of adjacent land regions to the entire area
     */
    public int getNumAdjacentLand()
    {
        return nTotalAdjacentLandRegions_;
    }
    
    /**
     * Num of adjacent areas to this area
     */
    public int getNumAdjacentAreas()
    {
        return nTotalAdjacentAreas_;
    }
    
    /**
     * Get count that each player owns, indexed by player id
     */
    public short[] getCountByPlayer(int nNumPlayers)
    {
        short count[] = new short[nNumPlayers];
        Territory[] ts = getTerritories();
        GamePlayer player;
        
        for (int i = 0; i < ts.length; i++)
        {
            player = ts[i].getGamePlayer();
            
            if (player != null)
            {
                int id = player.getID();
                // check id is within given range - helps with
                // games like War!'s Native players, where the
                // native player is greater than the player 
                if (id < nNumPlayers)
                {
                    count[id]++;
                }
            }
        }
        return count;
    }
    
    /**
     * Do all territories in the area belong to this player?
     */
    public boolean allBelongTo(GamePlayer pl)
    {
        int nCnt = 0;
        Territory ts[] = getTerritories();
        for (int i = 0; i < ts.length; i++)
        {
            if (ts[i].getGamePlayer() != pl)
            {
                    return false;
            }
            else if (ts[i].getGamePlayer() == pl)
            {
                nCnt++;
            }
        }
        
        // BUG 140 - must have some regions
        return (nCnt > 0);
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
}
