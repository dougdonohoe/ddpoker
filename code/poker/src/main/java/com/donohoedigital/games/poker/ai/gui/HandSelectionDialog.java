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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.List;

public class HandSelectionDialog extends OptionMenuDialog
        implements PropertyChangeListener, FocusListener, ListSelectionListener
{
    static Logger logger = LogManager.getLogger(HandSelectionDialog.class);

    private HandSelectionScheme profile_;
    private TypedHashMap dummy_ = new TypedHashMap();
    private DDTextField name_;
    private HandGroupGridPanel gridPanel_;
    private ListPanel groupsList_;
    private DDLabel groupsSummary_;
    private ChipRatingSlider strengthSlider_;

    
    /**
     * help text area
     */
    protected int getTextPreferredHeight()
    {
        return 30;
    }

    /**
     * create ui
     */
    public JComponent getOptions()
    {
        HandSelectionScheme profile = (HandSelectionScheme) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile, "No 'profile' in params");
        
        return getOptions(profile, STYLE);
    }
    
    public JComponent getOptions(HandSelectionScheme profile, String sStyle)
    {
        profile_ = profile;

        DDPanel base_ = new DDPanel(sStyle);

        DDPanel top = new DDPanel();

        DDPanel topformat = new DDPanel();
        BorderLayout layout = (BorderLayout) topformat.getLayout();
        layout.setHgap(10);
        top.add(topformat, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        // don't add to option list so we don't reset/set map
        OptionText ot = new OptionText(null, "handselectionname", STYLE, dummy_, 30, "^.+$", 200, true);
        topformat.add(ot, BorderLayout.WEST);
        name_ = ot.getTextField();
        name_.addPropertyChangeListener(this);
        name_.setText(profile_.getName());

        DDPanel topButtons = new DDPanel();

        DDButton desc = new GlassButton("description", "Glass");
        desc.setPreferredSize(new Dimension(80, 24));
        desc.setBorderGap(0, 0, 0, 0);
        desc.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setDescription();
            }
        });

        topButtons.setLayout(new GridLayout(1, 2, 4, 0));
        topButtons.add(desc);
        topformat.add(GuiUtils.CENTER(topButtons), BorderLayout.EAST);

        DDPanel handGroupsPanel = new DDPanel();

        handGroupsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        strengthSlider_ = new ChipRatingSlider(sStyle, "handstrength", 1, 10);
        strengthSlider_.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        strengthSlider_.addChangeListener(this);

        gridPanel_ = new HandGroupGridPanel(true);
        gridPanel_.hideSummaryText();
        gridPanel_.getSummaryLabel().setForeground(handGroupsPanel.getForeground());
        gridPanel_.addPropertyChangeListener(this);
        //gridPanel_.setPreferredSize(new Dimension(320, 240));

        groupsList_ = new ListPanel(GroupListItemPanel.class, sStyle);
        groupsList_.setBorder(
                BorderFactory.createEtchedBorder(
                        handGroupsPanel.getBackground().brighter(),
                        handGroupsPanel.getBackground().darker()));
        groupsList_.setSelectedIcon(ImageConfig.getImageIcon("pokericon16png"));
        groupsList_.setFocusable(true);

        groupsSummary_ = new DDLabel(GuiManager.DEFAULT, sStyle);
        groupsSummary_.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 0));

        DDPanel groupsPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        groupsPanel.setPreferredHeight(36 * 3 + 28);
        groupsPanel.add(groupsSummary_, BorderLayout.NORTH);
        groupsPanel.add(groupsList_, BorderLayout.CENTER);

        DDPanel strengthPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        strengthPanel.add(strengthSlider_, BorderLayout.CENTER);
        strengthPanel.add(new DDLabel("handstrength", sStyle), BorderLayout.WEST);

        DDPanel northPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        northPanel.add(GuiUtils.CENTER(strengthPanel), BorderLayout.SOUTH);
        northPanel.add(groupsPanel, BorderLayout.CENTER);

        handGroupsPanel.add(GuiUtils.CENTER(gridPanel_), BorderLayout.CENTER);
        handGroupsPanel.add(northPanel, BorderLayout.NORTH);

        groupsList_.addListSelectionListener(this);

        profile_.ensureEmptyGroup();

        groupsList_.setItems(profile_.getHandGroups());
        gridPanel_.setMutualExclusionList(profile_);
        groupsList_.setSelectedIndex(0);

        base_.add(top, BorderLayout.NORTH);
        base_.add(handGroupsPanel, BorderLayout.CENTER);

        base_.setPreferredWidth(600);

        return base_;
    }

    /**
     * Set description
     */
    private void setDescription()
    {
        TypedHashMap params = new TypedHashMap();
        params.setString(DescriptionDialog.PARAM_DESC, profile_.getDescription());
        Phase phase = context_.processPhaseNow("HandSelectionDescriptionDialog", params);
        String sDesc = (String) phase.getResult();

        if (sDesc != null)
        {
           profile_.setDescription(sDesc);
        }
    }
    
    /**
     * Focus to text field
     */
    protected Component getFocusComponent()
    {
        return name_;
    }
    
    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) 
    {   
        setResult(Boolean.FALSE);
        return super.processButton(button);
    }
    
    /**
     * Okay button press
     */
    protected void okayButton()
    {
        name_.removePropertyChangeListener(this);
        String sText = name_.getText();
        String sCurrent = profile_.getName();
        if (!sCurrent.equals(sText))
        {
            profile_.setName(sText);
        }

        setResult(Boolean.TRUE);
    }
    
    /**
     * Override to ignore non-Boolean results
     */
    public void setResult(Object o)
    {
        if (o instanceof Boolean)
        {
            super.setResult(o);
        }
    }
    
    /**
     * scroll text to visible
     */
    public void focusGained(FocusEvent e) {
        DDTextField t = (DDTextField) e.getSource();
        JScrollPane p = GuiUtils.getScrollParent(t);
        if (p != null)
        {
            Point loc = t.getLocation();
            loc = SwingUtilities.convertPoint(t.getParent(), loc, p.getViewport());
            p.getViewport().scrollRectToVisible(new Rectangle(loc, t.getSize()));
        }
    }
    
    /** 
     * EMPTY
     */
    public void focusLost(FocusEvent e) {
    }

    public void valueChanged(ListSelectionEvent e)
    {
        Object source = e.getSource();

        if (source == groupsList_)
        {
            HandGroup group = (HandGroup) groupsList_.getSelectedItem();
            gridPanel_.setHandGroup(group);
            strengthSlider_.setValue(group.getStrength());
            updateSummary();
        }
    }

    private void updateSummary()
    {
        List<HandGroup> al = profile_.getHandGroups();

        int totalClassCount = 0;
        int totalHandCount = 0;

        for (HandGroup group : al)
        {
            totalClassCount += group.getClassCount();
            totalHandCount += group.getHandCount();
        }

        groupsSummary_.setText(PropertyConfig.getMessage("msg.startinghands.summary", totalClassCount, PokerConstants.formatPercent(totalHandCount / 13.26)));
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals("value")) checkButtons();

        if (!evt.getPropertyName().equals("HANDS")) return;

        HandGroup changed = (HandGroup)groupsList_.getSelectedItem();

        // remove this group if now empty
        if (changed.getClassCount() == 0)
        {
            groupsList_.removeSelectedItem();
        }
        else
        {
            if (changed.getStrength() < 1)
            {
                changed.setStrength(1);
            }
            
            List items = groupsList_.getItems();

            HandGroup last = (HandGroup) items.get(items.size() - 1);

            // make sure there's an empty group at the end
            if (last.getClassCount() > 0)
            {
                groupsList_.addItem(new HandGroup());
            }

            groupsList_.setSelectedItem(changed);
        }

        groupsList_.sort();
    }

    protected boolean isValidCheck()
    {
        return name_.isValidData();
    }

    public void stateChanged(ChangeEvent e)
    {
        // strength slider
        gridPanel_.getHandGroup().setStrength(strengthSlider_.getValue());

        groupsList_.sort();
    }

    public static class GroupListItemPanel extends ListItemPanel
    {
        DDLabel index_;
        DDLabel summary_;
        DDLabel stats_;
        DDPanel leftTopPanel_;
        DDPanel leftPanel_;
        DDLabel strengthLabel_;
        ChipRatingPanel strength_;
        DDPanel strengthPanel_;

        boolean statsVisible = false;

        public GroupListItemPanel(ListPanel panel, Object item, String sStyle)
        {
            super(panel, item, sStyle);

            // use protected border
            bNormal_ = borderProtected_;
            bMouseDown_ = borderProtected_;
            setBorder(bNormal_);

            index_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            index_.setPreferredSize(new Dimension(50, (int) index_.getPreferredSize().getHeight()));
            index_.addMouseListener(this);

            summary_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            summary_.addMouseListener(this);

            stats_ = new DDLabel(GuiManager.DEFAULT, sStyle);
            stats_.setHorizontalAlignment(SwingUtilities.LEFT);
            stats_.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0));
            stats_.addMouseListener(this);

            strength_ = new ChipRatingPanel();
            strength_.addMouseListener(this);

            strengthLabel_ = new DDLabel(GuiManager.DEFAULT, sStyle);

            strengthPanel_ = new DDPanel(GuiManager.DEFAULT, sStyle);
            strengthPanel_.add(strength_, BorderLayout.NORTH);
            strengthPanel_.add(strengthLabel_, BorderLayout.CENTER);

            leftTopPanel_ = new DDPanel(GuiManager.DEFAULT, sStyle);

            leftTopPanel_.add(index_, BorderLayout.WEST);
            leftTopPanel_.add(summary_, BorderLayout.CENTER);

            leftPanel_ = new DDPanel(GuiManager.DEFAULT, sStyle);
            leftPanel_.add(leftTopPanel_, BorderLayout.NORTH);

            add(leftPanel_, BorderLayout.CENTER);
        }

        /**
         * Update text
         */
        public void update()
        {
            int index = getIndex();

            String summary = ((HandGroup) item_).getSummary();
            if (summary.length() > 60)
            {
                summary = summary.substring(0, summary.lastIndexOf(',', 60) + 2) + "...";
            }
            index_.setText(Integer.toString(index + 1));
            summary_.setText(summary);
            if (((HandGroup) item_).getClassCount() == 0)
            {
                stats_.setText(null);

                if (statsVisible)
                {
                    leftPanel_.remove(stats_);
                    remove(strengthPanel_);
                    statsVisible = false;
                }
            }
            else
            {
                int classCount = ((HandGroup) item_).getClassCount();

                stats_.setText(PropertyConfig.getMessage(
                        "msg.handgroup.summary." + (classCount == 1 ? "singular" : "plural"),
                        classCount, PokerConstants.formatPercent(((HandGroup) item_).getPercent())));

                int strength = ((HandGroup) item_).getStrength();

                strength_.setValue(strength);
                strengthLabel_.setText(PropertyConfig.getMessage("msg.handstrength." + strength));

                if (!statsVisible)
                {
                    leftPanel_.add(stats_, BorderLayout.CENTER);
                    add(strengthPanel_, BorderLayout.EAST);
                    statsVisible = true;
                }
            }

            repaint();
        }

        public void setIcon(ImageIcon icon)
        {
            index_.setIcon(icon);
        }
    }
}
