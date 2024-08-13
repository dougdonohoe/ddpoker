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
 * DDComboBoxIcon.java
 *
 * Created on November 20, 2002, 8:24 PM
 */

package com.donohoedigital.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.border.*;
import java.io.Serializable;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * This class draws the horizontal bars for a ComboBox
 */
public class DDComboBoxIcon implements Icon, Serializable {

    public void paintIcon(Component c, Graphics g, int x, int y) {
        JComponent component = (JComponent) c;
        int iconWidth = getIconWidth();

        x += 1;
        g.translate(x, y);

        g.setColor(component.getForeground());
        g.drawLine(0, 0, iconWidth - 1, 0);
        g.drawLine(1, 1, 1 + (iconWidth - 3), 1);
        g.drawLine(2, 2, 2 + (iconWidth - 5), 2);
        g.drawLine(3, 3, 3 + (iconWidth - 7), 3);
        g.drawLine(4, 4, 4 + (iconWidth - 9), 4);

        g.translate(-x, -y);
    }

    public int getIconWidth() {
        return 10;
    }

    public int getIconHeight() {
        return 5;
    }
}