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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

public class PlayerTypeDialog extends OptionMenuDialog implements PropertyChangeListener, FocusListener
{
    static Logger logger = LogManager.getLogger(PlayerTypeDialog.class);

    private PlayerType profile_;
    private TypedHashMap dummy_ = new TypedHashMap();
    private TypedHashMap orig_;
    private DDTextField name_;
    private GlassButton desc_;

    /**
     * help text area
     */
    protected int getTextPreferredHeight()
    {
        return 65;
    }

    /**
     * create ui
     */
    public JComponent getOptions()
    {
        PlayerType profile = (PlayerType) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile, "No 'profile' in params");
        
        return getOptions(profile, STYLE);
    }
    
    public JComponent getOptions(PlayerType profile, String sStyle)
    {
        profile_ = profile;
        boolean bDoMapLoad = true;

        // if a new profile, then use the empty map instead of the dummy
        // so the default values are initialized
        // this shows the basic flaw in the options stuff in that a UI has
        // to be created to set the default values.  In this case of
        // tournaments, it is okay to do this
        TypedHashMap map = profile_.getMap();
        if (map.size() == 0)
        {
            bDoMapLoad = false;
        }

        DDPanel base_ = new DDPanel(sStyle);

        ArrayList playStyles = new ArrayList();
        playStyles.add(new PlayStyle("Base Style"));

        HandSelectionPanel handSelectionPanel = new HandSelectionPanel(profile_, sStyle);

        PlayerTypeSlidersPanel playStylePanel = new PlayerTypeSlidersPanel(sStyle);

        playStylePanel.setItems(profile.getSummaryNodes(true));

        DDPanel top = new DDPanel();

        DDPanel topformat = new DDPanel();
        BorderLayout layout = (BorderLayout) topformat.getLayout();
        layout.setHgap(10);
        top.add(topformat, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        // don't add to option list so we don't reset/set map
        OptionText ot = new OptionText(null, "playertypename", STYLE, dummy_, 30, "^.+$", 200, true);
        topformat.add(ot, BorderLayout.WEST);
        name_ = ot.getTextField();
        name_.addPropertyChangeListener(this);
        name_.setText(profile_.getName());

        DDPanel topButtons = new DDPanel();

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

        topButtons.setLayout(new GridLayout(1, 2, 4, 0));
        topButtons.add(desc_);
        topformat.add(GuiUtils.CENTER(topButtons), BorderLayout.EAST);

        // save starting values for use during reset
        orig_ = new DMTypedHashMap();
        orig_.putAll(profile_.getMap());

        top.setPreferredWidth(600);
        playStylePanel.setPreferredWidth(600);
        playStylePanel.setPreferredHeight(300);

        base_.setLayout(new VerticalFlowLayout());
        base_.add(top);
        base_.add(handSelectionPanel);
        base_.add(playStylePanel);

        return base_;
    }

    /**
     * Set description
     */
    private void setDescription()
    {
        TypedHashMap params = new TypedHashMap();
        params.setString(DescriptionDialog.PARAM_DESC, profile_.getDescription());
        Phase phase = context_.processPhaseNow("PlayerTypeDescriptionDialog", params);
        String sDesc = (String) phase.getResult();

        if (sDesc != null)
        {
            profile_.setDescription(sDesc);
        }
    }

    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return name_;
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
     * msg text change
     */
    public void propertyChange(PropertyChangeEvent evt) {    
        if (evt.getPropertyName().equals("value")) checkButtons();
    }

    /**
     * scroll text to visible
     */
    public void focusGained(FocusEvent e) {
        DDTextField t = (DDTextField) e.getSource();
        JScrollPane p = GuiUtils.getScrollParent(t);
        if (p != null)
        {
            Point loc = t.getLocation();
            loc = SwingUtilities.convertPoint(t.getParent(), loc, p.getViewport());
            p.getViewport().scrollRectToVisible(new Rectangle(loc, t.getSize()));
        }
    }
    
    /** 
     * EMPTY
     */
    public void focusLost(FocusEvent e) {
    }
}
