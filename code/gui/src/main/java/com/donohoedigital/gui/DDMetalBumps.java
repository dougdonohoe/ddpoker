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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Implements the bump pattern used by various widgets.
 *
 * <p>The pattern is one pixel on, one pixel off, which does not survive being upscaled by the
 * device transform: at the fractional scales Windows uses (125%, 150%, 175%) each dot lands on a
 * fractional device pixel and the texture turns into a soft, banded wash.  macOS only ever
 * reports a whole-number scale, so the same code looks fine there.  The tile is therefore built
 * at device resolution and blitted 1:1 - see {@link #paintIcon}.
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

    private BumpBuffer getBuffer(GraphicsConfiguration gc, double scale, Color aTopColor,
                                 Color aShadowColor, Color aBackColor) {
        if (buffer != null && buffer.hasSameConfiguration(gc, scale, aTopColor, aShadowColor, aBackColor)) {
            return buffer;
        }
        BumpBuffer result = null;

        Enumeration<BumpBuffer> elements = buffers.elements();

        while (elements.hasMoreElements()) {
            BumpBuffer aBuffer = elements.nextElement();
            if (aBuffer.hasSameConfiguration(gc, scale, aTopColor, aShadowColor, aBackColor)) {
                result = aBuffer;
                break;
            }
        }
        if (result == null) {
            result = new BumpBuffer(gc, scale, aTopColor, aShadowColor, aBackColor);
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
        Graphics2D g2 = (g instanceof Graphics2D) ? (Graphics2D) g : null;
        GraphicsConfiguration gc = (g2 == null) ? null : g2.getDeviceConfiguration();
        double scale = RenderUtils.getDeviceScale(g2);

        buffer = getBuffer(gc, scale, topColor, shadowColor, backColor);

        if (scale == 1.0d) {
            tile(g, x, y, getIconWidth(), getIconHeight());
            return;
        }

        // Paint in device space with the transform reset, so the device-resolution tile is
        // blitted one for one with no resampling.  Changing the transform does not disturb the
        // clip already set on g2.
        AffineTransform old = g2.getTransform();
        Point2D origin = old.transform(new Point2D.Double(x, y), null);
        Point2D corner = old.transform(new Point2D.Double(x + getIconWidth(), y + getIconHeight()), null);
        int devX = (int) Math.round(origin.getX());
        int devY = (int) Math.round(origin.getY());
        try {
            g2.setTransform(new AffineTransform());
            tile(g2, devX, devY,
                    (int) Math.round(corner.getX()) - devX,
                    (int) Math.round(corner.getY()) - devY);
        } finally {
            g2.setTransform(old);
        }
    }

    /** Tile the buffer over the given rectangle, in whatever space {@code g} is currently in. */
    private void tile(Graphics g, int x, int y, int width, int height) {
        int bufferWidth = buffer.getImageSize().width;
        int bufferHeight = buffer.getImageSize().height;
        int x2 = x + width;
        int y2 = y + height;
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

    /** Logical pixels covered by one tile.  The image itself is this times the device scale. */
    static final int IMAGE_SIZE = 64;

    transient Image image;
    Color topColor;
    Color shadowColor;
    Color backColor;
    private final GraphicsConfiguration gc;
    private final double scale;
    private final Dimension imageSize;

    public BumpBuffer(GraphicsConfiguration gc, double scale, Color aTopColor, Color aShadowColor, Color aBackColor) {
        this.gc = gc;
        this.scale = scale;
        topColor = aTopColor;
        shadowColor = aShadowColor;
        backColor = aBackColor;
        imageSize = new Dimension(dev(IMAGE_SIZE), dev(IMAGE_SIZE));
        createImage();
        fillBumpBuffer();
    }

    public boolean hasSameConfiguration(GraphicsConfiguration gc, double aScale,
                                        Color aTopColor, Color aShadowColor,
                                        Color aBackColor) {
        if (this.gc != null) {
            if (!this.gc.equals(gc)) {
                return false;
            }
        } else if (gc != null) {
            return false;
        }
        return scale == aScale &&
               topColor.equals(aTopColor) && shadowColor.equals(aShadowColor) && backColor.equals(aBackColor);
    }

    public Image getImage() {
        return image;
    }

    /** Size of the tile in device pixels. */
    public Dimension getImageSize() {
        return imageSize;
    }

    /** Device pixel position/size for a coordinate expressed in the pattern's logical pixels. */
    private int dev(int logical) {
        return (int) Math.round(logical * scale);
    }

    private void fillBumpBuffer() {
        Graphics g = image.getGraphics();

        // four dots have to fit within each 4-logical-pixel cell, so a dot can be no wider than
        // the scale factor rounded down (1 device pixel at 125%, 2 at 200%)
        int dot = Math.max(1, (int) Math.floor(scale));

        g.setColor(backColor);
        g.fillRect(0, 0, imageSize.width, imageSize.height);

        g.setColor(topColor);
        for (int x = 0; x < IMAGE_SIZE; x += 4) {
            for (int y = 0; y < IMAGE_SIZE; y += 4) {
                g.fillRect(dev(x), dev(y), dot, dot);
                g.fillRect(dev(x + 2), dev(y + 2), dot, dot);
            }
        }

        g.setColor(shadowColor);
        for (int x = 0; x < IMAGE_SIZE; x += 4) {
            for (int y = 0; y < IMAGE_SIZE; y += 4) {
                g.fillRect(dev(x + 1), dev(y + 1), dot, dot);
                g.fillRect(dev(x + 3), dev(y + 3), dot, dot);
            }
        }
        g.dispose();
    }

    private void createImage() {
        if (gc != null) {
            image = gc.createCompatibleImage(imageSize.width, imageSize.height);
        } else {
            int[] cmap = {backColor.getRGB(), topColor.getRGB(), shadowColor.getRGB()};
            IndexColorModel icm = new IndexColorModel(8, 3, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
            image = ImageDef.createBufferedImage(imageSize.width, imageSize.height, BufferedImage.TYPE_BYTE_INDEXED, icm);
        }
    }
}
