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
 * TournamentProfileDialog.java
 *
 * Created on January 27, 2003, 3:49 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.gui.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import static com.donohoedigital.config.DebugConfig.*;

/**
 * @author Doug Donohoe
 */
public class TournamentProfileDialog extends OptionMenuDialog implements PropertyChangeListener, ActionListener, FocusListener
{
    static Logger logger = LogManager.getLogger(TournamentProfileDialog.class);

    static com.donohoedigital.base.Format fPerc = new com.donohoedigital.base.Format("%1.3f");
    private javax.swing.border.Border empty_ = null;
    private TournamentProfile profile_;
    private PokerGame game_; // used when editing during a tournament
    private TypedHashMap dummy_ = new TypedHashMap();
    private TypedHashMap labelignore_ = new TypedHashMap();
    private TypedHashMap orig_;
    private ArrayList rebuyOptions_ = new ArrayList();
    private ArrayList addonOptions_ = new ArrayList();
    private DDPanel base_;
    private DDTextField name_;
    private DDNumberSpinner numPlayers_;
    private DDNumberSpinner buyinCost_;
    private DDCheckBox rebuys_;
    private DDCheckBox addons_;
    private DDLabelBorder payout_;
    private DDNumberSpinner houseAmount_;
    private DDNumberSpinner spotPerc_;
    private DDNumberSpinner spotAmount_;
    private DDRadioButton buttonAuto_, buttonPerc_, buttonAmount_;
    private DDRadioButton buttonSatellite_;
    private boolean bDetailsTabReady_ = false;
    private SpotPanel spots_[] = new SpotPanel[TournamentProfile.MAX_SPOTS];
    private String saveA_[] = new String[TournamentProfile.MAX_SPOTS];
    private String saveP_[] = new String[TournamentProfile.MAX_SPOTS];
    private int nNumSpots_ = 0;
    private DDRadioButton buttonSelected_;
    private DDButton clear_;
    private ButtonGroup buttonGroup_;
    private DDPanel spotsParent_;
    private JScrollPane spotscroll_;
    private DDLabel total_;
    private boolean bUpdating_ = false;

    private DDTabbedPane tabs_;
    private OpponentMixPanel oppmix_;
    private LevelsTab leveltab_;


    /**
     * help text area
     */
    protected int getTextPreferredHeight()
    {
        return 55;
    }

    /**
     * create ui
     */
    public JComponent getOptions()
    {
        TournamentProfile profile = (TournamentProfile) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile, "No 'profile' in params");

        // usually null, unless editing in-tournament
        game_ = (PokerGame) context_.getGame();

        return getOptions(profile, STYLE);
    }

    /**
     * Get options using a specified profile
     */
    public JComponent getOptions(TournamentProfile profile, String sStyle)
    {
        STYLE = sStyle;
        profile_ = profile;

        // save starting values for use during reset
        orig_ = new DMTypedHashMap();
        orig_.putAll(profile_.getMap());

        // init pool so it accepts changes in user input
        profile.setPrizePool(-1, false);

        // need opponent mix for okay processing
        oppmix_ = new OpponentMixPanel(profile_);
        leveltab_ = new LevelsTab();
        base_ = new DDPanel(GuiManager.DEFAULT, STYLE); // used for colors in Sep

        // tabs
        ImageIcon error = ImageConfig.getImageIcon("taberror");
        ImageIcon icon = ImageConfig.getImageIcon("ddlogo20");
        tabs_ = new DDTabbedPane(STYLE, null, JTabbedPane.TOP);
        tabs_.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabs_.addTab("msg.description", icon, error, new DescTab());
        tabs_.addTab("msg.levels", icon, error, leveltab_);
        tabs_.addTab("msg.tourdetails", icon, error, new DetailsTab());
        tabs_.addTab("msg.opponentmix", icon, error, oppmix_);
        tabs_.addTab("msg.online", icon, error, new OnlineTab());

        // return our base
        return tabs_;
    }

    /**
     * Base class for tabs which use options
     */
    private abstract class OptionTab extends DDTabPanel
    {
        private ArrayList localOptions = new ArrayList();

        OptionTab()
        {
            setPreferredSize(new Dimension(700, 420));
        }

        protected void createUI()
        {
            createUILocal();
            processUI();
        }

        protected void processUI()
        {
            localOptions.clear();
            GuiUtils.getDDOptions(this, localOptions);
            TournamentProfileDialog.this.doMapLoad(localOptions, true);
            TournamentProfileDialog.this.addListeners(localOptions);
        }

        protected void cleanUI()
        {
            localOptions.clear();
            GuiUtils.getDDOptions(this, localOptions);
            TournamentProfileDialog.this.removeListeners(localOptions);
        }

        protected abstract void createUILocal();
    }

    /**
     * set map on each option to actual tournament map
     */
    private void doMapLoad(ArrayList options, boolean bResetToMap)
    {
        DDOption opt;
        for (int i = 0; i < options.size(); i++)
        {
            opt = ((DDOption) options.get(i));
            opt.setMap(profile_.getMap());
            if (bResetToMap) opt.resetToMap();
            else opt.saveToMap();
        }
    }

    /**
     * Description tab
     */
    private class DescTab extends OptionTab
    {
        /**
         * create UI at start since this is the initial panel
         */
        DescTab()
        {
            createUI();
        }

        protected void createUILocal()
        {
            setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, VerticalFlowLayout.LEFT));

            OptionText ot = new OptionText(null, "tournamentname", STYLE, dummy_, 30, "^.+$", 200, true);
            add(ot);
            name_ = ot.getTextField();
            name_.addPropertyChangeListener("value", TournamentProfileDialog.this);
            name_.setText(profile_.getName());

            add(new OptionTextArea(null, TournamentProfile.PARAM_DESC,
                                   STYLE, null, dummy_, 500, null, 12, 450));

            add(new OptionTextArea(null, TournamentProfile.PARAM_GREETING,
                                   STYLE, null, dummy_, 500, null, 5, 450));


            GuiUtils.setDDOptionLabelWidths(this);

            // after fixing widths,
            // don't add to option list so we don't reset/set map
            // we do this because profile name not stored in a
            // map (its a member of BaseProfile)
            ot.setIgnored(true);
        }

        /**
         * subclass can chime in on validity
         */
        protected boolean isValidCheck()
        {
            return name_.isValidData();
        }
    }

    /**
     * Levels tab
     */
    private class LevelsTab extends OptionTab
    {
        protected void createUILocal()
        {
            add(new LevelsPanel(this), BorderLayout.WEST);
        }

        public void reset()
        {
            cleanUI();
            removeAll();
            if (isSelectedTab()) createUI();
            repaint();
        }
    }

    /**
     * Online tab
     */
    private class OnlineTab extends OptionTab
    {
        protected void createUILocal()
        {
            DDPanel left = new DDPanel();
            left.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, VerticalFlowLayout.LEFT));
            add(left, BorderLayout.WEST);

            DDLabelBorder players = new DDLabelBorder("players", STYLE);
            players.setLayout(new GridLayout(0, 1, 0, -4));
            left.add(players);

            GlassButton invitees = new GlassButton("invitees", "Glass");
            invitees.setBorderGap(2, 4, 2, 4);
            invitees.setPreferredSize(new Dimension(75, 15));
            invitees.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    TypedHashMap params = new TypedHashMap();
                    params.setObject(PlayerListDialog.PARAM_PLAYER_LIST, profile_.getInvitees());
                    context_.processPhaseNow("InvitedPlayerList", params);
                }
            });

            OptionBoolean obs = new OptionBoolean(null, TournamentProfile.PARAM_INVITE_OBS, STYLE, dummy_, true);
            Dimension pref = obs.getPreferredSize();
            pref.height = 15;
            obs.setPreferredSize(pref);

            OptionDummy db = new OptionDummy(invitees, obs);
            db.setBorder(BorderFactory.createEmptyBorder(2, 5, 3, 0));
            db.setBorderLayoutGap(0, 10);

            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_INVITE_ONLY, STYLE, dummy_, true, db), players);
            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_ALLOW_DASH, STYLE, dummy_, true), players);
            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_ALLOW_ADVISOR, STYLE, dummy_, true), players);
            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_FILL_COMPUTER, STYLE, dummy_, true), players);
            OptionBoolean ob = OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_ALLOW_DEMO, STYLE, dummy_, true), players);
            // if running demo, don't let them shut out other demo users
            if (engine_.isDemo()) ob.setEnabled(false);
            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_ONLINE_ACTIVATED_ONLY, STYLE, dummy_, true), players);

            DDLabelBorder observers = new DDLabelBorder("observers", STYLE);
            left.add(observers);

            int nMin = (game_ != null && game_.isOnlineGame()) ? Math.max(game_.getNumObservers(), 0) : 0;
            OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_MAX_OBSERVERS, STYLE, dummy_, null, nMin, TournamentProfile.MAX_OBSERVERS, 50, true), observers);

            DDLabelBorder timeout = new DDLabelBorder("timeout", STYLE);
            left.add(timeout);

            DDPanel base = new DDPanel();
            base.setLayout(new GridLayout(0, 1, 0, 4));
            timeout.add(base, BorderLayout.CENTER);

            OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_TIMEOUT, STYLE, dummy_, null, TournamentProfile.MIN_TIMEOUT, TournamentProfile.MAX_TIMEOUT, 50, true), base);
            OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_THINKBANK, STYLE, dummy_, null, 0, TournamentProfile.MAX_THINKBANK, 50, true), base);

            ///
            /// boot disconnect/sitout
            ///

            DDLabelBorder bootbase = createBootControls(STYLE, dummy_, null);
            left.add(bootbase);
        }
    }

    /**
     * Boot controls labelborder.  Static for use from GameInfoDialog (case where profile is non-null, to allow
     * changing of profile in-game)
     */
    public static DDLabelBorder createBootControls(String STYLE, TypedHashMap dummy, TournamentProfile profile)
    {
        DDLabelBorder bootbase = new DDLabelBorder("boot", STYLE);
        bootbase.setLayout(new GridLayout(0, 1, 0, -4));

        Dimension size;
        OptionBoolean disconnect, sitout;

        // disconnect
        OptionInteger disconnectCount = new OptionInteger(null, TournamentProfile.PARAM_BOOT_DISCONNECT_COUNT,
                                                          STYLE, dummy, null,
                                                          TournamentProfile.MIN_BOOT_HANDS,
                                                          TournamentProfile.MAX_BOOT_HANDS, 60);
        disconnectCount.setEditable(true);
        disconnect = OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_BOOT_DISCONNECT, STYLE, dummy, true, disconnectCount), bootbase);
        if (profile != null)
        {
            disconnectCount.setMap(profile.getMap());
            disconnectCount.resetToMap();
            disconnect.setMap(profile.getMap());
            disconnect.resetToMap();
        }

        // sitout
        OptionInteger sitoutCount = new OptionInteger(null, TournamentProfile.PARAM_BOOT_SITOUT_COUNT,
                                                      STYLE, dummy, null,
                                                      TournamentProfile.MIN_BOOT_HANDS,
                                                      TournamentProfile.MAX_BOOT_HANDS, 60);
        sitoutCount.setEditable(true);
        sitout = OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_BOOT_SITOUT, STYLE, dummy, true, sitoutCount), bootbase);
        if (profile != null)
        {
            sitoutCount.setMap(profile.getMap());
            sitoutCount.resetToMap();
            sitout.setMap(profile.getMap());
            sitout.resetToMap();
        }

        // size to same
        size = disconnect.getCheckBox().getPreferredSize();
        size.width += 10;

        sitout.getCheckBox().setPreferredSize(size);
        disconnect.getCheckBox().setPreferredSize(size);
        return bootbase;
    }

    /**
     * money tab
     */
    private class DetailsTab extends OptionTab
    {
        protected void createUI()
        {
            super.createUI();

            // handle checkbox values
            checkBox(rebuys_);
            checkBox(addons_);

            // we are ready
            bDetailsTabReady_ = true;
            displayPrizePool();
            checkButtons();
        }

        protected void createUILocal()
        {
            DDPanel left = new DDPanel();
            left.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, VerticalFlowLayout.LEFT));
            add(left, BorderLayout.WEST);

            // num players / max at table
            DDPanel quantity = new DDPanel();
            left.add(quantity);
            quantity.setBorderLayoutGap(0, 10);

            // num players
            int nMax = (game_ != null && game_.isOnlineGame()) ? TournamentProfile.MAX_ONLINE_PLAYERS :
                       TournamentProfile.MAX_PLAYERS;
            int nMin = (game_ != null && game_.isOnlineGame()) ? Math.max(game_.getNumPlayers(), 2) : 2;
            OptionInteger oi = OptionMenu.add(new OptionInteger(null,
                                                                TournamentProfile.PARAM_NUM_PLAYERS,
                                                                STYLE, dummy_, null,
                                                                nMin,
                                                                nMax,
                                                                75, true),
                                              quantity, BorderLayout.WEST);
            oi.getSpinner().setUseBigStep(true);
            numPlayers_ = oi.getSpinner();
            if (engine_.isDemo() && !TESTING(EngineConstants.TESTING_DEMO)) numPlayers_.setEnabled(false);

            // max at table
            oi = OptionMenu.add(new OptionInteger(null,
                                                  TournamentProfile.PARAM_TABLE_SEATS,
                                                  STYLE, dummy_, null,
                                                  2,
                                                  PokerConstants.SEATS,
                                                  40, true),
                                quantity, BorderLayout.CENTER);

            // buyin
            DDLabelBorder buyin = new DDLabelBorder("buyin", STYLE);
            left.add(buyin);
            buyin.add(createBuyIn(), BorderLayout.CENTER);

            // rebuy
            DDLabelBorder rebuy = new DDLabelBorder("rebuys", STYLE);
            left.add(rebuy);
            rebuy.add(createRebuy(), BorderLayout.CENTER);

            // addon
            DDLabelBorder addon = new DDLabelBorder("addons", STYLE);
            left.add(addon);
            addon.add(createAddon(), BorderLayout.CENTER);

            // payout/alloc
            DDPanel payalloc = new DDPanel();
            payalloc.setBorderLayoutGap(10, 0);
            left.add(payalloc);

            // payout
            payout_ = new DDLabelBorder("payout", STYLE);
            payalloc.add(payout_, BorderLayout.NORTH);
            payout_.add(createPayout(), BorderLayout.CENTER);

            // adjust size based on payout
            Dimension ps = rebuy.getPreferredSize();
            payout_.setPreferredWidth(ps.width);
            buyin.setPreferredWidth(ps.width);
            rebuy.setPreferredWidth(ps.width);
            addon.setPreferredWidth(ps.width);

            // allocation
            DDPanel format = new DDPanel();
            format.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
            DDLabelBorder alloc = new DDLabelBorder("alloc", STYLE);
            add(format, BorderLayout.CENTER);
            format.add(alloc, BorderLayout.CENTER);
            alloc.add(createAlloc(), BorderLayout.CENTER);
        }

        /**
         * subclass can chime in on validity
         */
        protected boolean isValidCheck()
        {
            return isTotalCorrect(getTotal());
        }
    }

    /**
     * create levels
     */
    private class LevelsPanel extends DDPanel implements ListSelectionListener,
                                                         ActionListener
    {
        private GlassButton insertlevel;
        private GlassButton insertbreak;
        private GlassButton delete;
        private GlassButton verify;
        private ListPanel levelsList;
        private LevelsTab tab;

        public LevelsPanel(LevelsTab tab)
        {
            DDPanel base = this;
            this.tab = tab;
            base.setBorderLayoutGap(0, 20);

            // controls
            DDPanel controls = new DDPanel();
            controls.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 10, VerticalFlowLayout.LEFT));
            base.add(controls, BorderLayout.WEST);

            // game type
            OptionCombo combo = new OptionCombo(null, TournamentProfile.PARAM_GAMETYPE_DEFAULT,
                                                TournamentProfile.DATA_ELEMENT_GAMETYPE, STYLE, dummy_, 80, true);
            controls.add(combo);

            // minutes per level
            OptionInteger integer = new OptionInteger(null, TournamentProfile.PARAM_MINPERLEVEL_DEFAULT,
                                                      STYLE, dummy_, null,
                                                      1,
                                                      TournamentProfile.MAX_MINUTES, 50, true, true);
            controls.add(integer);

            // max raises
            integer = new OptionInteger(null, TournamentProfile.PARAM_MAXRAISES,
                                        STYLE, dummy_, null,
                                        1,
                                        TournamentProfile.MAX_MAX_RAISES, 35, true, true);
            controls.add(integer);

            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_MAXRAISES_NONE_HEADSUP,
                                             STYLE, dummy_, true), controls);

            GuiUtils.setDDOptionLabelWidths(controls);

            // levels
            DDPanel levels = new DDPanel();
            levels.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            levels.setBorderLayoutGap(5, 0);
            base.add(levels, BorderLayout.CENTER);

            // header
            LevelPanel llp;
            llp = new LevelPanel(null, new LevelListItem(TournamentProfileDialog.this, -1, false), STYLE);
            levels.add(llp, BorderLayout.NORTH);
            levels.setPreferredWidth(llp.getPreferredSize().width + 12);

            // panel with profile info
            levelsList = new ListPanel(LevelPanel.class, STYLE);
            levelsList.setFocusable(false);
            levelsList.addListSelectionListener(this);
            levels.add(levelsList, BorderLayout.CENTER);

            // add levels
            createLevels();

            // bottom - buttons/double checkbox
            DDPanel bottompanel = new DDPanel();
            bottompanel.setBorderLayoutGap(5, 0);
            levels.add(bottompanel, BorderLayout.SOUTH);

            // double at end
            DDPanel doublepanel = DDPanel.CENTER();
            bottompanel.add(doublepanel, BorderLayout.NORTH);
            OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_DOUBLE, STYLE,
                                             dummy_, true),
                           doublepanel, null);

            // control buttons
            DDPanel buttonpanel = new DDPanel();
            buttonpanel.setLayout(new GridLayout(1, 0, 5, 0));
            bottompanel.add(GuiUtils.CENTER(buttonpanel), BorderLayout.CENTER);

            insertlevel = new GlassButton("insertlevel", "Glass");
            insertlevel.addActionListener(this);
            buttonpanel.add(insertlevel);

            insertbreak = new GlassButton("insertbreak", "Glass");
            insertbreak.addActionListener(this);
            buttonpanel.add(insertbreak);

            delete = new GlassButton("deletelevel", "Glass");
            delete.addActionListener(this);
            buttonpanel.add(delete);

            verify = new GlassButton("verify", "Glass");
            verify.addActionListener(this);
            buttonpanel.add(verify);

            // init display
            levelsList.setSelectedIndex(0);
            checkButtons();
        }

        private void createLevels()
        {
            ArrayList levelitems = new ArrayList();
            for (int i = 0; i < profile_.getLastLevel(); i++)
            {
                levelitems.add(new LevelListItem(TournamentProfileDialog.this, i,
                                                 profile_.isBreak(i + 1)));
            }
            levelsList.setItems(levelitems);
        }

        public void valueChanged(ListSelectionEvent e)
        {
            checkButtons();
        }

        private void checkButtons()
        {
            LevelPanel selected = (LevelPanel) levelsList.getSelectedPanel();
            int nIndex = levelsList.getSelectedIndex();
            int size = levelsList.getItems().size();
            insertlevel.setEnabled(selected != null && (selected.canDelete() || nIndex == (size - 1)) && size < TournamentProfile.MAX_LEVELS);
            insertbreak.setEnabled(selected != null && (selected.canDelete() || nIndex == (size - 1)) && size < TournamentProfile.MAX_LEVELS);
            delete.setEnabled(selected != null && selected.canDelete() && size > 1);
        }

        public void actionPerformed(ActionEvent e)
        {
            Object button = e.getSource();
            if (button == insertlevel) insertLevel();
            else if (button == insertbreak) insertBreak();
            else if (button == delete) delete();
            else if (button == verify) verify();
        }

        private void insertLevel()
        {
            insertLevel(false);
        }

        private void insertBreak()
        {
            insertLevel(true);
        }

        private void insertLevel(boolean bBreak)
        {
            int index = levelsList.getSelectedIndex() + 1;

            LevelListItem item = new LevelListItem(TournamentProfileDialog.this,
                                                   // use index at end to avoid
                                                   // over-writing data.  Will
                                                   // get reset when update() called
                                                   levelsList.getItems().size(),
                                                   bBreak);
            levelsList.insertItem(index, item);
            LevelPanel lp = (LevelPanel) levelsList.getSelectedPanel();
            if (bBreak) lp.minutes.requestFocus();
            else lp.ante.requestFocus();

            if (bBreak) profile_.setBreak(item.index + 1, 15);
            // load from map for new break level to get the minutes
            // for regular level, we save blank values to map to
            // init
            ArrayList newOptions = new ArrayList();
            GuiUtils.getDDOptions(lp, newOptions);
            doMapLoad(newOptions, bBreak);
            TournamentProfileDialog.this.addListeners(newOptions);
        }

        private void delete()
        {
            // set dummy map so we won't get storage from focus lost
            LevelPanel panel = (LevelPanel) levelsList.getSelectedPanel();
            ArrayList options = new ArrayList();
            GuiUtils.getDDOptions(panel, options);
            DDOption dd;
            for (int i = 0; i < options.size(); i++)
            {
                dd = ((DDOption) options.get(i));
                dd.setMap(dummy_);
            }

            // remove listeners too
            TournamentProfileDialog.this.removeListeners(options);

            // remove item
            int index = levelsList.getSelectedIndex();
            levelsList.removeItem(index);

            // need to remove last item from map
            profile_.clearLevel(levelsList.getItems().size() + 1);
        }

        private void verify()
        {
            int nOldSelectedIndex = levelsList.getSelectedIndex();
            profile_.fixLevels();
            tab.cleanUI();
            createLevels();
            if (nOldSelectedIndex >= levelsList.getItems().size())
            {
                nOldSelectedIndex = levelsList.getItems().size() - 1;
            }
            levelsList.setSelectedIndex(nOldSelectedIndex);
            tab.processUI();
            repaint();
        }
    }

    /**
     * track level for use with ListPanel/ListItemPanel support
     */
    private class LevelListItem
    {
        public int index;
        public TournamentProfileDialog dialog;
        public boolean bBreak;

        public LevelListItem(TournamentProfileDialog dialog,
                             int level,
                             boolean bBreak)
        {
            this.dialog = dialog;
            this.index = level;
            this.bBreak = bBreak;
        }
    }

    /**
     * LevelPanel
     */
    public static class LevelPanel extends ListItemPanel
    {
        DDLabel label;
        DDTextField ante, small, big, minutes;
        OptionText otAnte, otSmall, otBig, otMinutes;
        OptionCombo ocGame;
        DDOption dd;
        boolean canDelete = true;

        public boolean canDelete()
        {
            return canDelete;
        }

        public LevelPanel(ListPanel panel, Object item, String sStyle)
        {
            super(panel, item, sStyle);

            LevelListItem levelitem = (LevelListItem) item;
            int i = levelitem.index;
            boolean bBreak = levelitem.bBreak;
            TournamentProfileDialog dialog = levelitem.dialog;

            int nNum = i + 1;

            // if in-game, need to set display-only those levels that have passed
            TypedHashMap map = dialog.profile_.getMap();
            boolean bDisplayOnly = false;
            if (nNum > 0 && dialog.game_ != null && dialog.game_.getLevel() > nNum &&
                !(map.getString(TournamentProfile.PARAM_ANTE + nNum, "").length() == 0 &&
                  map.getString(TournamentProfile.PARAM_SMALL + nNum, "").length() == 0 &&
                  map.getString(TournamentProfile.PARAM_BIG + nNum, "").length() == 0)
                    )
            {
                bDisplayOnly = true;
                canDelete = false;
            }

            // negative i means label
            if (i < 0)
            {
                sStyle = "OptionsDialogInfo";
                setDisplayOnly(true);
            }

            // layout
            setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.LEFT, 5, 0, HorizontalFlowLayout.CENTER));

            // label
            label = new DDLabel("level", sStyle);
            if (i >= 0)
            {
                label.setText(Integer.toString(nNum));
                label.setPreferredSize(new Dimension(20, 20));
            }
            else label.setPreferredSize(new Dimension(25, 12));
            add(label);

            // NOTE: we ignore ante/small/big for a break since
            // we don't care about those values - this has effect of not
            // setting profile_'s map to these values

            // ante
            otAnte = dialog.createText(TournamentProfile.PARAM_ANTE, sStyle, 6, 60, i, "msg.header.ante", bBreak ? ".*" : "^[0-9]*$");
            ante = otAnte.getTextField();
            ante.addFocusListener(dialog);
            add(otAnte);
            if (bDisplayOnly || bBreak)
            {
                otAnte.setDisplayOnly(true);
                ante.setBorder(dialog.empty_);
            }
            if (bBreak) ante.setText(PropertyConfig.getMessage("msg.break.edit"));
            if (bBreak || i < 0) otAnte.setIgnored(true);

            // small
            otSmall = dialog.createText(TournamentProfile.PARAM_SMALL, sStyle, 7, 82, i, "msg.header.small", "^[0-9]*$");
            small = otSmall.getTextField();
            small.addFocusListener(dialog);
            add(otSmall);
            if (bDisplayOnly || bBreak)
            {
                otSmall.setDisplayOnly(true);
                small.setBorder(dialog.empty_);
            }
            if (bBreak || i < 0) otSmall.setIgnored(true);

            // big
            otBig = dialog.createText(TournamentProfile.PARAM_BIG, sStyle, 7, 82, i, "msg.header.big", "^[0-9]*$");
            big = otBig.getTextField();
            big.addFocusListener(dialog);
            add(otBig);
            if (bDisplayOnly || bBreak)
            {
                otBig.setDisplayOnly(true);
                big.setBorder(dialog.empty_);
            }
            if (bBreak || i < 0) otBig.setIgnored(true);

            // time
            otMinutes = dialog.createText(TournamentProfile.PARAM_MINUTES, sStyle, 3, 60, i, "msg.header.time", "^[0-9]*$");
            minutes = otMinutes.getTextField();
            minutes.addFocusListener(dialog);
            add(otMinutes);
            if (bDisplayOnly)
            {
                otMinutes.setDisplayOnly(true);
                minutes.setBorder(dialog.empty_);
            }
            if (i < 0) otMinutes.setIgnored(true);

            // game type
            if (!bBreak)
            {
                dd = dialog.createCombo(TournamentProfile.PARAM_GAMETYPE, TournamentProfile.DATA_ELEMENT_GAMETYPE, sStyle, 80, i, "msg.header.game", bDisplayOnly);
                add(dd);
                if (i >= 0 && !bDisplayOnly)
                {
                    ocGame = (OptionCombo) dd;
                    ocGame.getComboBox().addFocusListener(dialog);
                    ocGame.getComboBox().setShowNotSelectedChoice(true);
                    ocGame.getComboBox().setRequired(false);
                }
                if (i < 0 || bDisplayOnly) dd.setIgnored(true);
            }
        }

        public void update()
        {
            int index = getIndex();
            LevelListItem levelitem = (LevelListItem) getItem();
            int oldlevel = levelitem.index + 1;
            levelitem.index = index;

            int level = index + 1;
            label.setText(Integer.toString(level));
            otAnte.setName(TournamentProfile.PARAM_ANTE + level);
            otSmall.setName(TournamentProfile.PARAM_SMALL + level);
            otBig.setName(TournamentProfile.PARAM_BIG + level);
            otMinutes.setName(TournamentProfile.PARAM_MINUTES + level);

            if (levelitem.bBreak)
            {
                int nMinutes = levelitem.dialog.profile_.getMinutes(oldlevel);
                //logger.debug("Update level " + level + " to " + nMinutes + " ("+oldlevel+")");
                // note that old minutes may be incorrect if deleting, due to
                // order of update() calls by the ListPanel.  However, in
                // those cases, it is fixed by saving the minutes to the map
                levelitem.dialog.profile_.setBreak(level, nMinutes);
                otMinutes.saveToMap(); // in case minutes defined in text field
            }
            else
            {
                otAnte.saveToMap();
                otSmall.saveToMap();
                otBig.saveToMap();
                otMinutes.saveToMap();
                //logger.debug("Update level " + level  + " ("+oldlevel+") big now: "+otBig.getTextField().getText());
            }
        }
    }

    /**
     * Buyin panel
     */
    private JComponent createBuyIn()
    {
        OptionInteger oi;

        // buy in
        DDPanel buyin = new DDPanel();
        buyin.setBorderLayoutGap(0, 5);

        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_BUYIN, STYLE, dummy_, null, 1, TournamentProfile.MAX_BUY, 70, true), buyin, BorderLayout.WEST);
        buyinCost_ = oi.getSpinner();
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_BUYINCHIPS, STYLE, dummy_, null, 1, TournamentProfile.MAX_CHIPS, 70, true), buyin, BorderLayout.CENTER);

        return buyin;
    }

    /**
     * Rebuy panel
     */
    private JComponent createRebuy()
    {
        OptionInteger oi;
        OptionBoolean ob;

        // rebuys
        DDPanel rebuy = new DDPanel();
        rebuy.setBorderLayoutGap(2, 0);

        ob = OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_REBUYS, STYLE, dummy_, false), rebuy, BorderLayout.NORTH);
        rebuys_ = ob.getCheckBox();
        rebuys_.addActionListener(this);

        DDPanel rebuydata = new DDPanel();
        rebuydata.setBorderLayoutGap(0, 5);
        rebuydata.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        rebuy.add(GuiUtils.WEST(rebuydata), BorderLayout.CENTER);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_REBUYCOST, STYLE, dummy_, null, 1, TournamentProfile.MAX_BUY, 70, true), rebuydata, BorderLayout.WEST);
        rebuyOptions_.add(oi);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_REBUYCHIPS, STYLE, dummy_, null, 1, TournamentProfile.MAX_CHIPS, 70, true), rebuydata, BorderLayout.CENTER);
        rebuyOptions_.add(oi);

        DDPanel rebuydata2 = new DDPanel();
        rebuydata2.setBorderLayoutGap(0, 5);
        rebuydata2.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        rebuy.add(GuiUtils.WEST(rebuydata2), BorderLayout.SOUTH);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_REBUY_UNTIL, STYLE, dummy_, null, 1, TournamentProfile.MAX_LEVELS, 45, true), rebuydata2, BorderLayout.WEST);
        rebuyOptions_.add(oi);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_MAXREBUYS, STYLE, dummy_, null, 0, TournamentProfile.MAX_REBUYS, 45, true), rebuydata2, BorderLayout.CENTER);
        rebuyOptions_.add(oi);

        DDPanel rebuydata3 = new DDPanel();
        rebuydata3.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rebuydata2.add(rebuydata3, BorderLayout.SOUTH);
        ButtonGroup exprgroup = new ButtonGroup();

        OptionRadio radio = OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_REBUYEXPR, STYLE, dummy_, "rebuyexpr.lte", exprgroup, PokerConstants.REBUY_LTE, null), rebuydata3);
        rebuyOptions_.add(radio);
        radio = OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_REBUYEXPR, STYLE, dummy_, "rebuyexpr.lt", exprgroup, PokerConstants.REBUY_LT, null), rebuydata3);
        rebuyOptions_.add(radio);

        Integer nDefault = profile_.getBuyinChips();
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_REBUYCHIPCNT, STYLE, dummy_, nDefault, 0, TournamentProfile.MAX_REBUY_CHIPS, 80, true), rebuydata3, BorderLayout.CENTER);
        rebuyOptions_.add(oi);

        return rebuy;
    }

    /**
     * addon panel
     */
    private JComponent createAddon()
    {
        OptionInteger oi;
        OptionBoolean ob;

        // addons
        DDPanel addon = new DDPanel();
        addon.setBorderLayoutGap(2, 0);

        ob = OptionMenu.add(new OptionBoolean(null, TournamentProfile.PARAM_ADDONS, STYLE, dummy_, false), addon, BorderLayout.NORTH);
        addons_ = ob.getCheckBox();
        addons_.addActionListener(this);

        DDPanel addondata = new DDPanel();
        addondata.setBorderLayoutGap(0, 5);
        addondata.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        addon.add(GuiUtils.WEST(addondata), BorderLayout.CENTER);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_ADDONCOST, STYLE, dummy_, null, 1, TournamentProfile.MAX_BUY, 70, true), addondata, BorderLayout.WEST);
        addonOptions_.add(oi);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_ADDONCHIPS, STYLE, dummy_, null, 1, TournamentProfile.MAX_CHIPS, 70, true), addondata, BorderLayout.CENTER);
        addonOptions_.add(oi);

        DDPanel addondata2 = new DDPanel();
        addondata2.setBorderLayoutGap(0, 5);
        addondata2.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        addon.add(GuiUtils.WEST(addondata2), BorderLayout.SOUTH);
        oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_ADDONLEVEL, STYLE, dummy_, null, 1, TournamentProfile.MAX_LEVELS, 45, true), addondata2, BorderLayout.WEST);
        addonOptions_.add(oi);

        return addon;
    }

    /**
     * Payout panel
     */
    private JComponent createPayout()
    {
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(0, 5);
        base.add(top, BorderLayout.NORTH);

        // payout type
        DDPanel typebase = new DDPanel();
        typebase.setLayout(new GridLayout(0, 1, 0, -4));
        top.add(typebase, BorderLayout.WEST);

        ButtonGroup payoutgroup = new ButtonGroup();

        OptionInteger payoutperc = new OptionInteger(null, TournamentProfile.PARAM_PAYOUTPERC, STYLE, dummy_, null, 1, TournamentProfile.MAX_PERC, 47, true);
        spotPerc_ = payoutperc.getSpinner();
        OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_PAYOUT, STYLE, dummy_, "payout.perc", payoutgroup, PokerConstants.PAYOUT_PERC, payoutperc), typebase);

        OptionInteger payoutnum = new OptionInteger(null, TournamentProfile.PARAM_PAYOUTNUM, STYLE, dummy_, null, 1, TournamentProfile.MAX_SPOTS, 47, true);
        spotAmount_ = payoutnum.getSpinner();
        OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_PAYOUT, STYLE, dummy_, "payout.num", payoutgroup, PokerConstants.PAYOUT_SPOTS, payoutnum), typebase);

        OptionRadio radio = OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_PAYOUT, STYLE, dummy_, "payout.sat", payoutgroup, PokerConstants.PAYOUT_SATELLITE), typebase);
        buttonSatellite_ = radio.getRadioButton();

        // sep
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setBackground(base_.getBackground().brighter());
        sep.setForeground(base_.getBackground().darker());
        top.add(sep, BorderLayout.CENTER);

        // house cut
        DDPanel housebase = new DDPanel();
        housebase.setLayout(new GridLayout(0, 1, 0, -4));
        top.add(GuiUtils.NORTH(housebase), BorderLayout.EAST);

        ButtonGroup housegroup = new ButtonGroup();

        OptionInteger houseperc = new OptionInteger(null, TournamentProfile.PARAM_HOUSEPERC, STYLE, dummy_, null, 0, TournamentProfile.MAX_HOUSE_PERC, 55, true);
        OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_HOUSE, STYLE, dummy_, "house.perc", housegroup, PokerConstants.HOUSE_PERC, houseperc), housebase);

        OptionInteger houseamount = new OptionInteger(null, TournamentProfile.PARAM_HOUSEAMOUNT, STYLE, dummy_, null, 0, TournamentProfile.MAX_HOUSE_AMOUNT, 55, true);
        houseAmount_ = houseamount.getSpinner();
        OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_HOUSE, STYLE, dummy_, "house.amount", housegroup, PokerConstants.HOUSE_AMOUNT, houseamount), housebase);

        return base;
    }

    /**
     * Allocation of payout
     */
    private JComponent createAlloc()
    {
        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(5, 20);
        base.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // left panel
        DDPanel left = new DDPanel();
        base.add(left, BorderLayout.WEST);

        // options
        OptionRadio radio;
        buttonGroup_ = new ButtonGroup();
        DDPanel allocbase = new DDPanel();
        allocbase.setLayout(new GridLayout(0, 1, 0, -4));
        left.add(allocbase, BorderLayout.NORTH);


        radio = OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_ALLOC, STYLE, dummy_, "alloc.auto", buttonGroup_, PokerConstants.ALLOC_AUTO, null), allocbase);
        buttonAuto_ = radio.getRadioButton();
        radio = OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_ALLOC, STYLE, dummy_, "alloc.perc", buttonGroup_, PokerConstants.ALLOC_PERC, null), allocbase);
        buttonPerc_ = radio.getRadioButton();
        radio = OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_ALLOC, STYLE, dummy_, "alloc.amount", buttonGroup_, PokerConstants.ALLOC_AMOUNT, null), allocbase);
        buttonAmount_ = radio.getRadioButton();

        // clear button
        clear_ = new GlassButton("clear", "Glass");
        left.add(GuiUtils.NORTH(GuiUtils.CENTER(clear_)), BorderLayout.CENTER);
        clear_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                clearSpots();
            }
        });

        // amount fields
        DDPanel center = new DDPanel();
        center.setPreferredSize(new Dimension(100, 100));
        base.add(center, BorderLayout.CENTER);

        spotsParent_ = new DDPanel();
        spotsParent_.setLayout(new GridLayout(0, 1, 0, 2));
        DDPanel format = new DDPanel();
        format.add(spotsParent_, BorderLayout.NORTH);

        // scroll
        spotscroll_ = new DDScrollPane(format, STYLE, null,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spotscroll_.setOpaque(false);
        GuiManager.addListeners(spotscroll_.getViewport());
        GuiManager.addListeners(spotscroll_);
        spotscroll_.getVerticalScrollBar().setUnitIncrement(22);
        spotscroll_.getVerticalScrollBar().setBlockIncrement(88);
        center.add(spotscroll_, BorderLayout.CENTER);

        // label
        DDPanel labelnum = new DDPanel();
        labelnum.setBorderLayoutGap(5, 0);
        center.add(labelnum, BorderLayout.NORTH);

        SpotPanel lsp;
        lsp = new SpotPanel(-1, STYLE);
        labelnum.add(lsp, BorderLayout.CENTER);

        // total
        total_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        base.add(total_, BorderLayout.SOUTH);
        total_.setHorizontalAlignment(SwingConstants.CENTER);

        return base;
    }

    /**
     * Clear spots
     */
    private void clearSpots()
    {
        // loop through all and calc total
        SpotPanel sp;
        for (int i = 0; i < TournamentProfile.MAX_SPOTS; i++)
        {
            sp = spots_[i];
            if (sp == null) continue;

            sp.spot.setText("");
        }
    }

    /**
     * Update paying spots
     */
    private void updateSpots()
    {
        // levels
        SpotPanel sp;
        int nNum = profile_.getNumSpots();
        if (buttonSatellite_.isSelected()) nNum = 1;
        DDRadioButton select = getSelectedButton();

        // if no change in # spots or alloction type, skip
        if (select == null || (nNum == nNumSpots_ && select == buttonSelected_)) return;

        // remember new values
        DDRadioButton old = buttonSelected_;
        nNumSpots_ = nNum;
        buttonSelected_ = select;

        boolean bAuto = buttonAuto_.isSelected();
        boolean bPerc = buttonPerc_.isSelected();

        spotsParent_.removeAll();
        for (int i = 0; i < TournamentProfile.MAX_SPOTS; i++)
        {
            sp = spots_[i];
            if (sp == null && i < nNumSpots_)
            {
                sp = new SpotPanel(i, STYLE);
                spots_[i] = sp;
            }

            if (sp == null) continue;

            if (i < nNumSpots_)
            {
                if (sp.spot.isDisplayOnly() != bAuto)
                {
                    sp.spot.setDisplayOnly(bAuto);
                    if (bAuto)
                        sp.spot.setBorder(empty_);
                    else
                        sp.spot.setBorder(DDTextField.TEXTBORDER);
                }

                if (!sp.spot.isEnabled()) sp.spot.setEnabled(true);

                if (bAuto) sp.spot.setRegExp("^.*$");
                else if (bPerc)
                    sp.spot.setRegExp("^([0-9\\,]*\\.?)\\%?|([0-9]*\\.([0-9][0-9][0-9]|[0-9][0-9]|[0-9]))\\%?$");
                else sp.spot.setRegExp(PokerConstants.REGEXP_DOLLAR_AMOUNT);
                spotsParent_.add(sp);
            }
            else
            {
                // set hidden items disabled for validation reasons
                if (sp.spot.isEnabled()) sp.spot.setEnabled(false);
            }
        }

        // update spots if alloc type changed
        if (old != buttonSelected_ && old != null)
        {
            setSpots(buttonSelected_, old);
        }

        spotscroll_.revalidate();
        spotscroll_.repaint();
    }

    /**
     * set spot fields
     */
    private void setSpots(DDRadioButton nu, DDRadioButton old)
    {
        // loop through all and calc total
        SpotPanel sp;
        String text;
        for (int i = 0; i < TournamentProfile.MAX_SPOTS; i++)
        {
            sp = spots_[i];
            if (sp == null) continue;

            text = sp.spot.getText();

            if (old == buttonAuto_ && (saveA_[i] == null || saveA_[i].length() == 0)) saveA_[i] = text;
            if (old == buttonAmount_) saveA_[i] = text;
            if (old == buttonPerc_) saveP_[i] = text;

            if (nu == buttonPerc_ && saveP_[i] != null && saveP_[i].length() > 0)
            {
                sp.spot.setText(saveP_[i]);
            }
            else if (nu == buttonAmount_ && saveA_[i] != null && saveA_[i].length() > 0)
            {
                sp.spot.setText(saveA_[i]);
            }
            else if (nu == buttonPerc_ || (nu == buttonAmount_ && old == buttonPerc_))
            {
                sp.spot.setText("");
            }
        }
    }

    /**
     * Set automatic spot percentages
     */
    private void setAutoSpots()
    {
        profile_.setAutoSpots();

        // loop through all and reset from new map value
        SpotPanel sp;
        for (int i = 0; i < TournamentProfile.MAX_SPOTS; i++)
        {
            sp = spots_[i];
            if (sp == null) continue;

            sp.otspot.resetToMap();
        }
    }

    /**
     * Get selected payout option (must be a way to use this using button group, but who cares)
     */
    private DDRadioButton getSelectedButton()
    {
        if (buttonAuto_.isSelected()) return buttonAuto_;
        if (buttonPerc_.isSelected()) return buttonPerc_;
        if (buttonAmount_.isSelected()) return buttonAmount_;
        return null;
    }

    /**
     * Update paying spots total label.
     */
    private void updateTotal()
    {
        double dTotal = getTotal();

        // update text
        if (buttonSatellite_.isSelected())
        {
            total_.setText(PropertyConfig.getMessage(isTotalCorrect(dTotal) ? "msg.total.sat.ok" : "msg.total.sat", dTotal));
        }
        else if (buttonPerc_.isSelected())
        {
            total_.setText(PropertyConfig.getMessage(isTotalCorrect(dTotal) ? "msg.total.perc.ok" : "msg.total.perc", fPerc.form(dTotal), fPerc.form(100.0d - dTotal)));
        }
        else
        {
            total_.setText(PropertyConfig.getMessage(isTotalCorrect(dTotal) ? "msg.total.amount.ok" : "msg.total.amount", dTotal, profile_.getPrizePool() - (int) dTotal));
        }
    }

    /**
     * Return if total prizes are correct
     */
    private boolean isTotalCorrect(double dTotal)
    {
        if (!bDetailsTabReady_) return true;

        double mustMatch;

        // get total validation
        if (buttonSatellite_.isSelected())
        {
            return dTotal > 0;
        }
        else if (buttonPerc_.isSelected())
        {
            mustMatch = 100.0d;
        }
        else
        {
            mustMatch = profile_.getPrizePool();
        }

        return dTotal == mustMatch;
    }

    /**
     * Return total
     */
    private double getTotal()
    {
        // loop through all and calc total
        SpotPanel sp;

        long dTotal = 0;
        long d;
        for (int i = 0; i < nNumSpots_; i++)
        {
            sp = spots_[i];
            if (sp == null) continue;

            try
            {
                String text = sp.spot.getText();
                StringBuilder sb = new StringBuilder();
                char c;
                for (int j = 0; j < text.length(); j++)
                {
                    c = text.charAt(j);
                    if (c >= '0' && c <= '9' || c == '-' || c == '.') sb.append(c);
                }
                text = sb.toString();
                d = (long) (Double.parseDouble(text) * TournamentProfile.ROUND_MULT);
            }
            catch (NumberFormatException nfe)
            {
                d = 0;
            }


            dTotal += d;
            //logger.debug(i + ": " + d + " = " + dTotal);
        }

        return ((double) dTotal) / (double) TournamentProfile.ROUND_MULT;
    }

    /**
     * SpotPanel
     */
    private class SpotPanel extends DDPanel
    {
        int nNum;
        DDLabel label;
        DDTextField spot;
        OptionText otspot;

        public SpotPanel(int i, String sStyle)
        {
            super(GuiManager.DEFAULT, sStyle);

            // negative i means label
            if (i < 0)
            {
                sStyle = "OptionsDialogInfo";
            }

            setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.LEFT, 5, 0, HorizontalFlowLayout.CENTER));
            nNum = i;

            // label
            label = new DDLabel("level", sStyle);
            if (i >= 0)
            {
                label.setText(Integer.toString(nNum + 1));
            }
            label.setPreferredSize(new Dimension(30, 20));
            add(label);

            // amount
            otspot = createText(TournamentProfile.PARAM_SPOTAMOUNT, sStyle, 11, 90, i, "msg.header.spotamount", "^.*$");
            spot = otspot.getTextField();
            spot.addFocusListener(TournamentProfileDialog.this);
            add(otspot);
            if (i < 0) otspot.setIgnored(true);

            // since these are added dynamically, make sure this dialog is
            // listened for changes and is initialized
            if (i >= 0 && bDetailsTabReady_)
            {
                checkChangeListener(otspot);
                otspot.setMap(profile_.getMap());
                otspot.resetToMap();
            }
        }
    }

    /**
     * monetary/numeric field
     */
    private OptionText createText(String sName, String sStyle, int nDigits,
                                  int nWidth, int i, String sLabel, String sRegExp)
    {
        OptionText ot;
        DDTextField text;

        if (i >= 0)
        {
            ot = new OptionText(null, sName, sStyle, dummy_, nDigits, sRegExp, nWidth, false);
            text = ot.getTextField();

            // change name used in map now that field has been created
            // and set default value.  Use display level name
            ot.setName(sName + (i + 1));
            ot.resetToDefault();
        }
        else
        {
            ot = new OptionText(null, sName, sStyle, labelignore_, 100, "^.*$", nWidth, false);
            text = ot.getTextField();
            text.setDisplayOnly(true);
            text.setText(PropertyConfig.getMessage(sLabel));
            text.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        return ot;
    }

    /**
     * combo field (returns OptionText for label, OptionCombo for normal)
     */
    private DDOption createCombo(String sName, String sDataElement, String sStyle,
                                 int nWidth, int i, String sLabel,
                                 boolean bDisplayOnly)
    {
        DDOption dd;
        DDTextField text;

        if (i >= 0 && !bDisplayOnly)
        {
            dd = new OptionCombo(null, sName, sDataElement, sStyle, dummy_, nWidth, false);

            // change name used in map now that field has been created
            // and set default value.  Use display level name
            dd.setName(sName + (i + 1));
            dd.resetToDefault();
        }
        else
        {
            OptionText ot;
            dd = ot = new OptionText(null, sName, sStyle, labelignore_, 100, "^.*$", nWidth, false);
            text = ot.getTextField();
            text.setDisplayOnly(true);
            if (i >= 0)
            {
                text.setText(profile_.getGameTypeDisplay(i + 1));
            }
            else
            {
                text.setText(PropertyConfig.getMessage(sLabel));
            }
            text.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        return dd;
    }

    /**
     * Focus to tabs
     */
    protected Component getFocusComponent()
    {
        return tabs_;
    }

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button)
    {
        setResult(Boolean.FALSE);
        return super.processButton(button);
    }

    /**
     * Okay button press
     */
    protected void okayButton()
    {
        oppmix_.processOkay();
        name_.removePropertyChangeListener(this);
        String sText = name_.getText();
        String sCurrent = profile_.getName();
        if (!sCurrent.equals(sText))
        {
            profile_.setName(sText);
        }

        setResult(Boolean.TRUE);

        // set dummy map to prevent updates upon exiting (due to lost focus)
        DMTypedHashMap dummy = new DMTypedHashMap();
        fillOptions();
        DDOption dd;
        for (int i = 0; i < options_.size(); i++)
        {
            dd = ((DDOption) options_.get(i));
            dd.setMap(dummy);
        }
    }

    /**
     * reset functionality - override to change map and reset to map
     */
    protected void resetButton()
    {
        profile_.getMap().clear();
        profile_.getMap().putAll(orig_);
        leveltab_.reset();

        fillOptions();
        DDOption dd;
        for (int i = 0; i < options_.size(); i++)
        {
            dd = ((DDOption) options_.get(i));
            dd.resetToMap();
        }
    }


    /**
     * Override to ignore non-Boolean results
     */
    public void setResult(Object o)
    {
        if (o instanceof Boolean)
        {
            super.setResult(o);
        }
    }

    /**
     * name text change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {

        if (evt.getSource() == name_)
        {
            super.getDialog().setTitle(PropertyConfig.getMessage(
                    "msg.windowtitle.tournamentProfileEdit.2",
                    name_.getText().trim()));
        }

        if (evt.getPropertyName().equals("value")) checkButtons();
    }

    /**
     * override to set prize pool amount
     */
    public void checkButtons()
    {
        if (bDetailsTabReady_ && !bUpdating_)
        {
            bUpdating_ = true;

            // if satellite payout, we just want an amount
            if (buttonSatellite_.isSelected())
            {
                buttonAmount_.setEnabled(true);
                if (!buttonAmount_.isSelected()) buttonAmount_.setSelected(true);
                buttonAuto_.setEnabled(false);
                buttonPerc_.setEnabled(false);
            }
            // no # spots to pay when rebuys on since total pool is not known
            else if (rebuys_.isSelected() || addons_.isSelected())
            {
                buttonAuto_.setEnabled(true);
                buttonPerc_.setEnabled(true);
                if (buttonAmount_.isSelected()) buttonAuto_.setSelected(true);
                buttonAmount_.setEnabled(false);
            }
            else
            {
                buttonAmount_.setEnabled(true);
                buttonAuto_.setEnabled(true);
                buttonPerc_.setEnabled(true);
            }
            updateMaxValues();
            displayPrizePool();
            updateSpots();
            if (buttonAuto_.isSelected())
            {
                setAutoSpots();
            }
            clear_.setEnabled(!buttonAuto_.isSelected());

            updateTotal();
            bUpdating_ = false;
        }

        // we do valid check automatically
        boolean bValid = tabs_.doValidCheck();
        if (defaultButton_ != null) defaultButton_.setEnabled(bValid);
    }

    /**
     * check boxes
     */
    public void actionPerformed(ActionEvent e)
    {
        checkBox(e.getSource());
    }

    /**
     * Check box handling
     */
    private void checkBox(Object o)
    {
        ArrayList list = null;
        boolean bOn = false;
        if (o == addons_)
        {
            list = addonOptions_;
            bOn = addons_.isSelected();
        }
        else if (o == rebuys_)
        {
            list = rebuyOptions_;
            bOn = rebuys_.isSelected();
        }

        if (list == null) return;

        DDOption ddOption;
        for (int i = 0; i < list.size(); i++)
        {
            ddOption = ((DDOption) list.get(i));
            ddOption.setEnabled(bOn);
            ddOption.saveToMap(); // need to save values to map when enable
        }
    }

    /**
     * Set prize pool text
     */
    private void displayPrizePool()
    {
        payout_.setText(PropertyConfig.getMessage("labelborder.payout.label", profile_.getPrizePool()));
        payout_.repaint();
    }

    /**
     * Update max value
     */
    private void updateMaxValues()
    {
        int nNumPlayers = numPlayers_.getValue();

        // check max spot amount
        int nMax = profile_.getMaxPayoutSpots(nNumPlayers);
        if (nMax == 0) nMax = 1;
        int nValue = spotAmount_.getValue();
        if (nValue > nMax) nValue = nMax;
        spotAmount_.setMax(nMax);
        spotAmount_.setValue(nValue);

        // max percentage is that which gets us closest to TournamentProfile.MAX_SPOTS
        nMax = profile_.getMaxPayoutPercent(nNumPlayers);
        if (nMax == 0) nMax = 1;
        nValue = spotPerc_.getValue();
        if (nValue > nMax) nValue = nMax;
        spotPerc_.setMax(nMax);
        spotPerc_.setValue(nValue);

        // max house take can't be more than 50% of buyin
        int nNewMax = buyinCost_.getValue() / 2;
        if (nNewMax > TournamentProfile.MAX_HOUSE_AMOUNT) nNewMax = TournamentProfile.MAX_HOUSE_AMOUNT;
        nValue = houseAmount_.getValue();
        nMax = houseAmount_.getMax();
        if (nMax > nNewMax)
        {
            if (nValue > nNewMax) nValue = nNewMax;
            houseAmount_.setValue(nValue);
        }
        houseAmount_.setMax(nNewMax);
    }

    /**
     * scroll text to visible
     */
    public void focusGained(FocusEvent e)
    {
        JComponent source = (JComponent) e.getSource();
        JScrollPane p = GuiUtils.getScrollParent(source);
        if (p != null)
        {
            Point loc = source.getLocation();
            loc = SwingUtilities.convertPoint(source.getParent(), loc, p.getViewport());
            p.getViewport().scrollRectToVisible(new Rectangle(loc, source.getSize()));
        }
    }

    /**
     * EMPTY
     */
    public void focusLost(FocusEvent e)
    {
    }
}
