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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.udp.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 18, 2006
 * Time: 11:12:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class EngineWindow extends BaseFrame
{
    protected static Logger logger = LogManager.getLogger(EngineWindow.class);

    private GameEngine engine_;
    private GameContext context_;
    private EngineBasePanel base_;
    private int DESIRED_MIN_WIDTH;
    private int DESIRED_MIN_HEIGHT;
    private boolean bFull_;

    /**
     * Constructor
     */
    public EngineWindow(GameEngine engine, GameContext context, String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        super();
        engine_ = engine;
        context_ = context;
        setName(sName);
        DESIRED_MIN_WIDTH = nDesiredMinWidth;
        DESIRED_MIN_HEIGHT = nDesiredMinHeight;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    /**
     * init to given size (or full screen if passed in)
     */
    public void init(GamePhase gamephase, boolean bMain, Dimension size, boolean bFull, String sTitle, boolean bResizable)
    {
        // create base panel and setup frame
        bFull_ = bFull;
        base_ = new EngineBasePanel(this, gamephase);
        setResizable(bResizable);
        boolean bReset = engine_.getCommandLineOptions().getBoolean("reset", false);

        // check size
        DisplayMode mode = getDisplayMode();
        EnginePrefs prefs = engine_.getPrefsNode();

        // size from prefs
        boolean bMaximized = prefs.getBoolean(EngineConstants.PREF_MAXIMIZED+"-"+getName(), false);
        int w = -1, h = -1;
        if (!bReset)
        {
            w = prefs.getInt(EngineConstants.PREF_W+"-"+getName(), w);
            h = prefs.getInt(EngineConstants.PREF_H+"-"+getName(),  h);
        }

        if (bResizable && w != -1 && h != -1)
        {
            size.height = h;
            size.width = w;
        }

        // if stored in prefs smaller than our desired minimum,
        // adjust
        if (size.width < DESIRED_MIN_WIDTH) size.width = DESIRED_MIN_WIDTH;
        if (size.height < DESIRED_MIN_HEIGHT) size.height = DESIRED_MIN_HEIGHT;

        // can't be bigger than screen size (and should be screen size in full)
        if (bFull || size.height > mode.getHeight()) size.height = mode.getHeight();
        if (bFull || size.width > mode.getWidth()) size.width = mode.getWidth();

        // testing stuff
        if (TESTING(EngineConstants.TESTING_CHANGE_STARTING_SIZE) && getName().equals("main"))
        {
            if (mode.getWidth() > EngineConstants.TESTING_CHANGE_SIZE_WIDTH &&
                mode.getHeight() > EngineConstants.TESTING_CHANGE_SIZE_HEIGHT)
            {
                size.width = EngineConstants.TESTING_CHANGE_SIZE_WIDTH;
                size.height = EngineConstants.TESTING_CHANGE_SIZE_HEIGHT;
            }
        }
        base_.setPreferredSize(size);

        // focus stuff
        base_.setFocusTraversalKeysEnabled(false); // prevent focus from leaving panel via tab

        // add actions for alt-q and crtl-q, meta-w, alt-w, ctrl-w
        AbstractAction quit = new QuitAction();
        AbstractAction close = new CloseAction();

        // alt-q and ctrl-q close app on windows/linux
        if (!Utils.ISMAC)
        {
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "gameenginequit", quit,
                            KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK);
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "gameenginequit", quit,
                            KeyEvent.VK_Q, KeyEvent.ALT_DOWN_MASK);
        }
        // apple-Q done via mac app interface

        // alt-w and ctrl-w close window on windows/linux (or quit if main window)
        if (!Utils.ISMAC)
        {
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "gameengineclose", bMain ? quit : close,
                            KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK);
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "gameengineclose", bMain ? quit : close,
                            KeyEvent.VK_W, KeyEvent.ALT_DOWN_MASK);
        }
        // mac version is apple-w
        else
        {
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                        "gameengineclose", bMain ? quit : close,
                        KeyEvent.VK_W, KeyEvent.META_DOWN_MASK);
        }

        // debug dump action
        GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "debug", new DumpAction(),
                            KeyEvent.VK_D, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

        // debug udp flag action
        GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "udpdebug", new UDPAction(),
                            KeyEvent.VK_F12, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

        // debug udp app flag action
        GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "udpdebug2", new UDPAction2(),
                            KeyEvent.VK_F11, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

        // Help action
        GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "help", new HelpAction(),
                            KeyEvent.VK_SLASH, Utils.ISMAC ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK);

        // the base_ is the content pane for the BaseApp frame_
        setContentPane(base_);
        setBackground(base_.getBackground());

        // set title
        setTitle(sTitle);

        // final frame setup
        validate();
        pack();
        center();

        // listener, set location
        if (!bFull)
        {
			EngineWindowAdapter adapter = new EngineWindowAdapter();
			addComponentListener(adapter);
			addWindowStateListener(adapter);

            // look for x,y setting
            int x = engine_.getCommandLineOptions().getInteger("x", -1);
            int y = engine_.getCommandLineOptions().getInteger("y", -1);

            if (DebugConfig.isTestingOn() && getName().equals("main") && (x != -1 || y != -1))
            {
                if (x == -1) x = getX();
                if (y == -1) y = 10;

                setLocation(x, y);
            }
            else if (bMaximized)
            {
                setMaximized();
            }
            else
            {
                if (!bReset)
                {
                    x = prefs.getInt(EngineConstants.PREF_X+"-"+getName(), -1);
                    y = prefs.getInt(EngineConstants.PREF_Y+"-"+getName(),  -1);
                }
                if (x != -1 && y != -1)
                {
                    setLocation(x,y);
                }
            }
        }
    }

     /**
     * Add dialog - skip EngineDialogs (handled by context)
     */
    public boolean addDialog(InternalDialog dialog)
    {
        if (dialog instanceof EngineDialog) return true;
        else return super.addDialog(dialog);
    }

    /**
     * Remove dialog, return true if removed, false if not there
     */
    public boolean removeDialog(InternalDialog dialog)
    {
        if (dialog instanceof EngineDialog) return true;
        return super.removeDialog(dialog);
    }

    /**
     * display
     */
    public void display()
    {
        super.display(bFull_);
    }

    /**
     * Quit action
     */
    private class QuitAction extends AbstractAction
    {
       public void actionPerformed(ActionEvent e)
       {
            engine_.quit();
       }
    }

    /**
     * Close action
     */
    private class CloseAction extends AbstractAction
    {
       public void actionPerformed(ActionEvent e)
       {
            context_.close();
       }
    }

    /**
     * Debug dump action
     */
    private class DumpAction extends AbstractAction
    {
       public void actionPerformed(ActionEvent e)
       {
            logger.debug(Utils.getAllStacktraces());
       }
    }

    /**
     * UDP debugging action
     */
    private class UDPAction extends AbstractAction
    {
       public void actionPerformed(ActionEvent e)
       {
            TOGGLE(UDPServer.TESTING_UDP);
            logger.debug("UDP flags turned " + (TESTING(UDPServer.TESTING_UDP) ? "on":"off"));
            UDPServer.setDebugFlags();
       }
    }

    /**
     * UDP debugging action
     */
    private class UDPAction2 extends AbstractAction
    {
       public void actionPerformed(ActionEvent e)
       {
            TOGGLE(EngineConstants.TESTING_UDP_APP);
            logger.debug("UDP APP flags turned " + (TESTING(EngineConstants.TESTING_UDP_APP) ? "on":"off"));
       }
    }


    /**
     * help action
     */
    private class HelpAction extends AbstractAction
    {
       public void actionPerformed(ActionEvent e)
       {
            context_.processPhase("Help");
       }
    }

    /**
     * Get context
     */
    public GameContext getGameContext()
    {
        return context_;
    }

    /**
     * Get engine base panel
     */
    public EngineBasePanel getBasePanel()
    {
        return base_;
    }

    /**
     * toFront (uniconify)
     */
    public void toFront()
    {
        if (isMinimized() && !Utils.ISMAC)
        {
            setNormal();
        }
        super.toFront();
    }

    ////
    //// Listen for window events
    ////

    /**
     * Class to handle window closing events plus state changes issues
     */
    private class EngineWindowAdapter extends WindowAdapter implements ComponentListener
    {
        private Point prevLocation_ = new Point();
        private Point location_ = new Point();
        private Dimension size_ = new Dimension();

        /**
         * allow us to store user's preference
         */
        public void windowStateChanged(WindowEvent e)
        {
            //logger.debug("Window state changed: "+ e.getNewState() + " is Max: "+ isMaximized());

            // ignore iconified events
            if ((e.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED)
            {
                return;
            }

            // store whether we are maximized
            boolean bMax = (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;

            EnginePrefs prefs = engine_.getPrefsNode();
            prefs.putBoolean(EngineConstants.PREF_MAXIMIZED+"-"+getName(), bMax);
        }

        /**
         * Invoked when the component's size changes.
         */
        public void componentResized(ComponentEvent e)
        {
            if (bFull_ || isMaximized() || (TESTING(EngineConstants.TESTING_CHANGE_STARTING_SIZE) && getName().equals("main"))) return;
            getContentPane().getSize(size_);

            // logger.debug("Window size changed: "+ size_+ " is Max: "+ isMaximized());

            // save
            EnginePrefs prefs = engine_.getPrefsNode();
            prefs.putInt(EngineConstants.PREF_W+"-"+getName(), size_.width);
            prefs.putInt(EngineConstants.PREF_H+"-"+getName(), size_.height);
        }

        /**
         * Invoked when the component's position changes.
         */
        public void componentMoved(ComponentEvent e)
        {
            if (bFull_ || isMaximized()) return;
            prevLocation_.setLocation(location_);
            getLocation(location_);
            if (prevLocation_.equals(location_)) return;

            //logger.debug("Window position changed: "+ location_+ " is Max: "+ isMaximized());

            // save
            DisplayMode mode = getDisplayMode();
            EnginePrefs prefs = engine_.getPrefsNode();
            // store if top corner is visible (more or less) and it isn't too far off-screen at bottom/right
            if (location_.x >= -50 && location_.x < (mode.getWidth() - (DESIRED_MIN_WIDTH/2))) prefs.putInt(EngineConstants.PREF_X+"-"+getName(), location_.x);
            if (location_.y >= -50 && location_.y < (mode.getHeight() - (DESIRED_MIN_HEIGHT/2))) prefs.putInt(EngineConstants.PREF_Y+"-"+getName(), location_.y);
        }

        /** NOT USED */
        public void componentShown(ComponentEvent e) {}

        /** NOT USED */
        public void componentHidden(ComponentEvent e) {}
    }

}
