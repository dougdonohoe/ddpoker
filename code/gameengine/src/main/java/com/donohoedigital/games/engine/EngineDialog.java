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
package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 18, 2006
 * Time: 11:12:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class EngineDialog extends InternalDialog
{
    protected static Logger logger = LogManager.getLogger(EngineDialog.class);

    private GameContext context_;
    private DialogBackground base_;
    private Component focus_;
    private int DESIRED_WIDTH;
    private int DESIRED_HEIGHT;

    /**
     * Constructor
     */
    EngineDialog(GameContext context, String sName, int nDesiredMinWidth, int nDesiredMinHeight)
    {
        super(context.getFrame(), sName, sName, false);
        context_ = context;
        DESIRED_WIDTH = nDesiredMinWidth;
        DESIRED_HEIGHT = nDesiredMinHeight;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    /**
     * init to given size (or full screen if passed in)
     */
    public void init(GamePhase gamephase, String sTitle, boolean bResizable)
    {
        ImageIcon winicon = ImageConfig.getImageIcon(gamephase.getString("dialog-windowtitle-image", "dialog-windowtitle-image"));
        setFrameIcon(winicon);
        setIconifiable(true);
        setResizable(bResizable);

        // create base panel and setup frame
        base_ = new DialogBackground(context_, gamephase, null, false, null);
        base_.setPreferredSize(new Dimension(DESIRED_WIDTH, DESIRED_HEIGHT));

        // focus stuff
        base_.setFocusTraversalKeysEnabled(false); // prevent focus from leaving panel via tab

        // add actions for alt-q and crtl-q, meta-w, alt-w, ctrl-w
        AbstractAction close = new CloseAction();

        // alt-w and ctrl-w close window on windows/linux (or quit if main window)
        if (!Utils.ISMAC)
        {
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "gameengineclose", close,
                            KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK);
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                            "gameengineclose", close,
                            KeyEvent.VK_W, KeyEvent.ALT_DOWN_MASK);
        }
        // mac version is apple-w
        else
        {
            GuiUtils.addKeyAction(base_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                        "gameengineclose", close,
                        KeyEvent.VK_W, KeyEvent.META_DOWN_MASK);
        }


        // the base_ is the content pane for the BaseApp frame_
        setContentPane(base_);
        setBackground(base_.getBackground());
        setTitle(sTitle);
    }

    /**
     * display
     */
    void display(InternalFrameListener listener)
    {
        showDialog(listener, InternalDialog.POSITION_CENTER, focus_);
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
     * Get context
     */
    public GameContext getGameContext()
    {
        return context_;
    }

    /**
     * Set center component
     */
    void setCenterComponent(JComponent c, Component focus)
    {
        base_.setCenterContents(c);
        focus_ = focus;
    }

    /**
     * Get base panel
     */
    DDPanel getBasePanel()
    {
        return base_;
    }

    /**
     * toFront (uniconify)
     */
    public void toFront()
    {
        if (isIcon)
        {
            setIcon(false);
        }
        super.toFront();
    }
}
