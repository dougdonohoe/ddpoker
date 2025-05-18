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

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2005
 * Time: 8:31:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDScrollPane extends JScrollPane implements DDComponent
{
    public static final int GAP = 2;

    public DDScrollPane(Component view, String sStyle, String bevelStyle, int nVerticalPolicy, int nHorizPolicy)
    {
        super(view);
        setOpaque(false);
        getDDHorizontalScrollBar().setStyle(sStyle);
        getDDVerticalScrollBar().setStyle(sStyle);
        setHorizontalScrollBarPolicy(nHorizPolicy);
        setVerticalScrollBarPolicy(nVerticalPolicy);
        setBorder(BorderFactory.createEmptyBorder());
        int nBottomGap = nHorizPolicy == HORIZONTAL_SCROLLBAR_NEVER ? 0 : GAP;
        int nRightGap = nVerticalPolicy == VERTICAL_SCROLLBAR_NEVER ? 0 : GAP;


        Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, nBottomGap, nRightGap);
        if (bevelStyle != null)
        {
            setViewportBorder(BorderFactory.createCompoundBorder(
                                emptyBorder,
                                new DDBevelBorder(bevelStyle, DDBevelBorder.LOWERED)));
        }
        else
        {
            setViewportBorder(emptyBorder);
        }
        GuiManager.init(this, GuiManager.DEFAULT, sStyle);
    }

    public void setOpaque(boolean b)
    {
        super.setOpaque(b);
        getViewport().setOpaque(b);
    }

    public DDScrollBar getDDHorizontalScrollBar()
    {
        return (DDScrollBar)getHorizontalScrollBar();
    }

    public DDScrollBar getDDVerticalScrollBar()
    {
        return (DDScrollBar)getVerticalScrollBar();
    }

    public JScrollBar createHorizontalScrollBar()
    {
        return new DDScrollBar(JScrollBar.HORIZONTAL);
    }

    public JScrollBar createVerticalScrollBar()
    {
        return new DDScrollBar(JScrollBar.VERTICAL);
    }

    public String getType()
    {
        return "scrollpane";
    }

    public boolean isUserScrolling()
    {
        return getDDHorizontalScrollBar().isUserScrolling() || getDDVerticalScrollBar().isUserScrolling();
    }

//    protected JViewport createViewport() {
//        return new MyViewport();
//    }
//
//    public class MyViewport extends JViewport
//    {
//        public void setViewPosition(Point p)
//        {
//            System.out.println("setViewPosition called: " + Utils.formatExceptionText(new Throwable()));
//
//            super.setViewPosition(p);
//        }
//    }
}
