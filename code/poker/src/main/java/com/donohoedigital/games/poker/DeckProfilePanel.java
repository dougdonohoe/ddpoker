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
 * DeckProfilePanel.java
 *
 * Created on May 18, 2004, 2:00 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author  Doug Donohoe
 */
public class DeckProfilePanel extends DDPanel implements ChangeListener
{
    static Logger logger = LogManager.getLogger(DeckProfilePanel.class);
    
    private static final String COMMON_NAME = "deckback";
    public static final String DECK_PROFILE = "deck";

    private DDLabelBorder displayBorder_;
    private ProfileList profileList_;
    private DeckCardPanel card_;
        
    /** 
     * Get component with options, also fill array with same options
     */
    public DeckProfilePanel(GameEngine engine, GameContext context, String STYLE)
    {
        super(COMMON_NAME);

        // holds data we are gathering
        setBorderLayoutGap(2, 10);
        setBorder(BorderFactory.createEmptyBorder(2,10,0,10));
        
        // get current profile list and sort it
        List<BaseProfile> profiles = DeckProfile.getProfileList();
        Collections.sort(profiles);

        // preview
        card_ = new DeckCardPanel();
        displayBorder_ = getPreviewPanel(card_, STYLE);
        add(displayBorder_, BorderLayout.CENTER);
        
        // all decks list
        DDLabelBorder pborder = new DDLabelBorder("deckbacks", STYLE);
        pborder.setPreferredSize(new Dimension(200, 0));
        add(pborder, BorderLayout.WEST);
        profileList_ = new DeckProfileList(engine, context, profiles, STYLE, DECK_PROFILE, COMMON_NAME, "pokericon16png", false);
        profileList_.addChangeListener(this);
        pborder.add(profileList_, BorderLayout.CENTER);

        // select 1st row
        profileList_.selectInit();
    }
    
    static DDLabelBorder getPreviewPanel(DeckCardPanel card, String STYLE)
    {
        // gap
        int g = 12;
        DDLabelBorder displayBorder = new DDLabelBorder("deckpreview", STYLE);
        Dimension size = new Dimension((int)CardPiece.CARD_W/2, (int)CardPiece.CARD_H/2);
        DDPanel format = new DDPanel();
        
        format.setBorder(BorderFactory.createEmptyBorder(0,g,g,g));
        format.setLayout(new XYLayout(size.width + g*2, size.height+g*2));
        format.add(card, new XYConstraints(g,g, size.width+g, size.height+g));
        displayBorder.setLayout(new CenterLayout());
        displayBorder.add(format);
        return displayBorder;
    }
    
    
    /**
     * Our list editor
     */
    private class DeckProfileList extends ProfileList
    {
        
        public DeckProfileList(GameEngine engine, GameContext context, List<BaseProfile> profiles,
                               String sStyle,
                               String sMsgName,
                               String sPanelName,
                               String sIconName,
                               boolean bUseCopyButton)
        {
            super(engine, context, profiles, sStyle, sMsgName, sPanelName, sIconName, bUseCopyButton);
            removeButton(super.buttonEdit_);
        }
        
        /** 
         * not used
         */
        protected BaseProfile createEmptyProfile() {
            return new DeckProfile();
        }
        
        /**
         * not used
         */
        protected BaseProfile copyProfile(BaseProfile profile, boolean bForEdit) {
            return new DeckProfile((DeckProfile)profile);
        }
        
    }

    /**
     * profile selected logic
     */
    public void stateChanged(ChangeEvent e)
    {
        DeckProfile pp = (DeckProfile) profileList_.getSelectedProfile();

        DeckProfile selected;
        if (pp != null)
        {
            // set current selected profile and update stats label
            selected = pp;
            BufferedImage img = ImageDef.getBufferedImage(selected.getFile());
            if (img == null)
            {
                // todo: warn/display invalid image
                card_.deck = null;
            }
            else
            {
                card_.deck = new ImageComponent(img, 1.0d);
            }
            displayBorder_.setText(PropertyConfig.getMessage("labelborder.deckpreview.label2", selected.getName()));
            displayBorder_.repaint();
        }
        else
        {
            card_.deck = null;
            displayBorder_.setText(PropertyConfig.getMessage("labelborder.deckpreview.label"));
            displayBorder_.repaint();
        }
    }

    /**
     * class to display deck
     */
    static class DeckCardPanel extends CardPanel
    {
        ImageComponent deck;

        public DeckCardPanel()
        {
            super(new CardPreview());
        }

        public void paintComponent(Graphics g)
        {            
            if (deck == null) return;
            ((CardPreview)cp_).deck = deck;
            super.paintComponent(g);
        }
    }
    
    private static class CardPreview extends CardPiece
    {
        ImageComponent deck;
        public CardPreview()
        {
            super(null, null, null, true, 0);
            setShadow(false);
            setCard(Card.SPADES_A);
        }
        
        public boolean drawCardUp()
        {
            return false;
        }
        
        public ImageComponent getDeckImage()
        {
            return deck;
        }
    }
}
