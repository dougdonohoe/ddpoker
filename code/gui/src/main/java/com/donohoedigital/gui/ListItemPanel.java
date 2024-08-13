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

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.*;

public abstract class ListItemPanel extends ButtonPanel implements Comparable<ListItemPanel>
{
    protected ListPanel listPanel_;
    protected Object item_;

    public ListItemPanel(ListPanel panel, Object item, String sStyle)
    {
        super(sStyle);

        item_ = item;
        listPanel_ = panel;
        // use protected border
        bNormal_ = borderProtected_;
        bMouseDown_ = borderProtected_;
        setBorder(bNormal_);
    }

    public ListPanel getListPanel()
    {
        return listPanel_;
    }

    public void setItem(Object item)
    {
        item_ = item;
    }

    /**
     * Update text
     */
    public void update()
    {
        repaint();
    }

    public Object getItem()
    {
        return item_;
    }

    public int getIndex()
    {
        return listPanel_.getItemIndex(item_);
    }

    public void setIcon(ImageIcon icon)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
        if ((listPanel_ != null) && isEnabled()) listPanel_.setSelectedItem(item_);
    }

    public int compareTo(ListItemPanel panel)
    {
        return item_.hashCode() - panel.item_.hashCode();
    }

    protected void addImpl(Component comp, Object constraints, int index)
    {
        super.addImpl(comp, constraints, index);
        addMouseListeners(comp);
    }

    protected void addMouseListeners(Component comp)
    {
        if (comp instanceof Container)
        {
            GuiUtils.addMouseListenerChildren((Container)comp, this);
        }
        comp.addMouseListener(this);
    }

    public String getHelpText()
    {
        return "";
    }

    public void mouseEntered(MouseEvent e)
    {
        super.mouseEntered(e);

        if (listPanel_ != null) listPanel_.updateHelpText(this);
    }

    public void mouseExited(MouseEvent e)
    {
        super.mouseExited(e);

        if (listPanel_ != null) listPanel_.updateHelpText(null);
    }
}
