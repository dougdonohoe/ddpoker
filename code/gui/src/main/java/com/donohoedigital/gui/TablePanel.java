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

import java.awt.*;

/**
 * Initial effort at a common panel for laying out aligned components in a table.  Currently supports
 * label/text field pairs and is extendable for adding other components.  Future implementation should
 * include support for more components (see extended classes for examples).
 */
public class TablePanel extends DDPanel
{
    protected GridBagLayout layout_ = null;
    protected GridBagConstraints constraints_ = null;

    /**
     * Create initialized panel.
     */
    public TablePanel()
    {
        layout_ = new GridBagLayout();
        constraints_ = new GridBagConstraints();
        setLayout(layout_);
    }

    /**
     * Add a label/text field pair as the next row in the table.
     *
     * @param labelName label name
     * @param textName text field name
     * @param style style
     * @param length text field length
     * @param value text field value
     * @param regExp text field regex
     * @param insets row insets
     *
     * @return the label/text field pair
     */
    public TextWidgets addTextField(String labelName,
                                    String textName,
                                    String style,
                                    int length,
                                    String value,
                                    String regExp,
                                    Insets insets)
    {
        int top = (insets != null) ? insets.top : 0;
        int left = (insets != null) ? insets.left : 0;
        int bottom = (insets != null) ? insets.bottom : 5;
        int right = (insets != null) ? insets.right : 0;

        DDLabel label = new DDLabel(labelName, style);
        constraints_.insets = new Insets(top, left, bottom, 5);
        constraints_.weightx = 0.0;
        constraints_.gridwidth = 1;
        constraints_.fill = GridBagConstraints.BOTH;
        layout_.setConstraints(label, constraints_);
        add(label);

        DDTextField text = new DDTextField(textName, style);
        text.setTextLengthLimit(length);
        text.setText(value);
        text.setRegExp(regExp);
        constraints_.insets = new Insets(top, 0, bottom, right);
        constraints_.weightx = 1.0;
        constraints_.gridwidth = GridBagConstraints.REMAINDER;
        layout_.setConstraints(text, constraints_);
        add(text);

        TextWidgets widgets = new TextWidgets();
        widgets.label_ = label;
        widgets.text_ = text;

        return widgets;
    }

    /**
     * Label/text field pair.
     */
    public static class TextWidgets
    {
        public DDLabel label_;
        public DDTextField text_;

        public boolean isEnabled()
        {
            return text_.isEnabled();
        }

        public void setEnabled(boolean bEnabled)
        {
            label_.setEnabled(bEnabled);
            text_.setEnabled(bEnabled);
        }

        public String getText()
        {
            return text_.getText().trim();
        }

        public void setText(String sText)
        {
            text_.setText(sText);
        }
    }
}
