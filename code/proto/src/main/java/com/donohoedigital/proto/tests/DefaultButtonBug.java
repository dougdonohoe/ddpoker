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
 * DefaultButtonBug.java
 *
 * Created on February 23, 2003, 9:24 AM
 */

package com.donohoedigital.proto.tests;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;


/**
 *
 * @author  Doug Donohoe
 */
public class DefaultButtonBug implements ActionListener
{
    /**
     * default button bug test
     */
    public static void main(String[] args) {
        try {
            DefaultButtonBug cursorbug = new DefaultButtonBug();
        }
        catch (Throwable ae)
        {
            System.out.println ("Ending due to Error: " + ae);
            System.exit(1);
        }  
    }
    
    
    /**
     * Create ui
     */
    public DefaultButtonBug()
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200,200);
        frame.setLocation(300,300);
        
        JPanel base = new JPanel();
        base.setLayout(new BorderLayout());
        
        JButton button = new JButton("Test Button");
        button.addActionListener(this);
        base.add(button, BorderLayout.SOUTH);
        base.setRequestFocusEnabled(true);
        

        frame.setContentPane(base); 
        frame.getRootPane().setDefaultButton(button);
        
        
        frame.setVisible(true);
        
        System.out.println("Focus set to base JPanel");
        base.requestFocus();
        
    }
    
    public void actionPerformed(ActionEvent e)
    {
        System.out.println("Button pressed");
    }
}
