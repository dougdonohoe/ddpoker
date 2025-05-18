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
/*
 * SendMessageDialog.java
 *
 * Created on March 18, 2003, 6:28 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.config.ImageConfig;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.games.config.GamePhase;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Doug Donohoe
 */
public abstract class SendMessageDialog extends DialogPhase implements DDMessageListener, ActionListener
{
    private static Logger sLogger = LogManager.getLogger(SendMessageDialog.class);

    // params
    public static final String PARAM_SLEEP_MILLIS = "sleep";
    public static final String PARAM_ERROR_KEY_MOD = "errormod";
    public static final String PARAM_IGNORE_INET_ERRORS = "ignoreinet";
    public static final String PARAM_FACELESS = "faceless";
    public static final String PARAM_FACELESS_ERROR = "facelesserror";

    // data
    protected DDMessage mReturn_;
    protected int nStatus_;

    // ui
    protected DDPanel bottom_;
    private JScrollPane scroll_;
    private DDPanel base_;
    private DDHtmlArea status_;
    private JScrollPane statusScroll_;
    private DDTextArea details_;
    private DDCheckBox checkbox_;
    protected int nHeightAdjust_ = 0;
    protected int nSleep_ = 300;
    private boolean bIgnoreInetErrors_ = false;
    private Boolean faceless_ = null;
    private boolean facelessError_;

    // don't use static because this isn't used frequently enough to keep
    // the memory around
    private String[] steps_;
    private String[] errors_;

    /**
     * Retrieve client info then do normal initialization
     */
    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase)
    {
        faceless_ = gamephase.getBoolean(PARAM_FACELESS);
        facelessError_ = gamephase.getBoolean(PARAM_FACELESS_ERROR, true);

        // Do not sleep when faceless to ensure a quick transition to the next UI state
        if (isFaceless())
        {
            gamephase.setInteger(PARAM_SLEEP_MILLIS, 0);
        }

        super.init(engine, context, gamephase);
    }

    /**
     * Optionally send message without showing the server connection status
     */
    @Override
    protected boolean isFaceless()
    {
        return faceless_ != null && faceless_;
    }

    /**
     * Set dialog non-closable
     */
    @Override
    public void start()
    {
        bIgnoreInetErrors_ = gamephase_.getBoolean(PARAM_IGNORE_INET_ERRORS, false);
        String MOD = getErrorMod();
        String port = "" + getPort();
        steps_ = new String[]{PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_CONNECTING),
                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_SENDING),
                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_WAITING_FOR_REPLY),
                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_RECEIVING),
                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_DONE)
        };


        errors_ = new String[]{PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_CONNECT_FAILED + MOD, port),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_TIMEOUT + MOD, port),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_SERVER_ERROR + MOD, port),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_UNKNOWN_HOST + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_UNKNOWN_ERROR + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_DNS_TIMEOUT + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_APPL_ERROR),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_DISABLED)
        };

        // get sleep amount
        nSleep_ = gamephase_.getInteger(PARAM_SLEEP_MILLIS, nSleep_);
        // can't close this
        getDialog().setClosable(false);

        // do local setup at start (overridable)
        localStart();

        // go go go
        super.start();
    }

    /**
     * Get mod string for error messages.  Default is to get
     * the PARAM_ERROR_KEY_MOD from gamephase (if missing, "" is used)
     */
    protected String getErrorMod()
    {
        return gamephase_.getString(PARAM_ERROR_KEY_MOD, "");
    }

    /**
     * Get the port used for out-bound connections.  Used for param 0 for
     * error messages.  Not used in base class.
     */
    protected int getPort()
    {
        return 0;
    }

    /**
     * Setup okay buttons
     */
    protected void localStart()
    {
        // okay button not needed until done
        okayButton_.setEnabled(false);
        okayButton_.setVisible(false);
    }

    boolean bDoingServerQuery_ = false;

    /**
     * When window is opened, send the message
     */
    @Override
    protected void opened()
    {
        if (doServerQuery())
        {
            EngineMessage msg = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                                  EngineMessage.PLAYER_NOTDEFINED,
                                                  EngineMessage.CAT_SERVER_QUERY);
            bDoingServerQuery_ = true;
            sendMessage(null, msg);
        }
        else
        {
            sendMessage(null);
        }
    }

    /**
     * Send the message to the given URL, or if null, use the default destination
     */
    protected void sendMessage(String sURL)
    {
        sendMessage(sURL, getMessage());
    }

    /**
     * Send message logic
     */
    private void sendMessage(String sURL, EngineMessage msg)
    {
        if (doSynchronousSend())
        {
            messageReceived(GameMessenger.SendEngineMessage(sURL, msg, null));
        }
        else
        {
            GameMessenger.SendEngineMessageAsync(sURL, msg, this);
        }
    }

    /**
     * subclass should override to true if we should query
     * server for new URL
     */
    protected boolean doServerQuery()
    {
        return false;
    }

    /**
     * subclass should override to true if we should make
     * synchronous requests
     */
    protected boolean doSynchronousSend()
    {
        return isFaceless();
    }

    /**
     * subclass should return the message being sent
     */
    protected abstract EngineMessage getMessage();

    /**
     * Return a key to lookup in properties for display
     */
    protected String getMessageKey()
    {
        return "No message specified!";
    }

    /**
     * Return a message to display (takes precedence over getMessageKey)
     */
    protected String getMessageString()
    {
        return null;
    }

    /**
     * create dialog
     */
    @Override
    public JComponent createDialogContents()
    {
        bottom_ = new DDPanel();
        base_ = new DDPanel();
        bottom_.add(base_, BorderLayout.CENTER);
        base_.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        DDLabel label = new DDLabel(GuiManager.DEFAULT, STYLE);
        label.setIcon(ImageConfig.getImageIcon("serverconnect"));
        base_.add(label, BorderLayout.NORTH);

        String sMsg = getMessageString();
        if (sMsg == null)
        {
            sMsg = PropertyConfig.getMessage(getMessageKey());
        }
        label.setText(sMsg);

        // status area
        status_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        status_.addHyperlinkListener(GuiUtils.HYPERLINK_HANDLER);
        status_.setBorder(BorderFactory.createEmptyBorder());
        statusScroll_ = new DDScrollPane(status_, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        statusScroll_.setOpaque(false);
        statusScroll_.getVerticalScrollBar().setBlockIncrement(100);
        statusScroll_.getVerticalScrollBar().setUnitIncrement(10);
        Dimension size = new Dimension(100, 165);
        statusScroll_.setPreferredSize(size);

        DDPanel holder = new DDPanel();
        holder.setBorder(EngineUtils.getStandardMenuTextBorder());
        holder.add(statusScroll_, BorderLayout.CENTER);
        base_.add(holder, BorderLayout.CENTER);

        // checkbox
        checkbox_ = new DDCheckBox("showdetails", STYLE);
        checkbox_.addActionListener(this);
        checkbox_.setVisible(false);
        base_.add(checkbox_, BorderLayout.EAST);

        // details
        details_ = new DDTextArea(GuiManager.DEFAULT, "ErrorDetails");
        details_.setDisplayOnly(true);
        details_.setFocusable(true);
        details_.setWrapStyleWord(false);
        details_.setLineWrap(false);
        int rows = 10;
        details_.setRows(rows);
        details_.setBorder(EngineUtils.getStandardMenuTextBorder());

        scroll_ = new DDScrollPane(details_, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll_.setOpaque(false);
        scroll_.getVerticalScrollBar().setBlockIncrement(100);
        scroll_.getVerticalScrollBar().setUnitIncrement(10);
        nHeightAdjust_ = details_.getPreferredSize().height;
        scroll_.setPreferredSize(new Dimension(size.width + checkbox_.getPreferredSize().width, nHeightAdjust_));

        return bottom_;
    }

    /**
     * Overriden to move up to accomodate possible error details
     */
    @Override
    protected int getDialogPosition(InternalDialog dialog)
    {
        dialog.setCenterAdjust(0, -(nHeightAdjust_ / 2));
        return InternalDialog.POSITION_CENTER_ADJUST;
    }

    /**
     * Change status message to reflect the new step
     */
    public void updateStep(int nStep)
    {
        setStatusText(steps_[nStep]);
    }

    /**
     * Do nothing
     */
    public void debugStep(int nStep, String sMsg)
    {
    }

    /**
     * when the return message is received, this is called
     */
    public void messageReceived(DDMessage message)
    {
        //message.debugPrint();
        mReturn_ = message;
        nStatus_ = message.getStatus();

        // BUG 294 - silently fail if ignoring internet errors
        // BUG 294 - meaning we only fail on application error
        if (bIgnoreInetErrors_ &&
            nStatus_ != DDMessageListener.STATUS_OK &&
            nStatus_ != DDMessageListener.STATUS_APPL_ERROR)
        {
            sLogger.warn("Ignoring error [" + nStatus_ + "]: " + errors_[nStatus_]);
            if (nStatus_ == DDMessageListener.STATUS_SERVER_ERROR ||
                nStatus_ == DDMessageListener.STATUS_UNKNOWN_ERROR)
            {
                String sMsg = getErrorMessageDetails();

                if (sMsg != null)
                {
                    sLogger.warn("Details: " + sMsg);
                }
            }
            updateStep(DDMessageListener.STEP_DONE);
            nStatus_ = DDMessageListener.STATUS_OK;
        }

        // if status good, remove dialog
        if (nStatus_ == DDMessageListener.STATUS_OK)
        {
            // if doing server query, process answer and return
            if (bDoingServerQuery_)
            {
                bDoingServerQuery_ = false;

                // update settings
                String sURL = mReturn_.getString(EngineMessage.PARAM_URL, null);

                // send original message (to new URL)
                sendMessage(sURL);
                return;
            }

            if (isAutoClose() || isFaceless())
            {
                // otherwise, message has been received
                // sleep in sep thread before generating remove dialog event
                Thread tWait = new Thread(
                        new Runnable()
                        {
                            public void run()
                            {
                                Utils.sleepMillis(nSleep_);
                                // need to invoke later so happens from swing thread
                                SwingUtilities.invokeLater(
                                        new Runnable()
                                        {
                                            public void run()
                                            {
                                                removeDialog();
                                            }
                                        }
                                );
                            }
                        }, "SendMessageDialog"
                );
                tWait.start();
            }
            else
            {
                // optionally display a status message from the server
                String sMsg = mReturn_.getApplicationStatusMessage();
                if (sMsg != null)
                {
                    setStatusText(sMsg);
                }

                showOkay();
            }
        }
        // else display error
        else
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        private int nStatus = nStatus_;

                        public void run()
                        {
                            displayError(nStatus);
                        }
                    }
            );
        }
    }

    /**
     * close automatically upon success?  Default is true.  Override to
     * return false, in which case OK button is set visible
     */
    protected boolean isAutoClose()
    {
        return true;
    }

    /**
     * is error displayed when faceless
     */
    protected boolean isFacelessError()
    {
        return facelessError_;
    }

    /**
     * Display an error message
     */
    protected void displayError(int nStatus)
    {
        // if faceless, make sure the dialog is visible
        if (isFaceless())
        {
            if (isFacelessError())
            {
                showDialog();
            }
            else
            {
                removeDialog();
                return;
            }
        }

        // if an application error, display the message sent up
        if (nStatus == DDMessageListener.STATUS_APPL_ERROR)
        {
            String sErrorMsg = mReturn_.getApplicationErrorMessage();
            if (sErrorMsg == null)
            {
                sErrorMsg = errors_[nStatus];
            }
            _setStatusText(sErrorMsg);

            // banned keys
            if (mReturn_.getBoolean(EngineMessage.PARAM_BANNED_KEY, false))
            {
                engine_.banLicenseKey();
            }

            // bad key
            if (mReturn_.getBoolean(EngineMessage.PARAM_BAD_KEY, false))
            {
                engine_.resetLicenseKey();
            }
        }
        // else a standard error, display the corresponding message
        else
        {
            _setStatusText(errors_[nStatus]);
        }

        showOkay();

        // show message details for unexpected errors
        if (nStatus == DDMessageListener.STATUS_SERVER_ERROR ||
            nStatus == DDMessageListener.STATUS_UNKNOWN_ERROR)
        {
            String sMsg = getErrorMessageDetails();

            if (sMsg != null)
            {
                checkbox_.setVisible(true);
                details_.setText(sMsg);
                base_.revalidate();
            }
        }
        else
        {
            if (checkbox_.isVisible())
            {
                checkbox_.setVisible(false);
                // make sure details are hidden
                if (checkbox_.isSelected())
                {
                    checkbox_.setSelected(false);
                    actionPerformed(null);
                }
                base_.revalidate();
            }
        }
    }

    /**
     * show okay button
     */
    private void showOkay()
    {
        okayButton_.setEnabled(true);
        okayButton_.setVisible(true);
    }

    /**
     * Get message text
     */
    private String getErrorMessageDetails()
    {
        String sMsg = null;
        String sException = mReturn_.getException();
        String sDDException = mReturn_.getDDException();
        if (sDDException != null)
        {
            sMsg = PropertyConfig.getMessage("msg.dderrordetails",
                                             sDDException,
                                             sException,
                                             mReturn_.getDataAsString());
        }
        else if (sException != null)
        {
            sMsg = PropertyConfig.getMessage("msg.errordetails",
                                             sException,
                                             mReturn_.getDataAsString());
        }
        return sMsg;
    }

    /**
     * Set message in status window
     */
    protected void setStatusText(final String sText)
    {
        // need to invoke later so happens from swing thread
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    String _sText = sText;

                    public void run()
                    {
                        _setStatusText(_sText);
                        status_.paintImmediately(status_.getX(), status_.getY(), status_.getWidth(), status_.getHeight());
                    }
                }
        );
    }

    /**
     * handle scrolling
     */
    private static Point ptop = new Point(0, 0);

    protected void _setStatusText(String sText)
    {
        status_.setText(sText);

        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        statusScroll_.getViewport().setViewPosition(ptop);
                    }
                }
        );
    }

    /**
     * Get status of return
     */
    public int getStatus()
    {
        return nStatus_;
    }

    /**
     * Get return message
     */
    public EngineMessage getReturnMessage()
    {
        return (EngineMessage) mReturn_;
    }

    /**
     * checkbox - show details
     */
    public void actionPerformed(ActionEvent e)
    {
        InternalDialog dialog = getDialog();
        if (checkbox_.isSelected())
        {
            base_.add(scroll_, BorderLayout.SOUTH);
            dialog.setSize(dialog.getWidth(), dialog.getHeight() + nHeightAdjust_);
        }
        else
        {
            base_.remove(scroll_);
            dialog.setSize(dialog.getWidth(), dialog.getHeight() - nHeightAdjust_);
        }
        base_.revalidate();
    }
}
