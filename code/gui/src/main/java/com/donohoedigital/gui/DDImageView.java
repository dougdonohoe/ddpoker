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
 * DDImageView.java
 *
 * Created on March 29, 2003, 3:42 PM
 */
package com.donohoedigital.gui;

import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.image.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDImageView extends DDView
{
    public static final String YADJ = "yadj";

    BufferedImage image_;
    static Logger logger = LogManager.getLogger(DDImageView.class);
    int nWidth_ = 0;
    int nHeight_= 0;
    int nYadj_ = 0;

    /**
     * Creates a new instance of DDImageView 
     */    
    public DDImageView(Element elem)
    {
        super(elem);

        String sTag = elem.getAttributes().getAttribute(StyleConstants.NameAttribute).toString();

        String src = (String)getElement().getAttributes().
                             getAttribute(HTML.Attribute.SRC);
        if (sTag.equals("ddimg"))
        {
            image_ = ImageConfig.getBufferedImage(src);
        }
        else
        {
            image_ = ImageConfig.getBufferedImageMatchingFile(src);
        }

		if (image_ == null)
		{
            logger.warn( "No image for src " + src);
			return;
		}
        
        nWidth_ = image_.getWidth();
        nHeight_ = image_.getHeight();
        
        boolean nWidthChanged = false;
        boolean nHeightChanged = false;
        
        String sValue = (String) getElement().getAttributes().getAttribute(HTML.Attribute.WIDTH);
        if (sValue != null && sValue.length() > 0)
        {
            nWidth_ = Integer.parseInt(sValue);
            nWidthChanged = true;
        }
        
        sValue = (String) getElement().getAttributes().getAttribute(HTML.Attribute.HEIGHT);
        if (sValue != null && sValue.length() > 0)
        {
            nHeight_ = Integer.parseInt(sValue);
            nHeightChanged = true;
        }
        
        if (nWidthChanged && !nHeightChanged)
        {
            double nRatio = (double) image_.getWidth() / (double) nWidth_;
            nHeight_ = (int) (nHeight_ / nRatio); 
        }
        
        if (!nWidthChanged && nHeightChanged)
        {
            double nRatio = (double) image_.getHeight() / (double) nHeight_;
            nWidth_ = (int) (nWidth_ / nRatio); 
        }

        sValue = (String) getElement().getAttributes().getAttribute(YADJ);
        if (sValue != null && sValue.length() > 0)
        {
            nYadj_ = Integer.parseInt(sValue);
        }
    }
  
    /**
     * paint
     */
    public void paint(Graphics g, Shape a) 
    {
		if (image_ == null) return;

        Rectangle rect = (a instanceof Rectangle) ? (Rectangle)a :
                         a.getBounds();

        g.drawImage(image_, rect.x, rect.y+nYadj_, rect.x+nWidth_, rect.y+nHeight_+nYadj_,
                                0, 0, image_.getWidth(), image_.getHeight(), null);
    }
    
    /** Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     * 		<code>View.Y_AXIS</code>
     * @return   the span the view would like to be rendered into.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view
     * @see View#getPreferredSpan
     *
     */
    public float getPreferredSpan(int axis) {
        if (axis == View.X_AXIS) return nWidth_;
        return nHeight_;
    }
}
