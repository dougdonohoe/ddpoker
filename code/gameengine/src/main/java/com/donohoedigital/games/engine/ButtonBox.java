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
 * ButtonBox.java
 *
 * Created on November 17, 2002, 4:46 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author  Doug Donohoe
 */
public class ButtonBox extends DDPanel implements AncestorListener
{
    static Logger logger = LogManager.getLogger(ButtonBox.class);
    
    public static final String PARAM_DEFAULT_BUTTON = "default-button";
    private HashMap buttons_ = new HashMap();
    private com.donohoedigital.gui.DDButton defaultButton_;
    private DDPanel buttonPanel_;
    
    /**
     * Defaults nGap to 5 (gap between buttons) and borderSpace to
     * 8 if vertical arrangement, 5 otherwise.
     */
    public ButtonBox(GameContext context, GamePhase gamephase, Phase phase, String sName,
                     boolean bVertical, boolean bSmallButton)
                        throws ApplicationError
    {
        this(context, gamephase, phase, sName, 8,
                (bVertical ? 8 : 4), bVertical, bSmallButton);
    }
    
    /** 
     * Creates a new instance of ButtonBox 
     */
    public ButtonBox(GameContext context, GamePhase gamephase, Phase phase, String sName,
                     int nGap, int nBorderSpace,
                     boolean bVertical, boolean bSmallButton)
                        throws ApplicationError
    {
        super(sName);
        
        addAncestorListener(this);
        
        List buttons = gamephase.getList("buttons");
        String sEnter = gamephase.getString(PARAM_DEFAULT_BUTTON);
        if (sEnter != null && sEnter.equals("NONE")) sEnter = null;
        
        buttonPanel_ = new DDPanel();             
        setBorder(BorderFactory.createEmptyBorder(bVertical ? nBorderSpace : 1,nBorderSpace,nBorderSpace,nBorderSpace));

        int nCols = 1;
        int nRows = 1;
        if (bVertical) nRows = 0;
        else nCols = 0;
        buttonPanel_.setLayout(new GridLayout(nRows, nCols, bVertical ? 0 : nGap, bVertical ? nGap : 0));

        // track default button
        boolean bDefaultSet = false;
        
        // loop through each button and add it to buttonbox
        if (buttons != null)
        {
            int nSize = buttons.size();
            String sButtonName;
            GlassButton button;
            EngineButtonListener listener;
            for (int i = 0; i < nSize; i++)
            {
                sButtonName = (String) buttons.get(i);
                listener = new EngineButtonListener(context, phase, sButtonName);
                button = new GlassButton(listener.getGameButton().getName(),
                                          bSmallButton ? "Glass":"GlassBig");
                button.addActionListener(listener);
                buttonPanel_.add(button);
                if (sEnter != null && button.getName().equals(sEnter))
                {
                    defaultButton_ = button;
                    bDefaultSet = true;
                }
                buttons_.put(button.getName(), button);
            }
        }
        
        if (sEnter != null && sEnter.length() > 0 && bDefaultSet == false)
        {
            logger.warn("Unable to set enter button " + sEnter + " (no matching button found)");
        }
        
        if (bVertical) {
            add(buttonPanel_, BorderLayout.NORTH);
        } else {
            DDPanel align = new DDPanel();
            align.setLayout(new CenterLayout());
            align.add(buttonPanel_, BorderLayout.CENTER);
            add(align, BorderLayout.SOUTH);
        }
    }
    
    public void removeButton(DDButton button)
    {
        buttonPanel_.remove(button);
    }
    
    public void addButton(DDButton button)
    {
        buttonPanel_.add(button);
    }
    
    /**
     * Return button with given name
     */
    public DDButton getButton(String sName)
    {
        return (DDButton) buttons_.get(sName);
    }

    /**
     * return button whose name starts with given string
     */
    public DDButton getButtonStartsWith(String sName)
    {
        Iterator iter = buttons_.keySet().iterator();
        String button;
        while (iter.hasNext())
        {
            button = (String) iter.next();
            if (button.startsWith(sName)) return getButton(button);
        }
        return null;
    }

    /**
     * Return list of DDButton's, keyed by name
     */
    public HashMap getButtons()
    {
        return buttons_;
    }
    
    /**
     * Get button associated with the enter key
     */
    public com.donohoedigital.gui.DDButton getDefaultButton()
    {
        return this.defaultButton_;
    }

    /**
     * When added to heirarchy, set default button
     */
    public void ancestorAdded(AncestorEvent event) {
        if (defaultButton_ != null)
        {
            JRootPane root = SwingUtilities.getRootPane(this);
            root.setDefaultButton(defaultButton_);
            //logger.debug("BUTTONBOX ROOT " + root.getName() +" Setting default button to " + ((defaultButton_ == null) ? "null" : defaultButton_.getName()));
        }
    }
    
    public void ancestorMoved(AncestorEvent event) {
    }
    
    /**
     * When removed from hierarchy, remove default button
     */
    public void ancestorRemoved(AncestorEvent event) {
        if (defaultButton_ != null)
        {
            JRootPane root = SwingUtilities.getRootPane(this);
            root.setDefaultButton(null);
            //logger.debug("BUTTONBOX ROOT " + root.getName() +" removing default button " + ((defaultButton_ == null) ? "null" : defaultButton_.getName()));
        }
    }
    
}
