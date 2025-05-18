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
 * Cursors.java
 *
 * Created on October 29, 2002, 9:07 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import java.awt.*;


/**
 *
 * @author  Doug Donohoe
 */
public class Cursors {
    
    //static Logger logger = LogManager.getLogger(Cursors.class);
    
    public static final Cursor HAND = createCursor("cursor-hand");
    public static final Cursor MOVE = createCursor("cursor-move");
    public static final Cursor DELETE = createCursor("cursor-delete");
    public static final Cursor SELECT = createCursor("cursor-select");
    public static final Cursor EMPTY = createCursor("cursor-empty");
    public static final Cursor CROSSHAIR = createCursor("cursor-xhair");
    
    public static final Cursor ALLOWED = createCursor("cursor-allowed");
    public static final Cursor PROHIBITED = createCursor("cursor-prohibited"); 
    public static final Cursor MOVEALLOWED = createCursor("cursor-move-allowed");
    public static final Cursor MOVEPROHIBITED = createCursor("cursor-move-prohibited");
    
    public static final Cursor SCROLLUP = createCursor("cursor-scroll-up");
    public static final Cursor SCROLLDOWN = createCursor("cursor-scroll-down");
    public static final Cursor SCROLLRIGHT = createCursor("cursor-scroll-right");
    public static final Cursor SCROLLLEFT = createCursor("cursor-scroll-left");

    public static final Cursor SCROLLUPLEFT = createCursor("cursor-scroll-up-left");
    public static final Cursor SCROLLUPRIGHT = createCursor("cursor-scroll-up-right");
    public static final Cursor SCROLLDOWNLEFT = createCursor("cursor-scroll-down-left");
    public static final Cursor SCROLLDOWNRIGHT = createCursor("cursor-scroll-down-right");
    
    public static final String CURSOR_SCROLL_NAME_START = "cursor-scroll";

    //Old - used in WaoI probably - retain incase needed again
    //public static final int CURSOR_WIDTH =  (Utils.ISMAC) ? 16 : 32;
    //public static final int CURSOR_HEIGHT = (Utils.ISMAC) ? 16 : 32;
    
    /**
     * Create appropriate cursor
     */
    private static Cursor createCursor(String sName)
    {
        if (Utils.ISMAC && Utils.IS14) sName += "-mac";
        return ImageConfig.createCursor(sName);
    }
}
