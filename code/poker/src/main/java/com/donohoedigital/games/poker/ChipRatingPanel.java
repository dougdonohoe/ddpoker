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
package com.donohoedigital.games.poker;

import com.donohoedigital.gui.DDPanel;
import com.donohoedigital.gui.DDLabel;
import com.donohoedigital.config.ImageConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;

public class ChipRatingPanel extends DDPanel
{
    private static final ImageIcon fullChip_ = ImageConfig.getImageIcon("rating16_full");
    private static final ImageIcon halfChip_ = ImageConfig.getImageIcon("rating16_half");
    private static final ImageIcon emptyChip_ = ImageConfig.getImageIcon("rating16_empty");

    private JLabel chips_[] = new JLabel[5];

    int value_ = 0;

    public ChipRatingPanel()
    {
        DDPanel chipGrid_ = new DDPanel();
        chipGrid_.setLayout(new GridLayout(1, 5, 2, 0));

        for (int i = 0; i < 5; ++i)
        {
            chips_[i] = new DDLabel();
            chips_[i].setText(null);
            chips_[i].setIcon(emptyChip_);
            chipGrid_.add(chips_[i]);
        }

        add(chipGrid_, BorderLayout.WEST);

        setPreferredSize(new Dimension(88, 16));
    }

    public void setValue(int value)
    {
        for (int i = 0; i < 5; ++i)
        {
            if (i * 2 == value - 1)
            {
                chips_[i].setIcon(halfChip_);
            }
            else if (i * 2 < value)
            {
                chips_[i].setIcon(fullChip_);
            }
            else
            {
                chips_[i].setIcon(emptyChip_);
            }

        }

        value_ = value;
    }

    public void addMouseListener(MouseListener listener)
    {
        for (int i = 0; i < 5; ++i)
        {
            if ((chips_ != null) && (chips_[i] != null))
            {
                chips_[i].addMouseListener(listener);
            }
        }

        super.addMouseListener(listener);
    }
}
