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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.base.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 12, 2005
 * Time: 4:10:28 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
public class CheatDash extends DashboardItem implements ChangeListener
{
    private static CheatDash impl_ = null;
    private List<DDOption> options_ = new ArrayList<DDOption>();
    private boolean bUpdating_ = false;

    /**
     * Cheat dash list
     */
    @SuppressWarnings({"ThisEscapedInObjectConstruction"})
    public CheatDash(GameContext context)
    {
        super(context, "cheat");
        impl_ = this;
    }

    /**
     * finish - clear impl_
     */
    @Override
    public void finish()
    {
        impl_ = null;
    }

    /**
     * create list of options
     */
    @Override
    protected JComponent createBody()
    {
        DDPanel cheatbase = new DDPanel();

        cheatbase.setLayout(new GridLayout(0, 1, 0, -8));
        GamePrefsPanel.addCheatOptions(GameEngine.getGameEngine().getPrefsNodeName(), cheatbase, "DashSmaller", new TypedHashMap(), false);

        GuiUtils.getDDOptions(cheatbase, options_);
        DDOption option;
        for (int i = options_.size() - 1; i >= 0; i--)
        {
            option = options_.get(i);
            option.addChangeListener(this);
        }
        return cheatbase;
    }

    /**
     * option changed - update display
     */
    public void stateChanged(ChangeEvent e)
    {
        if (bUpdating_) return;
        GamePrefsDialog.updatePrefs(context_);
    }

    /**
     * tell impl to update when prefs change (since we don't have prefs listeners)
     */
    public static void updatePrefs()
    {
        if (impl_ != null)
        {
            impl_._updatePrefs();
        }
    }

    /**
     * update each option to value stored in prefs
     */
    private void _updatePrefs()
    {
        bUpdating_ = true;
        DDOption option;
        for (int i = options_.size() - 1; i >= 0; i--)
        {
            option = options_.get(i);
            option.resetToPrefs();
        }
        bUpdating_ = false;
    }
}
