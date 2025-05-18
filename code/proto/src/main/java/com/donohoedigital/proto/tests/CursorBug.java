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
 * CursorBug.java
 *
 * Created on November 10, 2002, 2:54 PM
 */

package com.donohoedigital.proto.tests;

import javax.swing.*;
import java.awt.*;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;

/**
 *
 * @author  Doug Donohoe
 */
public class CursorBug
{
    /**
     * cursor bug test
     */
    public static void main(String[] args) {
        if (args.length == 0)
        {
            System.out.println("Usage: java com.donohoedigital.proto.tests.CursorBug <filename.gif|filename.png>");
            System.exit(1);
        }
        
        fFile_ = new File(args[0]);
        try {
            CursorBug cursorbug = new CursorBug();
        }
        catch (Throwable ae)
        {
            System.out.println ("Ending due to Error: " + ae);
            System.exit(1);
        }  
    }
    
    
    /**
     * Create War from config file
     */
    public CursorBug()
    {
        JFrame frame = new JFrame();
        frame.setSize(400,400);
        frame.getContentPane().setBackground(Color.blue);
        frame.setVisible(true);
        
        Toolkit tk = frame.getToolkit();
        Dimension d = tk.getBestCursorSize(16,16);
        int colors = tk.getMaximumCursorColors();
        System.out.println("Best size: " + d);
        System.out.println("Cursor colors:" + colors);
        
        ImageIcon image = getImageIcon();
        Cursor cursor = tk.createCustomCursor(image.getImage(), 
                                            new Point(1, 1),
                                            "test");
        frame.getContentPane().setCursor(cursor);
    }
    
    /**
     * Get image icon (wrapped around buffered image)
     */
    public ImageIcon getImageIcon()
    {
        if (icon_ == null)
        {
            BufferedImage bimage = getBufferedImage();
            if (bimage != null)
            {
                icon_ = new ImageIcon(bimage);
            }
        }
        
        return icon_;
    }
    
    BufferedImage bimage_;
    ImageIcon icon_ = null;
    static File fFile_;
    /**
     * Get buffered image for this image
     */
    public BufferedImage getBufferedImage()
    {
        if (bimage_ == null)
        {
            try {
                bimage_ = ImageIO.read(fFile_);
            } catch (Exception e)
            
            
            {System.out.println("Error creating buffered image from " + fFile_.getAbsolutePath());
            }
        }
        return bimage_;
    }
}
