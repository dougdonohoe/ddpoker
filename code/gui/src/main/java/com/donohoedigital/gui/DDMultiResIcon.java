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

import com.donohoedigital.config.ImageConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An icon that keeps higher-resolution copies of the same artwork and paints whichever one best
 * matches the display.  A single small bitmap drawn 1:1 in logical pixels is upscaled by the
 * device transform on a scaled display (Windows at 125%/150%, HiDPI), which is what makes such
 * icons look soft; here the artwork is resampled once to the exact device size and drawn with
 * the transform reset, so no scaling happens at paint time.
 *
 * <p>Variants are declared in images.xml next to the base image, using the same
 * <code>{name}.{pixel size}</code> convention {@link BaseFrame} uses for window icons:
 *
 * <pre>
 *   &lt;image name="window-title-icon"    location="icon_20x20.png"/&gt;
 *   &lt;image name="window-title-icon.32" location="icon_32x32.png"/&gt;
 * </pre>
 *
 * <p>The icon reports the base image's size as its size, so layout is unchanged - the variants
 * only affect how many real pixels are used to draw it.  Missing variants are skipped, so this
 * degrades to the base image alone.
 *
 * @author Doug Donohoe
 */
public class DDMultiResIcon implements Icon
{
    /**
     * Variant sizes looked for in images.xml.  Covers the common scales for the ~20px artwork
     * this is used for: 125% needs 25 device pixels, 150% 30, 200% 40, 300% 60.
     */
    private static final int[] VARIANT_SIZES = {32, 48, 64, 128};

    private final Image base_;
    private final int width_;
    private final int height_;

    /** source artwork by pixel width, smallest first */
    private final TreeMap<Integer, Image> variants_ = new TreeMap<>();

    /** artwork resampled to an exact device width, built on demand */
    private final Map<Integer, Image> rendered_ = new HashMap<>();

    /**
     * Load {@code name} from images.xml along with any '{name}.{size}' variants.
     *
     * <p>Returns null when the base image is not defined, matching what
     * {@link ImageConfig#getImageIcon(String, ImageIcon)} does.  Callers rely on that: the
     * dialog phases look up {@code getString("dialog-windowtitle-image", "dialog-windowtitle-image")},
     * so a phase that does not set the param resolves to a name no images.xml defines and is
     * expected to end up with no icon rather than an error.
     */
    public static Icon load(String name)
    {
        ImageIcon base = ImageConfig.getImageIcon(name, null);
        if (base == null) return null;

        DDMultiResIcon icon = new DDMultiResIcon(base.getImage());

        for (int size : VARIANT_SIZES)
        {
            ImageIcon variant = ImageConfig.getImageIcon(name + "." + size, null);
            if (variant != null) icon.addVariant(variant.getImage());
        }
        return icon;
    }

    private DDMultiResIcon(Image base)
    {
        base_ = base;
        width_ = base.getWidth(null);
        height_ = base.getHeight(null);
        addVariant(base);
    }

    private void addVariant(Image image)
    {
        variants_.put(image.getWidth(null), image);
    }

    public int getIconWidth()
    {
        return width_;
    }

    public int getIconHeight()
    {
        return height_;
    }

    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        Graphics2D g2 = (g instanceof Graphics2D) ? (Graphics2D) g : null;
        double scale = RenderUtils.getDeviceScale(g2);

        if (scale == 1.0d)
        {
            g.drawImage(base_, x, y, width_, height_, null);
            return;
        }

        // paint in device space so the artwork is drawn 1:1, at a whole-pixel origin
        AffineTransform old = g2.getTransform();
        Point2D origin = old.transform(new Point2D.Double(x, y), null);
        Point2D corner = old.transform(new Point2D.Double(x + width_, y + height_), null);
        int devX = (int) Math.round(origin.getX());
        int devY = (int) Math.round(origin.getY());

        Image image = renderedFor((int) Math.round(corner.getX()) - devX,
                                  (int) Math.round(corner.getY()) - devY);
        try
        {
            g2.setTransform(new AffineTransform());
            g2.drawImage(image, devX, devY, null);
        }
        finally
        {
            g2.setTransform(old);
        }
    }

    /**
     * The artwork at exactly this many device pixels, resampled once from the smallest variant
     * big enough to cover it (falling back to the largest available) and cached thereafter.
     */
    private Image renderedFor(int devWidth, int devHeight)
    {
        Image cached = rendered_.get(devWidth);
        if (cached != null) return cached;

        Map.Entry<Integer, Image> entry = variants_.ceilingEntry(devWidth);
        Image source = (entry != null) ? entry.getValue() : variants_.lastEntry().getValue();

        Image scaled;
        if (source.getWidth(null) == devWidth && source.getHeight(null) == devHeight)
        {
            scaled = source;
        }
        else
        {
            BufferedImage buffer = new BufferedImage(devWidth, devHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = buffer.createGraphics();
            RenderUtils.applyInterpolation(bg, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            bg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.drawImage(source, 0, 0, devWidth, devHeight, null);
            bg.dispose();
            scaled = buffer;
        }

        rendered_.put(devWidth, scaled);
        return scaled;
    }
}
