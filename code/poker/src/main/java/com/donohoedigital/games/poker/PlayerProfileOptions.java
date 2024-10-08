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
 * PlayerProfileOptions.java
 *
 * Created on November 10, 2004, 10:50 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.text.*;

/**
 *
 * @author  Doug Donohoe
 */
public class PlayerProfileOptions extends BasePhase implements ChangeListener
{
    static Logger logger = LogManager.getLogger(PlayerProfileOptions.class);
    
    public static final String PROFILE_NAME = "player";
    
    private DDHtmlArea text_;
    private MenuBackground menu_;
    private DDLabelBorder statsBorder_;
    private DDLabelBorder statusBorder_;
    private DDLabel statusLabel_;
    private JScrollPane scroll_;
    private DDPanel statsBase_;
    private PlayerProfile selected_ = null;
    private ProfileList profileList_;
    private DDLabel total_;
    private GlassButton dall_;

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);
        
        // name of style used for all widgets in data area
        String STYLE = gamephase_.getString("style", "default");
        
        // Create base panel which holds everything
        menu_ = new MenuBackground(gamephase);
        DDPanel menubox = menu_.getMenuBox();
        
        // put buttons in the menubox_
        ButtonBox buttonbox = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox.add(buttonbox, BorderLayout.SOUTH);
        
        // holds data we are gathering
        DDPanel data = new DDPanel("newtournament");
        BorderLayout layout = (BorderLayout) data.getLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        data.setBorder(BorderFactory.createEmptyBorder(2,10,5,10));
        menubox.add(data, BorderLayout.CENTER);

        // player list & stats
        DDPanel top = new DDPanel("newtournament");
        layout = (BorderLayout) top.getLayout();
        layout.setVgap(10);
        layout.setHgap(10);
        data.add(top, BorderLayout.NORTH);
        
        // get current profile list and sort it
        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        Collections.sort(profiles);
        
        // player list
        DDLabelBorder pborder = new DDLabelBorder("players", STYLE);
        pborder.setPreferredSize(new Dimension(200, 400));
        top.add(pborder, BorderLayout.WEST);
        profileList_ = new PlayerProfileList(engine_, context, profiles, STYLE, PROFILE_NAME, "newtournament", "pokericon16png", false);
        profileList_.addChangeListener(this);
        pborder.add(profileList_, BorderLayout.CENTER);
        
        // help text
        text_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        text_.setDisplayOnly(true);
        text_.setBorder(EngineUtils.getStandardMenuLowerTextBorder());
        data.add(text_, BorderLayout.CENTER);

        // online status
        DDPanel statuspanel = new DDPanel();
        statuspanel.setBorderLayoutGap(10, 0);
        top.add(statuspanel, BorderLayout.CENTER);

        statusBorder_ = new DDLabelBorder("onlinestatus", STYLE);
        layout = (BorderLayout) statusBorder_.getLayout();
        layout.setVgap(10);
        statuspanel.add(statusBorder_, BorderLayout.NORTH);

        statusLabel_ = new DDLabel("onlinestatus", "PokerStandardSmall");
        statusLabel_.setBorder(BorderFactory.createEmptyBorder(2,3,2,6));
        statusBorder_.add(statusLabel_, BorderLayout.WEST);

        // stats
        statsBorder_ = new DDLabelBorder("stats", STYLE);
        layout = (BorderLayout) statsBorder_.getLayout();
        layout.setVgap(10);
        statsBorder_.setPreferredSize(new Dimension(500, 370));
        statuspanel.add(statsBorder_, BorderLayout.CENTER);
        
        statsBase_ = new DDPanel();
        statsBase_.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 10, VerticalFlowLayout.LEFT));
        statsBase_.setBorder(BorderFactory.createEmptyBorder(2,3,2,6));

        scroll_ = new DDScrollPane(GuiUtils.NORTH(statsBase_), STYLE, null,
                                   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setPreferredSize(new Dimension(500, 300));
        scroll_.setOpaque(false);
        scroll_.getVerticalScrollBar().setUnitIncrement(60);
        scroll_.getVerticalScrollBar().setBlockIncrement(180);
        statsBorder_.add(scroll_, BorderLayout.NORTH);
        
        DDPanel bottom = new DDPanel();
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        layout = (BorderLayout) bottom.getLayout();
        layout.setVgap(5);
        statsBorder_.add(bottom, BorderLayout.CENTER);
        dall_ = new DeleteAllButton();
        bottom.add(GuiUtils.EAST(GuiUtils.CENTER(dall_)), BorderLayout.NORTH);
        
        total_ = new DDLabel(GuiManager.DEFAULT, "ProfileDetails");
        total_.setText("");
        bottom.add(total_, BorderLayout.CENTER);
        
        // select 1st row
        profileList_.selectInit();
    }
    
    /**
     * Create list editor for non-ui use (like from PokerStartMenu)
     */
    public static ProfileList getPlayerProfileList(GameEngine engine, GameContext context)
    {
        return new PlayerProfileList(engine, context, PROFILE_NAME);
    }
    
    /**
     * Our list editor
     */
    private static class PlayerProfileList extends ProfileList
    {
        private PlayerProfileList(GameEngine engine, GameContext context, String sMsgName)
        {
            super(engine, context, sMsgName);
        }
        
        private PlayerProfileList(GameEngine engine, GameContext context, List<BaseProfile> profiles,
                                  String sStyle,
                                  String sMsgName,
                                  String sPanelName,
                                  String sIconName,
                                  boolean bUseCopyButton)
        {
            super(engine, context, profiles, sStyle, sMsgName, sPanelName, sIconName, bUseCopyButton);
        }
        
        /** Create empty profile
         *
         */
        @Override
        protected BaseProfile createEmptyProfile() {
            PlayerProfile profile = new PlayerProfile("");
            profile.initCheck();
            return profile;
        }
        
        /** Return copy of given profile
         *
         */
        @Override
        protected BaseProfile copyProfile(BaseProfile profile, boolean bForEdit) {
            PlayerProfile pp = (PlayerProfile) profile;
            String sName = (bForEdit) ? pp.getName() : PropertyConfig.getMessage("msg.copy", pp.getName());
            return new PlayerProfile(pp, sName);
        }
        
        /**
         * store default profile
         */
        @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
        @Override
        public void rememberProfile(BaseProfile profile)
        {
            super.rememberProfile(profile);
            default_ = (PlayerProfile) profile;
            PokerDatabase.init(default_);
        }

        @Override
        protected boolean deleteProfile(BaseProfile profile)
        {
            // If online profile, then delete on the server.
            PlayerProfile playerProfile = (PlayerProfile) profile;
            
            // Delete on the client.
            PokerDatabase.delete(default_);

            File advisorFile = PlayerType.getAdvisorFile(playerProfile);

            if (advisorFile.exists())
            {
                advisorFile.delete();
            }

            return true;
        }
    }
    
    /**
     * Cache default profile
     */
    private static PlayerProfile default_ = null;
    
    /**
     * Return stored profile based on preference maintained by PlayerProfileList
     */
    public static PlayerProfile getDefaultProfile()
    {
        if (default_ == null)
        {
            String sName = ProfileList.getStoredProfile(PROFILE_NAME);
            String sCmdlineOverride = PokerMain.getPokerMain().getProfileOverride();

            // if we have a profile, look it up
            if (sName != null)
            {
                File file = PlayerProfile.getProfileFile(sName);
                if (file.exists()) 
                {    
                    default_ = new PlayerProfile(file,  true);
                }
                else // file doesn't exist, so forget it
                {
                    ProfileList.setStoredProfile(null, PROFILE_NAME);
                }
            }
            
            // still null (nothing in prefs), see if any files exist
            // and choose most recent modification and remember it
            if (default_ == null || sCmdlineOverride != null)
            {
                List<BaseProfile> list = PlayerProfile.getProfileList();
                PlayerProfile p = null;
                PlayerProfile choose = null;
                long lastmod = 0;
                for (int i = 0; list != null && i < list.size(); i++)
                {
                    p = (PlayerProfile) list.get(i);

                    if (sCmdlineOverride != null && p.getName().equalsIgnoreCase(sCmdlineOverride))
                    {
                        logger.debug("Using profile "+sCmdlineOverride+" instead of default "+(
                                     default_ == null ? "[null]" : default_.getName()));
                        choose = p;
                        break;
                    }

                    if (p.getLastModified() > lastmod)
                    {
                        choose = p;
                        lastmod = choose.getLastModified();
                    }
                }
                
                // if found one, remember it
                if (choose != null)
                {                    
                    if (sCmdlineOverride == null) ProfileList.setStoredProfile(choose, PROFILE_NAME);
                    default_ = choose;
                }
            }
            
            PokerDatabase.init(default_);
        }

        return default_;
    }
    
    /**
     * Start of phase
     */
    @Override
    public void start()
    {
        // set help text
        context_.getWindow().setHelpTextWidget(text_);
        context_.getWindow().showHelp(menu_.getMenuBox()); // init help

        // place the whole thing in the Engine's base panel
        context_.setMainUIComponent(this, menu_, false, profileList_);
        
        // check button states and focus
        checkButtons();
    }

    /**
     * Returns true
     */
    @Override
    public boolean processButton(GameButton button)
    {
        return true;
    }
    
    /**
     * set buttons enabled/disabled based on selection
     */
    private void checkButtons()
    {
        // JDD - with re-organization of player profile
        // stuff, no longer need to enforce that something
        // is selected when Done button clicked.  The
        // PokerStartMenu now enforces that a profile exists
        //boolean bSelected = (selected_ != null);
        //start_.setEnabled(bSelected);
    }
    
    /**
     * profile selected logic
     */
    public void stateChanged(ChangeEvent e)
    {
        PlayerProfile pp = (PlayerProfile) profileList_.getSelectedProfile();
        
        if (pp != null)
        {
            // set current selected profile and update stats label
            selected_ = pp;
            String statusMsg = (pp.isActivated() ? "msg.onlinestatus.enabled" : "msg.onlinestatus.disabled");
            statusLabel_.setText(PropertyConfig.getMessage(statusMsg));
            statusBorder_.setText(PropertyConfig.getMessage("labelborder.onlinestatus.label2", selected_.getName()));
            statusBorder_.repaint();
            setStats(selected_);
            statsBorder_.setText(PropertyConfig.getMessage("labelborder.stats.label2", selected_.getName()));
            statsBorder_.repaint();
        }
        else
        {
            selected_ = null;
            statusLabel_.setText(PropertyConfig.getMessage("label.onlinestatus.label"));
            statusBorder_.setText(PropertyConfig.getMessage("labelborder.onlinestatus.label"));
            statusBorder_.repaint();
            setStats(null);
            statsBorder_.setText(PropertyConfig.getMessage("labelborder.stats.label"));
            statsBorder_.repaint();
        }

        // set buttons
        checkButtons();
    }

    /**
     * stats
     */
    private void setStats(PlayerProfile selected)
    {
        //selected.debugPrint();
        statsBase_.removeAll();
        
        int nTotalPrize = 0;
        int nTotalSpent = 0;
        
        if (selected != null)
        {
            List<TournamentHistory> history = selected.getHistory();
            TournamentHistory hist;
            DDLabel label;
            DDPanel panel;
            DeleteButton button;
            if (history.isEmpty())
            {
                // label                
                label = new DDLabel(GuiManager.DEFAULT, "PlayerDetails");
                label.setText(PropertyConfig.getMessage("msg.hist.none"));
                statsBase_.add(label);
                dall_.setEnabled(false);
            }
            else
            {
                dall_.setEnabled(true);
                for (int i = 0; i < history.size(); ++i)
                {
                    hist = history.get(i);
                    
                    // panel
                    panel = new DDPanel();
                    statsBase_.add(panel);
                    
                    // label
                    label = new DDLabel(GuiManager.DEFAULT, "PlayerDetails");
                    label.setVerticalAlignment(SwingConstants.TOP);
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    label.setText(toHTML(hist));
                    panel.add(label, BorderLayout.WEST);
                    
                    // delete
                    button = new DeleteButton(i);
                    panel.add(GuiUtils.CENTER(button), BorderLayout.EAST);
                    panel.setPreferredWidth(480);

                    if (hist.getPlace() > 0)
                    {
                        nTotalPrize += hist.getPrize();
                        nTotalSpent += hist.getTotalSpent();
                    }
                }
            }
        }
        
        total_.setText(PropertyConfig.getMessage("msg.hist.total",
                                                 nTotalSpent,
                                                 nTotalPrize,
                                                 nTotalPrize - nTotalSpent));
        
        scroll_.revalidate();
    }

    private String toHTML(TournamentHistory hist)
    {
        GameEngine engine = GameEngine.getGameEngine();
        SimpleDateFormat formatter = PropertyConfig.getDateFormat((engine != null) ? engine.getLocale() : null);

        // NOTE: place is 0 on the cilent until a player finishes.        
        int place = hist.getPlace();
        return PropertyConfig.getMessage(place != 0 ? "msg.hist.info" : "msg.hist.unfinished",
                                         hist.getTournamentName(),
                                         PropertyConfig.getPlace(place),
                                         hist.getNumPlayers(),
                                         hist.getTotalSpent(),
                                         hist.getPrize(),
                                         formatter.format(hist.getEndDate())
        );
    }
    
    private class DeleteButton extends GlassButton implements ActionListener
    {
        int nIndex_;

        private DeleteButton(int n)
        {
            super("deletestats", "Glass");
            nIndex_ = n;
            addActionListener(this);
        }
        
        /** 
         * Delete button
         */
        public void actionPerformed(ActionEvent e) 
        {
            if (deleteHistory(context_, selected_, nIndex_))
            {
                setStats(selected_);
            }
        }
    }
    
    private class DeleteAllButton extends GlassButton implements ActionListener
    {
        private DeleteAllButton()
        {
            super("deleteall", "Glass");
            addActionListener(this);
        }
        
        /** 
         * Delete button
         */
        public void actionPerformed(ActionEvent e) 
        {
            if (deleteAllHistory(context_, selected_))
            {
                setStats(selected_);
            }
        }
    }

    public static boolean deleteHistory(GameContext context, PlayerProfile profile, int nIndex)
    {
        {
            TournamentHistory hist_ = profile.getHistory().get(nIndex);
            String date = hist_.getPlace() > 0 ?
                          PropertyConfig.getDateFormat(GameEngine.getGameEngine().getLocale()).format(hist_.getEndDate()) :
                          PropertyConfig.getMessage("msg.hist.unfinished2");
            if (EngineUtils.displayConfirmationDialog(context,
                    PropertyConfig.getMessage("msg.confirm.deletehist",
                                              Utils.encodeHTML(hist_.getTournamentName()),
                                              date,
                                              Utils.encodeHTML(profile.getName()))))
            {
                PokerDatabase.deleteTournament(profile.getHistory().remove(nIndex));
                return true;
            }
            return false;
        }
    }

    public static boolean deleteAllHistory(GameContext context, PlayerProfile profile)
    {
        if (EngineUtils.displayConfirmationDialog(context,
                PropertyConfig.getMessage("msg.confirm.deletehistall", Utils.encodeHTML(profile.getName()))))
        {
            PokerDatabase.deleteAllTournaments(profile);
            return true;
        }
        return false;
    }
}
