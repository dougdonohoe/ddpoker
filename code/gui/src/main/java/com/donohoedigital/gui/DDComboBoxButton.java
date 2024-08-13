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
 * DDComboBoxButton.java
 *
 * Created on December 30, 2002, 4:03 PM
 */

package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Copied MetalComboBoxButton code, but subclass DDButton instead
 * and make other UI tweaks
 *
 * @author  Doug Donohoe
 */
public class DDComboBoxButton extends GlassButton
{
    protected DDComboBox comboBox;
    protected JList listBox;
    protected CellRendererPane rendererPane;
    protected Icon comboIcon;
    protected boolean iconOnly = false;

    public final Icon getComboIcon() { return comboIcon;}
    public final void setComboIcon( Icon i ) { comboIcon = i;}
    public final boolean isIconOnly() { return iconOnly;}
    public final void setIconOnly( boolean isIconOnly ) { iconOnly = isIconOnly;}

    private DDComboBoxButton()
    {
        super( GuiManager.DEFAULT, "GlassCombo");
        DefaultButtonModel model = new DefaultButtonModel() {
            public void setArmed( boolean armed ) {
                super.setArmed( isPressed() ? true : armed );
            }
        };
        setModel( model );
        setFocusable(false);
    }

    public DDComboBoxButton( DDComboBox cb, Icon i, 
                                CellRendererPane pane, JList list ) {
        this();
        comboBox = cb;
        comboIcon = i;
        rendererPane = pane;
        listBox = list;
        setIcon(i);
        setHorizontalTextPosition(SwingConstants.LEFT);
        setBorderGap(3,3,2,3);
    }

    public DDComboBoxButton( DDComboBox cb, Icon i, boolean onlyIcon,
                                CellRendererPane pane, JList list ) {
        this( cb, i, pane, list );
        iconOnly = onlyIcon;
    }

    /**
     * we don't get actual focus, we draw focus based on
     * whether combo has focus
     */
    public boolean hasFocus()
    {
        if (comboBox == null) return false;
        return comboBox.hasFocus();
    }

    /**
     * Enabled based on combo
     */
    public boolean isEnabled()
    {
        if (comboBox == null) return false;
        return comboBox.isEnabled();
    }

    /**
     * override to set text in button to current value
     */
    public void paintComponent(Graphics g)
    {
        setText(comboBox.getSelectedDisplayValue());
        super.paintComponent(g);
    }
}
