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
/*
 * DialogBackground.java
 *
 * Created on November 24, 2002, 9:11 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DialogBackground extends DDPanel 
{
    //static Logger logger = LogManager.getLogger(DialogBackground.class);
    
    DDPanel dialogbox_;
    ButtonBox buttonbox_;
    DDCheckBox checkbox_;
    DDPanel buttonbase_;
    
    /** 
     * Creates a new instance of DialogBackground 
     */
    public DialogBackground(GameContext context, GamePhase gamephase, Phase phase,
                            boolean bNoShowOption, String sNoShowCheckboxName)
    {
        String sDialogStyle = gamephase.getString("dialog-style", "default");
        String sDefaultHelpName = gamephase.getString("dialog-help-name", "welcome");
        
        // background image
        JComponent parent = this;
        String sImageName = gamephase.getString("dialog-background-image");
        if (sImageName != null)
        {       
            // do stuff here
            ImageComponent ic = new ImageComponent(sImageName, 1.0);
            ic.setPreferredSize(null); // reset preferred size so we don't effect layout
            add(ic, BorderLayout.CENTER);
            ic.setLayout(new BorderLayout());
            ic.setScaleToFit(false);
            ic.setTile(true);
            parent = ic;
        }
        
        dialogbox_ = new DDPanel(sDefaultHelpName, sDialogStyle);
        
        // dialog buttons
        if (phase != null)
        {
            buttonbase_ = new DDPanel();
            dialogbox_.add(buttonbase_, BorderLayout.SOUTH);

            buttonbox_ = new ButtonBox(context, gamephase, phase, "empty", false, true);
            buttonbase_.add(buttonbox_, BorderLayout.CENTER);

            if (bNoShowOption)
            {
                checkbox_ = new DDCheckBox(sNoShowCheckboxName, sDialogStyle);
                checkbox_.setIconBackgroundPainted(false);
                DDPanel checkbase = new DDPanel();
                checkbase.add(checkbox_, BorderLayout.WEST);
                checkbase.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                buttonbase_.add(checkbase, BorderLayout.SOUTH);
            }

            // spacer to ensure minimum size
            DDPanel spacer = new DDPanel();


            int nMinWidth = gamephase.getInteger("dialog-minwidth", 300);
            spacer.setPreferredSize(new Dimension(nMinWidth, 0));
            dialogbox_.add(spacer, BorderLayout.NORTH);
        }
        parent.add(dialogbox_, BorderLayout.CENTER);
    }
    
    public void setCenterContents(JComponent c)
    {
        dialogbox_.add(c, BorderLayout.CENTER);
    }
    
    public ButtonBox getButtonBox()
    {
        return buttonbox_;
    }
    
    public DDCheckBox getNoShowCheckBox()
    {
        return checkbox_;
    }
    
    /**
     * Return panel holding buttons - can use to put something in any
     * area besides BorderLayout.CENTER
     */
    public DDPanel getButtonBase()
    {
        return buttonbase_;
    }
}
