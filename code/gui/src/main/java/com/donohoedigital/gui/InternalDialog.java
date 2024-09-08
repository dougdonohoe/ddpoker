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
 * InternalDialog.java
 *
 * Created on October 31, 2002, 3:27 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
    
/**
 *
 * @author  Doug Donohoe
 */
@SuppressWarnings({"PublicInnerClass"})
public class InternalDialog extends JInternalFrame implements DDWindow
{
    static Logger logger = Logger.getLogger(InternalDialog.class);
    
    protected BaseFrame frame_;
    private boolean bModal_ = false;
    private Component previousFocusOwner_;
    private boolean PERF = false;

    protected void finalize() throws Throwable {
        super.finalize();
        if (PERF) Perf.finalize(this, getName());
    }
    
    /**
     * Creates a new InternalDialog - set BaseFrame later
     */
    public InternalDialog(String sName, String sTitle, boolean bModal)
    {
        this(null, sName, sTitle, bModal);
    }
    
    /** 
     * Creates a new InternalDialog 
     */
    public InternalDialog(BaseFrame frame, String sName, String sTitle, boolean bModal)
    {
        super(sTitle, true, true);
        bModal_ = bModal;
        setName(sName);
        if (PERF) Perf.construct(this, getName());

        // store frame
        setBaseFrame(frame);
        
        // init
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        setDesktopIcon(new InternalIcon(this));
        
        // BUG 133 - turn off requesting of focus to
        // avoid apparent memory lead in KeyboardFocusManager.newFocusOwner
        if (Perf.isOn())
        {
            //TODO needed with JProfile? setRequestFocusEnabled(false);
        }

        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameIconified(InternalFrameEvent e)
            {
                InternalIcon icon = (InternalIcon) getDesktopIcon();

                if (icon != null) // && icon.shouldSetLocation())
                {
                    icon.setSize(175, icon.getHeight());
                    // change location is not needed for poker
                    //Point p = getLocation();
                    // move icon to center, bottom
                    //Point newLocation = new Point(//(getWidth() - icon.getWidth())/2 + p.x,
                     //       10,
                     //                           frame_.getContentPane().getHeight() - icon.getHeight() - 5);
                    //icon.setLocation(newLocation);
                    //icon.storeLocation(newLocation);
                }
            }
        });
        
        // set after title set so we have correct icons
        DDInternalFrameUI ddui = new DDInternalFrameUI(this);
        setUI(ddui);
    }
    
    /**
     * BUG 133 - Cleanup dialog when done
     */
    public void clean() {
        frame_ = null;
        focus_ = null;
        closed_ = null;
        previousFocusOwner_ = null;
        // empty panel allows previous phase to get cleanedup sooner
        setContentPane(new JPanel());
        setDesktopIcon(null);
    }
    
    /**
     * Icon
     */
    private class InternalIcon extends JInternalFrame.JDesktopIcon
    {
        boolean bFirstIconification = true;
        Point pLocation;
        
        public InternalIcon(JInternalFrame f)
        {
            super(f);
            Color cFixSwingBugIconBackground = MetalLookAndFeel.getWindowTitleBackground();
            setBackground(cFixSwingBugIconBackground);
            GuiUtils.setBackgroundChildren(this, cFixSwingBugIconBackground);
            setSize(500, getHeight());
        }

        // not needed for poker
//        /**
//         * Location should be set if the user did not move the
//         * icon since last iconified
//         */
//        public boolean shouldSetLocation()
//        {
//            if (bFirstIconification)
//            {
//                bFirstIconification = false;
//                return true;
//            }
//
//            if (getLocation().equals(pLocation))
//            {
//                return true;
//            }
//
//            return false;
//        }
//
//        public void storeLocation(Point p)
//        {
//            pLocation = p;
//        }
    }
    
    /**
     * Get BaseFrame used with this window
     */
    public BaseFrame getBaseFrame()
    {
        return frame_;
    }
    
    /**
     * Set base frame to be used with this window
     */
    public void setBaseFrame(BaseFrame frame)
    {
        frame_ = frame;
        rememberRealFocusOwner();
        //logger.debug("PREVIOUS set to " + previousFocusOwner_);
    }

    /**
     * set focus owner
     */
    private void rememberRealFocusOwner()
    {
        if (frame_ != null)
        {
            // when displaying this, remember previous focus owner
            // so long as it it isn't a dialog or iconified dialog
            Component owner = frame_.getFocusOwner();
            if (owner != null && GuiUtils.getInternalDialog(owner) == null &&
                !(owner.getParent() instanceof InternalIcon))
            {
                previousFocusOwner_ = owner;
            }
        }
    }

    /**
     * Used in showDialog - position dialog in center of screen
     */
    public static final int POSITION_CENTER = 1;
    
    /**
     * Used in showDialog - position dialog in center, top of screen
     */
    public static final int POSITION_CENTER_TOP = 2;
    
    /**
     * Used in showDialog - position dialog so as not to obscur location
     * specified in last call to setNotObscurredLocation()
     */
    public static final int POSITION_NOT_OBSCURRED = 3;
    
    /**
     * USed in showDialog - position dialog centered, but adjust by x,y
     * specified in last call to setCenterAdjust()
     */
    public static final int POSITION_CENTER_ADJUST = 4;
    
    /**
     * Used in showDialog - don't position dialog at all
     */
    public static final int POSITION_NONE = 5; 
    
    /**
     * Overriden to reset focus on setVisible(false)
     */
    public void setVisible(boolean b)
    {
        if (!b && isVisible())
        {
            resetFocus();
        }
        else if (b && !isVisible())
        {
            rememberRealFocusOwner();
            setFocus();
        }
        super.setVisible(b);
    }

    /**
     * Overriden to catch PropertyVetoException and
     * to reset focus on iconification
     */
    public void setIcon(boolean b)
    {
        if (b && !isIcon())
        {
            resetFocus();
        }
        else if (!b && isIcon())
        {
            rememberRealFocusOwner();
            setFocus();
        }
        
        try { super.setIcon(b);
        } catch (java.beans.PropertyVetoException ignored) {}
    }
    
    /**
     * Reset elsewhere when we are iconified or closed
     */
    private void resetFocus()
    {
       if (frame_ == null) return; // ignore first call from JInternalFrame constructor (before frame_ set)
       SwingUtilities.invokeLater(
            new Runnable() {
                BaseFrame frame = frame_;
                Component restoreTo = previousFocusOwner_;
                public void run() {
                    frame.restoreFocus(restoreTo);
                }
       });
    }
    
    // component that should get focus in this internaldialog
    private Component focus_;
    
    /**
     * Set focus here
     */
    private void setFocus()
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    // BUG 133 - this can happen in certain cases where a dialog
                    // is quickly dismissed
                    if (focus_ == null) return;

                    //logger.debug("Seting focus to: " +focus_);
                    // BUG 133 - turn off requesting of focus to
                    // avoid apparent memory leak in KeyboardFocusManager.newFocusOwner
                    if (!Perf.isOn())
                    {
                        focus_.requestFocus();
                    }
                }
            }
        );
    }
    
    /**
     * Show dialog.  If close is null,
     * this adds its own listener to call removeDialog() upon close
     * If close is provided, then it must call removeDialog()
     * when done.  An exception is thrown if no BaseFrame is defined.
     */
    public void showDialog(InternalFrameListener close, int nPOS)
    {
        showDialog(close, nPOS, null);
    }
    
    /**
     * Show dialog, specify component to get focus
     */
    public void showDialog(InternalFrameListener close, int nPOS, Component cFocus)
    {
        showDialog(close, nPOS, cFocus, true);
    }

    /**
     * Show dialog, specify component to get focus
     */
    public void showDialog(InternalFrameListener close, int nPOS, Component cFocus, boolean visible)
    {
        _showDialog(close, nPOS, cFocus, visible);
        if (bModal_) beginModal();
    }

    // close listener for dialog
    private InternalFrameListener close_ = null;
    
    // stuff for modal dialogs
    private BaseFrame.Modal modalUtil_;
    private JPanel mouseBlocker_ = null;

    // Listeners for modal mouse blocker layer
    private static final MouseAdapter MY_MOUSE = new MouseAdapter() {};
    private static final MouseMotionAdapter MY_MOUSE_MOTION = new MouseMotionAdapter() {};
    private static final MouseWheelListener MY_MOUSE_WHEEL = new MouseWheelListener() 
                            {  public void mouseWheelMoved(MouseWheelEvent e) {}};
    
    // list of all modal info
    private static Stack<ModalInfo> modal_ = new Stack<ModalInfo>();
    
    /**
     * Track modal info to handle multiple modal dialogs
     */
    private class ModalInfo
    {
        Integer nLayer;
        Integer nMouseBlockerLayer;
 
        public ModalInfo(Integer nLayer)
        {
            this.nLayer =nLayer;
            nMouseBlockerLayer = nLayer - 1;
        }
    }

    /**
     * Shows the dialog, using the given lcose listener
     */
    private void _showDialog(InternalFrameListener close, int nPOS, Component cFocus, boolean visible)
    {
        if (frame_ == null) throw new ApplicationError(ErrorCodes.ERROR_NULL,
                            "BaseFrame not defined in InternalDialog",
                            "Make sure to define BaseFrame before calling showDialog()");

        // only add listeners and modal blockers if actually visible
        if (visible)
        {
            // if no close listener, create
            if (close == null) close = new WindowButtonListener();

            // init
            focus_ = cFocus;
            close_ = close;
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addInternalFrameListener(close);

            if (bModal_)
            {                                         //QUESTION_MESSAGE   ERROR_MESSAGE  WARNING_MESSAGE
                putClientProperty("JInternalFrame.messageType", JOptionPane.QUESTION_MESSAGE);
                putClientProperty("JInternalFrame.frameType", "optionDialog");


                ModalInfo modal = null;
                if (modal_.size() > 0)
                {
                    modal = modal_.peek();
                }

                ModalInfo newmodal = new ModalInfo(modal != null ? modal.nLayer + 2 : JLayeredPane.MODAL_LAYER);
                modal_.add(newmodal);

                mouseBlocker_ = new JPanel();
                DisplayMode dm = frame_.getDisplayMode();
                mouseBlocker_.setSize(dm.getWidth(), dm.getHeight()); // set block size to that of display to handle resize
                mouseBlocker_.setOpaque(false);
                mouseBlocker_.addMouseListener(MY_MOUSE);
                mouseBlocker_.addMouseMotionListener(MY_MOUSE_MOTION);
                mouseBlocker_.addMouseWheelListener(MY_MOUSE_WHEEL);
                if (blockerListener_ != null) blockerListener_.blockerCreated(mouseBlocker_);
                frame_.getLayeredPane().add(this, newmodal.nLayer);
                frame_.getLayeredPane().add(mouseBlocker_, newmodal.nMouseBlockerLayer);
            }
            else
            {
                frame_.getLayeredPane().add(this, JLayeredPane.DEFAULT_LAYER);
            }

            switch (nPOS)
            {
                case POSITION_CENTER:
                    validate();
                    pack();
                    center();
                    break;

                case POSITION_CENTER_TOP:
                    validate();
                    pack();
                    centerTop();
                    break;

                case POSITION_NOT_OBSCURRED:
                    validate();
                    pack();
                    setNotObscurred(x_, y_, buffer_);
                    break;

                case POSITION_CENTER_ADJUST:
                    validate();
                    pack();
                    centerAdjust(xadjust_, yadjust_);

                default:
                    // make sure dialog is (still) visible
                    int x = getX();
                    int y = getY();
                    int w = frame_.getWidth();
                    int h = frame_.getHeight();
                    if (x < 0 || x > (w-50)) x = w/2;
                    if (y < 0 || y > (h-50)) y = h/2;
                    setLocation(x,y);
                    break;
            }

            setVisible(true);

            // paint immediately to avoid DDButtons being painted
            // before the rest of the window
            paintImmediately(0,0, getWidth(), getHeight());
        }

        // add this to list of dialogs (might already be there if
        // showing a previously faceless dialog)
        if (frame_.addDialog(this))
        {
            // notify
            if (opened_ != null) opened_.dialogAdded(this);
        }
    }

    /**
     * Removes this from the BaseFrame's layeredpane and calls dispose()
     */
    public void removeDialog()
    {
        if (frame_ == null) return; // closing before using, just ignore
        
        // remove this from list of dialogs (return if not there)
        if (!frame_.removeDialog(this)) return;

        // remove from view
        setVisible(false);
        
        // modal case - end modal, remove mouse blocker
        if (modalUtil_ != null)
        {
            modalUtil_.endModal();
            modalUtil_ = null;
        }
        if (mouseBlocker_ != null)
        {
            frame_.getLayeredPane().remove(this);
            frame_.getLayeredPane().remove(mouseBlocker_);
            modal_.pop();
            if (blockerListener_ != null) blockerListener_.blockerFinished(mouseBlocker_);
            mouseBlocker_ = null;
        }
        // normal case
        else
        {
            frame_.getLayeredPane().remove(this);
        }
        dispose();
        removeInternalFrameListener(close_);
        close_ = null;  
        
        // repaint frame immediately so window is gone zippily
        ((JComponent)frame_.getContentPane()).paintImmediately(0,0, frame_.getWidth(), frame_.getHeight());
        
        // notify
        if (closed_ != null) closed_.dialogRemoved(this);
    }
    
    /**
     * Default close listener for normal dialogs
     */
    private class WindowButtonListener extends InternalFrameAdapter
    {
        public void internalFrameClosing(InternalFrameEvent e) 
        {
            removeDialog();
        }
    }

    private DialogOpened opened_;
    private DialogClosed closed_;

    /**
     * set dialog opened interface for this
     */
    public void setDialogOpened(DialogOpened opened)
    {
        opened_ = opened;
    }

    /**
     * set dialog closed interface for this
     */
    public void setDialogClosed(DialogClosed closed)
    {
        closed_ = closed;
    }

    /**
     * Interface for notification of when we are opened
     */
    @SuppressWarnings({"PublicInnerClass"})
    public interface DialogOpened
    {
        public void dialogAdded(InternalDialog dialog);
    }

    /**
     * Interface for notification of when we are closed
     */
    @SuppressWarnings({"PublicInnerClass"})
    public interface DialogClosed
    {
        public void dialogRemoved(InternalDialog dialog);
    }

    /**
     * Bring dialog to front, select it and set focus to its desired component
     */
    public void moveToFrontSelected()
    {
        setFocus();
        try {
            setSelected(true);
            moveToFront();
        }
        catch (PropertyVetoException veto)
        {
            throw new ApplicationError(veto);
        }
    }
    
    /*
     * begins a modal dialog - makes sure window is visible first.
     */
    private synchronized void beginModal() 
    {
        /* Since all input will be blocked until this dialog is dismissed,
         * make sure its parent containers are visible first (this component
         * is tested below).  This is necessary for JApplets, because
         * because an applet normally isn't made visible until after its
         * start() method returns -- if this method is called from start(),
         * the applet will appear to hang while an invisible modal frame
         * waits for input.
         */
        
        if (isVisible() && !isShowing()) 
        {
            Container parent = this.getParent();
            while (parent != null) {
                if (!parent.isVisible()) {
                    parent.setVisible(true);
                }
                parent = parent.getParent();
            }
        }

        if (modalUtil_ == null)
        {
            modalUtil_ = frame_.newModal();
            modalUtil_.beginModal(false);
        }        
    }
    
    /**
     * Return if shown modally
     */
    public boolean isModal()
    {
        return bModal_;
    }
    
    /**
     * Center this pane within parent frame
     */
    public void center()
    {
        if (frame_ == null) return;
        Dimension size = getSize();
        int nScreenWidth = frame_.getWidth();
        int nScreenHeight = frame_.getHeight();
        int nX = (nScreenWidth - size.width) / 2;
        int nY = (nScreenHeight - size.height) / 2;
        
        if (!frame_.isFullScreen())
        {
            // TODO: get programmatically
            nY -= 15; // account for windows title bar
        }
        
        setLocation(nX, nY);
    }

    /**
     * Center this and then adjust by x,y set with setCenterAdjust()
     */
    public void centerAdjust(int xadjust, int yadjust)
    {
        center();
        setLocation(getX() + xadjust, getY() + yadjust);
    }
    
    /**
     * Center this pane within parent frame, near the top
     */
    public void centerTop()
    {
        if (frame_ == null) return;
        Dimension size = getSize();
        int nScreenWidth = frame_.getWidth();
        int nX = (nScreenWidth - size.width) / 2;
        int nY = 20;
        
        setLocation(nX, nY);
    }
    
    private int xadjust_= 0;
    private int yadjust_ = 0;

    /**
     * Set x,y offset to be used if calling show with
     * POSITION_CENTER_ADJUST as positioning hint
     */
    public void setCenterAdjust(int xadjust, int yadjust)
    {
        xadjust_ = xadjust;
        yadjust_ = yadjust;
    }
    
    private int x_= 0;
    private int y_ = 0;
    private int buffer_= 0;
    
    /**
     * Set location to be used if calling show with
     * POSITION_NOT_OBSCURRED as positioning hint
     */
    public void setNotObscurredLocation(int x, int y, int buffer)
    {
        x_ = x;
        y_ = y;
        buffer_ = buffer;
    }
    
    /**
     * Position so dialog doesn't obscur x,y
     */
    public void setNotObscurred(int x, int y, int buffer)
    {
        if (frame_ == null) return;
        int bw = getWidth();
        int bh = getHeight();
        
        int fw = frame_.getWidth();
        int fh = frame_.getHeight();
        
        // if buffer fits above point, center above it
        if (bh < (y - buffer)) 
        {
            y = (y - bh) / 2;
            x = (fw - bw) / 2; // center
        } else {
            // room to right of it
            if (bw < (fw-(x+buffer)))
            {
                y = (fh-bh)/2;
                x += ((fw-x)-bw)/2;
            }
            // room to left of it
            else if (bw < x - buffer)
            {
                y = (fh-bh)/2;
                x = (x-bw)/2;
            }
            else // to close to place well
            {
                int nTopDiff = bh - y;
                int nBottomDiff = bh - (fh - y);
                int nLeftDiff = bw - x;
                int nRightDiff = bw - (fw - x);
                
                // calculate smallest offscreen points
                int nVertical = Math.min(nTopDiff,nBottomDiff);
                int nHoriz = Math.min(nLeftDiff, nRightDiff);
                
                // if least space off screen on horizontal
                if (nHoriz < nVertical)
                {
                    y = (fh-bh)/2; // center on y axis
                    
                    if (nLeftDiff < nRightDiff)
                    {
                        x -= (bw + buffer);
                    }
                    else
                    {
                        x += buffer + Math.max(0, ((fw-x)-bw)/2);
                    }
                }
                else // more space on vert (y axis)
                {
                    x = (fw - bw)/2;
                    
                    if (nTopDiff < nBottomDiff)
                    {
                        y -= (bh + buffer);
                    }
                    else
                    {
                        y += (buffer + Math.max(0,((fh - y)-bh)/2));
                    }
                }
            }
        }
        
        setLocation(x,y);
    }
    
    /**
     * Override to ensure x,y are fine
     */
    public void setLocation(int x, int y)
    {
        Dimension size = getSize();
        int nScreenWidth = frame_.getWidth();
        int nScreenHeight = frame_.getHeight();

        if (x + size.width > nScreenWidth)
        {
            x = nScreenWidth - size.width;
            x -= 2; // scootch over
        }
        
        if (y + size.height > nScreenHeight)
        {
            y = nScreenHeight - size.height;
            y -= 2;
        }
        
        if (y < 0) y = 0;
        if (x < 0) x = 0;
        
        super.setLocation(x, y);
    }
    
    
    /**
     * Interface for handling mouse blockers
     */
    @SuppressWarnings({"PublicInnerClass"})
    public static interface ModalBlockerListener
    {
        public void blockerCreated(JPanel panel);
        public void blockerFinished(JPanel panel);
    }
    
    private static ModalBlockerListener blockerListener_ = null;
    
    /**
     * Set the blocker listener
     */
    public static void setModalBlockerListener(ModalBlockerListener listener)
    {
        blockerListener_ = listener;
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
        if (tHelp_ != null && sMessage != null && sMessage.length() > 0)
        {
            tHelp_.setText(sMessage);
            tHelp_.repaint();
        }
    }

    /**
     * Set message in help text area
     */
    public  void setMessage(String sMessage)
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
        if (bIgnore_) { bIgnore_ = false; return; }

        if (tHelp_ != null && source != null)
        {
            String sHelp = null;
            if (source instanceof DDCustomHelp)
            {
                sHelp = ((DDCustomHelp)source).getHelpText();
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
}
