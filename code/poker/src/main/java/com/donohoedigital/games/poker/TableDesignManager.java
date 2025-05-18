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
 * TableDesignManager.java
 *
 * Created on May 18, 2005, 7:58 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class TableDesignManager extends ProfileManagerPanel
{
    private static final String PROFILE_NAME = "tabledesign";
    private static TableDesign default_ = null;

    PokerGameboard.FauxPokerGameboard faux_;

    /**
     * Construct me
     */
    public TableDesignManager(GameEngine engine, GameContext context, String sStyle)
    {
        super(engine, context, GuiManager.DEFAULT, sStyle, PROFILE_NAME, TableDesign.class);
    }

    /**
     * Create summary widget - default is to create an HTML text area
     */
    protected JComponent createSummary(String sHelpName, String sStyle)
    {
        faux_ = new PokerGameboard.FauxPokerGameboard(GuiUtils.TRANSPARENT, GuiUtils.TRANSPARENT);
        return GuiUtils.CENTER(faux_);
    }

    /**
     * update summary
     */
    protected void updateSummary()
    {
        TableDesign pp = (TableDesign) profileList_.getSelectedProfile();

        if (pp != null)
        {
            // set current selected profile and update stats label
            faux_.setVisible(true);
            faux_.updateColors(pp.getColorTop(), pp.getColorBottom());

            if (isTitleCustomized())
            {
                summaryBorder_.setText(PropertyConfig.getMessage("labelborder."+sManagerType_+".summary.label2", pp.getName()));
                summaryBorder_.repaint();
            }
        } else
        {
            faux_.setVisible(false);

            if (isTitleCustomized())
            {
                summaryBorder_.setText(PropertyConfig.getMessage("labelborder."+sManagerType_+".summary.label"));
                summaryBorder_.repaint();
            }
        }
    }

    /**
     * store default profile
     */
    public void rememberProfile(BaseProfile profile)
    {
        super.rememberProfile(profile);
        default_ = (TableDesign) profile;
    }

    /**
     * Return stored profile based on preference maintained by ProfileList
     */
    public static TableDesign getDefaultProfile()
    {
        if (default_ == null)
        {
            String sName = ProfileList.getStoredProfile(PROFILE_NAME);
            if (sName != null)
            {
                File file = TableDesign.getProfileFile(sName);
                if (file.exists())
                {
                    default_ = new TableDesign(file,  true);
                }
                else // file doesn't exist, so forget it
                {
                    ProfileList.setStoredProfile(null, PROFILE_NAME);
                }
            }

            // still null (nothing in prefs), see if any files exist
            // and choose most recent modification or one named "Green"
            // and remember it
            if (default_ == null)
            {
                List<BaseProfile> list = TableDesign.getProfileList();
                TableDesign p = null;
                long lastmod = 0;
                for (int i = 0; list != null && i < list.size(); i++)
                {
                    TableDesign profile = ((TableDesign)list.get(i));
                    if (profile.getName().equals("Red"))
                    {
                        p = profile;
                        break;
                    }
                    if (profile.getLastModified() > lastmod)
                    {
                        p = profile;
                    }
                }

                // if found one, remember it
                if (p != null)
                {
                    ProfileList.setStoredProfile(p, PROFILE_NAME);
                    default_ = p;
                }
            }
        }

        return default_;
    }
}
