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
/**
 * This test takes a number up to 13 as an argument (assumes 2 by
 * default) and creates a multiple buffer strategy with the number of
 * buffers given.  This application enters full-screen mode, if available,
 * and flips back and forth between each buffer (each signified by a different
 * color).
 */

package com.donohoedigital.proto.tests;

import java.awt.*;
import java.awt.image.BufferStrategy;

public class MultiBufferTest {
    
    private boolean bFullScreen = false;
    
    private static Color[] COLORS = new Color[] {
        Color.red, Color.blue, Color.green, Color.white, Color.black,
        Color.yellow, Color.gray, Color.cyan, Color.pink, Color.lightGray,
        Color.magenta, Color.orange, Color.darkGray };
    private static DisplayMode[] BEST_DISPLAY_MODES = new DisplayMode[] {
        new DisplayMode(640, 480, 32, 0),
        new DisplayMode(640, 480, 16, 0),
        new DisplayMode(640, 480, 8, 0)
    };
    
    Frame mainFrame;
    
    public MultiBufferTest(int numBuffers, GraphicsDevice device) {
        try {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            mainFrame = new Frame(gc);
            if (bFullScreen)
            {
                mainFrame.setUndecorated(true);
                mainFrame.setIgnoreRepaint(true);
                device.setFullScreenWindow(mainFrame);
                if (device.isDisplayChangeSupported()) {
                    chooseBestDisplayMode(device);
                }
            }
            else
            {
                mainFrame.setSize(500,500);
                mainFrame.setLocation(200,200);
                mainFrame.setVisible(true);
            }
            Rectangle bounds = mainFrame.getBounds();
            mainFrame.createBufferStrategy(numBuffers);
            BufferStrategy bufferStrategy = mainFrame.getBufferStrategy();
            for (float lag = 1000.0f; lag > 0.00000006f; lag = lag / 1.33f) {
                for (int i = 0; i < numBuffers; i++) {
                    Graphics g = bufferStrategy.getDrawGraphics();
                    if (!bufferStrategy.contentsLost()) {
                        g.setColor(COLORS[i]);
                        g.fillRect(0,0,bounds.width, bounds.height);
                        bufferStrategy.show();
                        g.dispose();
                    }
                    try {
                        Thread.sleep((int)lag);
                    } catch (InterruptedException e) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            device.setFullScreenWindow(null);
        }
    }
    
    private static DisplayMode getBestDisplayMode(GraphicsDevice device) {
        for (int x = 0; x < BEST_DISPLAY_MODES.length; x++) {
            DisplayMode[] modes = device.getDisplayModes();
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].getWidth() == BEST_DISPLAY_MODES[x].getWidth()
                   && modes[i].getHeight() == BEST_DISPLAY_MODES[x].getHeight()
                   && modes[i].getBitDepth() == BEST_DISPLAY_MODES[x].getBitDepth()
                   ) {
                    return BEST_DISPLAY_MODES[x];
                }
            }
        }
        return null;
    }
    
    public static void chooseBestDisplayMode(GraphicsDevice device) {
        DisplayMode best = getBestDisplayMode(device);
        if (best != null) {
            device.setDisplayMode(best);
        }
    }
    
    public static void main(String[] args) {
        try {
            int numBuffers = 3;
            if (args != null && args.length > 0) {
                numBuffers = Integer.parseInt(args[0]);
                if (numBuffers < 2 || numBuffers > COLORS.length) {
                    System.err.println("Must specify between 2 and "
                        + COLORS.length + " buffers");
                    System.exit(1);
                }
            }
            GraphicsEnvironment env = GraphicsEnvironment.
                getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            MultiBufferTest test = new MultiBufferTest(numBuffers, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
