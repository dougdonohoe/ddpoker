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
package com.donohoedigital.gui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

class RoundGradientContext
    implements PaintContext {
  protected Point2D mPoint;
  protected double mRadius;
  protected Color mC1, mC2;
  
  public RoundGradientContext(Point2D p, Color c1, double r, Color c2) {
    mPoint = p;
    mC1 = c1;
    mRadius = r;
    mC2 = c2;
  }
  
  public void dispose() {}
  
  public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }
  
  public Raster getRaster(int x, int y, int w, int h) {
    WritableRaster raster =
        getColorModel().createCompatibleWritableRaster(w, h);
    
    int[] data = new int[w * h * 4];
    for (int j = 0; j < h; j++) {
      for (int i = 0; i < w; i++) {
        double distance = mPoint.distance(x + i, y + j);
        double ratio = distance / mRadius;
        if (ratio > 1.0)
          ratio = 1.0;
      
        int base = (j * w + i) * 4;
        data[base + 0] = (int)(mC1.getRed() + ratio *
            (mC2.getRed() - mC1.getRed()));
        data[base + 1] = (int)(mC1.getGreen() + ratio *
            (mC2.getGreen() - mC1.getGreen()));
        data[base + 2] = (int)(mC1.getBlue() + ratio *
            (mC2.getBlue() - mC1.getBlue()));
        data[base + 3] = (int)(mC1.getAlpha() + ratio *
            (mC2.getAlpha() - mC1.getAlpha()));
      }
    }
    raster.setPixels(0, 0, w, h, data);
    
    return raster;
  }
}
