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
package com.donohoedigital.gui;

import com.donohoedigital.config.ImageDef;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Implements the bump pattern used by various widgets
 */
public class DDMetalBumps implements Icon {

    protected int xBumps;
    protected int yBumps;
    protected Color topColor;
    protected Color shadowColor;
    protected Color backColor;

    static Vector<BumpBuffer> buffers = new Vector<>();
    BumpBuffer buffer;

    public DDMetalBumps(int width, int height,
                        Color newTopColor, Color newShadowColor, Color newBackColor) {
        setBumpArea(width, height);
        setBumpColors(newTopColor, newShadowColor, newBackColor);
    }

    private BumpBuffer getBuffer(GraphicsConfiguration gc, Color aTopColor,
                                 Color aShadowColor, Color aBackColor) {
        if (buffer != null && buffer.hasSameConfiguration(gc, aTopColor, aShadowColor, aBackColor)) {
            return buffer;
        }
        BumpBuffer result = null;

        Enumeration<BumpBuffer> elements = buffers.elements();

        while (elements.hasMoreElements()) {
            BumpBuffer aBuffer = elements.nextElement();
            if (aBuffer.hasSameConfiguration(gc, aTopColor, aShadowColor, aBackColor)) {
                result = aBuffer;
                break;
            }
        }
        if (result == null) {
            result = new BumpBuffer(gc, topColor, shadowColor, backColor);
            buffers.addElement(result);
        }
        return result;
    }

    public void setBumpArea(Dimension bumpArea) {
        setBumpArea(bumpArea.width, bumpArea.height);
    }

    public void setBumpArea(int width, int height) {
        xBumps = width / 2;
        yBumps = height / 2;
    }

    public void setBumpColors(Color newTopColor, Color newShadowColor, Color newBackColor) {
        topColor = newTopColor;
        shadowColor = newShadowColor;
        backColor = newBackColor;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        GraphicsConfiguration gc = (g instanceof Graphics2D) ? ((Graphics2D) g).getDeviceConfiguration() : null;

        buffer = getBuffer(gc, topColor, shadowColor, backColor);

        int bufferWidth = buffer.getImageSize().width;
        int bufferHeight = buffer.getImageSize().height;
        int iconWidth = getIconWidth();
        int iconHeight = getIconHeight();
        int x2 = x + iconWidth;
        int y2 = y + iconHeight;
        int savex = x;

        while (y < y2) {
            int h = Math.min(y2 - y, bufferHeight);
            for (x = savex; x < x2; x += bufferWidth) {
                int w = Math.min(x2 - x, bufferWidth);
                g.drawImage(buffer.getImage(),
                        x, y, x + w, y + h,
                        0, 0, w, h,
                        null);
            }
            y += bufferHeight;
        }
    }

    public int getIconWidth() {
        return xBumps * 2;
    }

    public int getIconHeight() {
        return yBumps * 2;
    }
}


class BumpBuffer {

    static final int IMAGE_SIZE = 64;
    static Dimension imageSize = new Dimension(IMAGE_SIZE, IMAGE_SIZE);

    transient Image image;
    Color topColor;
    Color shadowColor;
    Color backColor;
    private final GraphicsConfiguration gc;

    public BumpBuffer(GraphicsConfiguration gc, Color aTopColor, Color aShadowColor, Color aBackColor) {
        this.gc = gc;
        topColor = aTopColor;
        shadowColor = aShadowColor;
        backColor = aBackColor;
        createImage();
        fillBumpBuffer();
    }

    public boolean hasSameConfiguration(GraphicsConfiguration gc,
                                        Color aTopColor, Color aShadowColor,
                                        Color aBackColor) {
        if (this.gc != null) {
            if (!this.gc.equals(gc)) {
                return false;
            }
        } else if (gc != null) {
            return false;
        }
        return topColor.equals(aTopColor) && shadowColor.equals(aShadowColor) && backColor.equals(aBackColor);
    }

    public Image getImage() {
        return image;
    }

    public Dimension getImageSize() {
        return imageSize;
    }

    private void fillBumpBuffer() {
        Graphics g = image.getGraphics();

        g.setColor(backColor);
        g.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);

        g.setColor(topColor);
        for (int x = 0; x < IMAGE_SIZE; x += 4) {
            for (int y = 0; y < IMAGE_SIZE; y += 4) {
                g.drawLine(x, y, x, y);
                g.drawLine(x + 2, y + 2, x + 2, y + 2);
            }
        }

        g.setColor(shadowColor);
        for (int x = 0; x < IMAGE_SIZE; x += 4) {
            for (int y = 0; y < IMAGE_SIZE; y += 4) {
                g.drawLine(x + 1, y + 1, x + 1, y + 1);
                g.drawLine(x + 3, y + 3, x + 3, y + 3);
            }
        }
        g.dispose();
    }

    private void createImage() {
        if (gc != null) {
            image = gc.createCompatibleImage(IMAGE_SIZE, IMAGE_SIZE);
        } else {
            int[] cmap = {backColor.getRGB(), topColor.getRGB(), shadowColor.getRGB()};
            IndexColorModel icm = new IndexColorModel(8, 3, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
            image = ImageDef.createBufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_BYTE_INDEXED, icm);
        }
    }
}
