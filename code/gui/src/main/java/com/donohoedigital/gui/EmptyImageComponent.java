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
 * EmptyImageComponent.java
 *
 * Created on January 5, 2004, 2:19 PM
 */

package com.donohoedigital.gui;

import java.awt.*;
import java.awt.image.*;

/**
 * Class used to represent an empty image component - essentially
 * a shell used for width/height and sizing for other drawing
 * (see CardPiece in Poker for an example) 
 *
 * @author  donohoe
 */
public class EmptyImageComponent extends ImageComponent 
{
    private int nWidth_;
    private int nHeight_;
    
    /** 
     * Creates a new instance of EmptyImageComponent 
     */
    public EmptyImageComponent(int nWidth, int nHeight) 
    {
        nWidth_ = nWidth;
        nHeight_ = nHeight;
        init();
    }
    
    /**
     * Return height
     */
    public int getImageHeight()
    {
        return nHeight_;
    }
    
    /**
     * Return width
     */
    public int getImageWidth()
    {
        return nWidth_;
    }
    
    /**
     * Always returns true
     */
    public boolean isNonTransparent(int imagex, int imagey)
    {
        return true;
    }
    
    /**
     * Override to throw exception - not supported
     */
    public BufferedImage getImage()
    {
        throw new UnsupportedOperationException("getImage() not supported in EmptyImageComponent");
    }
    
    /**
     * Do nothing
     */
    public void drawImageAt(Graphics2D g, int x, int y, int width, int height)
    {
        // do nothing
    }
}
