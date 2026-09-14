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
package com.donohoedigital.gui;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.ImageConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class BaseFrame extends JFrame implements DDWindow
{
    static Logger logger = LogManager.getLogger(JFrame.class);

    // Window icon sizes.  Windows + Linux use these for title bar, taskbar and Alt-Tab.
    // On macOS, the Dock icon comes from the app bundle's .icns (or -Xdock:icon), but
    // these are used for the badge on a minimized window's Dock thumbnail.
    private static final int[] ICON_SIZES = {16, 32, 48, 64, 128};

    GraphicsDevice device_;
    private final List<InternalDialog> allDialogs_ = new ArrayList<>();

    public BaseFrame()
    {
        super();
        device_ = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        loadWindowIcons();
    }

    /**
     * Set the window icon(s) from the multi-resolution 'gui.icon.{size}' set, so each
     * context picks the best match.
     */
    private void loadWindowIcons()
    {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES)
        {
            ImageIcon icon = ImageConfig.getImageIcon("gui.icon." + size, null);
            if (icon != null) icons.add(icon.getImage());
        }

        if (!icons.isEmpty())
        {
            setIconImages(icons);
        }
        else
        {
            logger.warn("No window icon found in images.xml: 'gui.icon.{size}'");
        }
    }

    public void center()
    {
        Dimension size = getSize();
        Point center =  GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        int nX = center.x - (size.width / 2);
        int nY = center.y - (size.height / 2);
        setLocation(nX, nY);
    }

    /**
     * Display
     */
    public void display()
    {
        setVisible(true);
    }

    /**
     * Cleanup for exit
     */
    public void cleanup()
    {
        dispose();
    }

    /**
     * Maximizes the screen
     */
    public void setMaximized()
    {
        setExtendedState(MAXIMIZED_BOTH);
    }

    /**
     * return if maximized
     */
    public boolean isMaximized()
    {
        return (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    /**
     * Minimizes the screen
     */
    @SuppressWarnings("unused")
    public void setMinimized()
    {
        setExtendedState(ICONIFIED);
    }

    /**
     * return if minimized
     */
    public boolean isMinimized()
    {
        return (getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED;
    }

    /**
     * Sets frame to normal size
     */
    public void setNormal()
    {
        setExtendedState(NORMAL);
    }

    /**
     * Add dialog, return true if added, false if already there
     */
    public boolean addDialog(InternalDialog dialog)
    {
        if (!allDialogs_.contains(dialog))
        {
            allDialogs_.add(dialog);
            return true;
        }
        return false;
    }

    /**
     * Remove dialog, return true if removed, false if not there
     */
    public boolean removeDialog(InternalDialog dialog)
    {
        return allDialogs_.remove(dialog);
    }

    /**
     * Call removeDialog() on all open dialogs
     */
    public void removeAllDialogs()
    {
        List<InternalDialog> all = new ArrayList<>(allDialogs_);
        for (InternalDialog dialog : all)
        {
            dialog.removeDialog();
        }
    }

    /**
     * Restore focus to last dialog opened, return false if cannot do so
     */
    private boolean restoreFocusLastDialog()
    {
        if (allDialogs_.isEmpty()) return false;
        InternalDialog dialog;
        InternalDialog backup = null;

        // first look for visible/selected
        for (int i = allDialogs_.size() - 1; i >= 0; i--)
        {
            dialog = allDialogs_.get(i);
            if (dialog.isVisible() && !dialog.isIcon())
            {
                if (dialog.isSelected())
                {
                    //logger.debug("Setting focus to selected window " + dialog.getTitle());
                    dialog.moveToFrontSelected();
                    return true;
                }
                else if (backup == null)
                {
                    backup = dialog;
                }
            }
        }

        if (backup != null)
        {
            //logger.debug("Setting focus to unselected window " + backup.getTitle());
            backup.moveToFrontSelected();
            return true;
        }

        return false;
    }

    /**
     * Restore focus to right place (widget or internal dialog)
     */
    public void restoreFocus(Component restoreTo)
    {
        // first attempt to restore to a dialog
        if (restoreFocusLastDialog())
        {
            //logger.debug("restore to dialog");
            return;
        }

        // then restore to passed in component if it is visible
        if (restoreTo == null || !restoreTo.isVisible())
        {
            //logger.debug("XX BASE window activated focus to " + getContentPane());
            restoreTo = getContentPane();
        }

        restoreTo.requestFocus();
    }

    //
    // Convenience methods
    //

    /**
     * Get DisplayMode
     */
    public DisplayMode getDisplayMode()
    {
        return device_.getDisplayMode();
    }

    ////
    //// Help widget stuff - keep same in BaseFrame and InternalDialog
    ////

    private JTextComponent tHelp_ = null;
    private boolean bIgnore_ = false;
    static final String EMPTY = "";

    /**
     * Set widget used to display help text
     */
    public void setHelpTextWidget(JTextComponent t)
    {
        tHelp_ = t;
    }

    /**
     * Get current widget
     */
    public JTextComponent getHelpTextWidget()
    {
        return tHelp_;
    }

    /**
     * Set message in help text area (ignores null/0 length messages so that
     * widgets w/no message don't erase previous message)
     */
    public void setHelpMessage(String sMessage)
    {
        if (tHelp_ != null && sMessage != null && !sMessage.isEmpty())
        {
            tHelp_.setText(sMessage);
            tHelp_.repaint();
        }
    }

    /**
     * Set message in help text area
     */
    public void setMessage(String sMessage)
    {
        if (tHelp_ != null)
        {
            if (sMessage == null) sMessage = EMPTY;
            tHelp_.setText(sMessage);
            tHelp_.repaint();
        }
    }

    /**
     * Clear message in help text area
     */
    public void clearMessage()
    {
        setMessage(EMPTY);
    }

    /**
     * Show help for this component
     */
    public void showHelp(DDComponent source)
    {
        // if skipping next, do so
        if (bIgnore_)
        {
            bIgnore_ = false;
            return;
        }

        if (tHelp_ != null && source != null)
        {
            String sHelp = null;
            if (source instanceof DDCustomHelp)
            {
                sHelp = ((DDCustomHelp) source).getHelpText();
            }


            if (sHelp == null)
            {
                sHelp = GuiManager.getDefaultHelp(source);
            }

            setHelpMessage(sHelp);
        }
    }

    /**
     * Set to ignore next mouse enter (so we don't show help)
     */
    public void ignoreNextHelp()
    {
        bIgnore_ = true;
    }

    ////
    //// Modal stuff
    ////

    // list of all logged modals
    private final List<Modal> logged_ = new ArrayList<>();

    /**
     * get new Modal handler
     */
    public Modal newModal()
    {
        return new Modal();
    }

    /**
     * End all logged gui modal
     */
    public void endModalLogged()
    {
        if (logged_.isEmpty()) return;
        List<Modal> clone = new ArrayList<>(logged_);
        for (Modal modal : clone)
        {
            modal.endModal();
        }
    }

    /**
     * class for doing modal dialogs
     */
    @SuppressWarnings({"PublicInnerClass"})
    public class Modal
    {
        // The nested event loop that keeps events pumping while the dialog is
        // up.  Created in beginModal(), ended by endModal().  EDT-only.
        private SecondaryLoop loop_;

        /**
         * Begin a modal event loop.  Blocks here - while continuing to
         * dispatch events - until endModal() is called.
         */
        @SuppressWarnings("SameParameterValue")
        void beginModal(boolean bLog)
        {
            ApplicationError.assertTrue(SwingUtilities.isEventDispatchThread(), "Not in swing thread", Thread.currentThread().getName());

            if (bLog) logged_.add(this);
            try
            {
                loop_ = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();

                // enter() blocks until endModal() (loop.exit()) is called, while
                // events keep flowing through the normal EventQueue.dispatchEvent
                if (!loop_.enter())
                {
                    logger.warn("GUI modal loop failed to start");
                }
            }
            finally
            {
                loop_ = null;
                if (bLog) logged_.remove(this);
            }
        }

        /*
         * Ends the event loop started by beginModal().  Safe to call more than
         * once, or after the loop has already ended.
         */
        public void endModal()
        {
            SecondaryLoop loop = loop_;
            if (loop != null) loop.exit();
        }
    }
}
