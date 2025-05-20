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
/*
 * ChatPanel.java
 *
 * Created on December 7, 2004, 8:50 AM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

/**
 *
 * @author  donohoe
 */
public class ChatPanel extends DDPanel implements PropertyChangeListener, ChatHandler
{
    static Logger logger = LogManager.getLogger(ChatPanel.class);

    private boolean bOnlineInGame_;
    private int nDisplayOpt_ = -1;
    private JComponent center_;

    private GameContext context_;
    private PokerGame game_;
    private PokerPlayer local_;
    private ChatManager mgr_;

    protected PokerPrefsPlayerList muted_;
    protected String STYLE;
    protected String BEVEL_STYLE;

    protected ChatListPanel[] chatList_ = new ChatListPanel[2];
    protected DDTextField msg_;
    private DDPanel bottomControls_;

    protected String cDDPoker_;
    protected String cLocal_;
    protected String cRemote_;
    protected String cRemoteObs_;
    protected String cDirector_;
    protected String cDealer_;
    protected String cDirectorBG_;
    protected String cDealerBG_;

    private DDTabbedPane tab_;

    /**
     * new chat panel
     */
    public ChatPanel(PokerGame game, GameContext context, ChatManager mgr,
                     String sStyle, String sBevelStyle, boolean bInGame)
    {
        super();
        context_ = context;
        game_ = game;
        if (game_ != null) local_ = game_.getLocalPlayer();
        bOnlineInGame_ = game_ != null && game_.isOnlineGame() && bInGame;

        muted_ = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_MUTE);
        mgr_ = mgr;
        STYLE = sStyle;
        BEVEL_STYLE = sBevelStyle;

        cDDPoker_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.ddpoker", Color.green));
        cLocal_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.local", Color.blue));
        cRemote_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.remote", Color.red));
        cRemoteObs_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.remote.obs", Color.green));
        cDirector_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.director", Color.black));
        cDealer_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.dealer", Color.black));
        cDirectorBG_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.directorbg", Color.white));
        cDealerBG_ = Utils.getHtmlColor(StylesConfig.getColor("Chat.dealerbg", Color.white));

        createContents();
    }

    private GameListener gamelistener_ = new GameListener();

    /**
     * Game loaded, reset local player in chat (can change if we disconnect/reconnect)
     */
    private class GameListener implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            resetLocal();
        }
    }

    /**
     * get text field
     */
    public DDTextField getTextField()
    {
        return msg_;
    }

    /**
     * reset local player (can change in lobby if disconnect/reconnect)
     */
    public void resetLocal()
    {
        if (game_ != null) local_ = game_.getLocalPlayer();
    }

    public void setChatManager(ChatManager mgr)
    {
        mgr_ = mgr;
    }
    
    public void start()
    {
        // listen for loads (rejoin/reconnect)
        if (game_ != null) game_.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, gamelistener_);
        if (mgr_ != null) mgr_.setChatHandler(this);
    }
    
    public void finish()
    {
        if (game_ != null) game_.removePropertyChangeListener(PokerGame.PROP_GAME_LOADED, gamelistener_);
        if (mgr_ != null) mgr_.setChatHandler(null);
    }
    
    /**
     * Create chat components
     */
    private void createContents()
    {
        setBorderLayoutGap(5, 0);

        for (int i = 0; i < (bOnlineInGame_ ? 2 : 1); i++)
        {
            // in online game 0 == dealer chat (or both), 1 == player chat (if multiple tabs)
            chatList_[i] = new ChatListPanel(context_, ChatListPanel.ChatItemPanel.class, (i == 0 ? 75 : 50),
                                             STYLE, BEVEL_STYLE,
                                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            chatList_[i].setPreferredSize(getChatListPreferredSize());
        }

        // create display
        createDisplay(false);

        // bottom - text input and buttons (border gap to align with html, plus leave room on mac for resize ctrl)
        //          null case for subclasses
        boolean bAllowSend = game_ == null || game_.isOnlineGame();

        // clear button
        GlassButton clear = new GlassButton("chat.clear", "Glass");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (center_ == tab_)
                {
                    ChatTab tab = (ChatTab) tab_.getSelectedComponent();
                    tab.chatList.removeAllItems();
                }
                else
                {
                    chatList_[0].removeAllItems();
                    if (chatList_[1] != null) chatList_[1].removeAllItems();
                }
            }
        });

        if (bAllowSend || TESTING(PokerConstants.TESTING_CHAT_PERF) || TESTING(EngineConstants.TESTING_PERFORMANCE))
        {
            bottomControls_ = new DDPanel();
            bottomControls_.setBorderLayoutGap(0,5);
            bottomControls_.setBorder(BorderFactory.createEmptyBorder(0,0,0,DDScrollBar.SB_SIZE+2));
            add(bottomControls_, BorderLayout.SOUTH);

            // message text
            msg_ = new DDTextField(GuiManager.DEFAULT, STYLE, BEVEL_STYLE);
            //msg_.addFocusListener(new GuiUtils.FocusDebugger("chattext"));
            msg_.setTextLengthLimit(500);
            msg_.setRegExp("^.+$");
            msg_.addPropertyChangeListener("value", this);
            bottomControls_.add(msg_, BorderLayout.CENTER);

            DDPanel buttonbase = new DDPanel();
            buttonbase.setLayout(new GridLayout(1,2,3,0));//new FlowLayout(FlowLayout.CENTER, 5, 0));
            bottomControls_.add(buttonbase, BorderLayout.EAST);

            // send button
            GlassButton send = new GlassButton("chat.send", "Glass");
            send.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    // make sure we have something to send
                    if (msg_.isValidData())
                    {
                        sendChat();
                    }
                }
            });
            msg_.setDefaultOverride(send);

            buttonbase.add(send);

            if (game_ != null && game_.isInProgress())
            {
                if (local_.isHost())
                {
                    // send button
                    GlassButton sendall = new GlassButton("chat.sendall", "Glass");
                    sendall.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e)
                        {
                            // make sure we have something to send
                            if (msg_.isValidData())
                            {
                                sendChatAll();
                            }
                        }
                    });
                    buttonbase.add(sendall);
                }
                else
                {
                    // send button
                    GlassButton sendhost = new GlassButton("chat.sendhost", "Glass");
                    sendhost.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e)
                        {
                            // make sure we have something to send
                            if (msg_.isValidData())
                            {
                                sendChatPrivate(PokerPlayer.HOST_ID);
                            }
                        }
                    });
                    buttonbase.add(sendhost);
                }
            }

            buttonbase.add(clear);

            if (game_ != null)
            {
                addLobbyButton(buttonbase);
            }

            if (TESTING(PokerConstants.TESTING_CHAT_PERF) || TESTING(EngineConstants.TESTING_PERFORMANCE))
            {
                startTest_ = new GlassButton(GuiManager.DEFAULT, "Glass");
                startTest_.setText("Start");
                buttonbase.add(startTest_);
                startTest_.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        startTest_.setEnabled(false);
                        stopTest_.setEnabled(true);
                        startTest();
                    }
                });

                stopTest_ = new GlassButton(GuiManager.DEFAULT, "Glass");
                stopTest_.setText("Stop");
                stopTest_.setEnabled(false);
                buttonbase.add(stopTest_);
                stopTest_.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        startTest_.setEnabled(true);
                        stopTest_.setEnabled(false);
                        stopTest();
                    }
                });

                GlassButton dump = new GlassButton(GuiManager.DEFAULT, "Glass");
                dump.setText("Dump");
                buttonbase.add(dump);
                dump.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        logger.debug("Thread dump:\n" + Utils.getAllStacktraces());
                    }
                });
            }
        }
        else
        {
            DDPanel buttonbase = new DDPanel();
            buttonbase.setLayout(new GridLayout(0, 1, 0, 4));//new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5));
            buttonbase.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
            buttonbase.add(clear);
            addLobbyButton(buttonbase);

            // JDD 2019
            add(GuiUtils.setDDPanelDebug(GuiUtils.NORTH(buttonbase), false), BorderLayout.EAST);
        }

        // init
        checkButtons();
    }

    private void addLobbyButton(DDPanel buttonbase)
    {
        // don't add button if not online-activated
        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        if (!profile.isActivated()) return;

        GlassButton lobby = new GlassButton("chat.lobby", "Glass");
        lobby.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    if (OnlineLobby.showLobby(GameEngine.getGameEngine(), context_, PlayerProfileOptions.getDefaultProfile()))
                    {
                        context_.processPhase("OnlineLobby");
                    }
                }
            });
        buttonbase.add(lobby);
    }

    /**
     * create display based on user choice
     */
    public void updatePrefs()
    {
        createDisplay(true);
    }

    /**
     * create display based on user choice
     */
    private void createDisplay(boolean bRepaint)
    {
        int nOpt = PokerUtils.getIntOption(PokerConstants.OPTION_CHAT_DISPLAY, PokerConstants.DISPLAY_SPLIT);
        if (nDisplayOpt_ == nOpt || (!bOnlineInGame_ && center_ != null))
        {
            return;
        }
        nDisplayOpt_ = nOpt;

        // remove existing
        if (center_ != null) remove(center_);

        // split
        if (bOnlineInGame_ && nOpt == PokerConstants.DISPLAY_SPLIT)
        {
            center_ = new DDSplitPane(GuiManager.DEFAULT, STYLE, SwingConstants.VERTICAL,
                                                chatList_[1], chatList_[0]);
        }
        // tabs
        else if (bOnlineInGame_ && nOpt == PokerConstants.DISPLAY_TAB)
        {
            center_ = tab_ = new DDTabbedPane(STYLE, BEVEL_STYLE, DDTabbedPane.LEFT);
            tab_.setOpaque(false);
            tab_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            for (int i = 0; i < chatList_.length; i++)
            {
                tab_.addTab(PropertyConfig.getMessage("msg.chatpanel."+i), null, new ChatTab(chatList_[i]), null);
            }
        }
        // single
        else
        {
            center_ = chatList_[0];
        }

        // put it in center
        add(center_, BorderLayout.CENTER);

        // redisplay
        if (bRepaint)
        {
            revalidate();
            repaint();
        }
    }

    private class ChatTab extends DDTabPanel
    {
        ChatListPanel chatList;

        public ChatTab(ChatListPanel chatList)
        {
            this.chatList = chatList;
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            createUI(); // create now
        }

        protected void createUI()
        {
            add(chatList, BorderLayout.CENTER);
        }

        public ChatListPanel getChatListPanel()
        {
            return chatList;
        }
    }

    /**
     * remove bottom controls, replace with close button
     */
    public void removeBottomControls(final GameContext context)
    {
        remove(bottomControls_);

        // close button
        GlassButton close = new GlassButton("close", "Glass");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                context.close();
            }
        });

        add(GuiUtils.CENTER(close), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    /**
     * default size
     */
    protected Dimension getChatListPreferredSize()
    {
        return new Dimension(250, 100);
    }

    /**
     * focus widget
     */ 
    public JComponent getFocusWidget()
    {
        return msg_;
    }

    /**
     * Return true if text entry widget exists and has focus
     */
    public boolean hasFocus()
    {
        return msg_ != null && msg_.hasFocus();
    }

    /**
     * request focus
     */
    public void requestFocus()
    {
        if (msg_ != null) msg_.requestFocus();
    }

    /**
     * Send a chat message
     */
    protected void sendChat()
    {        
        String sMsg = Utils.encodeHTML(msg_.getText(), false);

        msg_.setText("");
        if (mgr_ != null) mgr_.sendChat(sMsg, game_.getCurrentTable(), null);

        displayMessage(local_.getID(), PokerConstants.CHAT_ALWAYS, sMsg, true);
    }

    /**
     * Send test chat
     */
    protected void sendChatTest(String sMsg, String sTestData)
    {
        mgr_.sendChat(sMsg, null, sTestData);
    }

    /**
     * Send all chat message
     */
    private void sendChatAll()
    {
        String sMsg = PropertyConfig.getMessage("msg.chat.fromhost", Utils.encodeHTML(msg_.getText()));

        msg_.setText("");
        if (mgr_ != null) mgr_.sendDirectorChat(sMsg, null);
    }

    /**
     * Send all chat message
     */
    private void sendChatPrivate(int nToID)
    {
        String sMsg = PropertyConfig.getMessage(nToID == PokerPlayer.HOST_ID ? "msg.chat.tohost" : "msg.chat.replyhost",
                                                Utils.encodeHTML(msg_.getText()), local_.getTable().getName());

        msg_.setText("");
        if (mgr_ != null) mgr_.sendChat(nToID, sMsg);

        displayMessage(local_.getID(), PokerConstants.CHAT_PRIVATE, sMsg, true);
    }

    /**
     * Chat message typed
     */
    public void propertyChange(PropertyChangeEvent evt) 
    {
        checkButtons();
    }    
    
    /**
     * enable/disable
     */
    private void checkButtons()
    {
        // for now, leave enabled always - too
        // distracting to turn on/off for each message
        // and not really necessary - JDD 3/16/05
        //send_.setEnabled(msg_.isValidData());
    }
    
    /**
     * OnlineMessage received. 
     */
    public void chatReceived(OnlineMessage omsg) 
    {   
        // calls to this are synchronized through the OnlineManager
        final String sMsg = omsg.getChat();
        final int nFrom = omsg.getFromPlayerID();
        final int nType = omsg.getChatType();

        // need to update from Swing thread
        SwingUtilities.invokeLater(
            new Runnable() {
                String _sMsg = sMsg;
                int _nFrom = nFrom;
                int _nType = nType;
                public void run() {
                    displayMessage(_nFrom, _nType, _sMsg, false);
                }
            }
        );
    }
    
    /**
     * add message to chat window
     */
    protected void displayMessage(int nFrom, int nType, String sMsg, boolean bLocal)
    {
        String bgColor = null;
        String chatColor = null;
        String sName = null;
        String sKey;
        ChatListPanel list = chatList_[0];

        if (nFrom == OnlineMessage.CHAT_DIRECTOR_MSG_ID || sMsg.startsWith("tahoezorro"))
        {
            chatColor = cDirector_;
            bgColor = cDirectorBG_;
            sKey = "msg.chat.director";
        }
        else if (nFrom == OnlineMessage.CHAT_DEALER_MSG_ID || sMsg.startsWith("lemongulch"))
        {
            chatColor = cDealer_;
            bgColor = cDealerBG_;
            sKey = "msg.chat.dealer";
            boolean bSkip = false;
            int nOpt = PokerUtils.getIntOption(PokerConstants.OPTION_CHAT_DEALER, PokerConstants.DEALER_ALL);
            switch (nType)
            {
                case PokerConstants.CHAT_ALWAYS:
                    break;

                case PokerConstants.CHAT_1:
                    if (nOpt == PokerConstants.DEALER_NONE)
                    {
                        bSkip = true;
                    }
                    break;

                case PokerConstants.CHAT_2:
                    if (nOpt == PokerConstants.DEALER_NONE ||
                        nOpt == PokerConstants.DEALER_NO_PLAYER_ACTION)
                    {
                        bSkip = true;
                    }
                    break;

                case PokerConstants.CHAT_TIMEOUT:
                    if (!PokerUtils.isOptionOn(PokerConstants.OPTION_CHAT_TIMEOUT, true))
                    {
                        bSkip = true;
                    }
                    break;

            }
            if (bSkip) return;
        }
        else
        {
            PokerPlayer player = game_.getPokerPlayerFromID(nFrom);
            if (player == null)
            {
                logger.warn("No player for chat, id=" + nFrom + " msg=" + sMsg);
                return;
            }

            // chat - check user prefs
            if (!bLocal && nType != PokerConstants.CHAT_PRIVATE)
            {
                // observers muted?
                if (player.isObserver())
                {
                    if (!PokerUtils.isOptionOn(PokerConstants.OPTION_CHAT_OBSERVERS, true)) return;
                }
                // players muted?
                else if (!PokerUtils.isOptionOn(PokerConstants.OPTION_CHAT_PLAYERS, true)) return;

                // muted specifically?
                if (muted_.containsPlayer(player.getName(), player.getKey())) return;
            }

            sName = Utils.encodeHTML(player.getName());
            chatColor = bLocal ? cLocal_ : (player.isObserver() ? cRemoteObs_ : cRemote_);
            if (isDDPoker(player.getName()) && !bLocal)
            {
                chatColor = cDDPoker_;
            }

            if (nType == PokerConstants.CHAT_PRIVATE)
            {
                sKey = player.isHost() ? "msg.chat.reply":"msg.chat.private";
            }
            else if (player.isDemo())
            {
                sKey = player.isObserver() ? "msg.chat.demo.obs" : "msg.chat.demo";
            }
            else
            {
                sKey = player.isObserver() ? "msg.chat.obs" : "msg.chat";
            }

            if (bOnlineInGame_ && nDisplayOpt_ != PokerConstants.DISPLAY_ONE) list = chatList_[1];
        }

        String sChat = PropertyConfig.getMessage(sKey, chatColor, sName, sMsg, bgColor);

        displayMessage(list, sChat, (!bLocal && nType == PokerConstants.CHAT_PRIVATE && local_.isHost()) ? nFrom : -1);
    }

    protected void displayMessage(ChatListPanel list, String sChat, int nReplyTo)
    {
        ChatPanel.ChatMessage msg = new ChatPanel.ChatMessage(sChat, nReplyTo);
        list.displayMessage(msg);
    }

    /**
     * is DD Poker user?
     */
    protected boolean isDDPoker(String sName)
    {
        sName = sName.toLowerCase();
        return sName.equals("doug donohoe") || sName.equals("ddpoker support") || sName.equals("greg king");
    }

    /**
     * a chat message - handle card replacement and reply to host
     */
    class ChatMessage
    {
        String sMsg;
        int nReplyTo;
        boolean bTable;

        ChatMessage(String sMsg, int nReplyTo)
        {
            // replace /[card][card]...[card] with DDCard
            StringBuilder sb = new StringBuilder(sMsg.length());
            char c;
            char lookahead[] = new char[2];
            int search = 0;
            for (int i = 0; i < sMsg.length(); i++)
            {
                c = sMsg.charAt(i);
                if (c == '/')
                {
                    // another /, restart search
                    if (search != 0)
                    {
                        sb.append(sMsg.substring(search, i));
                    }
                    search = i;
                    continue;
                }

                if (search != 0)
                {
                    getLookAhead(lookahead, sMsg, i);
                    if (c == '1' && lookahead[0] == '0')
                    {
                        continue;
                    }
                    char suit = lookahead[0];
                    char next = lookahead[1];
                    if ((('2' <= c && c <= '9') ||
                         (c == '0' || // ten
                          c == 't' || c == 'T' ||
                          c == 'j' || c == 'J' ||
                          c == 'q' || c == 'Q' ||
                          c == 'k' || c == 'K' ||
                          c == 'a' || c == 'A')
                         )
                      && (suit == 'h' || suit == 'H' ||
                          suit == 's' || suit == 'S' ||
                          suit == 'd' || suit == 'D' ||
                          suit == 'c' || suit == 'C')
                      && (next != '>')
                      )
                    {
                        sb.append("<DDCARD CARD=\"");
                        if (c == '0') c = 'T';
                        sb.append((""+c).toUpperCase());
                        sb.append((""+suit).toLowerCase());
                        sb.append("\">");
                        //noinspection AssignmentToForLoopParameter
                        i++; // bypass suit
                        search = i+1; // new search
                    }
                    else
                    {
                        sb.append(sMsg.substring(search, i+1));
                        search = 0;
                    }
                }
                else
                {
                    sb.append(c);
                }
            }

            this.sMsg = sb.toString();
            this.nReplyTo = nReplyTo;
            this.bTable = sMsg.toLowerCase().contains("table");
        }

        private void getLookAhead(char lookahead[], String s, int index)
        {
            Arrays.fill(lookahead, ':');
            int look = 0;
            for (int i = index + 1; i < s.length() && look < lookahead.length; i++)
            {
                lookahead[look++] = s.charAt(i);
            }
        }

        void reply()
        {
            // make sure we have something to send
            if (msg_.isValidData())
            {
                sendChatPrivate(nReplyTo);
            }
        }
    }

    /////
    ///// TESTING CODE
    /////

    private int CNT = 0;
    private TestThread test_;
    private GlassButton startTest_, stopTest_;

    private void startTest()
    {

        test_ = new TestThread();
        logger.debug("<<<<<<<<<<<<<<<<<<<< Started Test " + test_.nTestNum);
        test_.start();
    }

    private void stopTest()
    {
        test_.finish();
        logger.debug(">>>>>>>>>>>>>>>>>>>> Stopped Test " + test_.nTestNum);
        test_ = null;
    }

    private class TestThread extends Thread
    {
        int nTestNum;
        boolean bDone;
        int nNum = 1;
        int delay = 25;

        public TestThread()
        {
            super("TestThread-"+ ++CNT);
            nTestNum = CNT;

            try {
                delay = Integer.parseInt(msg_.getText());
            }
            catch (NumberFormatException ignored)
            {

            }
            if (delay < 10) delay = 10;
        }

        public void run()
        {
            String sMsg;
            int TESTSIZE = 250;
            StringBuilder sb = new StringBuilder(TESTSIZE);
            char c;
            for (int i = 0; i < TESTSIZE; i++)
            {
                c = (char) ((int)'a' + (i % 26));
                sb.append(c);
            }
            String sTestData = sb.toString();
            while (!bDone)
            {
                sMsg = "Test "+nTestNum+", Message " + nNum + " with " + sTestData.length() + " bytes, delay="+delay;
                nNum++;
                sendChatTest(sMsg, sTestData);
                displayTestMessage(sMsg);
                Utils.sleepMillis(delay);
            }
        }

        public void finish()
        {
            bDone = true;
        }
    }

    private void displayTestMessage(String sMsg)
    {
        DisplayTest test = new DisplayTest();
        test.sMsg = sMsg;
        SwingUtilities.invokeLater(test);
    }

    private class DisplayTest implements Runnable
    {
        String sMsg;
        public void run()
        {
            displayMessage(local_ == null ? 0 : local_.getID(), PokerConstants.CHAT_ALWAYS, sMsg, true);
        }
    }
}
