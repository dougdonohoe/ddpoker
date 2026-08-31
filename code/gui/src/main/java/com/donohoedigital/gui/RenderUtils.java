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

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * Rendering-hint helpers.
 *
 * <p>Deliberately free of static state: these are called from {@link ImageComponent#paintComponent},
 * which paints the splash screen before config files are loaded.  Anything with a static initializer
 * that touches StylesConfig/PropertyConfig (such as {@link GuiUtils}) blows up if class-loaded that
 * early, so these cannot live there.
 *
 * @author Doug Donohoe
 */
public final class RenderUtils
{
    private RenderUtils() {}

    /**
     * Apply an interpolation hint, returning the previous value (possibly null) so the caller
     * can restore it with {@link #restoreInterpolation}.  Java 2D defaults to nearest-neighbor,
     * which visibly stair-steps images whenever they are drawn at a size other than their
     * natural size -- including on scaled displays (Windows at 125%/150%, HiDPI), where the
     * device transform scales even images drawn 1:1 in logical pixels.
     */
    public static Object applyInterpolation(Graphics2D g, Object interpolation)
    {
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        return old;
    }

    /**
     * Restore the interpolation hint saved by {@link #applyInterpolation}.  A null old value
     * means the hint was unset, which Java 2D treats as nearest-neighbor.
     */
    public static void restoreInterpolation(Graphics2D g, Object old)
    {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           old == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : old);
    }

    /**
     * Device pixels per logical pixel for {@code g2}'s current transform.  Returns 1.0 for
     * anything other than a plain uniform scale-and-translate, since only that case can be
     * painted in device space by resetting the transform and drawing 1:1.
     *
     * <p>Use this rather than {@link #getDisplayScale} when the scale is needed during a paint:
     * it reflects the transform actually in effect, which may not be the screen's (printing, an
     * off-screen buffer, a component painted into an image).
     */
    public static double getDeviceScale(Graphics2D g2)
    {
        if (g2 == null) return 1.0d;

        AffineTransform tx = g2.getTransform();
        if (tx.getShearX() != 0.0d || tx.getShearY() != 0.0d) return 1.0d;

        double scale = tx.getScaleX();
        return (scale > 0.0d && scale == tx.getScaleY()) ? scale : 1.0d;
    }

    /**
     * Device pixels per logical pixel for the screen showing {@code c} (1.0 on an unscaled
     * display, 1.25 for Windows at 125%, 2.0 for a Retina-class screen).  Falls back to the
     * default screen when {@code c} is null or not yet realized, and to 1.0 in a headless
     * environment.
     *
     * <p>Multiply by this when generating a bitmap that will be drawn into a logical-pixel box
     * (thumbnails, scaled artwork): generating at logical size and letting the device transform
     * upscale is what makes such images look soft.
     */
    public static double getDisplayScale(Component c)
    {
        try
        {
            GraphicsConfiguration gc = (c == null) ? null : c.getGraphicsConfiguration();
            if (gc == null)
            {
                if (GraphicsEnvironment.isHeadless()) return 1.0d;
                gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                        .getDefaultScreenDevice().getDefaultConfiguration();
            }
            double scale = gc.getDefaultTransform().getScaleX();
            return (scale > 0) ? scale : 1.0d;
        }
        catch (Exception e)
        {
            return 1.0d;
        }
    }
}
