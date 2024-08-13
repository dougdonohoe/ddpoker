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
 * HelpTopic.java
 *
 * Created on August 05, 2003, 6:26 PM
 */

package com.donohoedigital.config;

import java.awt.*;
import java.net.*;

/**
 *
 * @author  donohoe
 */
public class HelpTopic
{
    //static Logger logger = Logger.getLogger(HelpTopic.class);
    
    String sName_;
    String sDisplay_;
    URL url_;

    /**
     * New help definition from name and its file
     */
    public HelpTopic(String sName, String sDisplay, URL url, int nIndent)
    {
        sName_ = sName;
        url_ = url;
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML>");
        if (nIndent == 0)
        {
            sb.append("<B>");
        }
        for (int i = 0; i < nIndent; i++)
        {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        sb.append(sDisplay);
        sDisplay_ = sb.toString();
    }
    
    /**
     * get file that this help resides in
     */
    public URL getHelpFile()
    {
        return url_;
    }
    
    /**
     * get name of this help
     */
    public String getName()
    {
        return sName_;
    }
    
    /**
     * get display name of this help topic
     */
    public String getDisplay()
    {
        return sDisplay_;
    }
    
    /**
     * Get contents
     */
    public String getContents()
    {
        return ConfigUtils.readURL(url_);
    }

    // seed value so 1st display is correct
    private Rectangle rect_ = new Rectangle(0,0,50,50);
    
    /**
     * set scroll pos (runtime only)
     */
    public void setScrollPosition(Rectangle rect)
    {
        rect_ = rect;
    }
    
    /**
     * Get scroll pos (runtime only)
     */
    public Rectangle getScrollPosition()
    {
        return rect_;
    }
}
    
