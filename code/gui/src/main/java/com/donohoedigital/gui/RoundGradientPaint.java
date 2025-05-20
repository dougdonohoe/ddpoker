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
package com.donohoedigital.gui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.ColorModel;

public class RoundGradientPaint
    implements Paint {
  protected Point2D mPoint;
  protected double mRadius;
  protected Color mPointColor, mBackgroundColor;
  
  public RoundGradientPaint(double x, double y, Color pointColor,
      double radius, Color backgroundColor) {
    mPoint = new Point2D.Double(x, y);
    mPointColor = pointColor;
    mRadius = radius;
    mBackgroundColor = backgroundColor;
  }
  
  public PaintContext createContext(ColorModel cm,
      Rectangle deviceBounds, Rectangle2D userBounds,
      AffineTransform xform, RenderingHints hints) {
    Point2D transformedPoint = xform.transform(mPoint, null);
    return new RoundGradientContext(transformedPoint, mPointColor,
        mRadius, mBackgroundColor);
  }
  
  public int getTransparency() {
    int a1 = mPointColor.getAlpha();
    int a2 = mBackgroundColor.getAlpha();
    return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
  }
}
