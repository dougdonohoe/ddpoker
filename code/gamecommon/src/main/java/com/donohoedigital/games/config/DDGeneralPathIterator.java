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
 * DDGeneralPathIterator.java
 *
 * Created on June 30, 2003, 6:14 PM
 */

package com.donohoedigital.games.config;

import org.apache.logging.log4j.*;

import java.awt.geom.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDGeneralPathIterator implements PathIterator 
{
    //static Logger logger = LogManager.getLogger(DDGeneralPathIterator.class);
    
    private GeneralPath path_;
    private static final int curvesize[] = {2, 2, 4, 6, 0};
    private ArrayList points_ = new ArrayList();
    float current_[];
    private int index_ = 0;
    
    /**
     * Copy results of path iterator here so we can
     * reuse.  Doubles data stored in the path, but
     * extra mem used is better than recreating the
     * path over and over and over considering
     * it is used for contains() functionality
     * for each path for each mouse movement (adds up!)
     */
    public DDGeneralPathIterator(GeneralPath path) 
    {
        path_ = path;
        PathIterator iter = path.getPathIterator(null);
        float coord[] = new float[6];
        float newd[];
        int ret;
        int num;
        int i = 0;
        while(!iter.isDone())
        {
            // get existing data
            ret = iter.currentSegment(coord);
            num = curvesize[ret];
            iter.next();
            
            // store it away
            newd = new float[num + 1];
            for (i = 0; i < num; i++)
            {
                newd[i] = coord[i];
            }
            
            // store curve type in extra space
            newd[num] = ret;
            points_.add(newd);
        }
    }
    
    public int currentSegment(double[] coords) 
    {
        current_ = (float[]) points_.get(index_);
        for (int i = 0; i < current_.length - 1; i++)
        {
            coords[i] = current_[i];
        }
        
        return (int) current_[current_.length - 1];
    }
    
    public int currentSegment(float[] coords) 
    {
        current_ = (float[]) points_.get(index_);
        for (int i = 0; i < current_.length - 1; i++)
        {
            coords[i] = current_[i];
        }
        
        return (int) current_[current_.length - 1];
    }
    
    public int getWindingRule() {
        return path_.getWindingRule();
    }
    
    public boolean isDone() {
        return (index_ >= points_.size());
    }
    
    public void next() {
        index_++;
    }
    
    public void reset() {
        index_ = 0;
    }
}
