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
/*
 * DDScrollTable.java
 *
 * Created on February 4, 2003, 2:49 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;


/**
 *
 * @author  Doug Donohoe
 */
public class DDScrollTable extends DDScrollPane
{
    DDTable table_;
    int columnWidths_[];

    /**
     * Creates a new instance of DDScrollTable - with a DDTable inside.  The
     * background of the scrollpane is set to that of the table.
     */
    public DDScrollTable(String sName, String sStyle, String sBevelStyle, String[] columnnames, int[] columnwidths)
    {
        super(null, sStyle, sBevelStyle, DDScrollPane.VERTICAL_SCROLLBAR_ALWAYS, DDScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        columnWidths_ = columnwidths;
        table_ = new DDTable(sName, sStyle, columnnames, columnwidths);
        table_.setFocusable(false);
        addMouseListener(table_);
        setViewportView(table_);
        getViewport().setBackground(table_.getBackground());
        setBackground(table_.getBackground());
        setForeground(table_.getBackground().brighter());
        setColumnHeader(new DDColumnHeaderView());
        getVerticalScrollBar().setBlockIncrement(50);
        getVerticalScrollBar().setUnitIncrement(12);
    }

    /**
     * Return DDTable use in this scrollpane
     */
    public DDTable getDDTable()
    {
        return table_;
    }

    /**
     *  set table opaque too
     */
    public void setOpaque(boolean b)
    {
        super.setOpaque(b);
        if (table_ != null) table_.setOpaque(b);
    }

    /**
     * return preferred width of table, including
     * scrollbar and border insets
     */
    public int getPreferredWidth()
    {
        int nWidth = getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER ? 0 :
                     getVerticalScrollBar().getPreferredSize().width; // add some for scrollbar
        for (int i = 0; i < columnWidths_.length; i++)
        {
            nWidth += columnWidths_[i];
        }
        Insets insets = getInsets();
        nWidth += (insets.left + insets.right);
        insets = getViewportBorder().getBorderInsets(getViewport());
        nWidth += (insets.left + insets.right);

        return nWidth;
    }

    /**
     * our own class to tweak column header view due to
     * our custom scrollbar.  This shifts headers over
     * so the header gaps line up with the table lines;
     * and adjusts width to avoid a gray blank spot
     * on the right of the header.
     */
    private class DDColumnHeaderView extends JViewport
    {
        public void setBounds(int x, int y, int width, int height)
        {
            x+=2; // shift over to align lines
            width = getViewport().getView().getWidth()-1; // fix width
            super.setBounds(x, y, width, height);
        }
    }
}
