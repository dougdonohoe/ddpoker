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
package com.donohoedigital.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2006
 * Time: 2:39:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class OptionDummy extends DDOption
{
    /**
     * Creates a new instance of DDOption.  If sPrefNode is null,
     * then a DummyPref is used, which is useful if one wants to
     * use the option widgets without saving to prefs (i.e. for layout)
     */
    public OptionDummy(JComponent c)
    {
        super(null, null, null, null);
        add(c, BorderLayout.CENTER);
    }

    /**
     * Creates a new instance of DDOption.  If sPrefNode is null,
     * then a DummyPref is used, which is useful if one wants to
     * use the option widgets without saving to prefs (i.e. for layout)
     */
    public OptionDummy(JComponent center, JComponent right)
    {
        super(null, null, null, null);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    /**
     * set disabled
     */
    public void setEnabled(boolean b)
    {
        for (Component c : getComponents())
        {
            c.setEnabled(b);
        }
    }

    /**
     * Is enabled?
     */
    public boolean isEnabled()
    {
        boolean bEnabled = true;
        for (Component c : getComponents())
        {
            bEnabled &= c.isEnabled();
        }
        return bEnabled;
    }

    public void resetToDefault()
    {
    }

    public void resetToPrefs()
    {
    }

    public void resetToMap()
    {
    }

    public void saveToMap()
    {
    }
}
