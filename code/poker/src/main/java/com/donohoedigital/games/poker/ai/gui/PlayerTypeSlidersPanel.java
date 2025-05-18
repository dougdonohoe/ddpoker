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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class PlayerTypeSlidersPanel extends DDPanel
{
    public static ChangeListener changeListener = null;

    private ListPanel listPanel_;
    private DDHtmlArea helpPanel_;

    public PlayerTypeSlidersPanel(String sStyle)
    {
        listPanel_ = new ListPanel(SliderItemPanel.class, sStyle);
        helpPanel_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);

        listPanel_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listPanel_.setHelpPanel(helpPanel_);
        helpPanel_.setPreferredSize(new Dimension(100, 80));
        helpPanel_.setBorder(BorderFactory.createEmptyBorder(4,8,0,8));

        DDLabelBorder playStyleBorder = new DDLabelBorder("playstyle", sStyle);
        playStyleBorder.add(listPanel_, BorderLayout.CENTER);

        add(playStyleBorder, BorderLayout.CENTER);
        add(helpPanel_, BorderLayout.SOUTH);
    }

    public static class SliderItemPanel extends ListItemPanel
    {
        private DDPanel borderPanel_;
        private MyPillPanel pill_ = null;
        private DDLabel label_ = null;
        private DDLabel disabled_ = null;
        private DDSlider slider_ = null;
        private DDLabel value_ = null;
        private boolean bUpdating_ = false;

        public SliderItemPanel(ListPanel panel, Object item, String sStyle)
        {
            super(panel, item, sStyle);

            AIStrategyNode itemx = (AIStrategyNode) item;

            borderPanel_ = new DDPanel();
            borderPanel_.setBorder(BorderFactory.createEmptyBorder(0, itemx.getIndent() * 16, 0, 0));

            pill_ = new MyPillPanel("DashboardHeader", itemx.getLabel());
            pill_.setExpanded(itemx.isExpanded());
            pill_.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    pillClicked();
                }
            });

            value_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            value_.setPreferredWidth(30);
            value_.setHorizontalAlignment(DDLabel.RIGHT);

            label_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            label_.setText(itemx.getLabel());

            if (itemx.getChildCount() > 0)
            {
                borderPanel_.add(pill_, BorderLayout.CENTER);
            }
            else
            {
                borderPanel_.add(label_, BorderLayout.CENTER);
            }

            slider_ = new DDSlider(GuiManager.DEFAULT, sStyle);
            slider_.setMinimum(0);
            slider_.setMaximum(100);
            slider_.setSnapToTicks(true);

            if (itemx.isEnabled())
            {
                slider_.setValue(itemx.getValue());
                slider_.setVisible(!itemx.isExpanded());
                slider_.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        AIStrategyNode item = (AIStrategyNode)getItem();

                        value_.setText(Integer.toString(slider_.getValue()));

                        //if (!bUpdating_ && !((DDSlider)e.getSource()).getValueIsAdjusting())
                        if (!bUpdating_)
                        {
                            item.setValue(slider_.getValue());
                            item.propagateValueChange();
                            if (PlayerTypeSlidersPanel.changeListener != null)
                            {
                                PlayerTypeSlidersPanel.changeListener.stateChanged(e);    
                            }
                        }

                    }
                });

                borderPanel_.add(slider_, BorderLayout.EAST);
            }
            else
            {
                disabled_ = new DDLabel("stratnotyetimplemented", sStyle);
                disabled_.setPreferredSize(slider_.getPreferredSize());
                disabled_.setHorizontalAlignment(JTextField.CENTER);
                borderPanel_.add(disabled_, BorderLayout.EAST);
            }

            add(borderPanel_, BorderLayout.CENTER);
        }

        public void update()
        {
            bUpdating_ = true;

            try
            {
                AIStrategyNode item = (AIStrategyNode)getItem();

                if (item.isEnabled())
                {
                    value_.setText(Integer.toString(item.getValue()));
                }
                else
                {
                    value_.setText("");
                }

                slider_.setValue(item.getValue());
                slider_.setVisible(!item.isExpanded());

                if (item.isExpanded() && item.isEnabled())
                {
                    if (value_.getParent() != null)
                    {
                        remove(value_);
                    }
                }
                else
                {
                    if (value_.getParent() == null)
                    {
                        add(value_, BorderLayout.EAST);
                    }
                }
            }
            finally
            {
                bUpdating_ = false;
            }
        }

        private void pillClicked()
        {
            AIStrategyNode item = (AIStrategyNode) item_;

            if (pill_.isExpanded() == item.isExpanded()) return;

            if (pill_.isExpanded())
            {
                item.setExpanded(true);

                addChildPanels(item);
            }
            else
            {
                item.setExpanded(false);

                removeChildPanels(item);
            }

            ListPanel listPanel = getListPanel();

            listPanel.updateItemPanels();
        }

        private void addChildPanels(AIStrategyNode item)
        {
            ListPanel listPanel = getListPanel();

            int index = listPanel.getItemIndex(item);

            for (int i = item.getChildCount() - 1; i >= 0; --i)
            {
                AIStrategyNode child = item.getChild(i);

                listPanel.insertItem(index + 1, child, false);

                if (child.isExpanded())
                {
                    ((SliderItemPanel)listPanel.getItemPanel(index + 1)).pill_.setExpanded(true);

                    addChildPanels(child);
                }
            }

        }

        private void removeChildPanels(AIStrategyNode item)
        {
            ListPanel listPanel = getListPanel();

            for (int i = item.getChildCount() - 1; i >= 0; --i)
            {
                AIStrategyNode child = item.getChild(i);

                int childIndex = listPanel.getItemIndex(child);

                if (childIndex >= 0)
                {
                    listPanel.removeItem(childIndex);
                }

                removeChildPanels(child);
            }
        }

        public String getHelpText()
        {
            return ((AIStrategyNode)getItem()).getHelpText();
        }
    }

    public static class MyPillPanel extends PillPanel
    {
        DDImageCheckBox check_;

        public MyPillPanel(String sStyle, String sText)
        {
            super(sStyle);
            setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));

            check_ = new DDImageCheckBox("dashboard", sStyle);
            check_.setVerticalAlignment(SwingConstants.TOP);
            check_.setText(sText);
            add(check_, BorderLayout.CENTER);
        }

        public void setText(String s)
        {
            check_.setText(s);
        }

        public void addActionListener(ActionListener l)
        {
            check_.addActionListener(l);
        }

        public void removeActionListener(ActionListener l)
        {
            check_.removeActionListener(l);
        }

        public void setExpanded(boolean b)
        {
            check_.setSelected(b);
        }

        public boolean isExpanded()
        {
            return check_.isSelected();
        }
    }

    public void setItems(ArrayList items)
    {
        listPanel_.setItems(items);
    }

    public DDHtmlArea getHelpPanel()
    {
        return helpPanel_;
    }
}
