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
 * ButtonPanel.java
 *
 * Created on January 20, 2003, 8:36 AM
 */

package com.donohoedigital.gui;

import com.donohoedigital.config.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  Doug Donohoe
 */
public class ButtonPanel extends DDPanel implements MouseListener
{
    //static Logger logger = LogManager.getLogger(ButtonPanel.class);
    
    // ui controls for mouse movement
    protected boolean bSelected_ = false;
    protected Color cMouseOverBG_;
    protected Color cNormalBG_;
    protected Color cMouseDownBG_;
    protected Color cNormalBGsave_;
    
    protected javax.swing.border.Border bNormal_ = BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(0, 3, 0, 3)
                    );
    protected javax.swing.border.Border bMouseDown_ = BorderFactory.createCompoundBorder(
                    BorderFactory.createLoweredBevelBorder(),
                    BorderFactory.createEmptyBorder(0, 3, 0, 3)
                    );
    protected javax.swing.border.Border bDisabled_ = bNormal_;
    protected javax.swing.border.Border borderDisplayOnly_ = BorderFactory.createEmptyBorder();
    
    protected javax.swing.border.Border borderProtected_ = BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createEmptyBorder(0, 3, 0, 3)
                    );

    // info
    private boolean bDisplayOnly_ = false;
    private boolean bProtected_ = false;
    private boolean bUseEmptyBorder_ = false;

    /**
     * Create a new instance of ButtonPanel, default background to transparent
     */
    public ButtonPanel(String sStyle)
    {
        this(sStyle, true);
    }
    
    /**
     * Creates a new instance of ButtonPanel 
     */
    public ButtonPanel(String sStyle, boolean bTransparentBackground) 
    {   
        this(GuiManager.DEFAULT, sStyle, bTransparentBackground);
    }

    /**
     * Creates a new instance of ButtonPanel 
     */
    public ButtonPanel(String sName, String sStyle, boolean bTransparentBackground) 
    {     

        super(sName, sStyle);
        // init this item
        setOpaque(true);
        setBorder(bNormal_);
        addMouseListener(this);
        
        // get colors
        cNormalBGsave_ = cNormalBG_ = getBackground();
        if (bTransparentBackground)
        {
            cNormalBGsave_ = cNormalBG_ = new Color(cNormalBG_.getRed(), cNormalBG_.getGreen(), cNormalBG_.getBlue(), 0);
            setBackground(cNormalBG_);
        }
        
        cMouseOverBG_ = StylesConfig.getColor(sStyle+".buttonpanel.mouseover.bg", Color.black);
        cMouseDownBG_ = StylesConfig.getColor(sStyle+".buttonpanel.mousedown.bg", Color.black);
    }
    
    /**
     * Remove listeners
     */
    public void cleanup()
    {
        removeMouseListener(this);
    }
    
    /**
     * Set selected
     */
    public void setSelected(boolean b)
    {
        if (bSelected_ == b) return;
        bSelected_ = b;
        
        if (bSelected_)
        {
            cNormalBG_ = cMouseDownBG_;
        }
        else
        {
            cNormalBG_ = cNormalBGsave_;
        }
        setBackground(cNormalBG_);
        repaint();
    }
    
    /**
     * Is selected?
     */
    public boolean isSelected()
    {
        return bSelected_;
    }
    
    /**
     * Set display only (if true, can't click button, no border)
     */
    public void setDisplayOnly(boolean b)
    {
        bDisplayOnly_ = b;
        if (b || bUseEmptyBorder_)
        {
            setBorder(borderDisplayOnly_);
        }
        else
        {
            setBorder(bNormal_);
        }
    }
    
    /** 
     * Is display only?
     */
    public boolean isDisplayOnly()
    {
        return bDisplayOnly_;
    }
    
    /**
     * Set protected - can't click (like display only, but keeps border)
     */
    public void setProtected(boolean b)
    {
        bProtected_ = b;
        if (bUseEmptyBorder_)
        {
            setBorder(borderDisplayOnly_);
        }
        else if (b)
        {
            setBorder(borderProtected_);
        }
        else
        {
            setBorder(bNormal_);
            setBackground(cNormalBG_);
        }
    }
    
    
    /**
     * Used to distinguish from normal protected button
     */
    public void setProtectedSelected()
    {
        setBorder(bMouseDown_);
        setBackground(cMouseOverBG_);
        repaint();
    }

    public void setUseEmptyBorder(boolean b)
    {
        bUseEmptyBorder_ = b;
        // force border refresh
        setProtected(isProtected());
    }

    /**
     * Is protected?
     */
    public boolean isProtected()
    {
        return bProtected_;
    }
    
    /**
     * Enabled
     */
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        
        if (!b)
        {
            setBackground(cNormalBG_);
            setBorder(bDisabled_);
        }
        else
        {
            setBorder(bNormal_);
        }

        repaint();
    }
    
    ///
    /// Mouse Listener methods
    ///
    public void mouseClicked(MouseEvent e) {
    }
    
    public void mouseEntered(MouseEvent e) {
        if (!isEnabled() || bDisplayOnly_ || bProtected_ || bSelected_) return;
        
        setBackground(cMouseOverBG_);
        repaint();
    }
    
    public void mouseExited(MouseEvent e) {
        if (!isEnabled() || bDisplayOnly_ || bProtected_ || bSelected_) return;
        setBackground(cNormalBG_);
        repaint();
    }
    
    public void mousePressed(MouseEvent e) {
        if (!isEnabled() || bDisplayOnly_ || bProtected_) return;
        //AudioConfig.playFX("button-click", 0);
        setBackground(cMouseDownBG_);
        if (!bUseEmptyBorder_) setBorder(bMouseDown_);
        repaint();
    }
    
    public void mouseReleased(MouseEvent e) {
        if (!isEnabled() || bDisplayOnly_ || bProtected_) return;
        if (contains(e.getPoint()))
        {
            setBackground(cMouseOverBG_);
            fireActionPerformed();
        }
        else
        {
            setBackground(cNormalBG_);
        }
        // force border refresh
        setProtected(isProtected());
        repaint();
    }
    
    ///
    /// Painting
    ///
    
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
    
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }
    
    protected void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                      e = new ActionEvent(this,
                                          ActionEvent.ACTION_PERFORMED,
                                          null,
                                          0,
                                          0);
                }
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }          
        }
    }

    public void setNormalBackground()
    {
        setBackground(cNormalBG_);
    }

    public void setMouseOverBackground()
    {
        setBackground(cMouseOverBG_);
    }
}
