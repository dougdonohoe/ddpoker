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
 * ChangePasswordDialog.java
 *
 * Created on January 25, 2003, 10:11 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.beans.*;

/**
 * @author Doug Donohoe
 */
public class SyncPasswordDialog extends DialogPhase implements PropertyChangeListener
{
    static Logger logger = LogManager.getLogger(SyncPasswordDialog.class);

    private PlayerProfile profile_;

    private TablePanel.TextWidgets newText_ = null;

    /**
     * create chat ui
     */
    @Override
    public JComponent createDialogContents()
    {
        // Use original profile information to determine how to update values.
        profile_ = (PlayerProfile) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");

        // add fields
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        TablePanel panel = new TablePanel();
        base.add(panel, BorderLayout.NORTH);

        newText_ = panel.addTextField("yourpassword", null, STYLE, 16, null, null, null);

        // add listeners
        newText_.text_.addPropertyChangeListener(this);

        checkButtons();

        return base;
    }

    /**
     * Focus to text field
     */
    @Override
    protected Component getFocusComponent()
    {
        return newText_.text_;
    }

    /**
     * Closes the dialog unless an error occurs saving the profile information
     */
    @Override
    public boolean processButton(GameButton button)
    {
        boolean bResult = false;
        boolean bSuccess = true;

        if (button.getName().equals(okayButton_.getName()))
        {
            // okay
            OnlineProfile profile = profile_.toOnlineProfile();
            profile.setPassword(newText_.getText());
            bResult = true;
            bSuccess = SendWanProfile.sendWanProfile(context_, OnlineMessage.CAT_WAN_PROFILE_SYNC_PASSWORD, profile, null);

            if (bSuccess)
            {
                // update local profile values
                profile_.setPassword(newText_.getText());
            }
            else
            {
                bResult = false;
            }
        }

        if (bSuccess)
        {
            removeDialog();
        }

        setResult(bResult);

        return bSuccess;
    }

    /**
     * msg text change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        checkButtons();
    }

    /**
     * Enable buttons
     */
    private void checkButtons()
    {
        okayButton_.setEnabled(newText_.getText().length() > 0);
    }
}