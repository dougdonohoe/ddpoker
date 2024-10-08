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
 * TestOnlineConnect.java
 *
 * Created on November 18, 2004, 12:16 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.network.*;
import com.donohoedigital.gui.*;
import com.donohoedigital.udp.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;

/**
 * @author Doug Donohoe
 */
public class TestPublicConnect extends SendMessageDialog implements OnlineMessageListener
{
    static Logger logger = LogManager.getLogger(TestPublicConnect.class);

    public static final String PARAM_URL = "url";

    static final ImageIcon OKAY = ImageConfig.getImageIcon("okay");
    static final ImageIcon FAIL = ImageConfig.getImageIcon("redx");
    static final ImageIcon WAIT = ImageConfig.getImageIcon("waiting");

    private DDLabel server_;

    /**
     * message to send to server
     */
    @Override
    protected EngineMessage getMessage()
    {
        OnlineMessage omsg = new OnlineMessage(OnlineMessage.CAT_TEST);
        PokerURL url = (PokerURL) gamephase_.getObject(PARAM_URL);
        omsg.setGUID(engine_.getGUID());
        omsg.setConnectURL(url);
        PokerGame game = (PokerGame) context_.getGame();
        if (game.getOnlineManager().isUDP())
        {
            UDPServer udp = ((UDPServer) game.getOnlineManager().getP2P());
            omsg.setUPDID(udp.getID(udp.getIP(udp.getDefaultChannel())));
        }
        EngineMessage msg = new EngineMessage();
        omsg.getData().copyTo(msg);
        return msg;
    }

    /**
     * Message to display to user
     */
    @Override
    protected String getMessageKey()
    {
        return "msg.testPublicConnect";
    }

    /**
     * Don't do server redirect query
     */
    @Override
    protected boolean doServerQuery()
    {
        return false;
    }

    @Override
    public JComponent createDialogContents()
    {
        JComponent ret = super.createDialogContents();

        // add our server status
        DDPanel status = new DDPanel();//DDPanel.CENTER();
        status.setBorder(BorderFactory.createEmptyBorder(0, 30, 5, 0));
        server_ = new DDLabel("servertest", STYLE);
        status.add(server_);
        server_.setIcon(WAIT);
        server_.setIconTextGap(10);
        server_.setHorizontalAlignment(SwingConstants.LEFT);
        bottom_.add(status, BorderLayout.SOUTH);


        return ret;
    }

    /**
     * start - set listener
     */
    @Override
    public void start()
    {
        PokerGame game = (PokerGame) context_.getGame();
        game.getOnlineManager().addOnlineMessageListener(this);
        super.start();
    }

    /**
     * finish - remove listener
     */
    @Override
    public void finish()
    {
        PokerGame game = (PokerGame) context_.getGame();
        game.getOnlineManager().removeOnlineMessageListener(this);
        super.finish();
    }

    /**
     * override so we don't auto-close
     */
    @Override
    protected boolean isAutoClose()
    {
        return false;
    }

    /**
     * Listen for receipt of message
     */
    public void messageReceived(OnlineMessage omsg)
    {
        if (omsg.getCategory() != OnlineMessage.CAT_TEST) return;

        String sGuid = omsg.getGUID();
        if (engine_.getGUID().equals(sGuid))
        {
            setStatusText(PropertyConfig.getMessage("msg.p2p.test.msgrcvd"));
            updateIcon(OKAY, "serverokay");
        }
        else
        {
            logger.warn("Received test message with different GUID: " + omsg);
        }
    }

    /**
     * Update icon on server status label
     */
    private void updateIcon(final ImageIcon icon, final String sName)
    {
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    ImageIcon _icon = icon;
                    String _sName = sName;

                    public void run()
                    {
                        server_.reinit(_sName, STYLE);
                        server_.setIcon(_icon);
                    }
                }
        );
    }

    /**
     * Override to change done step message
     */
    @Override
    public void updateStep(int nStep)
    {
        if (nStep == DDMessageListener.STEP_DONE)
        {
            setStatusText(PropertyConfig.getMessage("msg.p2p.test.done"));
        }
        else super.updateStep(nStep);
    }

    /**
     * Override to set icon upon failure
     */
    @Override
    protected void displayError(int nStatus)
    {
        updateIcon(FAIL, "serverfail");
        super.displayError(nStatus);
    }
}
