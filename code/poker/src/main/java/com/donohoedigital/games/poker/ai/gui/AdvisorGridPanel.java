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

import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import java.awt.*;
import java.awt.event.*;

public class AdvisorGridPanel extends DDPanel
{
    private PocketMatrixByte outcomes_ = new PocketMatrixByte();
    private PocketMatrixString outcomeStrings_ = new PocketMatrixString();

    private boolean bPreFlop_ = true;
    private boolean bMinorGrid_ = true;

    public AdvisorGridPanel()
    {
        setDoubleBuffered(true);    
    }

    public void setPreFlop(boolean b)
    {
        bPreFlop_ = b;
    }

    public void setMinorGrid(boolean b)
    {
        bMinorGrid_ = b;
    }

    public void setOutcome(Hand hand, int outcome)
    {
        outcomes_.set(hand, (byte)(outcome+1));
    }

    public void setOutcome(int rank1, int rank2, boolean suited, int outcome, String outcomeString)
    {
        if (suited)
        {
            for (int suit = CardSuit.CLUBS_RANK; suit <= CardSuit.SPADES_RANK; ++suit)
            {
                outcomes_.set(Card.getCard(suit, rank1), Card.getCard(suit, rank2), (byte)(outcome+1));
                outcomeStrings_.set(Card.getCard(suit, rank1), Card.getCard(suit, rank2), outcomeString);
            }
        }
        else
        {
            for (int suit1 = CardSuit.DIAMONDS_RANK; suit1 <= CardSuit.SPADES_RANK; ++suit1)
            {
                for (int suit2 = CardSuit.CLUBS_RANK; suit2 < suit1; ++suit2)
                {
                    outcomes_.set(Card.getCard(suit1, rank1), Card.getCard(suit2, rank2), (byte)(outcome+1));
                    outcomeStrings_.set(Card.getCard(suit1, rank1), Card.getCard(suit2, rank2), outcomeString);
                }
            }
        }
    }

    public void paintComponent(Graphics g1)
    {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;

        int w = getWidth();
        int h = getHeight();

        int s = (Math.min(w, h) - 14 - (bMinorGrid_ ? 39 : 0)) / 52;
        int major = (!bMinorGrid_) ? s*4+1 : (s+1)*4;
        int minor = (!bMinorGrid_) ? s : s+1;

        int x;
        int y;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, major*13+1, major*13+1);

        for (int n = 1; n < 13; ++n)
        {
            g.setColor(Color.DARK_GRAY);

            g.drawLine(1, n*major, major*13-1, n*major);
            g.drawLine(n*major, 1, n*major, major*13-1);
        }

        g.setColor(Color.LIGHT_GRAY);

        for (x = 1; x < 13; ++x)
        {
            for (y = 1; y < 13; ++y)
            {
                g.drawLine(x*major,y*major,x*major,y*major);
            }
        }

        for (int yr = 0; yr < 13; ++yr)
        {
            for (int xr = 0; xr < 13; ++xr)
            {
                if (bPreFlop_)
                {
                    int card1 = Card.getCard(CardSuit.CLUBS, Card.ACE-xr).getIndex();
                    int card2 = Card.getCard((xr > yr ? CardSuit.CLUBS : CardSuit.DIAMONDS), Card.ACE-yr).getIndex();

                    g.setColor(getColor(card1, card2));
                    g.fillRect(xr*major+1, yr*major+1, major-1, major-1);
                }
                else if (xr > yr) // suited
                {
                    for (int ys = 0; ys < 4; ++ys)
                    {
                        int card1 = Card.getCard(CardSuit.CLUBS_RANK+ys, Card.ACE-xr).getIndex();
                        int card2 = Card.getCard(CardSuit.CLUBS_RANK+ys, Card.ACE-yr).getIndex();

                        x = xr*4+(ys&2);
                        y = yr*4+(ys&1)*2;

                        //int value = value = getValue(xr*4+ys, yr*4+ys);

                        g.setColor(getColor(card1, card2));
                        g.fillRect((x/4)*major + (x%4)*minor + 1, (y/4)*major + (y%4)*minor + 1,
                                s*2 + (bMinorGrid_ ? 1 : 0), s*2 + (bMinorGrid_ ? 1 : 0));

                    }
                }
                else
                {
                    for (int ys = 0; ys < 4; ++ys)
                    {
                        for (int xs = 0; xs < 4; ++xs)
                        {
                            if (xs == ys)
                            {
                                g.setColor(Color.BLACK);
                            }
                            else
                            {
                                x = xr*4+xs;
                                y = yr*4+ys;

                                int card1 = Card.getCard(CardSuit.CLUBS_RANK+xs, Card.ACE-xr).getIndex();
                                int card2 = Card.getCard(CardSuit.CLUBS_RANK+ys, Card.ACE-yr).getIndex();

                                if ((xr == yr) && (x > y))
                                {
                                    g.setColor(getColor(card2, card1));
                                }
                                else
                                {
                                    g.setColor(getColor(card1, card2));
                                }
                            }
                            
                            g.fillRect(xr*major + xs*minor + 1, yr*major + ys*minor + 1, s, s);
                        }
                    }
                }
            }
        }
    }

    private static final Color colors_[] = new Color[] {
        Color.RED.darker().darker(),
        Color.RED.darker(),
        Color.YELLOW.darker().darker(),
        Color.YELLOW.darker(),
        Color.GREEN.darker().darker(),
        Color.GREEN.darker()
    };

    public int getValue(int card1, int card2)
    {
        return outcomes_.get(card1, card2)-1;
    }

    protected Color getColor(int card1, int card2)
    {
        if (card1 == card2) return Color.BLACK;

        switch (getValue(card1, card2))
        {
            case RuleEngine.OUTCOME_FOLD:
                return colors_[1];
            case RuleEngine.OUTCOME_CHECK_RAISE:
            case RuleEngine.OUTCOME_SLOW_PLAY:
                return colors_[2];
            case RuleEngine.OUTCOME_CHECK:
            case RuleEngine.OUTCOME_LIMP:
            case RuleEngine.OUTCOME_CALL:
                return colors_[3];
            case RuleEngine.OUTCOME_CONTINUATION_BET:
            case RuleEngine.OUTCOME_STEAL:
            case RuleEngine.OUTCOME_SEMI_BLUFF:
            case RuleEngine.OUTCOME_TRAP:
                return colors_[4];
            case RuleEngine.OUTCOME_ALL_IN:
            case RuleEngine.OUTCOME_OPEN_POT:
            case RuleEngine.OUTCOME_BET:
            case RuleEngine.OUTCOME_RAISE:
                return colors_[5];
            default:
                return Color.BLACK;
        }
    }

    private Hand getExemplar(MouseEvent e)
    {
        int w = getWidth();
        int h = getHeight();

        int s = (Math.min(w, h) - 14 - (bMinorGrid_ ? 39 : 0)) / 52;
        int major = (!bMinorGrid_) ? s*4+1 : (s+1)*4;

        int d = major * 13;

        if (bPreFlop_)
        {
            int x = e.getX() * 13 / d;
            int y = e.getY() * 13 / d;

            if ((x < 0) || (x > 12) || (y < 0) || (y > 12)) return null;

            int rank1 = Card.ACE - x;
            int rank2 = Card.ACE - y;

            if (rank1 >= rank2)
            {
                return new Hand(Card.getCard(CardSuit.CLUBS, rank1), Card.getCard(CardSuit.DIAMONDS, rank2));
            }
            else
            {
                return new Hand(Card.getCard(CardSuit.CLUBS, rank2), Card.getCard(CardSuit.CLUBS, rank1));
            }
        }
        else
        {
            int x = e.getX() * 52 / d;
            int y = e.getY() * 52 / d;

            if ((x < 0) || (x > 51) || (y < 0) || (y > 51)) return null;

            int rank1 = Card.ACE - x/4;
            int rank2 = Card.ACE - y/4;

            if (rank1 < rank2)
            {
                int suit = ((x%4)&2) + ((y%4)/2);

                return new Hand(Card.getCard(suit, rank2), Card.getCard(suit, rank1));
            }
            else
            {
                int suit1;
                int suit2;

                if (x > y)
                {
                    // pair dups above diagonal
                    suit1 = y%4;
                    suit2 = x%4;
                }
                else if (x < y)
                {
                    suit1 = x%4;
                    suit2 = y%4;
                }
                else
                {
                    return null;
                }

                if (suit1 == suit2) return null;

                return new Hand(Card.getCard(suit1, rank1), Card.getCard(suit2, rank2));
            }
        }
    }

    public String getHand(MouseEvent e)
    {
        int w = getWidth();
        int h = getHeight();

        int s = (Math.min(w, h) - 14 - (bMinorGrid_ ? 39 : 0)) / 52;
        int major = (!bMinorGrid_) ? s*4+1 : (s+1)*4;

        int d = major * 13;

        if (bPreFlop_)
        {
            int x = e.getX() * 13 / d;
            int y = e.getY() * 13 / d;

            int rank1 = Card.ACE - x;
            int rank2 = Card.ACE - y;

            if (rank1 == rank2)
            {
                return Card.getRankSingle(rank1) + Card.getRankSingle(rank2);
            }
            else if (rank1 > rank2)
            {
                return Card.getRankSingle(rank1) + Card.getRankSingle(rank2) + "o";
            }
            else
            {
                return Card.getRankSingle(rank2) + Card.getRankSingle(rank1) + "s";
            }
        }
        else
        {
            int x = e.getX() * 52 / d;
            int y = e.getY() * 52 / d;

            int rank1 = Card.ACE - x/4;
            int rank2 = Card.ACE - y/4;

            if (rank1 < rank2)
            {
                int suit = ((x%4)&2) + ((y%4)/2);

                return  Card.getRankSingle(rank2) +
                        CardSuit.forRank(suit).getAbbr() +
                        Card.getRankSingle(rank1) +
                        CardSuit.forRank(suit).getAbbr();
            }
            else
            {
                int suit1;
                int suit2;

                if (x > y)
                {
                    // pair dups above diagonal
                    suit1 = y%4;
                    suit2 = x%4;
                }
                else
                {
                    suit1 = x%4;
                    suit2 = y%4;
                }

                if (suit1 == suit2) return null;

                return  Card.getRankSingle(rank1) +
                        CardSuit.forRank(suit1).getAbbr() +
                        Card.getRankSingle(rank2) +
                        CardSuit.forRank(suit2).getAbbr();
            }
        }
    }

    public int getOutcome(MouseEvent e)
    {
        Hand exemplar = getExemplar(e);

        if (exemplar == null)
        {
            return RuleEngine.OUTCOME_NONE;
        }
        else
        {
            return outcomes_.get(exemplar)-1;
        }
    }

    public String getOutcomeString(MouseEvent e)
    {
        Hand exemplar = getExemplar(e);

        if (exemplar == null)
        {
            return null;
        }
        else
        {
            return outcomeStrings_.get(exemplar);
        }
    }

    public void clear()
    {
        outcomes_.clear((byte)0);
    }
}