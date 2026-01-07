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
 * DeckDialog.java
 *
 * Created on May 23, 2004, 7:43 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.beans.*;
import java.io.*;
import java.nio.channels.*;

/**
 *
 * @author  Doug Donohoe
 */
public class DeckDialog extends DialogPhase implements PropertyChangeListener
{
    static Logger logger = LogManager.getLogger(DeckDialog.class);
    
    private DeckProfile profile_;
    private JFileChooser choose_;
    private File selected_;
    private boolean bRegistered_;
    private DeckProfilePanel.DeckCardPanel card_;
    private DDLabelBorder displayBorder_;
    
    /**
     * create chat ui
     */
    public JComponent createDialogContents() 
    {
        profile_ = (DeckProfile) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");
        
        // contents
        DDPanel base = new DDPanel();
        BorderLayout layout = (BorderLayout) base.getLayout();
        layout.setVgap(5);
        base.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
        // Poker 1.2 - allow choosing deck regardless if user registered
        bRegistered_ = true; // UserRegistration.isRegistered();
        if (!engine_.isDemo() && bRegistered_)
        {
            DDLabel name = new DDLabel("deckimage", STYLE);
            name.setHorizontalAlignment(SwingConstants.CENTER);
            base.add(name, BorderLayout.NORTH);
            
            card_ = new DeckProfilePanel.DeckCardPanel();
            displayBorder_ = DeckProfilePanel.getPreviewPanel(card_, STYLE);
            DDPanel format = new DDPanel();
            format.add(displayBorder_);
            format.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        
            choose_ = new DDFileChooser("deckimage", STYLE, engine_.getPrefsNode().getPrefs());
            choose_.addChoosableFileFilter(new DeckProfile.DeckFilter());
            choose_.setAccessory(format);
            choose_.addPropertyChangeListener(this);
            base.add(choose_, BorderLayout.CENTER);
        
            checkButtons();
        }
        else
        {
            DDLabel name = new DDLabel(engine_.isDemo() ? "deckimagedemo" : "deckimagereg", "DisplayMessage");
            base.add(name, BorderLayout.NORTH);
        }
        
        return base;
    }
    
    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return choose_;
    }
    
    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) 
    {   
        Boolean bResult = Boolean.FALSE;
        
        if (!engine_.isDemo() && bRegistered_ && button.getName().equals(okayButton_.getName()))
        {
            // okay
            // copy file to save dir
            File dir = DeckProfile.getProfileDir(DeckProfile.DECK_DIR);
            File dest = null;
            try {
                dest = new File(dir, selected_.getName());
                // Create channel on the source
                FileChannel srcChannel = new FileInputStream(selected_).getChannel();

                // Create channel on the destination
                FileChannel dstChannel = new FileOutputStream(dest).getChannel();

                // Copy file contents from source to destination
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

                // Close the channels
                srcChannel.close();
                dstChannel.close();
                
                // remember file
                profile_.setFile(dest);
                bResult = Boolean.TRUE;
                
            } catch (Exception e) {
                logger.error("Unable to copy " + selected_.getAbsolutePath() + " to " +
                                    dir.getAbsolutePath());
                logger.error(Utils.formatExceptionText(e));
                EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.deck.copyfailed",selected_.getName(),
                                                    dir.getAbsolutePath()));
                return false;
            }            
        }
            
        removeDialog();
        setResult(bResult);

        return true;
    }

    /**
     * msg text change
     */
    public void propertyChange(PropertyChangeEvent evt) 
    {
        String sName = evt.getPropertyName();
        if (sName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY))
        {
            selected_ = choose_.getSelectedFile();
            if (selected_ != null)
            {
                // set current selected profile and update stats label
                BufferedImage img = ImageDef.getBufferedImage(selected_);
                if (img == null)
                {
                    // todo: warn/display invalid image
                    card_.deck = null;
                }
                else
                {
                    card_.deck = new ImageComponent(img, 1.0d);
                }
                //displayBorder_.setText(PropertyConfig.getMessage("labelborder.deckpreview.label2", selected_.getName()));
                displayBorder_.repaint();
            }
            else
            {
                card_.deck = null;
                //displayBorder_.setText(PropertyConfig.getMessage("labelborder.deckpreview.label"));
                displayBorder_.repaint();
            }
        }
        checkButtons();
    }
    
    /**
     * Enable buttons
     */
    private void checkButtons()
    {
        okayButton_.setEnabled(selected_ != null);
    }
}
