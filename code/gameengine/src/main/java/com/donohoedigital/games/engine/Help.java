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
/*
 * Help.java
 *
 * Created on March 29, 2003, 4:13 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


/**
 * @author Doug Donohoe
 */
public class Help extends BasePhase implements ListSelectionListener,
                                               HyperlinkListener, ActionListener
{
    //static Logger logger = Logger.getLogger(Help.class);

    private DDPanel base_;
    private DDHtmlArea html_;
    private DDTable table_;
    private TableModel model_;
    private DDImageButton bak_, fwd_;
    private int nHistIndex_ = 0;
    private List<HelpTopic> history_ = new ArrayList<HelpTopic>();
    private String STYLE;
    private boolean bRunning_ = false;

    // save game info
    private static final int[] COLUMN_WIDTHS = new int[]{
            175
    };
    private static final String[] COLUMN_NAMES = new String[]{
            "helptopic"
    };


    /**
     * init data
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        STYLE = gamephase_.getString("style", "default");

        createDialogContents();
    }

    /**
     * create chat ui
     */
    private void createDialogContents()
    {
        // contents
        base_ = new DDPanel();
        BorderLayout layout = (BorderLayout) base_.getLayout();
        layout.setVgap(5);
        base_.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String BEVEL_STYLE = gamephase_.getString("bevel-style", STYLE);

        // top - label/nav
        DDPanel topbase = new DDPanel();
        base_.setBorderLayoutGap(0, 2);
        base_.add(topbase, BorderLayout.NORTH);

        DDLabel label = new DDLabel("helpwindow", STYLE);
        topbase.add(label, BorderLayout.CENTER);

        DDPanel navbase = new DDPanel();
        navbase.setLayout(new GridLayout(1, 2, 0, 0));
        navbase.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        topbase.add(navbase, BorderLayout.EAST);
        bak_ = new DDImageButton("nav.bak");
        bak_.addActionListener(this);
        navbase.add(bak_);
        fwd_ = new DDImageButton("nav.fwd");
        fwd_.addActionListener(this);
        navbase.add(fwd_);

        // html display
        html_ = new HelpHtml();
        html_.addHyperlinkListener(this);
        DDScrollPane scroll = new DDScrollPane(html_, STYLE, BEVEL_STYLE, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        html_.setOpaque(true);
        scroll.setBackground(Color.WHITE);
        html_.setBackground(Color.WHITE);
        base_.add(scroll, BorderLayout.CENTER);

        ////
        //// Table
        ////
        DDScrollTable tScroll = new DDScrollTable(GuiManager.DEFAULT, STYLE, BEVEL_STYLE, COLUMN_NAMES, COLUMN_WIDTHS);
        tScroll.setPreferredSize(new Dimension(tScroll.getPreferredWidth(), 200));
        base_.add(tScroll, BorderLayout.WEST);

        table_ = tScroll.getDDTable();
        model_ = getModel();
        table_.setTableHeader(null);
        table_.setModel(model_);
        table_.setShowHorizontalLines(true);
        table_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table_.setColumnSelectionAllowed(false);
        table_.getSelectionModel().addListSelectionListener(this);
        //TODO: fix table so tab causes focus change
        //table_.setFocusable(true);
        //table_.setFocusTraversalKeysEnabled(true);
    }

    private class HelpHtml extends DDHtmlArea implements FocusListener, MouseListener
    {
        boolean bIgnoreGain = false;

        HelpHtml()
        {
            super(GuiManager.DEFAULT, "Help");
            setPreferredSize(new Dimension(525, 400));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFocusable(true);
            setFocusTraversalKeysEnabled(true);
            addFocusListener(this);
            addMouseListener(this);
        }

        public void mousePressed(MouseEvent e)
        {
            if (!hasFocus()) bIgnoreGain = true;
        }

        public void focusGained(FocusEvent e)
        {
            if (!bIgnoreGain && html_.getSelectionStart() == html_.getSelectionEnd())
            {
                html_.selectAll();
                html_.getCaret().setVisible(true);
            }
            bIgnoreGain = false;
        }

        public void focusLost(FocusEvent e)
        {
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mouseReleased(MouseEvent e)
        {
        }

        public void mouseEntered(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
        }
    }

    /**
     * Start of phase
     */
    @Override
    public void start()
    {
        String sTopic = gamephase_.getString(GameButton.PARAM_GENERIC, !bRunning_ ? "intro" : null);
        if (sTopic != null) displayHelpTopic(sTopic);

        // if users presses launch button again, this will be called.  don't run logic again in this case
        if (bRunning_) return;
        bRunning_ = true;

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, base_, true, table_);
    }

    /**
     * finish
     */
    @Override
    public void finish()
    {
        bRunning_ = false;
        super.finish();
    }

    /**
     * Called when a hypertext link is updated.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
        String sName = e.getDescription();

        // strip off .html if it is there
        int indexof = sName.indexOf(".html");
        if (!sName.startsWith("http") && !sName.startsWith("ok-") && indexof != -1)
        {
            sName = sName.substring(0, indexof);
        }
        if (!displayHelpTopic(sName))
        {
            if (sName.startsWith("http"))
            {
                // DD Poker 3, just go to the page
                Utils.openURL(sName);

                // Old way:  change name and display ok- below
                //String sURL = "ok-" + sName;
                //displayHelp(Utils.fixHtmlTextFor15(PropertyConfig.getMessage("msg.helptopic.http", sName, sURL)));
            }
            else if (sName.startsWith("ok-"))
            {
                String sURL = sName.substring(3);
                Utils.openURL(sURL);
            }
            else
            {
                displayHelp(PropertyConfig.getMessage("msg.helptopic.notfound", sName));
            }
            html_.setCaretPosition(0);
        }
    }

    /**
     * Display topic at current index
     */
    public void displayCurrentIndex()
    {
        HelpTopic topic = history_.get(nHistIndex_);
        displayHelpTopic(topic.getName());
    }

    /**
     * display given help topic
     */
    public boolean displayHelpTopic(String sTopic)
    {
        int nNum = model_.getRowCount();
        for (int i = 0; i < nNum; i++)
        {
            if (sTopic != null && sTopic.equals(model_.getHelpTopic(i).getName()))
            {
                table_.getSelectionModel().setSelectionInterval(i, i);
                table_.scrollRectToVisible(table_.getCellRect(i, 0, true));
                return true;
            }
        }
        return false;
    }

    HelpTopic selected_ = null;

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        ListSelectionModel lsm = table_.getSelectionModel();
        int index = lsm.getMinSelectionIndex();
        if (index >= 0)
        {
            //logger.debug("Not empty "+index);
            HelpTopic select = model_.getHelpTopic(index);
            if (select == selected_) return;
            if (selected_ != null)
            {
                //logger.debug(selected_.getName() + " STORING RECT: "+html_.getVisibleRect());
                selected_.setScrollPosition(html_.getVisibleRect());
            }
            selected_ = select;
            displaySelectedHelpTopic();
        }
        else
        {
            //logger.debug("Empty");
            selected_ = null;
        }
    }

    private TableModel getModel()
    {
        return new TableModel(HelpConfig.getHelpTopics());
    }

    /**
     * nav buttons
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == fwd_)
        {
            goFwd();
        }
        else
        {
            goBak();
        }
    }

    private void goFwd()
    {
        bSkipHist_ = true;
        nHistIndex_++;
        displayCurrentIndex();
        bSkipHist_ = false;
    }

    private void goBak()
    {
        bSkipHist_ = true;
        nHistIndex_--;
        displayCurrentIndex();
        bSkipHist_ = false;
    }

    private void checkButtons()
    {
        bak_.setEnabled(nHistIndex_ > 0);
        fwd_.setEnabled(nHistIndex_ < (history_.size() - 1));
    }

    private boolean bSkipHist_ = false;

    private void displaySelectedHelpTopic()
    {
        if (!bSkipHist_)
        {
            if (nHistIndex_ < (history_.size() - 1))
            {
                for (int i = (history_.size() - 1); i > nHistIndex_; i--)
                {
                    history_.remove(i);
                }
            }
            history_.add(selected_);
            nHistIndex_ = history_.size() - 1;
        }

        displayHelp(selected_.getContents());
        //html_.setCaretPosition(0);

        if (selected_.getScrollPosition() != null)
        {
            // BUG 239 - yuck
            // scroll rect doesn't work immediately?  Why?
            // not sure - so this hack makes it less "flashy"
            html_.setSkipNextRepaint(true);
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            //logger.debug(selected_.getName() + " scrollRectToVisible: "+ selected_.getScrollPosition());
                            html_.scrollRectToVisible(selected_.getScrollPosition());
                            html_.paintImmediately(0, 0, html_.getWidth(), html_.getHeight());
                        }
                    }
            );

        }

        checkButtons();
    }

    /**
     * add message to chat window
     */
    private void displayHelp(String sHelp)
    {
        html_.setText(sHelp);
    }

    /**
     * Used by table to display help topics
     */
    private class TableModel extends DefaultTableModel
    {
        private List<HelpTopic> topics;

        public TableModel(List<HelpTopic> topics)
        {
            this.topics = topics;
        }

        public HelpTopic getHelpTopic(int r)
        {
            return topics.get(r);
        }

        @Override
        public String getColumnName(int c)
        {
            return COLUMN_NAMES[c];
        }

        @Override
        public int getColumnCount()
        {
            return COLUMN_WIDTHS.length;
        }

        @Override
        public boolean isCellEditable(int r, int c)
        {
            return false;
        }

        @Override
        public int getRowCount()
        {
            if (topics == null)
            {
                return 0;
            }
            return topics.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex)
        {
            HelpTopic topic = getHelpTopic(rowIndex);
            switch (colIndex)
            {
                case 0:
                    return topic.getDisplay();
            }
            throw new ArrayIndexOutOfBoundsException("Invalid column value");
        }
    }
}
