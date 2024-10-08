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
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * subclass to handle selection and copy
 */
class ChatListPanel extends ListPanel implements MouseListener, MouseMotionListener, FocusListener
{
    static Logger logger = LogManager.getLogger(ChatListPanel.class);

    private static ImageIcon exportIcon_ = ImageConfig.getImageIcon("menuicon.export");
    private static final String WHITESPACE = "[\\s\\xA0]+";

    private GameContext context_;
    private Point start_ = null;
    private Point end_;

    // limit display
    private int MAX_MESSAGES = 500;
    private ArrayList messages_;

    /**
     * Create new panel specifying styles, scrollbar policies
     */
    public ChatListPanel(GameContext context, Class itemPanelClass, int nMax,
                         String sStyle, String bevelStyle, int nVerticalPolicy, int nHorizPolicy)
    {
        super(itemPanelClass, sStyle, bevelStyle, nVerticalPolicy, nHorizPolicy);
        context_ = context;
        MAX_MESSAGES = nMax;
        messages_ = new ArrayList(MAX_MESSAGES);
        DDPanel listParent = getListParent();
        listParent.setFocusTraversalKeysEnabled(true);
        listParent.setFocusable(true);
        listParent.addMouseMotionListener(this);
        listParent.addMouseListener(this);
        listParent.addFocusListener(this);
        listParent.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        GuiUtils.addKeyAction(listParent, JComponent.WHEN_FOCUSED,
                              "chatcopy", new CopyAction(),
                              KeyEvent.VK_C, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

        GuiUtils.addKeyAction(listParent, JComponent.WHEN_FOCUSED,
                              "chatselectall", new SelectAllAction(),
                              KeyEvent.VK_A, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);
        setViewportBorder(BorderFactory.createCompoundBorder(
                getViewportBorder(),
                BorderFactory.createMatteBorder(1, 1, 1, 1, getListParent().getBackground())));
        setFocusable(false); // Doug: turned off - not sure if this is what we want.
        getListParent().setOpaque(true);
        setFocusTraversalKeysEnabled(true);
        setItems(messages_);
    }

    /**
     * display the message
     */
    void displayMessage(ChatPanel.ChatMessage msg)
    {
        int last = messages_.size() - 1;

        if (messages_.size() == MAX_MESSAGES)
        {
            updateItem(0, msg);
            moveItemToLast(0);
        }
        else
        {
            ++last;
            addItem(msg, false);
        }

        SwingUtilities.invokeLater(new SelectIndex(last));
    }

    /**
     * select given index - do this via runnable so
     * item is rendered and sized appropriately
     */
    private class SelectIndex implements Runnable
    {
        int idx;

        SelectIndex(int idx)
        {
            this.idx = idx;
        }

        public void run()
        {
            // validate needed to make sure size is correct.
            // swing is weird - calling validate() in the
            // actual ListPanel code doesn't work
            validate();
            setSelectedIndex(idx);
        }
    }

    // focus control
    private boolean bIgnoreGain = false;

    /**
     * gain focus, set selection visible
     */
    public void focusGained(FocusEvent e)
    {
        setSelectionVisible(true);
        bIgnoreGain = false;
    }

    /**
     * lose focus, set selection invisible
     */
    public void focusLost(FocusEvent e)
    {
        setSelectionVisible(false);
    }

    /**
     * set selection visible on all
     */
    private void setSelectionVisible(boolean b)
    {
        ChatItemPanel chat;
        DDHtmlArea html = null;
        boolean bSetVisible = false;
        for (int i = 0; i < getItems().size(); i++)
        {
            chat = (ChatItemPanel) getItemPanel(i);
            html = chat.html_;
            if (html.getSelectionEnd() != html.getSelectionStart())
            {
                html.getCaret().setSelectionVisible(b);
                if (b) bSetVisible = true;
            }
        }

        // if setting visible, but nothing selected, then select all of last item
        if (b && !bSetVisible && html != null && !bIgnoreGain)
        {
            html.selectAll();
            html.getCaret().setSelectionVisible(true);
        }
    }

    /**
     * copy action
     */
    private class CopyAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            copy();
        }
    }

    /**
     * select all action
     */
    private class SelectAllAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            selectText(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
    }

    /**
     * copy all select text to clipboard
     */
    private void copy()
    {
        ChatItemPanel chat;
        DDHtmlArea html;
        StringBuilder sb = new StringBuilder();
        String sText;
        for (int i = 0; i < getItems().size(); i++)
        {
            chat = (ChatItemPanel) getItemPanel(i);
            html = chat.html_;
            if (html.getSelectionEnd() != html.getSelectionStart())
            {
                sText = html.getSelectedText();
                // replace white space \s and nbsp (ascii 160 == AO)
                sText = sText.replaceAll(WHITESPACE, " ");
                if (sb.length() > 0) sb.append("\n");
                sb.append(sText.trim());
            }
        }
        if (sb.length() == 0) return;
        //logger.debug("Copy: "+ sb);
        GuiUtils.copyToClipboard(sb.toString());
    }

    /**
     * start of selection, request focus
     */
    public void mousePressed(MouseEvent e)
    {
        start_ = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getListParent());
        bIgnoreGain = true;
        getListParent().requestFocus();
    }

    /**
     * drag - select text
     */
    public void mouseDragged(MouseEvent e)
    {
        Component c = (Component) e.getSource();
        Point thisend = SwingUtilities.convertPoint(c, e.getPoint(), getListParent());
        if (!thisend.equals(end_))
        {
            end_ = thisend;
            int x1 = Math.min(start_.x, end_.x);
            int x2 = Math.max(start_.x, end_.x);
            int y1 = Math.min(start_.y, end_.y);
            int y2 = Math.max(start_.y, end_.y);

            selectText(x1, y1, x2, y2);
            getViewport().scrollRectToVisible(new Rectangle(SwingUtilities.convertPoint(c, e.getPoint(), getViewport())));
        }
    }

    /**
     * select text that intersects box
     */
    private void selectText(int x1, int y1, int x2, int y2)
    {
        if (x1 == x2) x2 = x1 + 1;
        if (y1 == y2) y2 = y1 + 1;
        Point topleft = new Point(x1, y1);
        Point botright = new Point(x2, y2);
        Rectangle area = new Rectangle(x1, y1, x2 - x1, y2 - y1);

        ChatItemPanel chat;
        Rectangle bounds = new Rectangle();
        Point adjtopleft, adjbotright;
        DDHtmlArea html;
        for (int i = 0; i < getItems().size(); i++)
        {
            chat = (ChatItemPanel) getItemPanel(i);
            chat.getBounds(bounds);
            html = chat.html_;
            adjtopleft = SwingUtilities.convertPoint(getListParent(), topleft, html);
            adjbotright = SwingUtilities.convertPoint(getListParent(), botright, html);
            if (bounds.intersects(area))
            {
                if (bounds.contains(topleft))
                {
                    if (bounds.contains(botright))
                    {
                        html.select(html.viewToModel(adjtopleft), html.viewToModel(adjbotright));
                    }
                    else
                    {
                        html.select(html.viewToModel(adjtopleft), Integer.MAX_VALUE);
                    }
                }
                else if (bounds.contains(botright))
                {
                    html.select(0, html.viewToModel(adjbotright));
                }
                else
                {
                    html.selectAll();
                }
                html.getCaret().setSelectionVisible(true);
            }
            else
            {
                html.select(0, 0);
                html.getCaret().setSelectionVisible(false);
            }
        }
    }

    /**
     * select word that intersects start point
     */
    private void selectWord()
    {
        ChatItemPanel chat;
        Rectangle bounds = new Rectangle();
        Point adjstart;
        DDHtmlArea html;

        for (int i = 0; i < getItems().size(); i++)
        {
            chat = (ChatItemPanel) getItemPanel(i);
            chat.getBounds(bounds);
            html = chat.html_;
            adjstart = SwingUtilities.convertPoint(getListParent(), start_, html);
            if (bounds.contains(start_))
            {
                int pos = html.viewToModel(adjstart);
                int length = html.getDocument().getLength();
                int start = pos - 1;
                int end = pos + 1;
                while (start-- >= 0)
                {
                    try
                    {
                        String s = html.getDocument().getText(start, 1);
                        if (s.matches(WHITESPACE))
                        {
                            start++;
                            break;
                        }
                    }
                    catch (BadLocationException be)
                    {

                    }
                }
                while (end++ < length)
                {
                    try
                    {
                        String s = html.getDocument().getText(end, 1);
                        if (s.matches(WHITESPACE))
                        {
                            break;
                        }
                    }
                    catch (BadLocationException be)
                    {

                    }
                }
                if (start != end)
                {
                    html.select(start, end);
                    html.getCaret().setSelectionVisible(true);
                }
                return;
            }
        }
    }

    /**
     * single click - deselect
     */
    public void mouseClicked(MouseEvent e)
    {
        Point thisend = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getListParent());
        if (thisend.equals(start_))
        {
            if (e.getClickCount() == 2)
            {
                selectWord();
            }
            else
            {
                selectText(-1, -1, 0, 0);
            }
        }
    }

    /**
     * popup menu
     */
    public void mouseReleased(MouseEvent e)
    {
        if (!GuiUtils.isPopupTrigger(e, false)) return;
        if (getItems().size() == 0) return;

        DDPopupMenu menu = new DDPopupMenu();

        DDMenuItem title = new DDMenuItem(GuiManager.DEFAULT, "PopupMenu");
        title.setText(PropertyConfig.getMessage("menuitem.chat.title"));
        title.setDisplayMode(DDMenuItem.MODE_TITLE);
        menu.add(title);

        DDMenuItem item = new DDMenuItem(GuiManager.DEFAULT, "PopupMenu");
        item.setText(PropertyConfig.getMessage("menuitem.chat.export"));
        item.setIcon(exportIcon_);
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                TypedHashMap params = new TypedHashMap();
                params.setString(FileChooserDialog.PARAM_SUGGESTED_NAME, "chat");
                Phase choose = context_.processPhaseNow("ExportChat", params);
                Object oResult = choose.getResult();
                if (oResult != null && oResult instanceof File)
                {
                    File file = (File) oResult;
                    logger.info("Exporting chat to " + file.getAbsolutePath());
                    ConfigUtils.writeFile((File) oResult, toHtml(), false);
                }
            }
        });
        menu.add(item);

        Point where = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getListParent().getParent());
        menu.show(this, where.x, where.y);
    }

    /**
     * Return contents of chat list panel as html
     */
    private String toHtml()
    {
        String sDate = PropertyConfig.getDateFormat(GameEngine.getGameEngine().getLocale()).format(new Date(System.currentTimeMillis()));
        PokerGame game = (PokerGame) context_.getGame();
        String sDetails;

        // get game info
        if (game != null)
        {
            if (game.isOnlineGame())
            {
                sDetails = "In Online Game - <font color=blue>" + Utils.encodeHTML(game.getProfile().getName()) +
                           "</font> hosted by <font color=red>" + Utils.encodeHTML(game.getHost().getName()) +
                           "</font>";
                if (game.isPublic())
                {
                    sDetails += " (" + game.getPublicConnectURL() + ")";
                }
                else
                {
                    sDetails += " (private game)";
                }
            }
            else
            {
                sDetails = "In Practice Game - <font color=blue>" + Utils.encodeHTML(game.getProfile().getName()) +
                           "</font>";
            }
        }
        else
        {
            sDetails = "In Online Lobby";
        }

        // header
        StringBuilder sb = new StringBuilder("<HTML><HEAD><TITLE>");
        sb.append("DD Poker Chat Export - " + sDate);
        sb.append("</TITLE><BASE href=\"http://www.ddpoker.com/\"></HEAD><BODY>\n");

        // top table
        sb.append("<TABLE CELLSPACING=\"2\" CELLPADDING=\"0\"><TR>\n");
        sb.append("<TD><img src=\"images/pokericon32.jpg\">&nbsp;&nbsp;</TD><TD COLSPAN=2 style=\"font-size: 23px;\">DD Poker Chat Export</TD></TR>\n");
        sb.append("<TR><TD></TD><TD style=\"font-size: 15px;\"><B>Date:&nbsp;&nbsp;</B></TD>");
        sb.append("<TD style=\"font-size: 15px;\">" + sDate + "</TD></TR>\n");
        sb.append("<TR><TD></TD><TD style=\"font-size: 15px;\"><B>Where:&nbsp;&nbsp;</B></TD>");
        sb.append("<TD style=\"font-size: 15px;\">" + sDetails + "</TD></TR>\n");
        sb.append("</TD></TR></TABLE><BR>");

        // chat
        ChatItemPanel chat;
        Rectangle bounds = new Rectangle();
        String s;
        ChatPanel.ChatMessage msg;
        for (int i = 0; i < getItems().size(); i++)
        {
            chat = (ChatItemPanel) getItemPanel(i);
            chat.getBounds(bounds);
            msg = chat.getMessage();
            s = msg.sMsg;//html.getText();

            // <ddimg width="12" src="icon-small" yadj="-3" height="12">
            s = s.replaceAll("ddimg", "img");

            // results piece (jpg)
            s = s.replaceAll("src=\"(results-[0-9a-zA-Z\\-]+)\"", "src=\"gamehelp/images/$1.jpg\"");

            // icons for bet, raise, ante, etc. (png)
            s = s.replaceAll("src=\"([0-9a-zA-Z\\-]+)\"", "src=\"gamehelp/images/$1.png\"");

            // <ddcard card="Ks">
            s = s.replaceAll("<DDCARD CARD=\"(..)\">", "<img align=\"top\" src=\"images/cards/card_$1.png\">");
            sb.append(s);

            if (msg.bTable)
            {
                sb.append("<img src=\"images/spacer.gif\" width=\"1\" height=\"1\">");
            }
            sb.append("<BR>");
            sb.append("\n");
        }
        sb.append("</BODY></HTML>\n");

        // hash
        String sHash = SecurityUtils.getMD5Hash(sb.toString(), PokerConstants.CHAT_BYTES);
        sb.append("<!-- " + sHash + " -->");

        return sb.toString();
    }

    // not used
    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseMoved(MouseEvent e)
    {
    }


    DDHtmlArea styleproto_ = null;

    /**
     * chat item to display chat text
     */
    public static class ChatItemPanel extends ListItemPanel implements ActionListener
    {
        // keep track of one of the areas so we can share style sheet

        static Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
        static Border TABLE_BORDER = BorderFactory.createEmptyBorder(0, 0, 1, 0);

        DDHtmlArea html_;
        JComponent replyBase_;

        public ChatItemPanel(ListPanel p, Object item, String sStyle)
        {
            super(p, item, sStyle);
            if (false) Perf.construct(this, "Chat");

            ChatListPanel panel = (ChatListPanel) p;

            html_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle, null, panel.styleproto_);
            html_.setBorder(EMPTY_BORDER);
            html_.setOpaque(true);
            html_.setAlwaysAntiAlias(true);
            html_.addMouseListener(panel);
            html_.addMouseMotionListener(panel);

            if (panel.styleproto_ == null) panel.styleproto_ = html_;

            add(html_, BorderLayout.CENTER);
            setBorderLayoutGap(0, 5);

            setEnabled(false);
            setUseEmptyBorder(true);
        }

        public void finalize()
        {
            if (false) Perf.finalize(this, "Chat");
        }

        public void update()
        {
            ChatPanel.ChatMessage msg = getMessage();
            html_.setText(msg.sMsg);
            html_.setBorder(msg.bTable ? TABLE_BORDER : EMPTY_BORDER);

            if (msg.nReplyTo == -1)
            {
                if (replyBase_ != null)
                {
                    remove(replyBase_);
                    replyBase_ = null;
                }
            }
            else
            {
                if (replyBase_ == null)
                {
                    replyBase_ = new DDPanel();

                    GlassButton reply = new GlassButton("chat.reply", "Glass");
                    reply.addActionListener(this);
                    replyBase_ = GuiUtils.CENTER(reply);
                    replyBase_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                    add(replyBase_, BorderLayout.EAST);
                }
            }
        }

        public ChatPanel.ChatMessage getMessage()
        {
            return (ChatPanel.ChatMessage) item_;
        }

        public Dimension getPreferredSize()
        {
            return new Dimension(getListPanel().getItemWidth(), (int) super.getPreferredSize().getHeight());
        }

        public void actionPerformed(ActionEvent e)
        {
            ChatPanel.ChatMessage msg = (ChatPanel.ChatMessage) item_;
            msg.reply();
        }
    }
}
