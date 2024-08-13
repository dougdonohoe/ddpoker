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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

public class HandGroupDialog extends OptionMenuDialog implements PropertyChangeListener
{
    private String STYLE;

    private HandGroup profile_;

    private TypedHashMap dummy_ = new TypedHashMap();

    private DDPanel base_;
    private HandGroupGridPanel gridPanel_;
    private DDTextField name_;
    private GlassButton desc_;

    /**
     * Get component with options, also fill array with same options
     */
    protected JComponent getOptions()
    {
        profile_ = (HandGroup) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");

        STYLE = gamephase_.getString("style", "default");

        base_ = new DDPanel(GuiManager.DEFAULT, STYLE);

        gridPanel_ = new HandGroupGridPanel(profile_, true);
        gridPanel_.addPropertyChangeListener(this);
        gridPanel_.setHandGroup(profile_);

        //profile_ = gridPanel_.getHandGroup();

        DDPanel top = new DDPanel();

        DDPanel topformat = new DDPanel();
        BorderLayout layout = (BorderLayout) topformat.getLayout();
        layout.setHgap(10);
        top.add(topformat, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // don't add to option list so we don't reset/set map
        OptionText ot = new OptionText(null, "handgroupname", STYLE, dummy_, 30, "^.+$", 200, true);
        topformat.add(ot, BorderLayout.CENTER);
        name_ = ot.getTextField();
        name_.setText(profile_.getName());
        name_.addPropertyChangeListener(this);

        desc_ = new GlassButton("description", "Glass");
        desc_.setPreferredSize(new Dimension(80, 24));
        desc_.setBorderGap(0, 0, 0, 0);
        desc_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setDescription();
            }
        });
        topformat.add(desc_, BorderLayout.EAST);

        base_.add(top, BorderLayout.NORTH);
        base_.add(gridPanel_, BorderLayout.CENTER);

        return base_;
    }

    protected Component getFocusComponent()
    {
        return name_;
    }

    protected int getTextPreferredHeight()
    {
        return 40;
    }

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button)
    {
        setResult(Boolean.FALSE);
        return super.processButton(button);
    }

    /**
     * Okay button press
     */
    protected void okayButton()
    {
        name_.removePropertyChangeListener(this);
        String sText = name_.getText();
        String sCurrent = profile_.getName();
        if (!sCurrent.equals(sText))
        {
            profile_.setName(sText);
        }

        setResult(Boolean.TRUE);
    }

    /**
     * Override to ignore non-Boolean results
     */
    public void setResult(Object o)
    {
        if (o instanceof Boolean)
        {
            super.setResult(o);
        }
    }

    /**
     * Set description
     */
    private void setDescription()
    {
        TypedHashMap params = new TypedHashMap();
        params.setString(DescriptionDialog.PARAM_DESC, profile_.getDescription());
        Phase phase = context_.processPhaseNow("HandGroupDescriptionDialog", params);
        String sDesc = (String) phase.getResult();

        if (sDesc != null)
        {
            profile_.setDescription(sDesc);
        }
    }

    protected boolean isValidCheck()
    {
        return name_.isValidData() && (profile_.getClassCount() > 0);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();
        if (propertyName.equals("value") || propertyName.equals("HANDS")) checkButtons();
    }
}
