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
 * DieImage.java
 *
 * Created on November 23, 2002, 6:24 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.gui.*;
import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.text.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DieImage extends JPanel {
    
    int nPip_;
    Dimension dieSize_;
    
    /** Creates a new instance of DieImage */
    public DieImage(int nPip, int nWidth, int nHeight) {
        nPip_ = nPip;
        dieSize_ = new Dimension(nWidth, nHeight);
        setSize(new Dimension(nWidth, nHeight));
        setPreferredSize(new Dimension(nWidth, nHeight));
    }

    Color dieColor_ = Color.red;
    Color pipColor_ = Color.white;
    
    protected void paintComponent(Graphics g1) 
    {
        Dimension size = getSize();
        Graphics2D g = (Graphics2D) g1;
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                        RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(dieColor_);
        g.fillRoundRect(0, 0, size.width-1, size.height-1, size.width/10,size.width/10);
        g.setColor(dieColor_.darker());
        g.drawRoundRect(0, 0, size.width-1, size.height-1, size.width/10,size.width/10);
        
        switch(nPip_)
        {
            case 1:
                drawMiddle(g, size);
                break;
                
            case 2:
                drawUpperLeft(g, size);
                drawLowerRight(g, size);
                break;
                
            case 3:
                drawUpperLeft(g,size);
                drawMiddle(g, size);
                drawLowerRight(g,size);
                break;
                
            case 4:
                drawUpperLeft(g,size);
                drawUpperRight(g,size);
                drawLowerLeft(g,size);
                drawLowerRight(g,size);
                break;
                
            case 5:
                drawUpperLeft(g,size);
                drawUpperRight(g,size);
                drawLowerLeft(g,size);
                drawLowerRight(g,size);
                drawMiddle(g,size);
                break;
                
            case 6:
                drawUpperLeft(g,size);
                drawMiddleLeft(g, size);
                drawUpperRight(g,size);
                drawLowerLeft(g,size);
                drawMiddleRight(g,size);
                drawLowerRight(g,size);
                break;
        }
    }
        
    private double PIP = 3.3f/16f;
    private double PIPSIZE = 3.2f/16f;
    private void drawUpperLeft(Graphics2D g, Dimension size)
    {
        double x = size.width * PIP;
        double y = size.height * PIP;
        drawPip(g, x, y, size);
    }
    
    private void drawMiddleLeft(Graphics2D g, Dimension size)
    {
        double x = size.width * PIP;
        double y = size.height / 2;
        drawPip(g, x, y, size);
    }
    
    private void drawLowerLeft(Graphics2D g, Dimension size)
    {
        double x = size.width * PIP;
        double y = size.height - (size.height * PIP) - 0;
        drawPip(g, x, y, size);
    }    

    private void drawMiddle(Graphics2D g, Dimension size)
    {
        double x = size.width / 2;
        double y = size.height / 2;
        drawPip(g, x, y, size);
    }    
    
    private void drawUpperRight(Graphics2D g, Dimension size)
    {
        double x = size.width - (size.width * PIP) - 0;
        double y = size.height * PIP;
        drawPip(g, x, y, size);
    }
    
    private void drawMiddleRight(Graphics2D g, Dimension size)
    {
        double x = size.width - (size.width * PIP) - 0;
        double y = size.height / 2;
        drawPip(g, x, y, size);
    }
    
    private void drawLowerRight(Graphics2D g, Dimension size)
    {
        double x = size.width - (size.width * PIP) - 0;
        double y = size.height - (size.height * PIP) - 0;
        drawPip(g, x, y, size);
    }    
    
    private void drawPip(Graphics2D g, double x, double y, Dimension size)
    {
        x -= (PIPSIZE * size.getWidth())/2;
        y -= (PIPSIZE * size.getHeight())/2;
        g.setColor(pipColor_);
        //Ellipse2D.Double pip = new Ellipse2D.Double(x,y,(int)(size.getWidth() * PIPSIZE), (int)(size.getHeight() * PIPSIZE));
        g.fillOval((int)x,(int)y, (int)(size.getWidth() * PIPSIZE), (int)(size.getHeight() * PIPSIZE));
        //g.fill(pip);
        
        //g.setColor(pipColor_.darker());
        //g.drawOval((int)x,(int)y, (int)(size.getWidth() * PIPSIZE), (int)(size.getHeight() * PIPSIZE));
    }
}
