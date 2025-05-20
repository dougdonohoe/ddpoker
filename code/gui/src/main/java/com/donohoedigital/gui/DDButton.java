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
 * DDButton.java
 *
 * Created on November 16, 2002, 12:32 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DDButton extends JButton implements DDHasLabelComponent, DDExtendedComponent, AncestorListener
{
    //static Logger logger = LogManager.getLogger(DDButton.class);

    public static final int DISABLED_NONE = 0;
    public static final int DISABLED_GRAY = 1;
    public static final int DISABLED_BORDERLESS = 2;

    private BufferedImage bgImage_ = null;
    private Border bNormal_;
    private Border bDown_;
    private Color cMouseOverForeground_;
    private Color cDisabled_ = new Color(175,175,175,100);
    private boolean bToggle_ = false;
    protected int nDisableMode_ = DISABLED_GRAY;
    private boolean bHighlighted_ = false;
    
    private Color bgNormal_ = null;
    private Color bgHighlighted_ = null;
    private Color bgSelected_ = null;

    private Color fgNormal_ = null;
    private Color fgHighlighted_ = null;
    private Color fgSelected_ = null;
    private int action_ = 0;

    /**
     * Creates a new instance of DDButton 
     */
    public DDButton() {
        this(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    /** 
     * Creates a new instance of DDButton 
     */
    public DDButton(String sName) {
        this(sName, GuiManager.DEFAULT);
    }
    
    /** 
     * Creates a new instance of DDButton - sets name to sName
     */
    public DDButton(String sName, String sStyleName) {
        super();
        init(sName, sStyleName);
        bgNormal_ = getBackground();
        bgHighlighted_ = new Color((bgNormal_.getRed() + 128) / 2, (bgNormal_.getGreen() + 128) / 2, (bgNormal_.getBlue() + 128) / 2);
        bgSelected_ = bgNormal_.brighter().brighter();
        fgNormal_ = getForeground();
        fgHighlighted_ = new Color((fgNormal_.getRed() + 128) / 2, (fgNormal_.getGreen() + 128) / 2, (fgNormal_.getBlue() + 128) / 2);
        fgSelected_ = fgNormal_.brighter().brighter();
        GuiUtils.addKeyAction(this, JComponent.WHEN_FOCUSED,
                    "buttonenter", new GuiUtils.InvokeButton(this),
                    KeyEvent.VK_ENTER, 0);
    }
    
    /**
     * set border size
     */
    public void setBorderGap(int top, int left, int bottom, int right)
    {
        Border bEmpty = BorderFactory.createEmptyBorder(top,left,bottom,right);
        bNormal_ = BorderFactory.createCompoundBorder(
                                    BorderFactory.createBevelBorder(BevelBorder.RAISED),
                                    bEmpty
                                    );
        bDown_ =   BorderFactory.createCompoundBorder(
                                    BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                                    bEmpty
                                    );   
        setBorder(bNormal_);
    }
    
    /**
     * Return our type
     */
    public String getType() 
    {
        return "button";
    }
    
    /**
     * Set image used in background
     */
    public void setBackgroundImage(BufferedImage image)
    {
        bgImage_ = image;
    }
    
    /**
     * Get background image
     */
    public Image getBackgroundImage()
    {
        return bgImage_;
    }

    /**
     * Set the UI to DDButtonUI
     */
    protected void init(String sName, String sStyleName)
    {
        GuiManager.init(this, sName, sStyleName);
        setUI(createUI());
        setRolloverEnabled(true);
        setRequestFocusEnabled(false);
        addAncestorListener(this);
        setBorderGap(5,8,5,8);
    }

    /**
     * Get UI
     */
    protected ComponentUI createUI()
    {
        return DDButtonUI.createUI(this);
    }

    /**
     * Change name of button (to change label)
     */
    public void rename(String sName)
    {
        if (getName().equals(sName)) return;
        GuiManager.rename(this, sName);
    }
    
    /**
     * Set foreground color to use when mouse over button
     */
    public void setMouseOverForeground(Color c)
    {
        cMouseOverForeground_ = c;
    }
    
    /**
     * Get foreground color to use when mouse over button
     */
    public Color getMouseOverForeground()
    {
        return cMouseOverForeground_;
    }
    
    /**
     * Overridden to paint beveled border up/down based on whether button isArmed
     */
    protected void paintBorder(Graphics g) {    

        if (isBorderPainted()) {

            if (this.getModel().isArmed())
            {
                bDown_.paintBorder(this, g, 0, 0, getWidth(), getHeight());
            }
            else
            {
                bNormal_.paintBorder(this, g, 0, 0, getWidth(), getHeight());
            }
        }

        // overlay gray if disabled - we do it here since
        // the border is the last thing drawn
        if (!isEnabled() && ((nDisableMode_ & DISABLED_GRAY) != 0))
        {
            g.setColor(cDisabled_);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
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


    public boolean isBorderPainted()
    {
        return super.isBorderPainted() &&
            (isEnabled() || ((nDisableMode_ & DISABLED_BORDERLESS) == 0));
    }

    public void setIsToggle(boolean bToggle)
    {
        bToggle_ = bToggle;
    }

    public boolean getIsToggle()
    {
        return bToggle_;
    }

    public void setSelected(boolean bSelected)
    {
        super.setSelected(bSelected);

        setBackground(isSelected() ? bgSelected_ : (bHighlighted_ ? bgHighlighted_ : bgNormal_));
        setForeground(isSelected() ? fgSelected_ : (bHighlighted_ ? fgHighlighted_ : fgNormal_));
    }

    public void setHighlighted(boolean bHighlighted)
    {
        bHighlighted_ = bHighlighted;

        setBackground(isSelected() ? bgSelected_ : (bHighlighted_ ? bgHighlighted_ : bgNormal_));
        setForeground(isSelected() ? fgSelected_ : (bHighlighted_ ? fgHighlighted_ : fgNormal_));
    }

    public void setDisableMode(int nDisableMode)
    {
        nDisableMode_ = nDisableMode;
    }

    public int getDisableMode()
    {
        return nDisableMode_;
    }

    /**
     * Override to increase millis
     */
    public void doClick()
    {
        // must be less than 95 on mac
        super.doClick(90);
    }
    
    /**
     * Implemented to set rollover to false when this
     * widget is added - swing does't reset when this (or parent)
     * is removed and later readded.
     */
    public void ancestorAdded(AncestorEvent event) 
    {
        getModel().setRollover(false);
    }

    /**
     * Empty
     */
    public void ancestorMoved(AncestorEvent event) {
    }

    /**
     * Empty
     */
    public void ancestorRemoved(AncestorEvent event) {
    }
    
    // DEBUGGING
    protected void fireActionPerformed(ActionEvent event)
    {
        if (GuiUtils.TESTING_BUTTON)
        {
            GuiUtils.log(getName()+" fireActionPerformed()", event.toString());
        }
        if (getIsToggle())
        {
           setSelected(!isSelected());
        }
        super.fireActionPerformed(event);
    }

    /**
     * implemententation specific use - get purpose of button.
     */
    public void setActionID(int action)
    {
        action_ = action;
    }

    /**
     *  implementation specific use - set purpose of button
     */
    public int getActionID()
    {
        return action_;
    }

    // always anti alias?
    private boolean bAlwaysAntiAlias_ = false;

    /**
     * set whether anti aliases should always occur,
     * overriding GuiUtils.drawAntiAlias()
     */
    public void setAlwaysAntiAlias(boolean b)
    {
        bAlwaysAntiAlias_ = b;
    }

    /**
     * is GuiUtils.drawAntiAlias() overriden
     */
    public boolean isAlwaysAntiAlias()
    {
        return bAlwaysAntiAlias_;
    }

    /**
     * set disabled color
     */
    public void setDisabledColor(Color c)
    {
        cDisabled_ = c;
    }

    /**
     * get disabled color
     */
    public Color getDisabledColor()
    {
        return cDisabled_;
    }
}
