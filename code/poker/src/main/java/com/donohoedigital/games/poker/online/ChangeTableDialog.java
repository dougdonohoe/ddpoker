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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 16, 2005
 * Time: 1:35:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChangeTableDialog extends DialogPhase
{
    private TournamentDirector td_;
    private PokerGame game_;
    private PokerTable current_;
    private PokerTable selected_;
    private PokerPlayer player_;
    private JComponent focus_;

    public JComponent createDialogContents()
    {
        game_ = (PokerGame) context_.getGame();
        td_ = (TournamentDirector) context_.getGameManager();
        current_ = game_.getCurrentTable();
        player_ = game_.getLocalPlayer();

        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        base.setLayout(new GridLayout(0, 1, 0, 10));
        ButtonGroup group = new ButtonGroup();

        PokerTable table;
        int nNum = game_.getNumTables();
        for (int i = 0; i < nNum; i++)
        {
            table = game_.getTable(i);
            if (table != current_ && (table.isAllComputer() || table.getNumOccupiedSeats() == 0)) continue;
            addTable(base, group, table);
        }
        return base;
    }

    protected Component getFocusComponent()
    {
        return focus_;
    }

    private void addTable(DDPanel base, ButtonGroup group, PokerTable table)
    {
        DDPanel tblbase = new DDPanel();
        tblbase.setBorderLayoutGap(0, 10);
        base.add(tblbase);

        DDRadioButton choose = new TableRadio(table);
        choose.setVerticalTextPosition(SwingConstants.TOP);
        if (table == current_)
        {
            choose.setSelected(true);
            selected_ = current_;
            focus_ = choose;
        }
        group.add(choose);
        tblbase.add(GuiUtils.NORTH(choose), BorderLayout.WEST);

        StringBuilder sb = new StringBuilder("<HTML><TABLE CELLPADDING=0 CELLSPACING=0><TR><TD VALIGN=TOP><B>");
        sb.append(table.getName());
        sb.append("&nbsp;&nbsp;</B></TD<TD>");
        PokerPlayer p;
        boolean bBold;
        int nCnt = 0;
        for (int i = 0; i < PokerConstants.SEATS; i++)
        {
            p = table.getPlayer(i);
            if (p == null) continue;
            bBold = !p.isComputer();
            if (nCnt > 0) sb.append(", ");
            if (nCnt == 4 || nCnt == 8) sb.append("<BR>");
            if (bBold) sb.append("<B>");
            sb.append(Utils.encodeHTML(p.getDisplayName(true)));
            if (bBold) sb.append("</B>");
            nCnt++;
        }
        sb.append("</TD></TR></TABLE>");
        choose.setText(sb.toString());
    }

    private class TableRadio extends DDRadioButton implements ActionListener
    {
        PokerTable table;
        TableRadio(PokerTable table)
        {
            super(GuiManager.DEFAULT, STYLE);
            this.table = table;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            selected_ = table;
        }
    }


    public boolean processButton(GameButton button)
    {
        if (button.getName().startsWith("okay"))
        {
            if (selected_ != current_)
            {
                // change table
                td_.changeTable(player_, selected_);
            }
        }

        return super.processButton(button);
    }
}
