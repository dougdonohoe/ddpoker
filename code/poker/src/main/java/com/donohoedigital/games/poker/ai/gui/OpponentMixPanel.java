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
package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class OpponentMixPanel extends DDTabPanel
{
    private ListPanel typesList_;
    private TournamentProfile profile_;

    public OpponentMixPanel(TournamentProfile profile)
    {
        profile_ = profile;
    }

    public void createUI()
    {
        String sStyle = "OptionsDialog";

        List<BaseProfile> listItems = PlayerType.getProfileList();
        List<TypeListItem> typeItems = new ArrayList<TypeListItem>();

        Collections.sort(listItems);

        for (BaseProfile listItem : listItems)
        {
            PlayerType type = (PlayerType) listItem;
            typeItems.add(new TypeListItem(type, profile_.getPlayerTypePercent(type.getUniqueKey())));
        }

        typesList_ = new ListPanel(TypeListItemPanel.class, sStyle);
        typesList_.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        //typesList_.setSelectedIcon(ImageConfig.getImageIcon("pokericon16png"));
        typesList_.setPreferredSize(new Dimension(350, 300));
        typesList_.setItems(typeItems);
        typesList_.setFocusable(false);
        typesList_.updateItemPanels();

        // add
        add(typesList_, BorderLayout.WEST);
    }

    public void processOkay()
    {
        if (typesList_ == null) return; // UI never created (tab not viewed)

        List items = typesList_.getItems();

        for (int i = items.size()-1; i >= 0; --i)
        {
            TypeListItem item = (TypeListItem) items.get(i);
            profile_.setPlayerTypePercent(item.playerType_.getUniqueKey(), item.percent_);
        }
    }

    private class TypeListItem
    {
        private PlayerType playerType_;
        private int percent_;

        public TypeListItem(PlayerType playerType, int percent)
        {
            playerType_ = playerType;
            percent_ = percent;
        }
    }

    @SuppressWarnings({"PublicInnerClass"})
    public static class TypeListItemPanel extends ListItemPanel
    {
        private DDLabel lName_;
        private DDNumberSpinner fPercent_;
        private DDLabel lPercent_;
        private DDLabel lSymbol_;
        private DDPanel eastPanel_;

        public TypeListItemPanel(ListPanel panel, Object item, String sStyle)
        {
            super(panel, item, sStyle);
            setUseEmptyBorder(true);
            setProtected(true);
            setDisplayOnly(true);
            lName_ = new DDLabel(GuiManager.DEFAULT, sStyle);

            fPercent_ = new DDNumberSpinner(0, 100, 1, GuiManager.DEFAULT, sStyle);
            fPercent_.setEditable(true);
            fPercent_.setBigStep(10);
            addMouseListeners(fPercent_); // add manually since not in hierarchy when panel created
            fPercent_.addChangeListener(
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        percentValueChanged();
                    }
                }
            );

            addMouseWheelListener(fPercent_.getTextField());
            lName_.addMouseWheelListener(fPercent_.getTextField());

            lPercent_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            lPercent_.setHorizontalAlignment(DDTextField.RIGHT);
            lPercent_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            addMouseListeners(lPercent_);  // add manually since not in hierarchy when panel created

            lSymbol_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            lSymbol_.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

            eastPanel_ = new DDPanel(GuiManager.DEFAULT, sStyle);
            eastPanel_.setPreferredSize(new Dimension(75, (int) fPercent_.getPreferredSize().getHeight()));

            eastPanel_.add(lSymbol_, BorderLayout.EAST);

            add(lName_, BorderLayout.CENTER);
            add(eastPanel_, BorderLayout.EAST);
        }

        public void update()
        {
            TypeListItem item = (TypeListItem) getItem();
            String sName = item.playerType_.getName();

            lName_.setText(sName);

            if (((TypeListItem)getItem()).playerType_.isDefault())
            {
                if (fPercent_.getParent() == eastPanel_)
                {
                    eastPanel_.remove(fPercent_);
                }

                lPercent_.setText(Integer.toString(item.percent_));

                if (lPercent_.getParent() != eastPanel_)
                {
                    eastPanel_.add(lPercent_, BorderLayout.CENTER);
                }
            }
            else
            {
                if (lPercent_.getParent() == eastPanel_)
                {
                    eastPanel_.remove(lPercent_);
                }

                fPercent_.setValue(item.percent_);

                if (fPercent_.getParent() != eastPanel_)
                {
                    eastPanel_.add(fPercent_, BorderLayout.CENTER);
                }
            }

            lSymbol_.setText("%");

            repaint();
        }

        /*
        public void setIcon(ImageIcon icon)
        {
            lName_.setIcon(icon);
        }
        */

        public void setSelected(boolean b)
        {
            if (b && !fPercent_.getTextField().hasFocus())
            {
                fPercent_.requestFocus();
            }

            // don't call super to avoid changing color
            //super.setSelected(b);
        }

        public void percentValueChanged()
        {
            TypeListItem item = (TypeListItem) getItem();

            item.percent_ = fPercent_.getValue();

            List items = getListPanel().getItems();

            int sum = 0;

            TypeListItem tli;
            int defaultIndex = 0;

            for (int i = items.size() - 1; i >= 0; --i)
            {
                tli = (TypeListItem) items.get(i);

                if (tli.playerType_.isDefault())
                {
                    defaultIndex = i;
                }
                else
                {
                    sum += tli.percent_;
                }
            }

            if (sum < 100)
            {
                ((TypeListItem) items.get(defaultIndex)).percent_ = 100 - sum;
            }
            else
            {
                ((TypeListItem) items.get(defaultIndex)).percent_ = 0;

                sum -= 100;

                for (int i = items.size() - 1; i >= 1; --i)
                {
                    if (sum == 0) break;

                    if (items.get(i) == item) continue;

                    if (sum >= ((TypeListItem) items.get(i)).percent_)
                    {
                        sum -= ((TypeListItem) items.get(i)).percent_;
                        ((TypeListItem) items.get(i)).percent_ = 0;
                    }
                    else
                    {
                        ((TypeListItem) items.get(i)).percent_ -= sum;
                        break;
                    }
                }
            }

            getListPanel().updateItemPanels();
        }
    }
}
