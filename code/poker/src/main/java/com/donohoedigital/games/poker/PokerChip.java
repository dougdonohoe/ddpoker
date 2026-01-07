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
 * PokerChip.java
 *
 * Created on May 17, 2004, 7:43 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import java.awt.*;

/**
 *
 * @author  donohoe
 */
public class PokerChip implements ImageComponent.CustomImage
{
    static Logger logger = LogManager.getLogger(PokerChip.class);
    
    boolean bDrawFont_ = true;
    int nValue_;
    Font font_;
    Color rim_;
    Color middle_;
    Color text_;
    String sText_;
    double fontScale_;
    
    /** Creates a new instance of PokerChip */
    public PokerChip() 
    {
        font_ = StylesConfig.getFont("chip.value");
    }
    
    /** Creates a new instance of PokerChip */
    public PokerChip(boolean bDrawFont) 
    {
        super();
        bDrawFont_ = bDrawFont;
    }
    
    /**
     * set draw font
     */
    public void setDrawFont(boolean b)
    {
        bDrawFont_ = b;
    }
    
    /**
     * set the value of the chip
     */
    public void setValue(int n)
    {
        nValue_ = n;
        text_ = StylesConfig.getColor("chip.white");
        fontScale_ = 1.0d;
        if (nValue_ == 100000) {
            fontScale_ = .75d;
            text_ = StylesConfig.getColor("chip.black");
            sText_ = "100K";
        } else if (nValue_ == 50000) {
            fontScale_ = .85d;
            sText_ = "50K";
        } else if (nValue_ == 10000) {
            fontScale_ = .85d;
            sText_ = "10K";
        } else if (nValue_ == 5000) {
            sText_ = "5K";
        } else if (nValue_ == 1000) {
            sText_ = "1K";
        } else if (nValue_ == 500) {
            fontScale_ = .85d;
            sText_ = "500";
        } else if (nValue_ == 100) {
            fontScale_ = .85d;
            sText_ = "100";
        } else if (nValue_ == 25) {
            sText_ = "25";
        } else if (nValue_ == 5) {
            sText_ = "5";
        } else if (nValue_ == 1){
            sText_ = "1";
            text_ = StylesConfig.getColor("chip.black");
        }
        
        rim_ = StylesConfig.getColor("chip.rim." + sText_);
        middle_ = StylesConfig.getColor("chip.mid."+sText_);
    }
    
    static final Color shadow_ = new Color(0,0,0,80);
    public void paintCustom(Graphics2D g, int x, int y, int width, int height) 
    {
        // reduce by 1 to allow shadow to draw
        width -= 1;
        height -= 1;
        // hints
        Object old =g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.setColor(shadow_);
        g.fillOval(x+2,y+1,width-1,height-1);
        // rim
        g.setColor(rim_);
        g.fillOval(x, y, width-1, height-1);
        
        // middle
        g.setColor(middle_);
        int offset = width / 8;
        g.fillOval(x+offset, y+offset, (width - 2*offset - 1), (height - 2*offset - 1));
        
        // label
        if (bDrawFont_)
        {
            TextUtil util = new TextUtil(g, font_, sText_);
            float fx = x + ((width) / 2.0f)*.99f;
            float fy = y + ((height) / 2.0f)*.915f;
            util.prepareDraw(fx, fy, null, (width / 30.0d) * fontScale_, true);
            util.drawString(text_, null);
            util.finishDraw();
        }
        
        // reset
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }    
    
    
}
