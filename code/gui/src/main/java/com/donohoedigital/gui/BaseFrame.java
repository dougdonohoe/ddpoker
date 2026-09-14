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
import com.donohoedigital.base.Utils;
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

    GraphicsDevice device_;
    private final List<InternalDialog> allDialogs_ = new ArrayList<>();

    public BaseFrame()
    {
        super();
        device_ = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        ImageIcon icon = ImageConfig.getImageIcon("gui.icon");
        if (icon != null)
        {
            setIconImage(icon.getImage());
        }
        else
        {
            logger.warn("Icon not found in images.xml: 'gui.icon'");
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

    // sequence for modal thread
    private static int SEQ = 0;

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
        boolean bModal_ = false;

        /**
         * Begin a modal event loop.  End by calling endModal()
         */
        @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ChainOfInstanceofChecks", "SameParameterValue"})
        void beginModal(boolean bLog)
        {
            bModal_ = true;
            if (bLog) logged_.add(this);
            String sName = Thread.currentThread().getName();
            Thread.currentThread().setName("Modal-" + (SEQ++));
            //logger.debug("GUI Modal is started");
            try
            {
                ApplicationError.assertTrue(SwingUtilities.isEventDispatchThread(), "Not in swing thread", Thread.currentThread().getName());
                EventQueue theQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
                while (bModal_)
                {
                    // This is essentially the body of EventDispatchThread
                    AWTEvent event = theQueue.getNextEvent();
                    if (!bModal_) logger.warn("***** GUI dispatching when not modal: {}", event);
                    Object src = event.getSource();
                    // can't call theQueue.dispatchEvent, so I pasted its body here
                    if (event instanceof ActiveEvent)
                    {
                        ((ActiveEvent) event).dispatch();
                    }
                    else if (src instanceof Component)
                    {
                        ((Component) src).dispatchEvent(event);
                    }
                    else if (src instanceof MenuComponent)
                    {
                        ((MenuComponent) src).dispatchEvent(event);
                    }
                    else
                    {
                        logger.warn("Unable to dispatch event: {}", event);
                    }
                }
            }
            catch (InterruptedException e)
            {
                Thread.interrupted();
            }
            catch (Throwable t)
            {
                logger.debug("Error during modal: {}", Utils.formatExceptionText(t));
            }

            //logger.debug("GUI Modal is ended");
            Thread.currentThread().setName(sName);
            if (bLog) logged_.remove(this);
        }

        /*
         * Stops the event dispatching loop created by a previous call to
         * <code>beginModal</code>
         */
        public void endModal()
        {
            //logger.debug("GUI Modal is false");
            bModal_ = false;
        }
    }
}
