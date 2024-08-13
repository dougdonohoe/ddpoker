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
package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

public class TextUtil 
{
    //static Logger logger = Logger.getLogger(TextUtil.class);
    
    public double x;
    public double y;
    public double width;
    public double lineHeight;
    public double totalHeight;
    public double lineSpacing;
    public int numLines;
    public String sText;
    public Font fFont;
    public Graphics2D g;
    public FontRenderContext frc;
    AffineTransform old;
    Object oldAlias;
    boolean bAntiAlias;
    public double xadjust = 0;
    public double yadjust = 0;
    public LineMetrics metrics = null;
    
    
    public static final char SEP = '\n';
    public static final String SEPS = "\n";
    public static final String A2Z = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    /**
     * Create metrics using default line spacing of 1.5f
     */
    public TextUtil(Graphics2D g, Font fFont, String sText)
    {
        this(g, fFont, sText, 0.0f);
    }
    
    /**
     * Create metrics for drawing given string
     */
    public TextUtil(Graphics2D g, Font fFont, String sText, float fLineSpacing)
    {
        this.g = g;
        this.fFont = fFont;
        this.sText = sText;
       
        Font oldFont = g.getFont();
        g.setFont(fFont);
        frc = g.getFontRenderContext();
        
        // figure out number of lines we are drawing
        numLines = 1;
        for (int i = 0; i < sText.length(); i++)
        {
            if (sText.charAt(i) == SEP) numLines++;
        }
        metrics = g.getFont().getLineMetrics(A2Z, frc);
        lineHeight = (float) metrics.getDescent() + (metrics.getAscent() * .75f); // adjust for space above chars for accents and stuff
        lineSpacing = lineHeight * fLineSpacing;

        totalHeight = (lineHeight * numLines) + ((numLines - 1) * lineSpacing);
        //float starty = (float) (y - nTotalHeight/2) + nHeight; // add height once to adjust

        Rectangle2D stringbounds;
        width = 0.0f;

        // perf improvement - don't use tokenizer if not multiple lines
        if (numLines > 1) 
        {
            String sLine = null;
            StringTokenizer st = new StringTokenizer(sText, SEPS);
        
            while (st.hasMoreTokens())
            {
                sLine = st.nextToken();
                stringbounds = g.getFont().getStringBounds(sLine, frc);
                width = Math.max(width, (float)stringbounds.getWidth());
            }
        }
        else
        {
            stringbounds = g.getFont().getStringBounds(sText, frc);
            width = (float)stringbounds.getWidth();
        }
        
        g.setFont(oldFont);
    }
    
    /**
     * Prepare to draw string associated with TextMetrics - sets font
     * angle/size transforms and anti alias flag
     */
    public void prepareDraw(double x, double y, Integer nAngle, double dScale, boolean bAntiAlias)
    {
        this.x = x;
        this.y = y;
        this.bAntiAlias = bAntiAlias;
        
        // set font again
        g.setFont(fFont);
        
        // rotate as specified by angle
        old = g.getTransform();

        if (nAngle != null)
        {
            double nRadians = Math.toRadians(nAngle.intValue());
            g.transform(AffineTransform.getRotateInstance(nRadians, x, y));
        }

        // Scale font according to scale of board
        AffineTransform Tx = new AffineTransform();
        Tx.setToTranslation(x, y);
        Tx.scale(dScale, dScale);
        Tx.translate(-x, -y);
        g.transform(Tx);
        
        oldAlias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (bAntiAlias && (bAlwaysAntiAlias_ || GuiUtils.drawAntiAlias(fFont, dScale)))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        }
        else
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_OFF);
        }
    }
    
    /**
     * Draw the string
     */
    public void drawString(Color cForeground, Color cShadow)  
    {
        drawString(cForeground, cShadow, SwingConstants.CENTER);
    }
    
    /**
     * draw string
     */
    public void drawString(Color cForeground, Color cShadow, int nAlign)  
    {
        double starty;
        
        if (nAlign == SwingConstants.CENTER)
            starty = (float) (y - totalHeight/2) + lineHeight + yadjust; // add height once to adjust
        else
            starty = y + yadjust + lineHeight;

        boolean bDone = false;
        StringTokenizer st = null;
        String sLine = null;
        Rectangle2D stringbounds;
        
        // BUG 133 - optimize
        int nSep = sText.indexOf(SEP);
        if (nSep != -1)
        {
            st = new StringTokenizer(sText, SEPS);
        }
        else
        {
            sLine = sText;
        }
        
        // loop until done.  for string tokenizer, goes through
        // all tokens.  Otherwise, just does the text and done
        while (!bDone)
        {
            // perf - don't use string tokenizer if not necesary
            if (st != null)
            {
                sLine = st.nextToken();
            }
            
            stringbounds = g.getFont().getStringBounds(sLine, frc);
            double xx = (float) (x+xadjust);
            if (nAlign == SwingConstants.CENTER) xx -= (stringbounds.getWidth() / 2);
            else if (nAlign == SwingConstants.RIGHT) xx -= (stringbounds.getWidth());
            double yy = starty;

            // shadow (alpha makes it look etched)
            if (cShadow != null)
            {
                g.setColor(cShadow);
                g.drawString(sLine, (float)(xx+1), (float)(yy+1));
            }

            // text
            g.setColor(cForeground);
            g.drawString(sLine, (float)xx, (float)yy);
            
            // increment y for next line
            starty += lineHeight + lineSpacing;
            
            // if using a tokenizer, check if done
            if (st != null) bDone = !st.hasMoreTokens();
            else bDone = true;
        }
    }
    
    /**
     * Cleanup after drawing (reset antialias flag, transform
     */
    public void finishDraw()
    {
        // restore settings
        g.setTransform(old);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAlias);
    }

        // always anti alias?
    private boolean bAlwaysAntiAlias_ = false;

    /**
     * set whether anti aliases should always occur,
     * overriding GuiUtils.drawAntiAlias()
     */
    public void setAlwaysAntiAlias(boolean b)
    {
        bAlwaysAntiAlias_ = b;
    }

    /**
     * is GuiUtils.drawAntiAlias() overriden
     */
    public boolean isAlwaysAntiAlias()
    {
        return bAlwaysAntiAlias_;
    }
}
