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
 * DDCheckBox.java
 *
 * Created on February 23, 2003, 3:19 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.awt.*;

/**
 * @author Doug Donohoe
 */
public class DDCheckBox extends JCheckBox implements DDHasLabelComponent
{
    //static Logger logger = Logger.getLogger(DDCheckBox.class);

    private boolean bPaintIconBackground_ = true;
    private Color cCheckColor_ = null;

    /**
     * Creates a new instance of DDCheckBox
     */
    public DDCheckBox()
    {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }

    /**
     * Creates a new instance of DDCheckBox
     */
    public DDCheckBox(String sName)
    {
        super();
        init(sName, GuiManager.DEFAULT);
    }

    /**
     * Creates a new instance of DDCheckBox - sets name to sName
     */
    public DDCheckBox(String sName, String sStyleName)
    {
        super();
        init(sName, sStyleName);
    }

    /**
     * Return our type
     */
    public String getType()
    {
        return "checkbox";
    }

    /**
     * Set the UI to DDCheckBoxUI
     */
    protected void init(String sName, String sStyleName)
    {
        setUI(new DDCheckBoxUI());
        GuiManager.init(this, sName, sStyleName);
        setOpaque(false);
        setIcon(new CheckBoxIcon(this));
        setIconTextGap(8);
    }


    /**
     * Override to check swing thread
     */
    @Override
    public void setText(String sMsg)
    {
        GuiUtils.requireSwingThread();

        super.setText(sMsg);
    }


    /**
     * Set whether background icon should be painted
     */
    public void setIconBackgroundPainted(boolean b)
    {
        bPaintIconBackground_ = b;
    }

    /**
     * Override checkbox color
     */
    public void setCheckBoxColor(Color c)
    {
        cCheckColor_ = c;
    }

    /**
     * override disable text color
     */
    public void setDisabledTextColor(Color c)
    {
        ((DDCheckBoxUI) getUI()).setDisabledTextColor(c);
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    @Override
    public void repaint(long tm, int x, int y, int width, int height)
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, x, y, foo);
            foo.repaint(pRepaint.x, pRepaint.y, width, height);
            return;
        }

        super.repaint(tm, x, y, width, height);
    }

    /**
     * Override to set anti aliasing hit if isAntiAlias() is true
     */
    @Override
    public void paintComponent(Graphics g1)
    {
        Graphics2D g = (Graphics2D) g1;

        // we want font to look nice
        Object old = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (GuiUtils.drawAntiAlias(this))
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, old);
    }

    int nControlSize_ = 13;

    /**
     * Check box size
     */
    protected int getControlSize()
    {
        return nControlSize_;
    }

    /**
     * Set check box size
     */
    public void setControlSize(int n)
    {
        nControlSize_ = n;
    }

    boolean bDrawX_ = false;

    /**
     * If true, draw an X instead of a check
     */
    public void setDrawX(boolean b)
    {
        bDrawX_ = b;
    }

    /**
     * Drawing x?
     */
    public boolean isDrawX()
    {
        return bDrawX_;
    }

    boolean bProtected_ = false;

    public void setProtected(boolean b)
    {
        bProtected_ = b;
        setEnabled(!b);
    }

    public boolean isProtected()
    {
        return bProtected_;
    }

    private class CheckBoxIcon implements Icon
    {
        DDCheckBox cb_;

        CheckBoxIcon(DDCheckBox box)
        {
            cb_ = box;
        }

        protected int getControlSize()
        {
            return cb_.getControlSize();
        }

        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            ButtonModel model = cb_.getModel();
            int controlSize = getControlSize();

            if (model.isEnabled())
            {
                if (model.isPressed() && model.isArmed())
                {
                    g.setColor(cCheckColor_ != null ? cCheckColor_ : c.getForeground());//MetalLookAndFeel.getControlShadow() );
                    g.fillRect(x, y, controlSize - 1, controlSize - 1);
                    GuiUtils.drawPressed3DBorder(g, c, x, y, controlSize, controlSize); // JDD (copied to GuiUtils)
                    g.setColor(c.getBackground()); // JDD
                }
                else
                {
                    if (bPaintIconBackground_)
                    {
                        g.setColor(c.getBackground());//MetalLookAndFeel.getControlShadow() );
                        g.fillRect(x, y, controlSize - 1, controlSize - 1);
                    }
                    GuiUtils.drawFlush3DBorder(g, c, x, y, controlSize, controlSize); // JDD (copied to GuiUtils)
                    g.setColor(cCheckColor_ != null ? cCheckColor_ : c.getForeground()); // JDD
                }
                //g.setColor( MetalLookAndFeel.getControlInfo() );
            }
            else
            {
                if (bPaintIconBackground_)
                {
                    Color bg = c.getBackground();
                    bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 128);
                    g.setColor(bg);//MetalLookAndFeel.getControlShadow() );
                    g.fillRect(x, y, controlSize - 1, controlSize - 1);
                }
                g.setColor(MetalLookAndFeel.getControlShadow());
                GuiUtils.drawFlush3DBorder(g, c, x, y, controlSize, controlSize); // JDD (copied to GuiUtils)

                if (cb_.isProtected())
                {
                    g.setColor(cCheckColor_ != null ? cCheckColor_ : c.getForeground()); // JDD
                }
            }

            if (model.isSelected())
            {
                if (cb_.isDrawX())
                {
                    drawX(c, g, x, y);
                }
                else
                {
                    drawCheck(c, g, x, y);
                }
            }

        }

        protected void drawCheck(Component c, Graphics g, int x, int y)
        {
            int controlSize = getControlSize();
            g.fillRect(x + 3, y + 5, 2, controlSize - 8);
            g.drawLine(x + (controlSize - 4), y + 3, x + 5, y + (controlSize - 6));
            g.drawLine(x + (controlSize - 4), y + 4, x + 5, y + (controlSize - 5));
        }

        protected void drawX(Component c, Graphics g1, int x, int y)
        {
            Graphics2D g = (Graphics2D) g1;
            int controlSize = getControlSize();
            Stroke old = g.getStroke();
            g.setStroke(stroke_);
            g.drawLine(x + 3, y + 3, x + (controlSize - 5), y + (controlSize - 5));
            g.drawLine(x + 3, x + (controlSize - 5), x + (controlSize - 5), y + 3);
            g.setStroke(old);
        }

        public int getIconWidth()
        {
            return getControlSize();
        }

        public int getIconHeight()
        {
            return getControlSize();
        }
    } // End class CheckBoxIcon

    static BasicStroke stroke_ = new BasicStroke(2.0f);
}
