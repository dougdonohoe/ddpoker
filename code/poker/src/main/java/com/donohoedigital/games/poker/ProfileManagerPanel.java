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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.gui.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;

public class ProfileManagerPanel extends DDPanel implements ChangeListener
{
    static Logger logger = LogManager.getLogger(HandSelectionManager.class);

    private DDHtmlArea summaryHTML_;
    protected DDLabelBorder summaryBorder_;
    protected ProfileList profileList_;
    protected Class profileClass_;
    protected String sManagerType_;

    public ProfileManagerPanel(GameEngine engine, GameContext context, String sName,
                               String sStyle, String sManagerType,
                               Class profileClass)
    {
        super(sName, sStyle);

        profileClass_ = profileClass;
        sManagerType_ = sManagerType;

        String sHelpName = null;

        BorderLayout layout = (BorderLayout) getLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 10));

        // top
        DDPanel top = new DDPanel(sHelpName);
        layout = (BorderLayout) top.getLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        add(top, BorderLayout.CENTER);

        // left side
        DDPanel left = new DDPanel(sHelpName);
        top.add(left, BorderLayout.WEST);

        // get current profile list and sort it

        ArrayList profiles;

        try
        {
            profiles = (ArrayList)profileClass_.getMethod("getProfileList", (java.lang.Class[]) null).invoke(null, (java.lang.Object[]) null);
        }
        catch (Exception e)
        {
            throw new ApplicationError(e);
        }

        Collections.sort(profiles);

        // player list
        DDLabelBorder pborder = new DDLabelBorder(sManagerType, sStyle);
        pborder.setPreferredSize(new Dimension(250, 400));
        left.add(pborder, BorderLayout.CENTER);
        profileList_ = new MyProfileList(engine, context, profiles, sStyle, sManagerType, sHelpName, "pokericon16png", true);
        profileList_.addChangeListener(this);
        pborder.add(profileList_, BorderLayout.CENTER);

        // stats
        summaryBorder_ = new DDLabelBorder(sManagerType+".summary", sStyle);
        summaryBorder_.setPreferredSize(new Dimension(500, 370));
        top.add(summaryBorder_, BorderLayout.CENTER);
        summaryBorder_.add(createSummary(sHelpName, sStyle), BorderLayout.CENTER);
        updateSummary();
    }

    /**
     * Create summary widget - default is to create an HTML text area
     */
    protected JComponent createSummary(String sHelpName, String sStyle)
    {
        summaryHTML_ = new DDHtmlArea(sHelpName, sStyle);
        summaryHTML_.setDisplayOnly(true);
        summaryHTML_.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
        JScrollPane scroll = new DDScrollPane(summaryHTML_, sStyle, null,
                                   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        return scroll;
    }

    /**
     * Called when a spinner or tournament list changes
     */
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() == profileList_)
        {
            updateSummary();
        }
    }

    /**
     * update summary
     */
    protected void updateSummary()
    {
        BaseProfile pp = profileList_.getSelectedProfile();

        if (pp != null)
        {
            // set current selected profile and update stats label
            summaryHTML_.setText(pp.toHTML());
            summaryHTML_.setCaretPosition(0); // scroll to top

            if (isTitleCustomized())
            {
                summaryBorder_.setText(PropertyConfig.getMessage("labelborder."+sManagerType_+".summary.label2", pp.getName()));
                summaryBorder_.repaint();
            }
        } else
        {
            summaryHTML_.setText("");

            if (isTitleCustomized())
            {
                summaryBorder_.setText(PropertyConfig.getMessage("labelborder."+sManagerType_+".summary.label"));
                summaryBorder_.repaint();
            }
        }
    }

    /**
     * whether this profile has a customized summary title (per profile).  Default is true
     */
    protected boolean isTitleCustomized()
    {
        return true;
    }

    /**
     * Our list editor
     */
    private class MyProfileList extends ProfileList
    {
        public MyProfileList(GameEngine engine, GameContext context, ArrayList profiles,
                             String sStyle,
                             String sMsgName,
                             String sPanelName,
                             String sIconName,
                             boolean bUseCopyButton)
        {
            super(engine, context, profiles, sStyle, sMsgName, sPanelName, sIconName, bUseCopyButton);
        }

        /**
         * Create empty profile
         */
        protected BaseProfile createEmptyProfile()
        {
            return (BaseProfile)ConfigUtils.newInstance(profileClass_, new Class[] { String.class }, new Object[] { "" });
        }

        /**
         * Copy profile
         */
        protected BaseProfile copyProfile(BaseProfile profile, boolean bForEdit)
        {
            String sName = bForEdit ? profile.getName() : PropertyConfig.getMessage("msg.copy", profile.getName());
            return (BaseProfile) ConfigUtils.newInstance(profileClass_, new Class[]{profileClass_, String.class}, new Object[]{profile, sName});
        }

        /**
         * store default profile
         */
        public void rememberProfile(BaseProfile profile)
        {
            super.rememberProfile(profile);
            if (ProfileManagerPanel.this != null)
            {
                ProfileManagerPanel.this.rememberProfile(profile);
            }
        }
    }

    /**
     * For subclass to do something if profile changes
     */
    protected void rememberProfile(BaseProfile profile)
    {

    }
}
