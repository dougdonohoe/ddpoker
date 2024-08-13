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
 * DDComboBoxUI.java
 *
 * Created on November 16, 2002, 12:57 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;
import java.awt.*;
import java.beans.*;

/**
 * Copied MetalComboBoxUI and made tweaks - for
 * some reason subclassing MetalComboBoxUI didn't really
 * work.  See JDD for changes (in source).
 * 
 * @author  Doug Donohoe
 */
public class DDComboBoxUI extends BasicComboBoxUI 
{
    //static Logger logger = Logger.getLogger(DDComboBoxUI.class);

    public static ComponentUI createUI(JComponent c) {
        return new DDComboBoxUI();
    }

    // we don't paint - the DDComboBoxButton does everything now
    // note: this no longer supports complex combo widgets like
    // we had in War! AoI
    public void paint(Graphics g, JComponent c) {
    }

    protected ComboBoxEditor createEditor() {
        return new MetalComboBoxEditor.UIResource();
    }

    protected ComboPopup createPopup() {
        return new DDComboPopup( comboBox );
    }

    protected JButton createArrowButton() {
        JButton button = new DDComboBoxButton( (DDComboBox)comboBox,
                                                  new DDComboBoxIcon(),
                                                  comboBox.isEditable(),
                                                  currentValuePane,
                                                  listBox );
        return button;
    }

    public PropertyChangeListener createPropertyChangeListener() {
        return new MetalPropertyChangeListener();
    }

    /**
     * This inner class is marked &quot;public&quot; due to a compiler bug.
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <FooUI>.
     */          
    public class MetalPropertyChangeListener extends BasicComboBoxUI.PropertyChangeHandler
    {
        public void propertyChange(PropertyChangeEvent e) {
            super.propertyChange( e );
            String propertyName = e.getPropertyName();

            if ( propertyName.equals( "editable" ) ) {
                DDComboBoxButton button = (DDComboBoxButton)arrowButton;
                button.setIconOnly( comboBox.isEditable() );
                comboBox.repaint();
            } else if ( propertyName.equals( "background" ) ) {
                Color color = (Color)e.getNewValue();
                listBox.setBackground(color);
                
            } else if ( propertyName.equals( "foreground" ) ) {
                Color color = (Color)e.getNewValue();
                listBox.setForeground(color);
            }
        }
    }

    /**
     * As of Java 2 platform v1.4 this method is no longer used. Do not call or
     * override. All the functionality of this method is in the
     * MetalPropertyChangeListener.
     *
     * @deprecated As of Java 2 platform v1.4.
     */
    protected void editablePropertyChanged( PropertyChangeEvent e ) { }

    protected LayoutManager createLayoutManager() {
        return new MetalComboBoxLayoutManager();
    }

    /**
     * This inner class is marked &quot;public&quot; due to a compiler bug.
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <FooUI>.
     */          
    public class MetalComboBoxLayoutManager extends BasicComboBoxUI.ComboBoxLayoutManager {
        public void layoutContainer( Container parent ) {
            layoutComboBox( parent, this );
        }
        public void superLayout( Container parent ) {
            super.layoutContainer( parent );
        }
    }

    // This is here because of a bug in the compiler.  
    // When a protected-inner-class-savvy compiler comes out we
    // should move this into MetalComboBoxLayoutManager.
    public void layoutComboBox( Container parent, MetalComboBoxLayoutManager manager ) {
        if ( comboBox.isEditable() ) {
            manager.superLayout( parent );
        }
        else {
            if ( arrowButton != null ) {
                Insets insets = comboBox.getInsets();
                int width = comboBox.getWidth();
                int height = comboBox.getHeight();
                arrowButton.setBounds( insets.left, insets.top,
                                       width - (insets.left + insets.right),
                                       height - (insets.top + insets.bottom) );
            }
        }
    }

    /**
     * As of Java 2 platform v1.4 this method is no
     * longer used.
     *
     * @deprecated As of Java 2 platform v1.4.
     */
    protected void removeListeners() {
        if ( propertyChangeListener != null ) {
            comboBox.removePropertyChangeListener( propertyChangeListener );
        }
    }

    public Dimension getMinimumSize( JComponent c ) {
        if ( !isMinimumSizeDirty ) {
            return new Dimension( cachedMinimumSize );
        }

        Dimension size = null;

        if ( !comboBox.isEditable() &&
             arrowButton != null &&
             arrowButton instanceof DDComboBoxButton ) {

            DDComboBoxButton button = (DDComboBoxButton)arrowButton;
            Insets buttonInsets = button.getInsets();
            Insets insets = comboBox.getInsets();

            size = getDisplaySize();
            size.width += insets.left + insets.right;
            size.width += buttonInsets.left + buttonInsets.right;
            size.width += buttonInsets.right + button.getComboIcon().getIconWidth();
            size.height += insets.top + insets.bottom;
            size.height += buttonInsets.top + buttonInsets.bottom;
        }
        else if ( comboBox.isEditable() &&
                  arrowButton != null &&
                  editor != null ) {
            size = super.getMinimumSize( c );
            Insets margin = arrowButton.getMargin();
            size.height += margin.top + margin.bottom;
        }
        else {
            size = super.getMinimumSize( c );
        }

        // JDD +3 - allow more room for icon
        cachedMinimumSize.setSize( size.width+3, size.height ); 
        isMinimumSizeDirty = false;

        return new Dimension( cachedMinimumSize );
    }

    /**
     * JDD - changed Metal to DD and added override
     */          
    public class DDComboPopup extends BasicComboPopup
    {
        public DDComboPopup( JComboBox cBox) {
            super( cBox );
        }

        // JDD - override this to repaint when click outside of
        // popup - this causes combo to repaint w/out
        // mouseover foreground color
        protected void firePopupMenuWillBecomeInvisible() 
        {
            super.firePopupMenuWillBecomeInvisible();
            comboBox.repaint();
        }
    }
}
