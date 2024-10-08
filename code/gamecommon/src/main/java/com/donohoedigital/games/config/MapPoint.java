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
 * MapPoint.java
 *
 * Created on November 10, 2002, 4:49 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.config.*;

import org.jdom2.*;

/**
 *
 * @author  Doug Donohoe
 */
public class MapPoint 
{
    //static Logger logger = LogManager.getLogger(MapPoint.class);
    
    public int x_ = 0;
    public int y_ = 0;
    protected Integer NO_ANGLE = new Integer(0);
    protected Integer angle_ = NO_ANGLE; 
    
    
    public static final String NO_TYPE = "none";
    protected String sType_ = NO_TYPE;
    
    /** 
     * New MapPoint from x,y
     */
    public MapPoint(int x, int y) {
        x_ = x;
        y_ = y;
    }
    
    /** 
     * New MapPoint from x,y,type
     */
    public MapPoint(int x, int y, String sType) {
        x_ = x;
        y_ = y;
        sType_ = sType;
    }
    
    /** 
     * New MapPoint from x,y,angle,type
     */
    public MapPoint(int x, int y, int angle, String sType) {
        x_ = x;
        y_ = y;
        angle_ = new Integer(angle);
        sType_ = sType;
    }
    
    /**
     * New MapPoint from an XML Element
     */
    public MapPoint(Element point, Namespace ns, String sAttrErrorDesc)
                throws ApplicationError
    {
        x_ = XMLConfigFileLoader.getIntegerAttributeValue(point, "x", true, sAttrErrorDesc).intValue();
        y_ = XMLConfigFileLoader.getIntegerAttributeValue(point, "y", true, sAttrErrorDesc).intValue();
        angle_ = XMLConfigFileLoader.getIntegerAttributeValue(point, "angle", false, sAttrErrorDesc, NO_ANGLE);
        sType_ = XMLConfigFileLoader.getStringAttributeValue(point, "type", false, sAttrErrorDesc, NO_TYPE);
    }
    
    /**
     * X value of the point
     */
    public int getX()
    {
        return x_;
    }
    
    /**
     * set X value of the point
     */
    public void setX(int x)
    {
        x_ = x;
    }
    
    /**
     * Y value of the point
     */
    public int getY()
    {
        return y_;
    }
    
    /**
     * set Y value of the point
     */
    public void setY(int y)
    {
        y_ = y;
    }

    /**
     * get type of point
     */
    public String getType()
    {
        return sType_;
    }
    
    /**
     * set type of point
     */
    public void setType(String sType)
    {
        sType_ = sType;
    }
    
    /**
     * Get angle 
     */
    public Integer getAngle()
    {
        return angle_;
    }
    
    /**
     * Set angle
     */
    public void setAngle(int nAngle)
    {
        angle_ = new Integer(nAngle);
    }
    
    /**
     * Return true if passed in object is equal
     * to this point (x==x, y==y) and borders equal
     */
    public boolean equals(Object obj) {
	if (this == obj) return true;
        
        if (obj instanceof MapPoint)
        {
            MapPoint p = (MapPoint) obj;
            if (p.x_ == x_ && p.y_ == y_ && p.sType_.equals(sType_)) return true;
            // angle doesn't effect equality
        }
        
        return false;
    }
    
    /**
     * Write XML which represents this point to the given writer
     */
    public void printXML(XMLWriter writer, int nIndent)
    {
        writer.printIndent(nIndent);
        writer.printElementStartOpen("point");
        writer.printAttribute("x", new Integer(x_));
        writer.printAttribute("y", new Integer(y_));
        if (angle_ != null && !angle_.equals(NO_ANGLE)) writer.printAttribute("angle", angle_);
        if (sType_ != null && !sType_.equals(NO_TYPE)) writer.printAttribute("type", sType_);
        writer.printElementEndLine();
    }

    /**
     * String representation of point for debugging
     */
    public String toString()
    {
        return shortDesc();
    }
    
    /**
     * short string desc
     */
    public String shortDesc()
    {
        String sRet = "(" + x_ + "," + y_ + ")";
        if (sType_ != null && !sType_.equals(NO_TYPE)) sRet = sType_ + " " + sRet;
        if (angle_ != null && !angle_.equals(NO_ANGLE)) sRet += " " + angle_ + " degrees";
        return sRet;
    }
}
