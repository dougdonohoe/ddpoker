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
package com.donohoedigital.gui;

import com.donohoedigital.config.PropertyConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DDPagingTable extends DDPanel implements ActionListener
{
    private static final String DEFAULT_COMPONENT_STYLE = "PagingTable";
    private static final String DEFAULT_PAGING_MESSAGE = "resultlist";

    private DDScrollTable table_ = null;
    private DDPanel pagingPanel_ = null;
    private DDLabel label_ = null;
    private GlassButton refreshButton_ = null;
    private GlassButton prevButton_ = null;
    private GlassButton nextButton_ = null;

    private String pagingMsg_ = null;
    private int offset_ = 0;
    private int rowCount_ = 0;

    /**
     * Creates a new instance of DDPagingTable using the given parameters.
     */
    public DDPagingTable(String name,
                         String componentStyle,
                         String pagingMessage,
                         String[] columnNames,
                         int[] columnTypes,
                         int offset,
                         int rowCount)
    {
        offset_ = offset;
        rowCount_ = rowCount;

        if (componentStyle == null) {
            componentStyle = DEFAULT_COMPONENT_STYLE;
        }

        pagingMsg_ = (pagingMessage != null) ? pagingMessage : DEFAULT_PAGING_MESSAGE;

        table_ = new DDScrollTable(name, componentStyle, "BrushedMetal", columnNames, columnTypes);
        add(table_, BorderLayout.NORTH);

        if (rowCount < 0) {
            // No paging, so just show a normal scrollable table.
            return;
        }

        pagingPanel_ = new DDPanel();
        add(pagingPanel_, BorderLayout.SOUTH);

        label_ = new DDLabel(GuiManager.DEFAULT, componentStyle);
        label_.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        pagingPanel_.add(GuiUtils.CENTER(label_), BorderLayout.NORTH);

        DDPanel buttonPanel = new DDPanel();
        GridLayout layout = new GridLayout(1, 3);
        layout.setHgap(10);
        buttonPanel.setLayout(layout);
        pagingPanel_.add(GuiUtils.CENTER(buttonPanel), BorderLayout.SOUTH);

        refreshButton_ = new GlassButton("refresh", "Glass");
        refreshButton_.addActionListener(this);
        buttonPanel.add(refreshButton_);

        prevButton_ = new GlassButton("previous", "Glass");
        prevButton_.addActionListener(this);
        buttonPanel.add(prevButton_);

        nextButton_ = new GlassButton("next", "Glass");
        nextButton_.addActionListener(this);
        buttonPanel.add(nextButton_);
    }

    public void actionPerformed(ActionEvent event)
    {
        String text = PropertyConfig.getMessage("msg." + pagingMsg_ + ".fetching");
        label_.setText(text);

        // Move the offset.
        Object source = event.getSource();

        if (prevButton_ == source)
        {
            offset_ -= rowCount_;
        }
        else if (nextButton_ == source)
        {
            offset_ += rowCount_;
        }

        // disable buttons so multiple in a row don't happen
        prevButton_.setEnabled(false);
        nextButton_.setEnabled(false);
        refreshButton_.setEnabled(false);

        // invoke later to allow label to repaint and buttons to disable
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                 // Update the model and panel with the new offset.
                DDPagingTableModel model = (DDPagingTableModel) table_.getDDTable().getModel();
                model.refresh(offset_, rowCount_);
                refreshPagingPanel();
            }
        });
    }

    private void refreshPagingPanel()
    {
        if (pagingPanel_ == null)
        {
            return;
        }

        // Set values based on current offset, row count, and total count.
        DDPagingTableModel model = (DDPagingTableModel) table_.getDDTable().getModel();
        int totalCount = model.getTotalCount();

        if (totalCount > 0) {
            // Check if the offset moved past the end of the result set.
            if (offset_ >= totalCount)
            {
                // Assumes the model moved back to the beginning of the result set.
                offset_ = 0;
            }

            Object[] params = new Object[3];
            params[0] = offset_ + 1;
            params[1] = offset_ + model.getRowCount();
            params[2] = totalCount;
            String text = PropertyConfig.getMessage(("msg." + pagingMsg_ + ".paging"), params);
            label_.setText(text);

            prevButton_.setEnabled((offset_ + 1) > rowCount_);
            nextButton_.setEnabled((offset_ + rowCount_) < totalCount);
        }
        else
        {
            // Reset everything if no results.
            offset_ = 0;
            String text = PropertyConfig.getMessage("msg." + pagingMsg_ + ".none");
            label_.setText(text);
            prevButton_.setEnabled(false);
            nextButton_.setEnabled(false);
        }
        refreshButton_.setEnabled(true);
    }

    /**
     * Set the table model.
     */
    public void setModel(DDPagingTableModel model)
    {
        table_.getDDTable().setModel(model);
        refreshPagingPanel();
    }

    /**
     * Get the scroll table containing the contents.
     */
    public DDScrollTable getDDScrollTable()
    {
        return table_;
    }
}
