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
 * ItalicBug.java
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
public class ItalicBug
{
    /**
     * cursor bug test
     */
    public static void main(String[] args) {
        
        try {
            ItalicBug cursorbug = new ItalicBug();
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
    public ItalicBug()
    {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        panel.setLayout(new BorderLayout());
        
        JLabel label = new JLabel("Lucida Sans Regular - italic - 32");
        Font newfont = new Font("Lucida Sans Regular",  Font.ITALIC, 32); 
        label.setFont(newfont);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(Color.yellow);
        label.setBackground(Color.black);
        label.setOpaque(true);
        panel.add(label, BorderLayout.NORTH);
        
        JLabel label2 = new JLabel("Bogus Name - italic - 32");
        Font newfont2 = new Font("Bogus Name",  Font.ITALIC, 32); 
        label2.setFont(newfont2);
        label2.setHorizontalAlignment(SwingConstants.CENTER);
        label2.setForeground(Color.yellow);
        label2.setBackground(Color.black);
        label2.setOpaque(true);
        panel.add(label2, BorderLayout.CENTER);       
        
        frame.setContentPane(panel);
        frame.setSize(600,200);
        frame.setLocation(200,200);
        frame.setVisible(true);
        
    }
}
