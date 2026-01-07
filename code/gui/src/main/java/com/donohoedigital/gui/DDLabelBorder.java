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
/*
 * DDLabelBorder.java
 *
 * Created on April 17, 2002, 3:47 PM
 */

package com.donohoedigital.gui;


import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDLabelBorder extends JPanel implements DDHasLabelComponent 
{
    TitledBorder border_;
    String sText_;
    
    /** 
     * Creates a new instance of DDLabelBorder 
     */
    public DDLabelBorder() {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    public DDLabelBorder(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    public DDLabelBorder(String sName, String sStyle)
    {
        super();
        init(sName, sStyle);
    }

    /**
     * Adjust border layout.  Will throw ClassCastException if
     * this panel is using another layout
     */
    public void setBorderLayoutGap(int vGap, int hGap)
    {
        BorderLayout layout = (BorderLayout) getLayout();
        layout.setVgap(vGap);
        layout.setHgap(hGap);
    }
    

    private void init(String sName, String sStyle)
    {
        setOpaque(false);
        setLayout(new BorderLayout());
        GuiManager.init(this, sName, sStyle);
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(getBackground().brighter(),getBackground().darker()),
                BorderFactory.createEmptyBorder(0, 2, 1, 2));
        border_ = new DDTitledBorder(
                border, 
                sText_,
                TitledBorder.LEFT, TitledBorder.TOP,
                getFont(), getForeground());
        setBorder(border_);
    }
        
    public void setEnabled(boolean b)
    {
        // set disabled color
        if (b) border_.setTitleColor(getForeground());
        else border_.setTitleColor(GuiUtils.COLOR_DISABLED_TEXT);

        super.setEnabled(b);
    }
    
    public void reinit(String sName, String sStyle)
    {
        GuiManager.init(this, sName, sStyle);
    }

    /**
     * get border title text
     */
    public String getText()
    {
        return sText_;
    }
    
    /**
     * Set text of border title
     */
    public void setText(String s)
    {
        sText_ = s;
        if (border_ != null) border_.setTitle(sText_);
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    public void repaint()
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, 0, 0, foo);
            foo.repaint(pRepaint.x, pRepaint.y, getWidth(), getHeight());
        }
        else 
        {
            super.repaint();
        }
    }

    public String getType()
    {
        return "labelborder";
    }

    public void setPreferredHeight(int height) {
        setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), height));
    }

    public void setPreferredWidth(int width) {
        setPreferredSize(new Dimension(width, (int) getPreferredSize().getHeight()));
    }
}
