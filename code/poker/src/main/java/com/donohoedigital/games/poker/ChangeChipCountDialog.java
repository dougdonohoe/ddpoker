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
 * ChangeChipCountDialog.java
 *
 * Created on April 15, 2005, 9:28 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class ChangeChipCountDialog extends DialogPhase
{
    //static Logger logger = LogManager.getLogger(ChangeChipCountDialog.class);

    public static final String PARAM_PLAYER = "player";

    private PokerPlayer player_;
    private DDNumberSpinner amount_;
    private PokerTable table_;

    /**
     * create chat ui
     */
    public JComponent createDialogContents() 
    {
        player_ = (PokerPlayer) gamephase_.getObject(PARAM_PLAYER);
        ApplicationError.assertNotNull(player_, "No 'player' in params");
        table_ = player_.getTable();

        // contents
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(0, 5);
        base.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
        DDLabel name = new DDLabel(GuiManager.DEFAULT, STYLE);
        name.setText(PropertyConfig.getMessage("msg.changechipcount", player_.getName()));
        base.add(name, BorderLayout.WEST);
        
        amount_ = new ChipText();
        amount_.setValue(player_.getChipCount());
        base.add(amount_, BorderLayout.CENTER);
        
        checkButtons();
        
        return base;
    }
    
    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return amount_.getTextField();
    }
    
    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) 
    {
        if (button.getName().equals(okayButton_.getName()))
        {
            int nOldChip = player_.getChipCount();
            int nNewChip = PokerUtils.roundAmountMinChip(table_, amount_.getValue());
            if (nNewChip == 0) nNewChip = table_.getMinChip();
            if (nNewChip != nOldChip)
            {
                PokerGame game = (PokerGame) context_.getGame();
                game.addExtraChips(nNewChip - nOldChip);
                player_.setChipCount(nNewChip);
                player_.adjustChipCountAtStart(nNewChip - nOldChip);
            }
        }

        return super.processButton(button);
    }
    
    /**
     * Enable buttons
     */
    private void checkButtons()
    {
        boolean bOkay = amount_.isValidData();
        okayButton_.setEnabled(bOkay);
    }

    /**
     * money text field vaildation
     */
    private class ChipText extends DDNumberSpinner implements ChangeListener
    {
        public ChipText()
        {
            // min is smallest chip - don't let a player go bust cuz they could be in a hand
            super(table_.getMinChip(), Math.max(9999999, player_.getChipCount()), table_.getMinChip(), GuiManager.DEFAULT, STYLE);
            setBigStep(1000);
            setUseBigStep(true);
            addChangeListener(this);
            setEditable(true);
            resetPreferredSize();
        }

        public void stateChanged(ChangeEvent e)
        {
            checkButtons();
        }
    }
}
