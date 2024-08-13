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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;

import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class HandSelectionPanel extends DDPanel
{
    static ChangeListener changeListener = null;

    private DDComboBox handSelectionFull_;
    private DDComboBox handSelectionShort_;
    private DDComboBox handSelectionVeryShort_;
    private DDComboBox handSelectionHup_;

    PlayerType profile_;

    public HandSelectionPanel(PlayerType profile, String sStyle)
    {
        profile_ = profile;

        DDLabel handSelectionLabelFull = new DDLabel("handselection", sStyle);
        DDLabel handSelectionLabelShort = new DDLabel("handselection", sStyle);
        DDLabel handSelectionLabelVeryShort = new DDLabel("handselection", sStyle);
        DDLabel handSelectionLabelHup = new DDLabel("handselection", sStyle);

        DataElement handSelectionElement =
                new DataElement("HandSelectionScheme", HandSelectionScheme.getProfileList(), null);

        handSelectionFull_ = new DDComboBox(handSelectionElement, sStyle);
        handSelectionShort_ = new DDComboBox(handSelectionElement, sStyle);
        handSelectionVeryShort_ = new DDComboBox(handSelectionElement, sStyle);
        handSelectionHup_ = new DDComboBox(handSelectionElement, sStyle);


        handSelectionFull_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                profile_.setHandSelectionFull((HandSelectionScheme)handSelectionFull_.getSelectedItem());
                if (changeListener != null)
                {
                    changeListener.stateChanged(null);
                }
            }
        });
        handSelectionShort_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                profile_.setHandSelectionShort((HandSelectionScheme)handSelectionShort_.getSelectedItem());
                if (changeListener != null)
                {
                    changeListener.stateChanged(null);
                }
            }
        });
        handSelectionVeryShort_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                profile_.setHandSelectionVeryShort((HandSelectionScheme)handSelectionVeryShort_.getSelectedItem());
                if (changeListener != null)
                {
                    changeListener.stateChanged(null);
                }
            }
        });
        handSelectionHup_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                profile_.setHandSelectionHup((HandSelectionScheme)handSelectionHup_.getSelectedItem());
                if (changeListener != null)
                {
                    changeListener.stateChanged(null);
                }
            }
        });

        HandSelectionScheme selected;

        selected = profile_.getHandSelectionFull();

        if (selected == null)
        {
            handSelectionFull_.setSelectedIndex(0);
        } else
        {
            handSelectionFull_.setSelectedItem(selected);
        }

        selected = profile_.getHandSelectionShort();

        if (selected == null)
        {
            handSelectionShort_.setSelectedIndex(0);
        } else
        {
            handSelectionShort_.setSelectedItem(selected);
        }

        selected = profile_.getHandSelectionVeryShort();

        if (selected == null)
        {
            handSelectionVeryShort_.setSelectedIndex(0);
        } else
        {
            handSelectionVeryShort_.setSelectedItem(selected);
        }

        selected = profile_.getHandSelectionHup();

        if (selected == null)
        {
            handSelectionHup_.setSelectedIndex(0);
        } else
        {
            handSelectionHup_.setSelectedItem(selected);
        }

        DDLabelBorder handSelectionBorder = new DDLabelBorder("playstyle", sStyle);

        handSelectionLabelFull.setText("Full Table (7-10 players):");
        handSelectionLabelShort.setText("Short Handed (5-6 players):");
        handSelectionLabelVeryShort.setText("Very Short Handed (3-4 players):");
        handSelectionLabelHup.setText("Heads Up (2 players):");
        handSelectionBorder.setText("Hand Selection");

        handSelectionBorder.setLayout(new GridLayout(4, 2, 4, 4));

        handSelectionBorder.add(handSelectionLabelFull);
        handSelectionBorder.add(handSelectionFull_);
        handSelectionBorder.add(handSelectionLabelShort);
        handSelectionBorder.add(handSelectionShort_);
        handSelectionBorder.add(handSelectionLabelVeryShort);
        handSelectionBorder.add(handSelectionVeryShort_);
        handSelectionBorder.add(handSelectionLabelHup);
        handSelectionBorder.add(handSelectionHup_);

        setPreferredWidth(600);
        setPreferredHeight(100);

        add(handSelectionBorder, BorderLayout.CENTER);
    }

}
