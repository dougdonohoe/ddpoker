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
 * DescriptionDialog.java
 *
 * Created on April 29, 2004, 02:18 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.gui.*;
import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.comms.*;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.geom.*;
import java.text.*;
import java.beans.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DescriptionDialog extends DialogPhase
{
    //static Logger logger = LogManager.getLogger(DescriptionDialog.class);
    
    public static String PARAM_DESC = "desc";
    
    private DDTextArea desc_;
    
    /**
     * create chat ui
     */
    public JComponent createDialogContents() 
    {
        String sDesc = gamephase_.getString(PARAM_DESC);
        String sLabelName = gamephase_.getString("desc-label-name", "description");

        // contents
        DDPanel base = new DDPanel();
        BorderLayout layout = (BorderLayout) base.getLayout();
        layout.setVgap(5);
        base.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
        DDLabel name = new DDLabel(sLabelName, STYLE);
        base.add(name, BorderLayout.NORTH);
        
        desc_ = new DDTextArea(GuiManager.DEFAULT, STYLE);
        JScrollPane scroll = new JScrollPane(desc_);
        scroll.setBackground(desc_.getBackground());
        scroll.setForeground(desc_.getForeground());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLoweredBevelBorder());
        scroll.setPreferredSize(new Dimension(600, 200));
        base.add(scroll, BorderLayout.CENTER);
        desc_.setTextLengthLimit(500);
        desc_.setWrapStyleWord(true);
        desc_.setLineWrap(true);
        desc_.setText(sDesc);
        desc_.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        
        return base;
    }
    
    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return desc_;
    }
    
    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) 
    {   
        setResult(null);
        if (button.getName().equals(okayButton_.getName()))
        {
            // okay
            String sText = desc_.getText();
            if (sText.length() > 0)
            {
                setResult(sText);
            }
        }
            
        removeDialog();
        return true;
    }
}
