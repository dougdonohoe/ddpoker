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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class ChipRatingSlider extends DDPanel implements ChangeListener
{
    private ChipRatingPanel chips_;
    private DDLabel label_;
    private DDSlider slider_;
    private DDPanel leftInner_;
    private DDPanel leftOuter_;
    private DDPanel pad_;
    private String sType_;

    public ChipRatingSlider(String sStyle, String sType, int minValue, int maxValue)
    {
        sType_ = sType;

        chips_ = new ChipRatingPanel();
        label_ = new DDLabel(GuiManager.DEFAULT, sStyle);
        slider_ = new DDSlider(GuiManager.DEFAULT, sStyle);
        leftInner_ = new DDPanel(GuiManager.DEFAULT, sStyle);
        leftOuter_ = new DDPanel(GuiManager.DEFAULT, sStyle);
        pad_ = new DDPanel(GuiManager.DEFAULT, sStyle);

        label_.setText(PropertyConfig.getMessage("msg." + sType_ + ".0"));

        label_.setPreferredSize(new Dimension(120, (int) label_.getPreferredSize().getHeight()));
        label_.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        chips_.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        slider_.setMinimum(minValue);
        slider_.setMaximum(maxValue);
        slider_.setOrientation(JSlider.HORIZONTAL);
        slider_.setPreferredSize
                (new Dimension((int)chips_.getPreferredSize().getWidth() + 16, (int)slider_.getPreferredSize().getHeight()));
        slider_.addChangeListener(this);

        leftInner_.add(chips_, BorderLayout.NORTH);
        leftInner_.add(slider_, BorderLayout.CENTER);

        leftOuter_.add(leftInner_, BorderLayout.WEST);
        leftOuter_.add(label_, BorderLayout.CENTER);

        add(leftOuter_, BorderLayout.WEST);
        add(pad_, BorderLayout.CENTER);

        setValue(minValue);
    }

    public void stateChanged(ChangeEvent event)
    {
        int value = slider_.getValue();

        chips_.setValue(value);

        label_.setText(PropertyConfig.getMessage("msg." + sType_ + "." + value));

        // now fire my own listeners
        fireStateChanged(event);
    }

    public void setValue(int value)
    {
        slider_.setValue(value);
        chips_.setValue(value);
    }

    public int getValue()
    {
        return slider_.getValue();
    }
    public void addChangeListener(ChangeListener l)
    {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l)
    {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners()
    {
        return (ChangeListener[]) listenerList.getListeners(
                ChangeListener.class);
    }

    protected void fireStateChanged(ChangeEvent event)
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == ChangeListener.class)
            {
                ((ChangeListener) listeners[i + 1]).stateChanged(event);
            }
        }
    }

    public String getLabelText()
    {
        return label_.getText();
    }
}
