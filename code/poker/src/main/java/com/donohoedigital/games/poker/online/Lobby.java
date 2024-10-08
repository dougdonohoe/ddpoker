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
 * Lobby.java
 *
 * Created on November 27, 2004, 4:19 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
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
public class Lobby extends BasePhase implements ChangeListener, PropertyChangeListener, DDTable.TableMenuItems
{
    static Logger logger = LogManager.getLogger(Lobby.class);
    
    private DDHtmlArea text_;
    private ButtonBox buttonbox_;
    private MenuBackground menu_;
    private DDPanel menubox_;
    private DDButton start_;
    private DDButton cancel_;
    private DDButton edit_;
    private PokerGame game_;
    private Shutdown shutdown_;

    private PlayerModel model_;
    private ObserverModel omodel_;
    private DDScrollTable playerScroll_;
    private DDScrollTable observerScroll_;
    private DDTable table_;
    private DDTable otable_;
    private ChatPanel chat_;
    private boolean bHost_;
    private HostStatus status_;
    private OnlineManager mgr_;
    private DDLabelBorder playerborder_;
    private DDLabelBorder observerborder_;
    private OptionBoolean obFill_;
    private DDLabel lFill_;

    private PokerPrefsPlayerList muted_;
    private PokerPrefsPlayerList banned_;
    private boolean bDone_ = false;
    private boolean bStarting_ = false;
    private Thread threadAlive_ = null;

    /**
     * Creates a new instance of Lobby 
     */
    public Lobby() {
    }

    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        // saving
        context_.setSpecialSavePhase(gamephase_.getName());

        // game
        game_ = (PokerGame) context.getGame();
        muted_ = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_MUTE);
        banned_ = PokerPrefsPlayerList.getSharedList(PokerPrefsPlayerList.LIST_BANNED);

        // online manager
        mgr_ = game_.getOnlineManager();
        if (mgr_ == null)
        {
            // if no online manager, create it.  This is null
            // when loading game from save file.
            mgr_ = game_.initOnlineManager(null);
        }

        // name of style used for all widgets in data area
        String STYLE = gamephase_.getString("style", "default");
        bHost_ = gamephase_.getBoolean("host", false);

        // shutdown hook to remove public game
        if (bHost_ && game_.isPublic())
        {
            shutdown_ = new Shutdown();
            Runtime.getRuntime().addShutdownHook(shutdown_);
        }

        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        menubox_ = menu_.getMenuBox();
        String sHelpName = menu_.getHelpName();
        
        // put buttons in the menubox_
        buttonbox_ = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox_.add(buttonbox_, BorderLayout.SOUTH);
        start_ = buttonbox_.getDefaultButton();
        cancel_ = buttonbox_.getButtonStartsWith("cancel");
        edit_ = buttonbox_.getButtonStartsWith("edit");


        // holds data we are gathering
        DDPanel data = new DDPanel(sHelpName);
        data.setBorderLayoutGap(10, 10);
        data.setBorder(BorderFactory.createEmptyBorder(2,10,5,10));
        menubox_.add(data, BorderLayout.CENTER);

        // help text
        text_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());
        data.add(text_, BorderLayout.CENTER);
        
        // formatting
        DDPanel middle = new DDPanel();
        middle.setBorderLayoutGap(0, 10);
        data.add(middle, BorderLayout.NORTH);

        DDPanel west = new DDPanel();
        west.setBorderLayoutGap(10, 0);
        middle.add(west, BorderLayout.WEST);

        ///
        /// player list
        ///
        playerborder_ = new DDLabelBorder("playerlist", STYLE);
        west.add(playerborder_, BorderLayout.CENTER);
        DDPanel playerbase = new DDPanel();
        playerbase.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 3));
        playerbase.setBorderLayoutGap(2,0);
        playerborder_.add(playerbase, BorderLayout.NORTH);
        
        playerScroll_ = new DDScrollTable(GuiManager.DEFAULT, "PokerPrefsPlayerList", "BrushedMetal", COLUMN_NAMES, COLUMN_WIDTHS);
        playerScroll_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        playerScroll_.setPreferredSize(new Dimension(playerScroll_.getPreferredWidth(), bHost_ ? 220 : 227));
        playerbase.add(playerScroll_, BorderLayout.CENTER);

        table_ = playerScroll_.getDDTable();
        model_ = new PlayerModel(game_);
        table_.setTableMenuItems(this);
        table_.setSelectOnRightClick(true);
        table_.setModel(model_);
        table_.setShowHorizontalLines(true);
        table_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        if (bHost_)
        {
            obFill_ = OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_FILL_COMPUTER,
                                                                        "PokerPrefsPlayerList", new TypedHashMap(), true),
                                                                        playerbase, BorderLayout.SOUTH);
            obFill_.setMap(game_.getProfile().getMap());
            obFill_.addChangeListener(this);
        }
        else
        {
            lFill_ = new DDLabel(GuiManager.DEFAULT, "PokerPrefsPlayerList");
            playerbase.add(lFill_, BorderLayout.SOUTH);
        }


        ///
        /// observer list
        ///
        observerborder_ = new DDLabelBorder("observerlist", STYLE);
        west.add(observerborder_, BorderLayout.SOUTH);
        DDPanel observerbase = new DDPanel();
        observerbase.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 3));
        observerborder_.add(observerbase, BorderLayout.NORTH);

        observerScroll_ = new DDScrollTable(GuiManager.DEFAULT, "PokerPrefsPlayerList", "BrushedMetal", OCOLUMN_NAMES, COLUMN_WIDTHS);
        observerScroll_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        observerScroll_.setPreferredSize(new Dimension(observerScroll_.getPreferredWidth(), 100));
        observerbase.add(observerScroll_, BorderLayout.WEST);

        otable_ = observerScroll_.getDDTable();
        omodel_ = new ObserverModel(game_);
        otable_.setTableMenuItems(this);
        otable_.setSelectOnRightClick(true);
        otable_.setModel(omodel_);
        otable_.setShowHorizontalLines(true);
        otable_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        otable_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        /////// RIGHT SIDE ///////
        DDPanel rightbase = new DDPanel();
        rightbase.setBorderLayoutGap(10, 0);
        middle.add(rightbase, BorderLayout.CENTER);
        
        ///
        /// URLs
        ///
        if (bHost_)
        {
            rightbase.add(createURLPanel(game_, STYLE, "LobbyURL", "BrushedMetal", 20), BorderLayout.NORTH);
        }
        else
        {
            rightbase.add(createStatusPanel(STYLE, "LobbyURL"), BorderLayout.NORTH);
        }

        ///
        /// chat
        ///
        DDLabelBorder chatborder = new DDLabelBorder("chat", STYLE);
        rightbase.add(chatborder, BorderLayout.CENTER);
        DDPanel chatbase = new DDPanel();
        chatbase.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
        chatbase.setBorderLayoutGap(0, 10);
        chatborder.add(chatbase, BorderLayout.CENTER);       
        chat_ = new ChatPanel(game_, context_, mgr_, "ChatInGame", "BrushedMetal", false);
        chatbase.add(chat_, BorderLayout.CENTER);

        // update
        updateProfileData();
    }

    /**
     * set playerborder
     */
    private void updateProfileData()
    {
        TournamentProfile profile = game_.getProfile();
        if (obFill_ != null)
        {
            obFill_.setMap(profile.getMap());
            obFill_.resetToMap();
        }
        if (lFill_ != null)
        {
            lFill_.setText(PropertyConfig.getMessage(profile.isFillComputer() ?
                                                     "msg.lobby.fill.on" : "msg.lobby.fill.off"));
        }
        playerborder_.setText(PropertyConfig.getMessage("msg.lobby.player",
                                                        profile.getMaxOnlinePlayers()));
        observerborder_.setText(PropertyConfig.getMessage("msg.lobby.observer",
                                                          profile.getMaxObservers()));
        playerborder_.repaint();
        observerborder_.repaint();
    }

   /**
     * Create panel to display URLs
     */
    public DDLabelBorder createStatusPanel(String STYLE, String CONTENTS_STYLE)
    {
        DDLabelBorder statusborder = new DDLabelBorder("hoststatus", STYLE);
        status_ = new HostStatus(game_, CONTENTS_STYLE, false);
        status_.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 5));
        statusborder.add(status_, BorderLayout.CENTER);
        return statusborder;
    }

    /**
     * Create panel to display URLs
     */
    public static DDLabelBorder createURLPanel(PokerGame game, String STYLE,
                                               String URLSTYLE, String BEVELSTYLE, int nLeftBorder)
    {
        DDLabelBorder urlborder = new DDLabelBorder("urls", STYLE);

        DDPanel urlbase = new DDPanel();
        urlbase.setBorder(BorderFactory.createEmptyBorder(0, nLeftBorder, 5, 5));
        urlbase.setBorderLayoutGap(3,0);
        urlborder.add(urlbase, BorderLayout.CENTER);

        OnlineConfiguration.Widgets w1,w2;

        String sLan = game.getLanConnectURL();
        String sPub = game.getPublicConnectURL();


        w1 = OnlineConfiguration.addIPText("connect.lan2", urlbase, BorderLayout.NORTH, URLSTYLE, BEVELSTYLE, null);
        w1.text.setText(sLan);
        w1.button.setBorderGap(2,5,2,6);

        w2 = OnlineConfiguration.addIPText("connect.pub2", urlbase, BorderLayout.CENTER, URLSTYLE, BEVELSTYLE, null);
        if (sPub == null) {
            sPub = PropertyConfig.getMessage("msg.no.pub.url");
            w2.button.setEnabled(false);
        }
        w2.text.setText(sPub);
        w2.button.setBorderGap(2,5,2,6);

        // size labels the same
        Dimension l1 = w1.label.getPreferredSize();
        Dimension l2 = w2.label.getPreferredSize();
        int width = Math.max(l1.width, l2.width);
        Dimension tNew = new Dimension(width, l1.height);
        w1.label.setPreferredSize(tNew);
        w2.label.setPreferredSize(tNew);

        return urlborder;
    }

    /**
     * Start of phase
     */
    public void start()
    {
        // set help text
        context_.getWindow().setHelpTextWidget(text_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help
        
        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, chat_.getFocusWidget());

        // init chat
        chat_.start();

        // if host, wakeup alive thread so new msg sent indicating
        // we are ready
        if (bHost_)
        {
            // lanmanager could be null if starting lobby from save file
            LanManager lanManager = ((PokerMain)engine_).getLanManager();
            if (lanManager != null) lanManager.wakeAliveThread();
        }
        // client listen for game loads
        else
        {
            game_.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
        }

        // alive thread (only needed for non-UDP transport since UDP layer handles that)
        if (!mgr_.isUDP())
        {
            threadAlive_ = new LobbyAlive();
            threadAlive_.start();
        }
    }
    
    /**
     * clear special save phase
     */
    public boolean processButton(GameButton button) 
    {
        if (button.getName().startsWith("cancel"))
        {
            String sMsg;

            // if client and we have tables, we are in countdown - show diff message
            if (!bHost_ && game_.getNumTables() > 0)
            {
                // copied from ExitPoker
                String sDetails = PropertyConfig.getMessage("msg.confirm.client", PropertyConfig.getMessage("msg.confirm.leave"));
                sMsg = PropertyConfig.getMessage("msg.quitgame.confirm", sDetails);
            }
            else
            {
                sMsg = PropertyConfig.getMessage(bHost_ ? "msg.confirm.lobby.host": "msg.confirm.lobby.client");
            }
            if (!EngineUtils.displayConfirmationDialog(context_, sMsg))
            {
                return false;
            }

            // cancel status panel
            finishStatus();

            // host - cancel game, restart game engine
            if (bHost_)
            {
                // quit game - manager will handle host/client quitting
                mgr_.cancelGame();
                removeOnlineGame();
                context_.restart();
            }
            // client - leave game and go back to previous menu
            else
            {
                mgr_.quitGame();
                context_.setGame(null);
            }
        }
        else if (start_ != null && button.getName().equals(start_.getName()))
        {
            if (game_.getNumPlayers() < 2 &&
                    (!(TESTING(PokerConstants.TESTING_ALLOW_SINGLE_PLAYER_ONLINE) && game_.getProfile().isFillComputer())))
            {
                GameButton bSwitch = EngineUtils.displayConfirmationDialogCustom(context_, "OnlineSwitchPractice",
                                     PropertyConfig.getMessage("msg.confirm.switch"), null,null,null);

                if (bSwitch != null && bSwitch.getName().startsWith("yesPractice"))
                {
                    mgr_.cancelGame();
                    removeOnlineGame();
                    // restart tournament starts current profile as a new game
                    TypedHashMap params = new TypedHashMap();
                    params.setObject(RestartTournament.PARAM_PROFILE, game_.getProfile());
                    context_.restart("RestartTournament", params);
                }
                return false;
            }

            String sMsg = PropertyConfig.getMessage("msg.lobby.start.confirm");
            if (!EngineUtils.displayConfirmationDialog(context_, sMsg, "lobby.start"))
            {
                return false;
            }

            // cancel status panel
            finishStatus();

            // prevent double click on this button
            start_.setEnabled(false);

            // can't cancel
            cancel_.setEnabled(false);

            // can't edit
            edit_.setEnabled(false);

            // no switching/banning
            bStarting_ = true;
        }
        else if (button.getName().startsWith("help"))
        {
            return true;
        }
        else if (button.getName().startsWith("options"))
        {
            return true;
        }
        else if (button.getName().startsWith("edit"))
        {
            edit();
            return true;
        }

        context_.setSpecialSavePhase(null);
        return true;
    }

    private boolean bEditing_ = false;
    /**
     * Edit button
     */
    private void edit()
    {
        TournamentProfile old = game_.getProfile();

        // show profile
        TournamentProfile copy = new TournamentProfile(old, old.getName());
        TypedHashMap params = new TypedHashMap();
        params.setObject(ProfileList.PARAM_PROFILE, copy);
        Phase phase = context_.processPhaseNow("EditProfile.tournament", params);
        Boolean changed = (Boolean) phase.getResult();

        // if something changed, update
        if (changed)
        {
            copy.setCreateDate(old);
            copy.fixAll();
            game_.setProfile(copy);

            bEditing_ = true;
            updateProfileData();
            sendUpdateMessage(true);
            bEditing_ = false;
        }
    }

    /**
     * fill changed
     */
    public void stateChanged(ChangeEvent e)
    {
        if (bEditing_) return;
        sendUpdateMessage(false);
    }

    /**
     * game loaded - updated props
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                updateProfileData();
            }
        });
    }

    /**
     * notify of edit
     */
    private void sendUpdateMessage(boolean bChat)
    {
        // change update date so it updates in LAN clients
        game_.getProfile().setUpdateDate();

        // update all players and send chat
        mgr_.sendDirtyPlayerUpdateToAll(false, true);
        if (bChat) mgr_.sendDirectorChat(PropertyConfig.getMessage("msg.chat.host.edit"), null);

        // update lan data
        LanManager lanManager = ((PokerMain)engine_).getLanManager();
        if (lanManager != null) lanManager.wakeAliveThread();

        // update server
        if (game_.isPublic())
        {
            OnlineServer manager = OnlineServer.getWanManager();
            manager.updateGameProfile(game_);
        }
    }

    /**
     * Finish
     */
    public void finish()
    {
        // remove listener
        if (!bHost_) game_.removePropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);

        // cancel status panel (just to make sure)
        finishStatus();

        // other cleanup
        chat_.finish();
        model_.cleanup();
        omodel_.cleanup();

        // remove shutdown hook
        if (shutdown_ != null) Runtime.getRuntime().removeShutdownHook(shutdown_);

        // finish alive thread
        bDone_ = true;
        try {
            if (threadAlive_ != null)
            {
                threadAlive_.interrupt();
                threadAlive_.join();
            }
        } catch (InterruptedException ie) {
            Thread.interrupted();
        }

        // super
        super.finish();
    }

    /**
     * cleanup host status panel
     */
    private void finishStatus()
    {
        if (status_ != null)
        {
            status_.finish();
            status_ = null;
        }
    }

    /**
     * Remove the game from the public list
     */
    private void removeOnlineGame()
    {
        // no server processing if not the host or not a public game
        if (!bHost_ || !game_.isPublic())
        {
            return;
        }

        OnlineServer.getWanManager().removeWanGame(game_);
    }

    /*
     * get selected player
     */
    private PokerPlayer getSelectedPlayer(DDTable table)
    {
        int n = table.getSelectedRow();
        if (n < 0) return null;
        if (table == table_) return model_.getPokerPlayer(n);
        else return omodel_.getPokerObserverAt(n);
    }

    /**
     * handle click - show menu
     * @param table
     */
    public boolean isItemsToBeAdded(DDTable table)
    {
        return getSelectedPlayer(table) != null;
    }

    public void addMenuItems(DDTable table, DDPopupMenu menu)
    {
        PokerPlayer p = getSelectedPlayer(table);
        if (!p.isLocallyControlled() && p.isHuman())
        {
            menu.add(new ShowTournamentTable.MutePlayer("PokerTable", p, muted_.containsPlayer(p.getName(), p.getKey()), mgr_, true));
            menu.add(new ShowTournamentTable.BanPlayer(context_, "PokerTable", p, banned_.containsPlayer(p.getName(), p.getKey()), mgr_, mgr_, true, bHost_ && !bStarting_));
        }

        if (bHost_ && p.isHuman() && !bStarting_)
        {
            menu.add(new SwitchPlayer(context_, "PokerTable", p, mgr_));
        }
    }

    private static ImageIcon switchIcon_ = ImageConfig.getImageIcon("menuicon.switch");

    /**
     * mute menu item
     */
    private static class SwitchPlayer extends DDMenuItem implements ActionListener
    {
        GameContext context;
        OnlineManager mgr;
        PokerPlayer player;

        public SwitchPlayer(GameContext context, String sStyle, PokerPlayer p, OnlineManager mgr)
        {
            super(GuiManager.DEFAULT, sStyle);
            this.context = context;
            this.player = p;
            this.mgr = mgr;
            setDisplayMode(DDMenuItem.MODE_NORMAL);
            setText(PropertyConfig.getMessage(p.isObserver() ? "menuitem.switch.toplayer":"menuitem.switch.toobserver", p.getName()));
            setIcon(switchIcon_);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            // check to see if room for another player
            // no need to check observer quantity since adding an extra observer doesn't affect gameplay
            if (player.isObserver() && !mgr.isSpaceForPlayer())
            {
                EngineUtils.displayInformationDialog(context, PropertyConfig.getMessage("msg.switch.noroom",
                                                                                Utils.encodeHTML(player.getName())));
                return;
            }

            // do switch
            mgr.switchPlayer(player);
        }
    }

    /**
     * Shutdown hook to make as complete an attempt as possible to keep the back end in sync.
     */
    private class Shutdown extends Thread
    {
        public Shutdown()
        {
            setName("Lobby-Shutdown");
        }
        public void run()
        {
            logger.debug("Shutting down...");
            removeOnlineGame();
        }
    }

    /**
     * alive thread
     */
    private class LobbyAlive extends Thread
    {
        LobbyAlive()
        {
            super("LobbyAlive");
        }

        public void run()
        {
            while (!bDone_)
            {
                try {
                    Utils.sleepMillis(OnlineManager.ALIVE_SLEEP_MILLIS);

                    if (!bDone_) {
                        mgr_.alive(false);
                    }
                }
                catch (Throwable t)
                {
                    logger.error("LobbyAlive caught an unexcepted exception: " + Utils.formatExceptionText(t));
                }
            }
        }
    }

    // client table info
    private static final int[] COLUMN_WIDTHS = new int[] {
        25, 175
    };
    private static final String[] COLUMN_NAMES = new String[] {
        "playernum", LanClientInfo.LAN_PLAYER_NAME
    };
    private static final String[] OCOLUMN_NAMES = new String[] {
        "playernum", "observer"
    };

    /**
     * Used by table to display players in game
     */
    private class PlayerModel extends DefaultTableModel implements PropertyChangeListener
    {
        private PokerGame game;

        public PlayerModel(PokerGame game) 
        {
            this.game = game;            
            game.addPropertyChangeListener(Game.PROP_PLAYERS, this);
            game.addPropertyChangeListener(Game.PROP_PLAYERS_LIST, this);
        }

        public void cleanup()
        {
            game.removePropertyChangeListener(Game.PROP_PLAYERS, this);
            game.removePropertyChangeListener(Game.PROP_PLAYERS_LIST, this);
        }
        
        public PokerPlayer getPokerPlayer(int r) {
            return game.getPokerPlayerAt(r);
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
            if (game == null) {
                return 0;
            }
            return game.getNumPlayers();
        }

        public Object getValueAt(int rowIndex, int colIndex) 
        {
            PokerPlayer player = getPokerPlayer(rowIndex);
            
            if (COLUMN_NAMES[colIndex].equals("playernum"))
            {
                return "" + (rowIndex + 1);
            }
            else if (COLUMN_NAMES[colIndex].equals(LanClientInfo.LAN_PLAYER_NAME))
            {
                return player.getDisplayName(true);
            }
            return "[bad column]";
        }
        
        private int nIgnoreNext_ = 0;
        /**
         * Update when player list changes (need to invoke from SwingThread)
         */
        public void propertyChange(PropertyChangeEvent evt)
        {
            // if updating whole list, ignore (new count) - 1 add messages
            // to avoid mutliple updates as the list is rebuilt.  Refresh
            // occurs on last add (thus the decrement by one)
            if (evt.getPropertyName().equals(Game.PROP_PLAYERS_LIST))
            {
                int nNew = (Integer) evt.getNewValue();
                if (nNew > 0) nIgnoreNext_ = nNew - 1;
                return;
            }
            else
            {
                if (nIgnoreNext_ > 0)
                {
                    nIgnoreNext_--;
                    return;
                }

                boolean bLan = false;

                // added
                if (evt.getNewValue() != null && ((PokerPlayer)evt.getNewValue()).isHuman())
                {
                    AudioConfig.playFX("playerjoin");
                    if (bHost_) bLan = true;
                }

                // removed
                if (evt.getOldValue() != null && ((PokerPlayer)evt.getOldValue()).isHuman())
                {
                    // don't send removed updates for host (only case where host removed is
                    // switch player - we'll send lan update on the cooresonding add)
                    // we do this check to prevent a small NPE on lan guests monitoring this
                    // game in join window ... due to need for a host in the code for
                    // PokerGame.isAcceptingRegistrations()
                    if (bHost_ && !((GamePlayer) evt.getOldValue()).isHost()) bLan = true;
                }

                // update lan with new data
                if (bLan)
                {
                    LanManager lanManager = ((PokerMain)engine_).getLanManager();
                    if (lanManager != null) lanManager.wakeAliveThread();
                }

                // table changed
                GuiUtils.invoke(new Runnable() {
                    public void run() {
                        fireTableDataChanged();
                    }
                });
            }
        }
    }

    /**
     * Used by table to display observers of game
     */
    private class ObserverModel extends DefaultTableModel implements PropertyChangeListener
    {
        private PokerGame game;

        public ObserverModel(PokerGame game)
        {
            this.game = game;
            game.addPropertyChangeListener(Game.PROP_OBSERVERS, this);
            game.addPropertyChangeListener(Game.PROP_OBSERVERS_LIST, this);
        }

        public void cleanup()
        {
            game.removePropertyChangeListener(Game.PROP_OBSERVERS, this);
            game.removePropertyChangeListener(Game.PROP_OBSERVERS_LIST, this);
        }

        public PokerPlayer getPokerObserverAt(int r) {
            return game.getPokerObserverAt(r);
        }

        public String getColumnName(int c) {
            return OCOLUMN_NAMES[c];
        }

        public int getColumnCount() {
            return COLUMN_WIDTHS.length;
        }

        public boolean isCellEditable(int r, int c) {
            return false;
        }

        public int getRowCount() {
            if (game == null) {
                return 0;
            }
            return game.getNumObservers();
        }

        public Object getValueAt(int rowIndex, int colIndex)
        {
            PokerPlayer player = getPokerObserverAt(rowIndex);

            if (OCOLUMN_NAMES[colIndex].equals("playernum"))
            {
                return "" + (rowIndex + 1);
            }
            else if (OCOLUMN_NAMES[colIndex].equals("observer"))
            {
                return player.getDisplayName(true);
            }
            return "[bad column]";
        }

        private int nIgnoreNext_ = 0;
        /**
         * Update when observer list changes (need to invoke from SwingThread)
         */
        public void propertyChange(PropertyChangeEvent evt)
        {
            // if updating whole list, ignore (new count) - 1 add messages
            // to avoid mutliple updates as the list is rebuilt.  Refresh
            // occurs on last add (thus the decrement by one)
            if (evt.getPropertyName().equals(Game.PROP_OBSERVERS_LIST))
            {
                int nNew = (Integer) evt.getNewValue();
                if (nNew > 0) nIgnoreNext_ = nNew - 1;
                return;
            }
            else
            {
                if (nIgnoreNext_ > 0)
                {
                    nIgnoreNext_--;
                    return;
                }

                if (evt.getNewValue() != null && ((PokerPlayer)evt.getNewValue()).isHuman())
                {
                    AudioConfig.playFX("observerjoin");
                }
                GuiUtils.invoke(new Runnable() {
                    public void run() {
                        fireTableDataChanged();
                    }
                });
            }
        }
    }
}
