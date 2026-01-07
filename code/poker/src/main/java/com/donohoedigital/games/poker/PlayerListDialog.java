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
/*
 * PlayerListDialog.java
 *
 * Created on November 27, 2004, 4:19 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.p2p.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 *
 * @author  donohoe
 */
public class PlayerListDialog extends DialogPhase implements PropertyChangeListener, ListSelectionListener
{
    static Logger logger = LogManager.getLogger(PlayerListDialog.class);

    public static final String PARAM_PLAYER_LIST = "playerlist";

    private AbstractPlayerList list_;
    private boolean bShared_ = false;

    private DDButton add_;
    private DDButton delete_;
    private DDTextField text_;

    private DDTextArea area_;

    private PlayerModel model_;
    private DDTable table_;
    private DDTabbedPane tabs_;

    private boolean bIgnoreTextChange_ = false;
    private String selected_ = null;

    /**
     * Creates a new instance of PlayerListDialog 
     */
    public PlayerListDialog() {
    }


    public JComponent createDialogContents()
    {
        // holds data we are gathering
        DDPanel data = new DDPanel();
        data.setBorder(BorderFactory.createEmptyBorder(10,10,5,10));
        data.setBorderLayoutGap(10,0);

        ///
        /// entry field and buttons
        ///
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(0, 10);
        data.add(top, BorderLayout.NORTH);

        DDLabel name = new DDLabel("playername", STYLE);
        top.add(name, BorderLayout.WEST);

        text_ = new DDTextField(GuiManager.DEFAULT, STYLE);
        text_.setTextLengthLimit(PlayerProfileDialog.PLAYER_NAME_LIMIT);
        text_.setRegExp(PlayerProfileDialog.PLAYER_NAME_REGEXP);
        text_.addPropertyChangeListener("value", this);

        top.add(text_, BorderLayout.CENTER);

        // buttons
        add_ = new GlassButton("add", "Glass");
        text_.setDefaultOverride(add_);
        add_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                list_.add(getName(true), null, false);
                model_.fireTableDataChanged();
                propertyChange(null); // force selection of new row
                updateTextFromList();
                text_.selectAll(); // allow quick adds
            }
        });
        add_.setBorderGap(2,5,2,6);

        delete_ = new GlassButton("delete", "Glass");
        delete_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                list_.remove(getName(true), false);
                model_.fireTableDataChanged();
                if (model_.getRowCount() > 0) table_.getSelectionModel().setSelectionInterval(0,0); // allow quick deletes
                updateTextFromList();
            }
        });
        delete_.setBorderGap(2,5,2,6);

        DDPanel buttonbase = new DDPanel();
        buttonbase.setLayout(new GridLayout(1, 0, 5, 0));
        buttonbase.add(add_);
        buttonbase.add(delete_);
        top.add(GuiUtils.CENTER(buttonbase), BorderLayout.EAST);

        ///
        /// player list (tabs)
        ///

        tabs_ = new DDTabbedPane(STYLE, null, JTabbedPane.TOP);
        //tabs_.setOpaque(false);
        tabs_.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        ImageComponent ic = new ImageComponent("ddlogo20", 1.0d);
        ImageComponent error = new ImageComponent("taberror", 1.0d);

        data.add(tabs_, BorderLayout.CENTER);

        // player list
        list_ = (AbstractPlayerList) gamephase_.getObject(PARAM_PLAYER_LIST);
        if (list_ == null)
        {
            list_ = PokerPrefsPlayerList.getUniqueList(gamephase_.getString("listname", "UNDEFINED"));
            bShared_ = true;
        }

        // add tabs
        tabs_.addTab("msg.playerlist.table", ic, error, new TableTab());
        tabs_.addTab("msg.playerlist.text", ic, error, new TextTab());

        return data;
    }

    /**
     * Base class for tabs
     */
    private abstract class ListTab extends DDTabPanel
    {
        ListTab()
        {
            setPreferredSize(new Dimension(320,200));
        }

        public void createUI()
        {
            createUILocal();
        }

        protected abstract void createUILocal();
    }

    /**
     * Table edit
     */
    private class TableTab extends ListTab
    {
        protected void createUILocal()
        {
            DDScrollTable playerScroll = new DDScrollTable(GuiManager.DEFAULT, "PokerPrefsPlayerList", "BrushedMetal", COLUMN_NAMES, COLUMN_WIDTHS);
            playerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            playerScroll.setPreferredSize(new Dimension(playerScroll.getPreferredWidth(), 220));

            table_ = playerScroll.getDDTable();
            model_ = new PlayerModel(list_);
            table_.setModel(model_);
            table_.setShowHorizontalLines(true);
            table_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table_.getSelectionModel().addListSelectionListener(PlayerListDialog.this);

            add(playerScroll, BorderLayout.CENTER);
        }

        public void initUI()
        {
            super.initUI();
            updateListFromText();
            text_.requestFocus();
        }
    }

    private void updateListFromText()
    {
        if (area_ != null)
        {
            list_.fromCSV(area_.getText(), false);
            model_.fireTableDataChanged();
            propertyChange(null); // force selection of new row
        }
    }

    private void updateTextFromList()
    {
        if (area_ != null)
        {
            area_.setText(list_.toCSV());
        }
    }

    /**
     * Text edit
     */
    private class TextTab extends ListTab
    {
        protected void createUILocal()
        {
            area_ = new DDTextArea(GuiManager.DEFAULT, STYLE);
            area_.setLineWrap(true);
            area_.setWrapStyleWord(true);
            DDScrollPane scroll = new DDScrollPane(area_, STYLE, null,
                                               JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scroll, BorderLayout.CENTER);
        }

        public void initUI()
        {
            super.initUI();
            updateTextFromList();
        }
    }

    /**
     * clear special save phase
     */
    public boolean processButton(GameButton button) 
    {
        if (button.getName().startsWith("ok"))
        {
            if (tabs_.getSelectedComponent() instanceof TextTab)
            {
                updateListFromText();
            }
            list_.save();
            if (bShared_) PokerPrefsPlayerList.getSharedList(list_.getName()).fetch(); // update shared list
        }

        return super.processButton(button);
    }

    /**
     * Finish
     */
    public void finish()
    {
        super.finish();
    }

    /**
     * Table row selected
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if (e != null && e.getValueIsAdjusting()) return;

        ListSelectionModel lsm = table_.getSelectionModel();
        int index = lsm.getMinSelectionIndex();
        if (index >= 0 )
        {
            selected_ = model_.getPlayerName(index);
            bIgnoreTextChange_ = true;
            text_.setText(selected_);
            bIgnoreTextChange_ = false;        }
        else
        {
            selected_ = null;
        }

        checkButtons();
    }

    /**
     * Called when value changes on the text fields
     */
    public void propertyChange(PropertyChangeEvent evt) {

        if (bIgnoreTextChange_) return;
        checkButtons();

        // see if we match something in the table, if so, highlight it
        String sName = getName(false);
        //logger.debug("Text change: <"+sName+">");

        ListSelectionModel selmodel = table_.getSelectionModel();
        int nNum = model_.getRowCount();
        for (int i = 0; sName != null && i < nNum; i++)
        {
            if (sName.equals(model_.getPlayerName(i)))
            {
                selmodel.setSelectionInterval(i, i);
                table_.scrollRectToVisible(table_.getCellRect(i, 0, true));
                return;
            }
        }

        if (!selmodel.isSelectionEmpty())
        {
            selmodel.clearSelection();
        }
    }

    /**
     * Turn buttons on / off
     */
    private void checkButtons()
    {
        delete_.setEnabled(selected_ != null);
        add_.setEnabled(selected_ == null && text_.isValidData());
    }

    /**
     * get value in name field
     */
    private String getName(boolean bTrim)
    {
        String sName = text_.getText();
        if (bTrim && sName != null) sName = sName.trim();
        return sName;
    }

    // client table info
    private static final int[] COLUMN_WIDTHS = new int[] {
        282
    };
    private static final String[] COLUMN_NAMES = new String[] {
        LanClientInfo.LAN_PLAYER_NAME
    };

    /**
     * Used by table to display players in game
     */
    private class PlayerModel extends DefaultTableModel
    {
        private AbstractPlayerList list;

        public PlayerModel(AbstractPlayerList list)
        {
            this.list = list;
        }

        public String getPlayerName(int rowIndex)
        {
            return list.get(rowIndex).getName();
        }

        public String getColumnName(int c) {
            return COLUMN_NAMES[c];
        }

        public int getColumnCount() {
            return COLUMN_WIDTHS.length;
        }

        public boolean isCellEditable(int r, int c) {
            return false;
        }

        public int getRowCount() {
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        public Object getValueAt(int rowIndex, int colIndex) 
        {
            if (COLUMN_NAMES[colIndex].equals(LanClientInfo.LAN_PLAYER_NAME))
            {
                return getPlayerName(rowIndex);
            }
            return "[bad column]";
        }
    }
}
