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
/*
 * OptionMenuDialog.java
 *
 * Created on June 06, 2003, 3:16 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Doug Donohoe
 */
public abstract class OptionMenuDialog extends DialogPhase implements ChangeListener, GuiUtils.CheckListener
{
    //static Logger logger = LogManager.getLogger(OptionMenuDialog.class);

    private DDHtmlArea text_;
    private DDPanel data_;
    protected DDButton defaultButton_;
    protected List<DDOption> options_ = new ArrayList<DDOption>();

    /**
     * Create contents
     */
    @Override
    public JComponent createDialogContents()
    {
        defaultButton_ = super.okayButton_;

        // holds data we are gathering
        String sDefaultHelpName = gamephase_.getString("dialog-help-name", "empty");
        data_ = new DDPanel(sDefaultHelpName);
        BorderLayout layout = (BorderLayout) data_.getLayout();
        layout.setVgap(2);
        data_.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Text area for displaying information
        text_ = new DDHtmlArea(GuiManager.DEFAULT, "HelpTextDialog");
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());
        text_.doLayout();
        text_.setPreferredSize(new Dimension(10, getTextPreferredHeight()));
        data_.add(text_, BorderLayout.SOUTH);

        return data_;
    }

    protected int getTextPreferredHeight()
    {
        return 90;
    }

    @Override
    public void start()
    {
        // add options here (instead of init) so we know which phase we came from
        data_.add(getOptions(), BorderLayout.CENTER);

        // add listeners
        fillOptions();
        addListeners(options_);

        // set help text
        getDialog().setHelpTextWidget(text_);
        getDialog().showHelp(data_); // init help

        // check button states and focus
        checkButtons();

        // super
        super.start();
    }

    /**
     * add listeners to all options in list
     */
    public void addListeners(List<DDOption> options)
    {
        for (DDOption option : options)
        {
            checkChangeListener(option);
        }
    }

    /**
     * add listeners to all options in list
     */
    public void removeListeners(List<DDOption> options)
    {
        for (DDOption option : options)
        {
            option.removeChangeListener(this);
        }
    }

    /**
     * verify listener is on given option
     */
    protected void checkChangeListener(DDOption option)
    {
        option.removeChangeListener(this);
        option.addChangeListener(this);
    }

    /**
     * Returns true
     */
    @Override
    public boolean processButton(GameButton button)
    {
        if (button.getName().equals(defaultButton_.getName()))
        {
            okayButton();
            return super.processButton(button);
        }
        else if (button.getName().startsWith("reset"))
        {
            resetButton();
        }
        else if (button.getName().startsWith("cancel"))
        {
            return super.processButton(button);
        }
        return true;
    }

    /**
     * Okay button press
     */
    protected void okayButton()
    {
    }

    /**
     * reset functionality - default is to reset from default values
     */
    protected void resetButton()
    {
        fillOptions();
        for (DDOption option : options_)
        {
            option.resetToDefault();
        }
    }

    protected void fillOptions()
    {
        options_.clear();
        GuiUtils.getDDOptions(data_, options_);
    }

    /**
     * Enabled okay button if everything is valid
     */
    protected void checkButtons()
    {
        boolean bValid = isDDOptionsValid() && isValidCheck();

        if (defaultButton_ != null) defaultButton_.setEnabled(bValid);
    }

    /**
     * return true if all dd options contained within this dialog are valid
     */
    protected boolean isDDOptionsValid()
    {
        fillOptions();
        DDOption dd;
        boolean bValid = true;
        for (int i = 0; bValid && i < options_.size(); i++)
        {
            dd = options_.get(i);
            if (!dd.isEnabled()) continue;
            bValid &= dd.isValidData();
        }
        return bValid;
    }

    /**
     * Extra validation check called during checkButtons()
     */
    protected boolean isValidCheck()
    {
        return true;
    }

    /**
     * Get component with options, also fill array with same options
     */
    protected abstract JComponent getOptions();

    /**
     * An option changed
     */
    public void stateChanged(ChangeEvent e)
    {
        checkButtons();
    }

}
