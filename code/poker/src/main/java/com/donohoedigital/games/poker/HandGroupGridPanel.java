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

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class HandGroupGridPanel extends DDPanel implements ActionListener, KeyListener, FocusListener
{
    //static Logger logger = LogManager.getLogger(HandGroupGridPanel.class);

    private HandButton handButtons[][] = new HandButton[Card.ACE + 1][Card.ACE + 1];
    private DDLabel summaryLabel_;

    private HandGroup group_ = null;
    private boolean bEnabled_ = true;
    private int lastFocusX_ = -1;
    private int lastFocusY_ = -1;
    private JComponent FIRST, LAST;

    private List<HandGroup> alMutualExclusionList_ = null;

    private int count_ = 0;

    public HandGroupGridPanel(boolean bLabels)
    {
        this(null, bLabels);
    }

    public HandGroupGridPanel(HandGroup group, boolean bLabels)
    {
        super("Hand Group Grid", "HandGroupGrid");

        JPanel gridPanel = new DDPanel(GuiManager.DEFAULT, "HandGroupGrid");
        gridPanel.setLayout(new GridLayout(13, 13));

        for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
        {
            for (int rank2 = Card.ACE; rank2 >= Card.TWO; --rank2)
            {
                HandButton button = new HandButton(rank1, rank2, bLabels);
                button.addActionListener(this);
                gridPanel.add(button);
                handButtons[rank1][rank2] = button;
           }
        }

        FIRST = handButtons[Card.ACE][Card.ACE];
        LAST = handButtons[Card.TWO][Card.TWO];

        summaryLabel_ = new DDLabel("blank", "HandGroupDetails");
        summaryLabel_.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

        add(gridPanel, BorderLayout.CENTER);
        add(summaryLabel_, BorderLayout.SOUTH);

        gridPanel.setOpaque(false);

        if (group == null)
        {
            group = new HandGroup();
        }

        setHandGroup(group);

        //Dimension size = getPreferredSize();
        //setPreferredSize(new Dimension((int)size.getHeight() * 4 / 3, (int)size.getHeight()));
    }

    public void hideSummaryText()
    {
        remove(summaryLabel_);
    }

    public void setHandGroup(HandGroup group)
    {
        count_ = 0;

        group_ = group;

        for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
        {
            for (int rank2 = Card.ACE; rank2 >= Card.TWO; --rank2)
            {
                boolean toggled = (group != null) && group.contains(rank1, rank2, (rank2 < rank1));

                if (toggled) ++count_;

                handButtons[rank1][rank2].setSelected(toggled);

                if (alMutualExclusionList_ != null)
                {
                    boolean highlighted = false;

                    for (int i = 0; i < alMutualExclusionList_.size() - 1; ++i)
                    {
                        HandGroup exclusionGroup = alMutualExclusionList_.get(i);

                        if (exclusionGroup == group) continue;

                        if (exclusionGroup.contains(rank1, rank2, (rank2 < rank1)))
                        {
                            highlighted = true;
                            break;
                        }
                    }

                    handButtons[rank1][rank2].setHighlighted(highlighted);
                }
            }
        }

        setSummaryText();
    }

    public HandGroup getHandGroup()
    {
        return group_;
    }

    public void setSummaryText()
    {
        if (count_ == 0)
        {
            summaryLabel_.setText("");
        }
        else
        {
            summaryLabel_.setText(PropertyConfig.getMessage(
                    "msg.handgroup.summary." + (count_ == 1 ? "singular" : "plural"),
                    count_, PokerConstants.formatPercent(group_.getPercent())));
        }
    }

    public void setEnabled(boolean bEnabled)
    {
        bEnabled_ = bEnabled;
        for (int rank1 = Card.ACE; rank1 >= Card.TWO; --rank1)
        {
            for (int rank2 = Card.ACE; rank2 >= Card.TWO; --rank2)
            {
                handButtons[rank1][rank2].setEnabled(bEnabled_);
            }
        }
    }

    public boolean getEnabled()
    {
        return bEnabled_;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof HandButton)
        {
            HandButton button = (HandButton)e.getSource();

            button.setHighlighted(false);

            if (button.isSelected())
            {
                ++count_;

                if (button.suited)
                {
                    group_.setContainsSuited(button.rank1, button.rank2, true);

                    if (alMutualExclusionList_ != null)
                    {
                        for (int i = alMutualExclusionList_.size()-1; i >= 0; --i)
                        {
                            HandGroup exclusionGroup = alMutualExclusionList_.get(i);

                            if (exclusionGroup != group_)
                            {
                                exclusionGroup.setContainsSuited(button.rank1, button.rank2, false);
                            }
                        }
                    }
                }
                else if (button.pair)
                {
                    group_.setContainsPair(button.rank1, true);
                    if (alMutualExclusionList_ != null)
                    {
                        for (int i = alMutualExclusionList_.size() - 1; i >= 0; --i)
                        {
                            HandGroup exclusionGroup = alMutualExclusionList_.get(i);

                            if (exclusionGroup != group_)
                            {
                                exclusionGroup.setContainsPair(button.rank1, false);
                            }
                        }
                    }
                }
                else
                {
                    group_.setContainsOffsuit(button.rank1, button.rank2, true);
                    if (alMutualExclusionList_ != null)
                    {
                        for (int i = alMutualExclusionList_.size() - 1; i >= 0; --i)
                        {
                            HandGroup exclusionGroup = alMutualExclusionList_.get(i);

                            if (exclusionGroup != group_)
                            {
                                exclusionGroup.setContainsOffsuit(button.rank1, button.rank2, false);
                            }
                        }
                    }
                }
            }
            else
            {
                --count_;

                if (button.suited)
                {
                    group_.setContainsSuited(button.rank1, button.rank2, false);
                }
                else if (button.pair)
                {
                    group_.setContainsPair(button.rank1, false);
                }
                else
                {
                    group_.setContainsOffsuit(button.rank1, button.rank2, false);
                }
            }

            setSummaryText();

            firePropertyChange("HANDS", null, null);
        }
    }

    ////
    //// Key/Focus listener for custom focus changes
    ////

    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * handle custom focus change
     */
    public void keyPressed(KeyEvent e)
    {
        HandButton button = (HandButton) e.getSource();
        int x = button.rank1;
        int y = button.rank2;

        switch (e.getKeyCode())
        {
            case KeyEvent.VK_TAB:
                if (e.isShiftDown())
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(FIRST);
                else
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(LAST);
                return;

            case KeyEvent.VK_UP:
                x += 1;
                break;

            case KeyEvent.VK_DOWN:
                x -= 1;
                break;

            case KeyEvent.VK_RIGHT:
                y -= 1;
                break;

            case KeyEvent.VK_LEFT:
                y += 1;
                break;
        }

        if (x > Card.ACE) x = Card.TWO;
        if (x < Card.TWO) x = Card.ACE;
        if (y > Card.ACE) y = Card.TWO;
        if (y < Card.TWO) y = Card.ACE;

        lastFocusX_ = x;
        lastFocusY_ = y;

        handButtons[x][y].requestFocus();

    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void focusGained(FocusEvent e)
    {
        if (!(e.getOppositeComponent() instanceof HandButton))
        {
            HandButton lastFocus = (lastFocusX_ == -1 || lastFocusY_ == -1) ? null : handButtons[lastFocusX_][lastFocusY_];
            if (e.getSource() != lastFocus && lastFocus != null)
            {
                lastFocus.requestFocus();
                return;
            }
        }
        HandButton button = (HandButton) e.getSource();
        lastFocusX_ = button.rank1;
        lastFocusY_ = button.rank2;
    }

    public void focusLost(FocusEvent e)
    {
    }

    /**
     * Custom button
     */
    private class HandButton extends DDButton
    {
        int rank1;
        int rank2;
        boolean suited;
        boolean pair;

        @SuppressWarnings({"unchecked"})
        public HandButton(int rank1, int rank2, boolean bLabel)
        {
            super("blank", (rank2 < rank1 ? "HandGroupGrid.suited" : (rank2 > rank1) ? "HandGroupGrid.offsuit" : "HandGroupGrid.pair"));

            this.rank1 = rank1;
            this.rank2 = rank2;
            this.suited = (rank2 < rank1);
            this.pair = (rank2 == rank1);

            if (bLabel)
            {
                String label;

                if (suited)
                {
                    label = Card.getRankSingle(rank1) + Card.getRankSingle(rank2) + "s";
                }
                else
                {
                    label = Card.getRankSingle(rank2) + Card.getRankSingle(rank1);
                }

                setText(label);
            }

            setBorderGap(0, 0, 0, 0);
            setIsToggle(true);
            setFocusPainted(false);
            setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
            setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
            addKeyListener(HandGroupGridPanel.this);
            addFocusListener(HandGroupGridPanel.this);

            setDisableMode(DDButton.DISABLED_BORDERLESS);
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (hasFocus())
            {
                g.setColor(new Color(255,255,255,100));
                g.fillRect(2,2,getWidth()-4, getHeight()-4);
            }
        }
    }

    public void setMutualExclusionList(HandSelectionScheme list)
    {
        alMutualExclusionList_ = list.getHandGroups();
    }

    public DDLabel getSummaryLabel()
    {
        return summaryLabel_;
    }

    public void setBackground(int rank1, int rank2, boolean suited, Color bg)
    {
        if (suited && rank2 > rank1)
        {
            int swap = rank1;
            rank1 = rank2;
            rank2 = swap;
        }
        handButtons[rank1][rank2].setBackground(bg);
    }
}
