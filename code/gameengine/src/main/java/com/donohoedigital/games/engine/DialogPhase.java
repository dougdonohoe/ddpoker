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
 * DialogPhase.java
 *
 * Created on November 26, 2002, 5:47 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.prefs.*;

/**
 * @author Doug Donohoe
 */
public abstract class DialogPhase extends BasePhase implements InternalDialog.DialogOpened, InternalDialog.DialogClosed
{
    static Logger logger = LogManager.getLogger(DialogPhase.class);

    /**
     * "dialog-modal" prop value
     */
    protected boolean bModal_ = true;

    /**
     * "dialog-no-show-option" prop value
     */
    protected boolean bNoShowOption_ = false;

    /**
     * "dialog-no-show-key" prop value
     */
    protected String sNoShowKey_;

    /**
     * "style" prop value
     */
    protected String STYLE = null;

    /**
     * Window title param
     */
    public static final String PARAM_WINDOW_TITLE_KEY = "dialog-windowtitle-prop";

    /**
     * Dialog no-show-option param
     */
    public static final String PARAM_NO_SHOW_OPTION = "dialog-no-show-option";

    /**
     * Dialog no-show-key param
     */
    public static final String PARAM_NO_SHOW_KEY = "dialog-no-show-key";

    /**
     * Dialog no-show-checkbox-name param
     */
    public static final String PARAM_NO_SHOW_NAME = "dialog-no-show-checkbox-name";

    /**
     * Option to create, but don't show immediately
     */
    public static final String PARAM_CREATE_NO_SHOW = "dialog-create-no-show";

    /**
     * modal
     */
    public static final String PARAM_MODAL = "dialog-modal";

    // set if we don't show this because user asked not to see it again
    private boolean bDontShow_ = false;
    private boolean bCheckCreateNoShow_ = false;

    // key used for question
    private String sNoShowCheckboxName_ = null;

    /**
     * Init phase, storing engine and gamephase.  Called createUI()
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        super.init(engine, context, gamephase);

        bModal_ = gamephase_.getBoolean(PARAM_MODAL, false);
        bNoShowOption_ = gamephase_.getBoolean(PARAM_NO_SHOW_OPTION, false);

        if (bNoShowOption_)
        {
            sNoShowCheckboxName_ = gamephase.getString(PARAM_NO_SHOW_NAME, "noshowdialog");
            sNoShowKey_ = gamephase.getString(PARAM_NO_SHOW_KEY, null);
            ApplicationError.assertNotNull(sNoShowKey_, "dialog-no-show-key not defined");

            if (isNoShowSelected())
            {
                bDontShow_ = true;
            }
        }

        // style for this stuff
        STYLE = gamephase_.getString("style", "default");

        if (!bDontShow_)
        {
            createUI();
        }
    }

    /**
     * Can be used by subclass to mimic behavoir of don't show
     * functionality - basically, don't show the UI and activate
     * the button associated with the enter key (the default button).
     * Needs to be called before calling init method.
     */
    protected void setDontShow(boolean bDontShow)
    {
        bDontShow_ = bDontShow;
    }

    /**
     * Return value of dont show
     */
    protected boolean isDontShow()
    {
        return bDontShow_;
    }

    /**
     * Display the dialog without a visible UI.
     */
    protected boolean isFaceless()
    {
        return false;
    }

    /**
     * Background for dialog
     */
    protected DialogBackground back_;

    /**
     * Internal Dialog used for dialog
     */
    private InternalDialog dialog_;

    /**
     * Represents default button (window close)
     */
    protected DDButton cancelButton_;

    /**
     * Represents default button (enter key)
     */
    protected DDButton okayButton_;

    /**
     * Used to handle window closing events, etc.
     */
    protected DialogListener myListener_;


    /**
     * Trigger the default button, as if the user pressed it
     */
    public void triggerDefaultButton()
    {
        GameButton button = null;
        String sEnterButton = gamephase_.getString(ButtonBox.PARAM_DEFAULT_BUTTON, null);
        if (sEnterButton != null)
        {
            List buttons = gamephase_.getList("buttons");
            String sButtonDef;
            for (int i = 0; i < buttons.size(); i++)
            {
                sButtonDef = (String) buttons.get(i);
                if (GameButton.isMatch(sButtonDef, sEnterButton))
                {
                    button = new GameButton(sButtonDef);
                }
            }
            context_.buttonPressed(button, this);
        }
    }

    /**
     * Displays dialog modal/not modal (dialog-modal) prop.
     * Window close associated with default button (dialog-window-close-activates-button) prop
     */
    public void start()
    {
        // if we don't show this, then get figure out the default button
        // (triggered by enter) and call processButton on that
        if (bDontShow_)
        {
            triggerDefaultButton();
            return;
        }

        // for cached phases, if calling start on a dialog already visible,
        // just move it to the front and selected it
        if (dialog_.isVisible())
        {
            dialog_.moveToFrontSelected();
            return;
        }

        // see if we create this but don't show - only check 1st time (creation)
        if (!bCheckCreateNoShow_)
        {
            bCheckCreateNoShow_ = true;
            if (gamephase_.getBoolean(PARAM_CREATE_NO_SHOW, false)) return;
        }

        // "show", faceless or otherwise
        if (isFaceless())
        {
            _showDialog(true);
        }
        else
        {
            _showDialog(false);
        }

        // since already shown, if redisplayed, go back to where it was
        // useful for cached phases
        pos_ = InternalDialog.POSITION_NONE;
    }

    private int pos_ = InternalDialog.POSITION_CENTER;

    /**
     * Show a dialog if not already visible (useful for faceless dialogs)
     */
    protected void showDialog()
    {
        _showDialog(false);
    }

    /**
     * show dialog logic
     */
    private void _showDialog(boolean bFaceless)
    {
        int nLocation = getDialogPosition(dialog_);
        Component c = getFocusComponent();
        dialog_.showDialog(myListener_, nLocation, c, !bFaceless);
    }

    /**
     * Hide a dialog if already visible
     */
    protected void hideDialog()
    {
        dialog_.setVisible(false);
        BaseFrame frame = context_.getFrame();
        ((JComponent) frame.getContentPane()).paintImmediately(0, 0, frame.getWidth(), frame.getHeight());
    }

    /**
     * Get location for dialog.  Can be overriden to position dialog elsewhere.
     * Returns InternalDialog.POSITION_CENTER by default.
     */
    protected int getDialogPosition(InternalDialog dialog)
    {
        return pos_;
    }

    /**
     * Get focus component.  Can be overriden to specify starting focus component.
     * Returns the dialog by default
     */
    protected Component getFocusComponent()
    {
        return null; //  (okayButton_ == null ? (Component) dialog_ : (Component) okayButton_);
    }

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button)
    {
        setResult(button);
        removeDialog();
        return true;
    }

    /**
     * Return dialog used by this phase
     */
    public InternalDialog getDialog()
    {
        return dialog_;
    }

    /**
     * Remove dialog from view
     */
    public void removeDialog()
    {
        if (dialog_ != null)
        {
            dialog_.removeDialog();
        }
    }

    /**
     * Override this to provide contents of dialog
     */
    public abstract JComponent createDialogContents();

    /**
     * create UI to show dice rolls
     */
    public void createUI()
    {
        String sWindowTitle = PropertyConfig.getStringProperty(
                gamephase_.getString(PARAM_WINDOW_TITLE_KEY, PARAM_WINDOW_TITLE_KEY),
                "This Space For Rent"); // no title found so leave funny title
        dialog_ = new InternalDialog(sWindowTitle, sWindowTitle, bModal_);
        dialog_.setDialogOpened(this);
        dialog_.setDialogClosed(this);
        dialog_.getRootPane().setName(sWindowTitle);

        if (isFaceless())
        {
            myListener_ = new DialogListener(this);
        }
        else
        {
            myListener_ = new PhaseDialogListener(this);
        }

        // set baseframe
        BaseFrame frame = context_.getFrame();
        ApplicationError.assertNotNull(frame, "BaseFrame is null");
        dialog_.setBaseFrame(frame);

        ImageIcon winicon = ImageConfig.getImageIcon(gamephase_.getString("dialog-windowtitle-image", "dialog-windowtitle-image"));
        dialog_.setFrameIcon(winicon);
        dialog_.setIconifiable(gamephase_.getBoolean("dialog-iconifiable", false));
        back_ = new DialogBackground(context_, gamephase_, this, bNoShowOption_, sNoShowCheckboxName_);
        dialog_.setContentPane(back_);

        if (bNoShowOption_)
        {
            back_.getNoShowCheckBox().addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            Preferences prefs = Prefs.getUserPrefs(EnginePrefs.NODE_DIALOG_PHASE);
                            prefs.putBoolean(sNoShowKey_, back_.getNoShowCheckBox().isSelected());
                        }
                    });
        }

        ///
        /// Handle Window close / delete events - typically cancel
        ///
        String sCloseButton = gamephase_.getString("dialog-window-close-activates-button", null);
        if (sCloseButton != null && sCloseButton.length() > 0)
        {
            cancelButton_ = getMatchingButton(sCloseButton);
            if (cancelButton_ == null)
            {
                throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                           "dialog-window-close-activates-button not found: " + sCloseButton,
                                           "Make sure this button is defined");
            }
            myListener_.setButton(cancelButton_);

            GuiUtils.InvokeButton hk = new GuiUtils.InvokeButton(cancelButton_);
            // add key action so "delete/backspace" triggers close button
// JDD - leave out for now cuz it prevents backspace in text fields
//           GuiUtils.addKeyAction(back_, JComponent.WHEN_IN_FOCUSED_WINDOW,
//                            "handlebackspace", hk, 
//                            KeyEvent.VK_BACK_SPACE, 0);
            GuiUtils.addKeyAction(back_, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                  "handledelete", hk,
                                  KeyEvent.VK_DELETE, 0);
        }

        ///
        /// Handle ENTER key presses - typically okay
        ///
        String sEnterButton = gamephase_.getString(ButtonBox.PARAM_DEFAULT_BUTTON, null);
        if (sEnterButton != null && sEnterButton.length() > 0)
        {
            okayButton_ = getMatchingButton(sEnterButton);
            if (okayButton_ == null)
            {
                throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                           ButtonBox.PARAM_DEFAULT_BUTTON + " not found: " + sEnterButton,
                                           "Make sure this button is defined");
            }
        }

        // get contents (from subclass)
        // put everything in a nice dialog background
        JComponent contents = createDialogContents();
        back_.setCenterContents(contents);

        // layout
        dialog_.pack(); // needed to get proper layout
    }

    /**
     * Repaint the dialog
     */
    protected void repaint()
    {
        dialog_.pack();
        dialog_.repaint();
    }

    /**
     * Get DDButton from dialog background's button box
     */
    public DDButton getMatchingButton(String sButtonName)
    {
        return back_.getButtonBox().getButton(sButtonName);
    }

    /**
     * Remove named button from dialog
     */
    public void removeMatchingButton(String sButtonName)
    {
        DDButton button = getMatchingButton(sButtonName);
        if (button != null)
        {
            back_.getButtonBox().removeButton(button);
        }
    }

    /**
     * should this dialog be displayed?
     */
    private boolean isNoShowSelected()
    {
        return isDialogHidden(sNoShowKey_);
    }

    /**
     * Return whether the "do not show" checkbox associated with
     * the given key is selected (indicating the dialog won't
     * be displayed)
     */
    public static boolean isDialogHidden(String sKey)
    {
        Preferences prefs = Prefs.getUserPrefs(EnginePrefs.NODE_DIALOG_PHASE);
        return prefs.getBoolean(sKey, false);
    }

    /**
     * Used to activate default button
     */
    private class DialogListener extends InternalFrameAdapter
    {
        DialogPhase phase_;
        DDButton button_;

        public DialogListener(DialogPhase phase)
        {
            phase_ = phase;
        }

        public void setButton(DDButton button)
        {
            button_ = button;
        }

        /**
         * Detect when window close icon is pressed - activate associated button
         */
        public void internalFrameClosing(InternalFrameEvent e)
        {
            if (button_ != null)
            {
                button_.doClick(120);
            }
        }
    }

    /**
     * Used to start/finish the phase
     */
    private class PhaseDialogListener extends DialogListener
    {
        public PhaseDialogListener(DialogPhase phase)
        {
            super(phase);
        }

        /**
         * Catch actual closing so finish() can be called
         */
        public void internalFrameClosed(InternalFrameEvent e)
        {
            phase_.finish();
        }

        /**
         * When window displayed, call this
         */
        public void internalFrameOpened(InternalFrameEvent e)
        {
            phase_.opened();
        }

    }

    /**
     * called when dialog is opened
     */
    protected void opened()
    {
    }

    /**
     * Called when the dialog is added - does initialization
     * for faceless dialogs
     */
    public void dialogAdded(InternalDialog dialog)
    {
        if (isFaceless())
        {
            opened();
        }
    }

    /**
     * Called when the dialog is removed - does cleanup
     * so subclass needs to call it if overriden
     */
    public void dialogRemoved(InternalDialog dialog)
    {
        GuiUtils.requireSwingThread();

        if (isFaceless())
        {
            finish();
        }

        // cleaning dialog because not cached
        if (!gamephase_.isCached())
        {
            dialog_.clean();
        }
    }
}
