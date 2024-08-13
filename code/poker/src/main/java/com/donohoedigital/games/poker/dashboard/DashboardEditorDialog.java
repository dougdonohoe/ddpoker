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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import com.zookitec.layout.*;

import javax.swing.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 18, 2005
 * Time: 12:54:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class DashboardEditorDialog extends DialogPhase
{
    //static Logger logger = Logger.getLogger(DashboardEditorDialog.class);

    public static final String PARAM_DASHMGR = "dashmgr";

    private DashboardManager mgr_;
    private DDPanel base_;

    public JComponent createDialogContents()
    {
        mgr_ = (DashboardManager) gamephase_.getObject(PARAM_DASHMGR);
        base_ = new DDPanel();
        base_.setLayout(new ExplicitLayout());
        base_.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        sync();
        return GuiUtils.CENTER(base_);
    }

    public void finish()
    {
        DashboardItem item;
        for (int i = 0; i < mgr_.getNumItems(); i++)
        {
            item = mgr_.getItem(i);
            item.setEditor(null);
        }

        super.finish();
    }

    private void sync()
    {
        mgr_.sort();
        ExplicitLayout layout = (ExplicitLayout) base_.getLayout();
        ExplicitConstraints ec;
        Expression y;
        base_.removeAll();
        Expression width = MathEF.constant(220);
        JComponent previous = null;
        DashboardHeader editor = null;

        DashboardItem item;
        for (int i = 0; i < mgr_.getNumItems(); i++)
        {
            item = mgr_.getItem(i);

            // editor
            editor = (DashboardHeader) item.getEditor();
            if (editor == null)
            {
                editor = createEditor(item);
                item.setEditor(editor);
            }

            editor.up_.setEnabled(i != 0);
            editor.down_.setEnabled(true);
            editor.check_.setSelected(item.isDisplayed());

            if (previous == null)
            {
                y = ContainerEF.top(base_);
            }
            else
            {
                y = ComponentEF.bottom(previous).add(2);
            }
            ec = new ExplicitConstraints(editor,
                                         MathEF.constant(0), y,
                                         width,
                                         ComponentEF.preferredHeight(editor));
            base_.add(editor, ec);

            previous = editor;
        }

        // disable down button
        editor.down_.setEnabled(false);
        editor.up_.setEnabled(true);

        // set preferred size so scrolling works
        layout.setPreferredLayoutSize(width, ComponentEF.bottom(previous));
        base_.revalidate();
        base_.repaint();
    }

    private DashboardHeader createEditor(DashboardItem item)
    {
        DashboardHeader header = new DashboardHeader("DashboardHeader", true);
        header.setText(item.getTitle());
        header.up_.addActionListener(new MoveListener(item, true));
        header.down_.addActionListener(new MoveListener(item, false));
        header.check_.addActionListener(new ShowListener(item));
        return header;
    }

    private class MoveListener implements ActionListener
    {
        boolean bUp;
        DashboardItem item;

        public MoveListener(DashboardItem item, boolean bUp)
        {
            this.item = item;
            this.bUp = bUp;
        }

        public void actionPerformed(ActionEvent e)
        {
            mgr_.moveItem(item, bUp);
            sync();
        }
    }

    private class ShowListener implements ActionListener
    {
        DashboardItem item;

        public ShowListener(DashboardItem item)
        {
            this.item = item;
        }

        public void actionPerformed(ActionEvent e)
        {
            DDCheckBox box = (DDCheckBox) e.getSource();
            item.setInDashboardEditor(box.isSelected());
        }
    }
}
