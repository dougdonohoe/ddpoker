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
 * TableDesignDialog.java
 *
 * Created on May 18, 2005, 8:33 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 *
 * @author  Doug Donohoe
 */
public class TableDesignDialog extends DialogPhase implements ChangeListener, PropertyChangeListener
{
    static Logger logger = LogManager.getLogger(TableDesignDialog.class);
    
    private TableDesign profile_;
    private DDTextField name_;
    private ColorChooserPanel topChooser_, bottomChooser_;
    private PokerGameboard.FauxPokerGameboard faux;

    /**
     * create chat ui
     */
    public JComponent createDialogContents() 
    {
        profile_ = (TableDesign) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");
        
        // contents
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(10,10);
        base.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        DDPanel namebase = new DDPanel();
        namebase.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        base.add(namebase, BorderLayout.NORTH);

        DDLabel nameLabel = new DDLabel("profilename", STYLE);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        namebase.add(nameLabel, BorderLayout.WEST);

        name_ = new DDTextField(GuiManager.DEFAULT, STYLE);
        name_.setTextLengthLimit(30);
        name_.setText(profile_.getName());
        name_.setRegExp("^.+$");
        name_.addPropertyChangeListener("value", this);
        namebase.add(name_, BorderLayout.CENTER);

        DDPanel colors = new DDPanel();
        base.add(colors, BorderLayout.WEST);

        DDLabelBorder top = new DDLabelBorder("top", STYLE);
        topChooser_ = new ColorChooserPanel(STYLE, profile_.getColorTop(), "engine.basepanel");
        topChooser_.addChangeListener(this);
        top.add(topChooser_, BorderLayout.CENTER);
        colors.add(top, BorderLayout.NORTH);

        DDButton swap = new GlassButton("swap", "Glass");
        JComponent swapcenter = GuiUtils.CENTER(swap);
        swapcenter.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        colors.add(swapcenter, BorderLayout.CENTER);
        swap.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                Color old = profile_.getColorBottom();
                profile_.setColorBottom(profile_.getColorTop());
                profile_.setColorTop(old);
                faux.updateColors(profile_.getColorTop(), profile_.getColorBottom());
                bottomChooser_.setColor(profile_.getColorBottom());
                topChooser_.setColor(profile_.getColorTop());
            }
        });
        DDLabelBorder bottom = new DDLabelBorder("bottom", STYLE);
        bottomChooser_ = new ColorChooserPanel(STYLE, profile_.getColorBottom(), "engine.basepanel");
        bottomChooser_.addChangeListener(this);
        bottom.add(bottomChooser_, BorderLayout.CENTER);
        colors.add(bottom, BorderLayout.SOUTH);

        DDLabelBorder preview = new DDLabelBorder("tabledesign.summary", STYLE);
        base.add(preview, BorderLayout.CENTER);

        faux = new PokerGameboard.FauxPokerGameboard(profile_.getColorTop(), profile_.getColorBottom());
        base.add(GuiUtils.CENTER(faux), BorderLayout.CENTER);

        checkButtons();

        return base;
    }
    
    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return name_;
    }

    private boolean CHANGED = false;

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) 
    {   
        Boolean bResult = Boolean.FALSE;

        if (button.getName().equals(okayButton_.getName()))
        {
            // okay
            String sText = name_.getText();
            String sCurrent = profile_.getName();
            if (!sText.equals(sCurrent))
            {
                // name changed
                profile_.setName(sText);
                bResult = Boolean.TRUE;
            }

            if (CHANGED) bResult = Boolean.TRUE;
        }

        removeDialog();
        setResult(bResult);

        return true;
    }
    
    /**
     * Enable buttons
     */
    private void checkButtons()
    {
        okayButton_.setEnabled(name_.isValidData() &&
                               topChooser_.isValidData() &&
                               bottomChooser_.isValidData());
    }

    /**
     * color changed
     */
    public void stateChanged(ChangeEvent e)
    {
        CHANGED = true;

        if (e.getSource() == topChooser_)
        {
            profile_.setColorTop(topChooser_.getColor());
            faux.updateTop(topChooser_.getColor());
        }
        else
        {
            profile_.setColorBottom(bottomChooser_.getColor());
            faux.updateBottom(bottomChooser_.getColor());
        }

        checkButtons();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        checkButtons();
    }
}
