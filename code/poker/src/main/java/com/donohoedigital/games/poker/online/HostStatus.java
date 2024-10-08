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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: May 29, 2005
 * Time: 1:14:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostStatus extends DDPanel implements HostConnectionListener, Runnable
{
    static Logger logger = LogManager.getLogger(HostStatus.class);

    // LEDs
    private static ImageIcon REDLED = ImageConfig.getImageIcon("led-red");
    private static ImageIcon GREENLED = ImageConfig.getImageIcon("led-green");
    private static ImageIcon YELLOWLED = ImageConfig.getImageIcon("led-yellow");

    // members
    private PokerGame game_;
    private OnlineManager mgr_;
    private PokerPlayer host_;
    private PokerPlayer local_;
    private boolean bConnected_ = false;
    private boolean bInGame_;

    // ui
    private DDLabel status_;
    private DDCheckBox details_;
    private PokerErrorDialog error_;

    /**
     * Create status panel
     */
    public HostStatus(PokerGame game, String STYLE, boolean bInGame)
    {
        bInGame_ = bInGame;
        game_ = game;
        host_ = game_.getHost();
        local_ = game_.getLocalPlayer();
        mgr_ = game_.getOnlineManager();
        mgr_.setHostConnectionListener(this);

        DDPanel base = new DDPanel();
        base.setBorderLayoutGap(0, 10);
        add(GuiUtils.WEST(base));

        status_ = new DDLabel("hostconnect", STYLE);
        status_.setIconTextGap(8);
        status_.setIcon(GREENLED); // set an LED for sizing
        base.add(status_, BorderLayout.CENTER);

        details_ = new DDCheckBox("showdetails2", STYLE);
        details_.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!details_.isSelected())
                {
                    if (error_ != null)
                    {
                        error_.removeDialog();
                    }
                }
            }
        });
        base.add(details_, bInGame ? BorderLayout.SOUTH : BorderLayout.EAST);
        if (!bInGame) status_.setPreferredHeight(details_.getPreferredSize().height);
        updateStatus(true);
    }

    /**
     * Called when host connection lost
     */
    public void hostConnectionLost()
    {
        logger.debug("Connection to host lost, reconnect process initiated...");
        clearCards();
        updateStatusInSwing();
    }

    /**
     * if in-game, clear cards and results from display
     * so no repaint errors occur on reload
     */
    private void clearCards()
    {
        if (PokerUtils.getPokerGameboard() != null)
        {
            GuiUtils.invoke(new Runnable() {
                public void run()
                {
                    game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
                    PokerUtils.clearCards(true);
                    PokerUtils.clearResults(game_.getGameContext(), true);
                }
            });
        }
    }

    /**
     * update status in swing loop
     */
    private void updateStatusInSwing()
    {
        GuiUtils.invoke(new Runnable() {
            public void run()
            {
                updateStatus(false);
            }
        });
    }

    /**
     * update UI
     */
    private void updateStatus(boolean bInit)
    {
        boolean bConnected = host_.getConnection() != null;
        if (bConnected != bConnected_ || bInit)
        {
            bConnected_ = bConnected;
            if (bConnected_)
            {
                status_.setIcon(GREENLED);
                status_.setText(PropertyConfig.getMessage("msg.host.connected"));
                details_.setVisible(false);
            }
            else
            {
                status_.setIcon(REDLED);
                status_.setText(PropertyConfig.getMessage("msg.host.disconnected"));
                details_.setVisible(true);
                details_.setSelected(true);
                reconnect();
            }
        }
    }

    // reconnect stuff
    private boolean bAbort_ = false;
    private Thread reconnect_;
    private boolean bReconnected_;
    private int nAttempts_ = 0;

    /**
     * reconnect logic
     */
    private void reconnect()
    {
        if (reconnect_ == null)
        {
            reconnect_ = new Thread(this, "Reconnect");
            reconnect_.start();
        }
    }

    /**
     * cleanup
     */
    public void finish()
    {
        mgr_.setHostConnectionListener(null);
        bAbort_ = true;
        if (error_ != null) error_.removeDialog();
        error_ = null;
    }

    /**
     * runnable interface - reconnect loop
     */
    public void run()
    {
        bReconnected_ = false;
        nAttempts_ = 0;
        while (!bReconnected_ && !bAbort_)
        {
            if (nAttempts_ == 0)
            {
                GuiUtils.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        DDMessage init = new DDMessage();
                        init.setStatus(DDMessageListener.STATUS_APPL_ERROR);
                        init.setApplicationErrorMessage(PropertyConfig.getMessage(
                                bInGame_ ? "msg.msgerror.disconnect.p2p.game" : "msg.msgerror.disconnect.p2p.lobby"));
                        handleError(init, false);
                    }
                });
            }
            Utils.sleepMillis(nAttempts_ == 0 ? 1000 : 5000);

            // if game marked over (due to host cancel), set abort flag
            if (game_.getOnlineMode() == PokerGame.MODE_CANCELLED) bAbort_ = true;

            // if aborting, continue
            if (bAbort_) continue;

            GuiUtils.invokeAndWait(new Runnable() {
                public void run()
                {
                    if (game_.getOnlineMode() == PokerGame.MODE_CANCELLED) bAbort_ = true;

                    if (bAbort_) return;
                    status_.setIcon(YELLOWLED);
                    logger.debug("Attempting to reconnect...");
                    Object o = mgr_.joinGame(local_.isObserver(), true, false);
                    nAttempts_++;
                    if (o == Boolean.TRUE)
                    {
                        DDMessage reconnect = new DDMessage();
                        reconnect.setStatus(DDMessageListener.STATUS_OK);
                        handleError(reconnect, false);
                        bReconnected_ = true;
                    }
                    else
                    {
                        status_.setIcon(REDLED);
                        if (o instanceof DDMessage) handleError((DDMessage) o, true);
                    }
                }
            });
        }
        reconnect_ = null;
        updateStatusInSwing();
    }

    /**
     * show error
     */
    private void handleError(DDMessage msg, boolean bLog)
    {
        // log a message
        int nStatus = msg.getStatus();
        String sLog = null;
        switch(nStatus)
        {
            case DDMessageListener.STATUS_OK:
                sLog = null;
                logger.info("Reconnect succeeded.");
                break;

            case DDMessageListener.STATUS_CONNECT_FAILED:
                sLog = "connect failed";
                break;

            case DDMessageListener.STATUS_TIMEOUT:
                sLog = "timeout";
                break;

            case DDMessageListener.STATUS_SERVER_ERROR:
                sLog = "server error";
                break;

            case DDMessageListener.STATUS_UNKNOWN_HOST:
                sLog = "unknown host";
                break;

           case DDMessageListener.STATUS_UNKNOWN_ERROR:
                sLog = "unknown error";
                break;

           case DDMessageListener.STATUS_DNS_TIMEOUT:
                sLog = "dns timeout";
                break;

           case DDMessageListener.STATUS_APPL_ERROR:
                sLog = msg.getApplicationErrorMessage();
                break;

            default:
                sLog = "unknown status ("+ nStatus+ ')';
        }
        if (bLog && sLog != null) logger.error("Reconnected failed: " + sLog);

        // show to user if details checkbox selected and not aborting
        if (!details_.isSelected() || bAbort_) return;
        PokerURL url = local_.getConnectURL();
        TypedHashMap params = new TypedHashMap();
        params.setObject(MessageErrorDialog.PARAM_MESSAGE, msg);
        params.setObject(MessageErrorDialog.PARAM_ERROR_KEY_MOD, PokerP2PDialog.P2P_ERROR_MSG_MODIFIER);
        params.setString(PokerP2PDialog.PARAM_DISPLAY, PropertyConfig.getMessage("msg.reconnect", url));
        params.setObject(PokerErrorDialog.PARAM_URL, url);
        params.setInteger(PokerErrorDialog.PARAM_ATTEMPT, nAttempts_);
        error_ = (PokerErrorDialog) game_.getGameContext().processPhaseNow("PokerErrorDialog", params);
    }
}
