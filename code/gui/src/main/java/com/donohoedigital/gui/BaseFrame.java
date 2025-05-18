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
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

public class BaseFrame extends JFrame implements DDWindow
{
    static Logger logger = LogManager.getLogger(JFrame.class);

    GraphicsDevice device_;
    BaseFrame thisFrame;
    boolean bFixAltTabBug_ = false;
    boolean bIgnoreNextDeactive_ = false;
    boolean bActive_ = false;
    DisplayMode dmLastSet_ = null;
    private final List<InternalDialog> allDialogs_ = new ArrayList<>();

    public BaseFrame()
    {
        super();
        thisFrame = this;
        device_ = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        addWindowListener(new BaseFrameWindowAdapter());
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

    public GraphicsDevice getGraphicsDevice()
    {
        return device_;
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
     * Display (default no full screen)
     */
    public void display()
    {
        display(false);
    }

    /**
     * display
     */
    public void display(boolean bFull)
    {
        if (bFull && Utils.ISWINDOWS && Utils.IS141)
        {
            setMinimized();
        }
        setVisible(true);

        // need to run later so that window is drawn first/initialized
        if (bFull)
        {
            //frame_.setDisplayMode(800, 600, 32, 75); // TODO: configure, get depth/refresh from current
            SwingUtilities.invokeLater(
                    this::setFullScreenOn
            );
        }
    }

    /**
     * Cleanup for exit (dispose, turn off full screen mode)
     */
    public void cleanup()
    {
        dispose();
        if (isFullScreenSupported())
        {
            try
            {
                setFullScreenWindow(null);
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    /**
     * Set new display mode
     */
    public void setDisplayMode(int nWidth, int nHeight, int nBitDepth, int nRefreshRate)
    {
        setDisplayMode(new DisplayMode(nWidth, nHeight, nBitDepth, nRefreshRate));
    }

    /**
     * Set new display mode
     */
    public void setDisplayMode(DisplayMode dm)
    {
        if (!isFullScreen() || !isDisplayChangeSupported()) return;
        if (dm == null) return;
        dmLastSet_ = dm;
        //System.out.println("Setting display mode to: " + dm);
        _setDisplayMode(dm);
        setSize(new Dimension(dm.getWidth(), dm.getHeight()));
        validate();

        // need to repaint content pane after set display mode
        SwingUtilities.invokeLater(
                () -> {
                    getContentPane().validate();
                    getContentPane().repaint();
                }
        );
    }

    /**
     * Turn full screen mode on.  Returns true if succeeded.
     * False if failed (i.e., already in full screen mode).
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean setFullScreenOn()
    {
        if (!isFullScreenSupported()) return false;

        if (!isFullScreen())
        {
            if (bActive_) bIgnoreNextDeactive_ = true;

            // order of following calls is important
            dispose();
            setUndecorated(true);
            setFullScreenWindow(this);
            //setIgnoreRepaint(true); // seems this is only good for animated apps, not swing
            setResizable(false);
            return true;
        }
        return false;
    }

    /**
     * Turn full screen mode off.  Returns true if succeeded.
     * False if failed (i.e., not currently in full screen mode).
     */
    public boolean setFullScreenOff()
    {
        return setFullScreenOff(false);
    }

    /**
     * Private function for use to handle alt-tab bug
     */
    private boolean setFullScreenOff(boolean bAltTabBug)
    {
        if (!isFullScreenSupported()) return false;

        if (isFullScreen())
        {
            setFullScreenWindow(null); // must call first or get exceptions in text area scrolling
            dispose(); // release all native window resources
            setUndecorated(false); // add back window border
            setResizable(true); // make resizable
            //setIgnoreRepaint(false); // process repaints (already on - see above)
            setVisible(true);

            // if turning off due to alt-tab, set state to iconified and
            // set flag for processing later to restore full-screen
            if (bAltTabBug)
            {
                setExtendedState(Frame.ICONIFIED);
                bFixAltTabBug_ = true;
            }
            // if not restoring full screen, set last display mode to null
            else
            {
                dmLastSet_ = null;
            }

            return true;
        }

        return false;
    }

    /**
     * Return whether frame is in full screen mode
     */
    public boolean isFullScreen()
    {
        if (!isFullScreenSupported()) return false;

        return (getFullScreenWindow() != null);
    }

    /**
     * Maximizes the screen.  Does nothing if frame isFullScreen()
     */
    public void setMaximized()
    {
        if (isFullScreen()) return;
        setExtendedState(MAXIMIZED_BOTH);
    }

    /**
     * return if maximized
     */
    public boolean isMaximized()
    {
        if (isFullScreen()) return false;
        return (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    /**
     * Minimizes the screen.  Does nothing if frame isFullScreen()
     */
    public void setMinimized()
    {
        if (isFullScreen()) return;
        setExtendedState(ICONIFIED);
    }

    /**
     * return if minimized
     */
    public boolean isMinimized()
    {
        if (isFullScreen()) return false;
        return (getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED;
    }

    /**
     * Sets frame to normal size.  Does nothing if frame isFullScreen()
     */
    public void setNormal()
    {
        if (isFullScreen()) return;
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
     * Unselected (de activate) all dialogs
     */
    public void unselectAllDialogs()
    {
        if (allDialogs_.isEmpty()) return;
        InternalDialog dialog;

        for (int i = allDialogs_.size() - 1; i >= 0; i--)
        {
            dialog = allDialogs_.get(i);
            if (dialog.isSelected())
            {
                try
                {
                    dialog.setSelected(false);
                }
                catch (PropertyVetoException ignored)
                {
                }
            }
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
//        else
//        {
//            //logger.debug("YY BASE window activated focus to " + restoreTo);
//        }

        restoreTo.requestFocus();
    }

    /**
     * Used to track window events to deal with alt-tab bug
     */
    private class BaseFrameWindowAdapter extends WindowAdapter
    {
        /**
         * If re-iconifying after alt-tabbing from fullscreen mode,
         * we need to validate to make sure window is
         * relayed out properly
         */
        @Override
        public void windowDeiconified(WindowEvent e)
        {

            if (bFixAltTabBug_)
            {
                //System.out.println("Deiconified - restoring");
                //validate(); // needed only if de-iconifing and not going to full-screen
                bActive_ = true; // need to set here to make next call work right

                setFullScreenOn(); // go back to full screen
                setDisplayMode(dmLastSet_); // set back to last display mode
                bFixAltTabBug_ = false;
            }
        }

        @Override
        public void windowActivated(WindowEvent e)
        {
            //System.out.println("Window Activated " + getName());
            bActive_ = true;
            //restoreFocus(null); // TODO: remove perm?  turned off for testing for 2.5 (leave off for now - seems not needed)
        }

        /**
         * If window deactivated in full screen mode,
         * need to reset values to work around JDK 1.4.1 bug
         */
        @Override
        public void windowDeactivated(WindowEvent e)
        {
            bActive_ = false;

            // ignore the deactivated event after moving to full
            // screen mode
            if (bIgnoreNextDeactive_)
            {
                //System.out.println("Ignoring deactivate");
                bIgnoreNextDeactive_ = false;
                return;
            }

            //System.out.println("Window Deactivated " + getName());
            if (isFullScreen())
            {
                //System.out.println("Turning full screen off");
                setFullScreenOff(true);
            }
        }
    }

    ////
    //// Convienence methods
    ////

    /**
     * Return whether full screen is supported
     */
    public boolean isFullScreenSupported()
    {
        return device_.isFullScreenSupported();
    }

    /**
     * Return whether display change is supported
     */
    public boolean isDisplayChangeSupported()
    {
        return device_.isDisplayChangeSupported();
    }

    /**
     * Set full screen window
     */
    private void setFullScreenWindow(Window w)
    {
        //logger.debug("Setting full screen window: " + w);
        device_.setFullScreenWindow(w);
    }

    /**
     * Return full screen window
     */
    private Window getFullScreenWindow()
    {
        return device_.getFullScreenWindow();
    }

    /**
     * Get DisplayMode
     */
    public DisplayMode getDisplayMode()
    {
        return device_.getDisplayMode();
    }

    /**
     * Get all DisplayModes
     */
    public DisplayMode[] getDisplayModes()
    {
        return device_.getDisplayModes();
    }

    /**
     * Set DisplayMode
     */
    private void _setDisplayMode(DisplayMode dm)
    {
        device_.setDisplayMode(dm);
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
         * Sets up DELETE / BACKSPACE / SPACE as keys which end modal
         * and then calls beginModal()
         */
        public void beginModalEscapable(JComponent comp)
        {
            HandleKey hk = new HandleKey();

            GuiUtils.addEscapeKeyActions(comp, hk);
            comp.addMouseListener(hk);
            beginModal();
            GuiUtils.removeEscapeKeyActions(comp);
            comp.removeMouseListener(hk);
        }

        /**
         * Begin a modal event loop.  End by calling endModal()
         */
        public void beginModal()
        {
            beginModal(true);
        }

        /**
         * Begin a modal event loop.  End by calling endModal()
         */
        @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ChainOfInstanceofChecks"})
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
                    if (!bModal_) logger.warn("***** GUI dispatching when not modal: " + event);
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
                        logger.warn("Unable to dispatch event: " + event);
                    }
                }
            }
            catch (InterruptedException e)
            {
                Thread.interrupted();
            }
            catch (Throwable t)
            {
                logger.debug("Error during modal: " + Utils.formatExceptionText(t));
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

        private class HandleKey extends AbstractAction implements MouseListener
        {
            // end modal
            public void actionPerformed(ActionEvent e)
            {
                endModal();
            }

            public void mouseReleased(MouseEvent e)
            {
                if (e.getButton() != MouseEvent.BUTTON1 &&
                    !e.isShiftDown() && !e.isControlDown())
                {
                    endModal();
                }
            }

            public void mouseClicked(MouseEvent e)
            {
            }

            public void mouseEntered(MouseEvent e)
            {
            }

            public void mouseExited(MouseEvent e)
            {
            }

            public void mousePressed(MouseEvent e)
            {
            }
        }
    }


}
