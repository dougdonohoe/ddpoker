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

import com.donohoedigital.gui.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CardSelectorPanel extends DDPanel implements ActionListener
{
    CardPiece cardPieces[][] = new CardPiece[CardSuit.SPADES_RANK + 1][Card.ACE + 1];
    ButtonPanel cardButtons[][] = new ButtonPanel[CardSuit.SPADES_RANK + 1][Card.ACE + 1];
    ButtonPanel unknownButtons[] = new ButtonPanel[CardSuit.SPADES_RANK + 1];

    private static final int INIT = -9;
    private static final int NONE = -10;
    private int selectedSuit_ = INIT;
    private int selectedRank_ = INIT;
    //private Deck deck_ = new Deck(false);

    public CardSelectorPanel(Card current, boolean bShowUnknown)
    {
        super("Card Selector", "CardSelector");
        setOpaque(true);
        setBackground(StylesConfig.getColor("PokerTable.menuitem.bg"));

        DDPanel grid = new DDPanel();
        grid.setLayout(new GridLayout(4,bShowUnknown?14:13,1,1));

        //DDPanel bottom = new DDPanel();
        //bottom.setLayout(new GridLayout(1,0,1,1));

        add(grid, BorderLayout.CENTER);
        //add(GuiUtils.CENTER(bottom), BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(2,1,1,1));

        Card card;
        Dimension size = new Dimension(20,26);
        ButtonPanel p;
        for (int suit = 0; suit < CardSuit.NUM_SUITS; suit++)
        {
            for (int rank = Card.TWO; rank <= Card.ACE; ++rank)
            {
                card = Card.getCard(suit, rank);
                p = addGridElement(card, suit, rank, size, grid);
                if (card.equals(current)) p.setSelected(true);
            }
            // unknown button
            if (bShowUnknown)
            {
                p = addGridElement(Card.BLANK, suit, -1, size, grid);
                if (Card.BLANK.equals(current)) p.setSelected(true);
            }
        }

        // close button - looks ugly, so leave off
        //addGridElement(null, -1, -1, size, bottom);
    }

//    public Deck getDeck()
//    {
//        return deck_;
//    }

    private ButtonPanel addGridElement(Card card, int suit, int rank, Dimension size, JComponent parent)
    {
        ButtonPanel cardButtonPanel;
        CardPiece cardPiece;
        JComponent c;
        if (card != null)
        {
            cardPiece = new CardThumbnail(card);
            cardButtonPanel = new CardButtonPanel(card, cardPiece);
            if (rank >= 0)
            {
                cardPieces[suit][rank] = cardPiece;
                cardButtons[suit][rank] = cardButtonPanel;
            }
            else
            {
                unknownButtons[suit] = cardButtonPanel;
            }
            c = new CardPanel(cardPiece);
        }
        else
        {
            cardButtonPanel = new ButtonPanel(GuiManager.DEFAULT, "CardSelector", false);
            ImageComponent x = new ImageComponent("redx-small", 1.0);
            x.setCentered(true);
            x.setScaleToFit(false);
            c = x;
        }
        c.setPreferredSize(size);
        cardButtonPanel.add(c);
        cardButtonPanel.setUseEmptyBorder(true);
        c.addMouseListener(cardButtonPanel);
        cardButtonPanel.addActionListener(this);
        parent.add(cardButtonPanel);
        return cardButtonPanel;
    }

    private class CardButtonPanel extends ButtonPanel
    {
        Card card;
        CardPiece cp;
        CardButtonPanel(Card card, CardPiece cp)
        {
            super(card.getDisplay(), "CardSelector", false);
            this.card = card;
            this.cp = cp;
        }

        public void setBackground(Color c)
        {
            if (cp != null) cp.setFillColor(c);
            super.setBackground(c);
        }
    }

    public void setSelected(Card c)
    {
        setSelected(c.getCardSuit().getRank(), c.getRank());
    }

    public void setSelected(int suit, int rank)
    {
        if (selectedSuit_ != suit && selectedRank_ != rank)
        {
            selectedSuit_ = suit;
            selectedRank_ = rank;
            fireSelectionChanged();
        }
    }

    public void reinit(Card c)
    {
        selectedSuit_ = INIT;
        selectedRank_ = INIT;

        int suit = c.getSuit();
        int rank = c.getRank();

        for (int s = CardSuit.CLUBS_RANK; s <= CardSuit.SPADES_RANK; ++s)
        {
            for (int r = Card.TWO; r <= Card.ACE; ++r)
            {
                cardButtons[s][r].mouseExited(null); // clear highlighted item too
                if ((suit == s) && (rank == r))
                {
                    cardButtons[s][r].setSelected(true);
                }
                else
                {
                    cardButtons[s][r].setSelected(false);
                }
            }

            if (unknownButtons[s] != null)
            {
                unknownButtons[s].mouseExited(null); // clear highlighted item too
                unknownButtons[s].setSelected(suit == CardSuit.UNKNOWN_RANK);
            }
        }
    }

    public Card getSelectedCard()
    {
        if (selectedSuit_ == INIT || selectedRank_ == INIT ||
            selectedSuit_ == NONE || selectedSuit_ == NONE) return null;

        if (selectedSuit_ == CardSuit.UNKNOWN_RANK && selectedRank_ == Card.UNKNOWN)
        {
            return Card.BLANK;
        }

        return Card.getCard(selectedSuit_, selectedRank_);
    }

    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        if (source instanceof CardButtonPanel)
        {
            setSelected(((CardButtonPanel)source).card);
        }
        else
        {
            setSelected(NONE, NONE);
        }
    }

    public void addActionListener(ActionListener listener)
    {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener)
    {
        listenerList.remove(ActionListener.class, listener);
    }

    private void fireSelectionChanged()
    {
        Object[] listeners = listenerList.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == ActionListener.class)
            {
                ((ActionListener) listeners[i + 1]).actionPerformed(new ActionEvent(this, 0, null));
            }
        }
    }

}
