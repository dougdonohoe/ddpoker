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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.base.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;
import com.zookitec.layout.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 16, 2005
 * Time: 12:06:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DashboardPanel extends DDPanel
{
    static Logger logger = LogManager.getLogger(DashboardPanel.class);

    private DashboardManager mgr_;
    private DDPanel dashitems_;
    private DDScrollPane sp_;

    public DashboardPanel(DashboardManager mgr)
    {
        mgr_ = mgr;

        // border
        Border outer = BorderFactory.createEmptyBorder(0,5,5,5);
        Border inner = new DDBevelBorder("BrushedMetal", DDBevelBorder.LOWERED);
        Border compound = BorderFactory.createCompoundBorder(outer, inner);
        Border inner2 = BorderFactory.createEmptyBorder(0,2,2,2);
        //Border compound2 = BorderFactory.createCompoundBorder(compound, inner2);
        setBorder(compound);


        // base
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(3,0);
        base.setOpaque(true);
        base.setBackground(new Color(0,0,0,60));
        base.setBorder(inner2);
        add(base, BorderLayout.CENTER);

        // top - Dashboard label and edit button
        base.add(createTopPanel(), BorderLayout.NORTH);

        // middle - scrollpane with DashboardItems
        dashitems_ = new DDPanel();

        dashitems_.setLayout(new ExplicitLayout());
        sp_ = new DDScrollPane(dashitems_, "ChatInGame", null,
                                           JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                           JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp_.setOpaque(false);
        base.add(sp_, BorderLayout.CENTER);

        // sync layout with DashboardManager
        sync();
    }

    private void sync()
    {
        mgr_.sort();
        ExplicitLayout layout = (ExplicitLayout) dashitems_.getLayout();
        ExplicitConstraints ec;
        Expression y;
        dashitems_.removeAll();
        Expression bodyX = MathEF.constant(4);
        Expression width = ContainerEF.width(this);
        JComponent previous = null;
        JComponent header;
        JComponent body;

        DashboardItem item;
        for (int i = 0; i < mgr_.getNumItems(); i++)
        {
            item = mgr_.getItem(i);
            if (item.isInDashboard())
            {
                item.createUI(this);

                // header
                header = item.getHeader();
                if (previous == null)
                {
                    y = ContainerEF.top(dashitems_);
                }
                else
                {
                    y = ComponentEF.bottom(previous).add(2);
                }
                ec = new ExplicitConstraints(header,
                                             MathEF.constant(0), y,
                                             width,
                                             ComponentEF.preferredHeight(header));
                dashitems_.add(header, ec);


                // body
                body = item.getBody();

                int nPrefHeight = item.getPreferredBodyHeight();

                Expression bH = nPrefHeight == 0 ? ComponentEF.preferredHeight(body) : MathEF.constant(nPrefHeight);

                ec = new ExplicitConstraints(body,
                                             bodyX, ComponentEF.bottom(header),
                                             MathEF.max(
                                             width.subtract(bodyX),
                                             ComponentEF.preferredWidth(body)),
                                             bH,
                                             0.0, 0.0, true, true);

/*
                ec = new ExplicitConstraints(body,
                                             bodyX, ComponentEF.bottom(header),
                                             MathEF.max(
                                             width.subtract(bodyX),
                                             ComponentEF.preferredWidth(body)),
                                             ComponentEF.preferredHeight(body),

                                             0.0, 0.0, true, true);
                                             */
                dashitems_.add(body, ec);

                previous = body;
            }
        }

        // set preferred size so scrolling works
        layout.setPreferredLayoutSize(MathEF.constant(0),
                                      previous == null ? MathEF.constant(0) : ComponentEF.bottom(previous));
        dashitems_.revalidate();
        repaint();
    }

    private JComponent createTopPanel()
    {

        ImageComponent dashBG = new ImageComponent("dashboard", 1.0);
        dashBG.setScaleToFit(false);
        dashBG.setCentered(false);
        dashBG.setLayout(new ExplicitLayout());

        GlassButton edit = new GlassButton("editdash", "Glass");
        edit.setPreferredSize(new Dimension(40,21));
        // use explicit contraint since centering is slightly off
        dashBG.add(edit, new ExplicitConstraints(
                             edit,
                             ContainerEF.right(dashBG).subtract(ComponentEF.preferredWidth(edit)),
                             ContainerEF.top(dashBG).add(5)));
        edit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                TypedHashMap params = new TypedHashMap();
                params.setObject(DashboardEditorDialog.PARAM_DASHMGR, mgr_);
                mgr_.getGame().getGameContext().processPhaseNow("DashboardEditorDialog", params);
                sync();
                mgr_.stateChanged();
            }
        });

        return dashBG;
    }

    /**
     * Called by DashboardItem when user requested it be removed from dashboard
     */
    void itemRemoveRequested(DashboardItem item)
    {
        sync();
        mgr_.stateChanged();;
    }

    /**
     * Called by dashboard item when user closed an item
     */
    void itemOpenClose(DashboardItem item)
    {
        // notify manager
        mgr_.stateChanged();
    }

    /**
     * cleanup upon exit
     */
    public void finish()
    {
        DashboardItem item;
        for (int i = 0; i < mgr_.getNumItems(); i++)
        {
            item = mgr_.getItem(i);
            item.finish();
        }
    }
}
