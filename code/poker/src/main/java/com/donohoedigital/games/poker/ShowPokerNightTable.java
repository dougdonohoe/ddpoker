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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.config.StylesConfig;
import com.donohoedigital.games.config.GamePhase;
import com.donohoedigital.games.engine.EngineUtils;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.engine.Phase;
import com.donohoedigital.games.engine.ProfileList;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ShowPokerNightTable extends ShowPokerTable implements PropertyChangeListener, GameClockListener, SwingConstants
{
    private PokerButton buttonStartPause_;
    private PokerButton buttonRewind_;
    private PokerButton buttonForward_;
    private PokerImageButton buttonPrizes_;
    private PokerImageButton buttonEdit_;
    private PokerImageButton buttonCalc_;

    private DDLabel labelPaused_;
    private DDLabel labelDetails_;
    private DisplayLabel labelTime_;
    private DisplayLabel labelSblind_;
    private DisplayLabel labelBblind_;
    private DisplayLabel labelLevel_;
    private DisplayLabel labelAnte_;
    private DDLabel labelLevelX_;
    private DDLabel labelSblindX;
    private DDLabel labelBblindX;
    private DDLabel labelAnteX_;

    private DDText labelNextBlinds_;

    /**
     * different layout for poker clock
     */
    @Override
    protected void createPanels()
    {
        // panels
        DDPanel center = createCenterPanel();
        DDPanel buttons = createButtonPanel(true);
        ImageComponent banner = new ImageComponent(engine_.isDemo() ? "pokermenu-demo" : "pokermenu", 1.0d);

        // HACK - change default size of map so it better occupies
        // the space below the banner/buttons, which is a different
        // aspect ratio then the default board.  The engine doesn't
        // support multiple game definitions and this works and
        // is much quicker than adding that support
        Dimension size = board_.getDefaultMapSize();
        size.setSize(800, 435);

        // top
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(-40, 0);

        top.add(GuiUtils.CENTER(buttons), BorderLayout.CENTER);
        top.add(GuiUtils.CENTER(banner), BorderLayout.NORTH);

        base_.setLayout(new BorderLayout());
        base_.add(top, BorderLayout.NORTH);
        //base_.add(GuiUtils.NORTH(buttons), BorderLayout.WEST);
        base_.add(center, BorderLayout.CENTER);
    }

    @Override
    public void subclassInit(GameEngine engine, GamePhase gamephase)
    {
        // we just draw on bg pattern, don't use table
        board_.setUseImage(false);

        // NOTE: x,y is based on layout of 1200x900 (which is arbitrary now)
        //       PREF W, PREF H, defines the preferred aspect ratio
        //       W defines the width as a fraction of overall width

        String DF = GuiManager.DEFAULT;
        float HD = 2.9f; // header font scaling factor
        float PF = 2.5f; // pill label font scaling factor
        float NF = 6.9f; // amount font scaling factor (level/ante)
        float NF2 = 7.8f; // amount font scaling factor (big/small blind)
        float NF3 = 2.0f; // details/next scaling factor
        float W = 1200f;
        float H = 900f;

        // labels                          PREF W,  PREF H,     X           Y        W

        labelName_ = new DisplayPill(800 / HD, 50 / HD, 200f / W, 0, 800f / W, CENTER, TOP, DF);
        labelName_.setForeground(StylesConfig.getColor("clock.fg")); // top label same color as clock
        labelTime_ = new DisplayLabel(460 / 12f, 143.5f / 12f, 200f / W, 72 / H, 800f / W, RIGHT, TOP, DF, "PokerClock");

        labelLevelX_ = new DisplayPill(200 / PF, 40 / PF, 55f / W, 400 / H, 200f / W, CENTER, TOP, "levelx");
        labelLevel_ = new DisplayLabel(200 / NF, 87 / NF, 55f / W, 455 / H, 200f / W, CENTER, TOP, DF, "PokerClockBlind");
        labelAnteX_ = new DisplayPill(500 / PF, 40 / PF, 645f / W, 400 / H, 500f / W, CENTER, TOP, "ante");
        labelAnte_ = new DisplayLabel(500 / NF, 87 / NF, 645f / W, 455 / H, 500f / W, CENTER, TOP, DF, "PokerClockBlind");

        labelSblindX = new DisplayPill(500 / PF, 40 / PF, 55f / W, 580 / H, 500f / W, CENTER, TOP, "sblind");
        labelSblind_ = new DisplayLabel(500 / NF2, 105 / NF2, 55f / W, 635 / H, 500f / W, CENTER, TOP, DF, "PokerClockBlind");
        labelBblindX = new DisplayPill(500 / PF, 40 / PF, 645f / W, 580 / H, 500f / W, CENTER, TOP, "bblind");
        labelBblind_ = new DisplayLabel(500 / NF2, 105 / NF2, 645f / W, 635 / H, 500f / W, CENTER, TOP, DF, "PokerClockBlind");

        // game buttons                                             PF, PH, x,             y,               w
        buttonRewind_ = new PokerButton(getGameButton("rewind"), 33, 33, 15.5f / 42.0f, 810.0f / H, 1.5f / 42.0f, true);
        buttonStartPause_ = new PokerButton(getGameButton("start"), 60, 20, 18.0f / 42.0f, 800.0f / H, 6f / 42.0f, true);
        buttonForward_ = new PokerButton(getGameButton("forward"), 33, 33, 25.0f / 42.0f, 810.0f / H, 1.5f / 42.0f, true);

        // info
        labelPaused_ = new DisplayLabel(600 / 7.0f, 90 / 5.0f, 150f / W, 435 / H, 600f / W, CENTER, TOP, DF, "PokerClock");
        labelNextBlinds_ = new DisplayLabel(480 / NF3, 120 / NF3, 5f / W, 790 / H, 480f / W, LEFT, TOP);
        labelDetails_ = new DisplayLabel(430 / NF3, 120 / NF3, 790 / W, 790 / H, 430f / W, LEFT, TOP);

        // listeners/borders
        buttonRewind_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                rewind();
            }
        });
        buttonRewind_.setBorderGap(0, 0, 0, 0);

        buttonStartPause_.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        game_.getGameClock().toggle();
                    }
                });

        buttonForward_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                forward();
            }
        });
        buttonForward_.setBorderGap(0, 0, 0, 0);

        buttonEdit_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                edit();
            }
        });

        // customizations
        customizeLabel(labelTime_, 10);
        customizeLabel(labelAnte_, 0);
        customizeLabel(labelBblind_, 0);
        customizeLabel(labelSblind_, 0);
        customizeLabel(labelLevel_, 0);

        // display init
        PokerUtils.updateTime(game_, labelTime_);
        updateLevel();
        checkButtons();
    }

    private void customizeLabel(DisplayLabel label, int nRight)
    {
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                new DDBevelBorder("BrushedMetal", DDBevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(0, 0, 0, nRight)));
        label.setIgnoreInsets(new Insets(2, 2, 2, 2));
    }

    @Override
    protected DDPanel createButtonPanel(boolean bHorizontal)
    {
        // create buttons
        buttonQuit_ = new PokerImageButton(this, getGameButton("quit"));
        buttonSave_ = new PokerImageButton(this, getGameButton("save"));
        buttonInfo_ = new PokerImageButton(this, getGameButton("info"));
        buttonOptions_ = new PokerImageButton(this, getGameButton("options"));
        buttonHelp_ = new PokerImageButton(this, getGameButton("help"));
        buttonEdit_ = new PokerImageButton(this, getGameButton("edit"));
        buttonPrizes_ = new PokerImageButton(this, getGameButton("prizepool"));
        buttonCalc_ = new PokerImageButton(this, getGameButton("calc"));

        // button layout - in north west corner of right panel
        DDPanel buttonpanel = new DDPanel();
        if (bHorizontal)
        {
            buttonpanel.setLayout(new GridLayout(1, 8, 0, 0));
            buttonpanel.setPreferredSize(new Dimension(buttonQuit_.getWidth() * 8, buttonQuit_.getHeight()));
        }
        else
        {
            buttonpanel.setLayout(new GridLayout(5, 1, 0, 0));
        }

        // add buttons
        buttonpanel.add(buttonQuit_);
        buttonpanel.add(buttonSave_);
        buttonpanel.add(buttonInfo_);
        buttonpanel.add(buttonOptions_);
        buttonpanel.add(buttonEdit_);
        buttonpanel.add(buttonPrizes_);
        buttonpanel.add(buttonCalc_);
        buttonpanel.add(buttonHelp_);

        return buttonpanel;
    }

    /**
     * Edit button
     */
    private void edit()
    {
        GameClock clock = game_.getGameClock();
        boolean pause = clock.isRunning();

        if (pause)
        {
            clock.stop();
        }

        TournamentProfile old = game_.getProfile();
        int secondsRemaining = clock.getSecondsRemaining();
        boolean bUpdateSeconds = game_.getSecondsInLevel(game_.getLevel()) == secondsRemaining;

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

            int newSecondsInLevel = game_.getSecondsInLevel(game_.getLevel());
            if (bUpdateSeconds || secondsRemaining > newSecondsInLevel)
            {

                clock.setSecondsRemaining(newSecondsInLevel);
            }
        }

        if (pause)
        {
            clock.start();
        }
    }

    /**
     * Rewind clock
     */
    private void rewind()
    {
        synchronized (game_.getGameClock())
        {
            int secondsInLevel = game_.getSecondsInLevel(game_.getLevel());
            boolean bAtStart = secondsInLevel == game_.getGameClock().getSecondsRemaining();

            if (EngineUtils.displayConfirmationDialog(context_, PropertyConfig.getMessage(bAtStart ?
                                                                                          "msg.confirm.rewind" : "msg.confirm.rewind.c"), "rewindforward"))
            {
                if (bAtStart)
                {
                    game_.prevLevel();
                }
                else
                {
                    game_.getGameClock().setSecondsRemaining(secondsInLevel);

                }
                checkButtons();
            }
        }
    }

    /**
     * advance clock
     */
    private void forward()
    {
        synchronized (game_.getGameClock())
        {
            if (EngineUtils.displayConfirmationDialog(context_, PropertyConfig.getMessage("msg.confirm.forward"), "rewindforward"))
            {
                game_.nextLevel();
            }
        }
    }

    /**
     * enable/disable buttons as appropriate
     */
    private void checkButtons()
    {
        boolean rewind = (game_.getLevel() > 1 ||
                          (game_.getLevel() == 1 && game_.getGameClock().getSecondsRemaining() !=
                                                    game_.getSecondsInLevel(1)));

        if (rewind != buttonRewind_.isEnabled())
        {
            buttonRewind_.setEnabled(rewind);
        }

        boolean forward = !engine_.isDemo() || (game_.getLevel() < PokerNight.DEMO_MAX);

        if (forward != buttonForward_.isEnabled())
        {
            buttonForward_.setEnabled(forward);
        }

        boolean startpause = !engine_.isDemo() || (game_.getLevel() <= PokerNight.DEMO_MAX);

        if (startpause != buttonStartPause_.isEnabled())
        {
            buttonStartPause_.setEnabled(startpause);
        }
    }

    /**
     * GameClockListener implementation.
     * <p/>
     * Does nothing, subclasses may override.
     *
     * @param clock
     */
    public void gameClockTicked(GameClock clock)
    {
        // enable rewind on tick of clock
        if (game_.getLevel() == 1 && !buttonRewind_.isEnabled()) checkButtons();
        PokerUtils.updateTime(game_, labelTime_);
    }

    /**
     * GameClockListener implementation.
     * <p/>
     * Does nothing, subclasses may override.
     *
     * @param clock
     */
    public void gameClockSet(GameClock clock)
    {
        PokerUtils.updateTime(game_, labelTime_);
    }


    /**
     * GameClockListener implementation.
     * <p/>
     * Clears "*** PAUSED ***" label, changes button text to "Start".
     *
     * @param clock
     */
    public void gameClockStarted(GameClock clock)
    {
        buttonStartPause_.rename("pause");
        labelPaused_.setText("");
    }

    /**
     * GameClockListener implementation.
     * <p/>
     * Sets "*** PAUSED ***" label, changes button text to "Pause".
     *
     * @param clock
     */
    public void gameClockStopped(GameClock clock)
    {
        buttonStartPause_.rename("start");
        labelPaused_.setText(PropertyConfig.getMessage("msg.paused"));
        checkButtons();
    }

    /**
     * Ignore repetitive keystrokes
     */
    @Override
    protected boolean filterKeyStrokeDuplicates()
    {
        return true;
    }

    /**
     * hot keys
     */
    @Override
    protected boolean handleKeyPressed(KeyEvent event)
    {
        int key = event.getKeyCode();

        if (event.getModifiersEx() == 0 || event.isShiftDown())
        {
            switch (key)
            {
                case KeyEvent.VK_S:
                case KeyEvent.VK_P:
                case KeyEvent.VK_SPACE:
                    if (buttonStartPause_.isEnabled())
                    {
                        buttonStartPause_.doClick(10);
                        return true;
                    }
                    break;
                case KeyEvent.VK_Z:
                    if (buttonPrizes_.isEnabled())
                    {
                        buttonPrizes_.doClick(10);
                        return true;
                    }
                    break;
                case KeyEvent.VK_E:
                    if (buttonEdit_.isEnabled())
                    {
                        buttonEdit_.doClick(10);
                        return true;
                    }
                    break;
            }
        }

        return super.handleKeyPressed(event);
    }

    /**
     * start
     */
    @Override
    public void start()
    {
        // add game listeners
        game_.addPropertyChangeListener(PokerGame.PROP_CURRENT_LEVEL, this);
        game_.addPropertyChangeListener(PokerGame.PROP_PROFILE, this);

        // time listener
        game_.getGameClock().addGameClockListener(this);

        // super
        super.start();
    }

    /**
     * cleanup
     */
    @Override
    public void finish()
    {
        // remove game listeners
        game_.removePropertyChangeListener(PokerGame.PROP_CURRENT_LEVEL, this);
        game_.removePropertyChangeListener(PokerGame.PROP_PROFILE, this);

        // time listener
        game_.getGameClock().removeGameClockListener(this);

        // super
        super.finish();
    }

    /**
     * level changed and/or profile changed
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        GuiUtils.invoke(update_);
    }

    // runnable for in swing thread
    private Runnable update_ = new Runnable()
    {
        public void run()
        {
            updateLevel();
            checkButtons();
        }
    };

    /**
     * Update display
     */
    protected void updateLevel()
    {
        TournamentProfile profile = game_.getProfile();

        // get level in game
        int nLevel = game_.getLevel();
        int nNextLevel = nLevel + 1;

        // if a current table exists, use level in that game instead
        PokerTable table = game_.getCurrentTable();
        if (table != null) nLevel = table.getLevel();

        // level
        if (labelLevel_ != null)
        {
            labelLevel_.setText(PropertyConfig.getMessage("msg.level", nLevel));
        }

        // use shorter blinds in home mode
        boolean bDoK = false;//game_.isClockMode();
        boolean bDoM = false;//bDoK;

        // ante
        if (labelAnte_ != null)
        {
            if (profile.isBreak(nLevel))
            {
                labelAnte_.setText("");
            }
            else
            {
                int nAnte = profile.getAnte(nLevel);
                labelAnte_.setText(PropertyConfig.getAmount(nAnte, bDoK, bDoM));
            }
        }

        // blinds
        if (labelSblind_ != null)
        {
            if (profile.isBreak(nLevel))
            {
                labelSblind_.setText("");
                labelBblind_.setText(PropertyConfig.getMessage("msg.break"));
            }
            else
            {
                labelSblind_.setText(PropertyConfig.getAmount(profile.getSmallBlind(nLevel), bDoK, bDoM));
                labelBblind_.setText(PropertyConfig.getAmount(profile.getBigBlind(nLevel), bDoK, bDoM));

            }
        }


        // next
        if (labelNextBlinds_ != null)
        {
            if (profile.isBreak(nNextLevel))
            {
                labelNextBlinds_.setText(PropertyConfig.getMessage("msg.next.break",
                                                                   profile.getMinutes(nNextLevel)));
            }
            else
            {
                int nNextAnte = profile.getAnte(nNextLevel);
                labelNextBlinds_.setText(PropertyConfig.getMessage(nNextAnte > 0 ? "msg.next.ante" : "msg.next",
                                                                   PropertyConfig.getAmount(profile.getSmallBlind(nNextLevel), bDoK, bDoM),
                                                                   PropertyConfig.getAmount(profile.getBigBlind(nNextLevel), bDoK, bDoM),
                                                                   PropertyConfig.getAmount(profile.getAnte(nNextLevel), bDoK, bDoM)));
            }
        }

        // rebuy/addon details
        if (labelDetails_ != null)
        {
            boolean bAddon = profile.isAddons();
            boolean bRebuy = profile.isRebuys();
            int nLastRebuy = profile.getLastRebuyLevel();
            int nMaxRebuy = profile.getMaxRebuys();
            int nAddonLevel = profile.getAddonLevel();

            String sRebuy = null;
            String sAddon = null;

            // rebuy message
            if (bRebuy)
            {
                if (nLevel > nLastRebuy)
                {
                    sRebuy = PropertyConfig.getMessage("msg.rebuy.done",
                                                       nLastRebuy);
                }
                else
                {
                    sRebuy = PropertyConfig.getMessage(nMaxRebuy > 0 ? "msg.rebuy.until.max" : "msg.rebuy.until",
                                                       nLastRebuy,
                                                       nMaxRebuy);
                }

            }
            else
            {
                sRebuy = PropertyConfig.getMessage("msg.rebuy.none");
            }

            // figure out add on message
            if (bAddon)
            {
                if (nLevel > nAddonLevel)
                {
                    sAddon = PropertyConfig.getMessage("msg.addon.done",
                                                       nAddonLevel);
                }
                else
                {
                    sAddon = PropertyConfig.getMessage("msg.addon.when",
                                                       nAddonLevel);
                }
            }
            else
            {
                sAddon = PropertyConfig.getMessage("msg.addon.none");
            }

            labelDetails_.setText(PropertyConfig.getMessage("msg.details", sRebuy, sAddon));
        }

    }
}