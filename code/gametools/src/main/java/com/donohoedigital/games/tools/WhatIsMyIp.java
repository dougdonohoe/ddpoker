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
 * OnlineTest.java
 *
 * Created on April 5, 2004, 8:33 AM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import org.apache.logging.log4j.*;

/**
 * @author donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class WhatIsMyIp implements DDMessageListener
{

    static Logger logger = LogManager.getLogger(WhatIsMyIp.class);

    // display
    private String[] errors_;

    /**
     * Init
     */
    private void init()
    {
        DDMessage.setDefaultRealKey("2100-0005-5596-3554");
        DDMessage.setDefaultVersion(new Version(1, 2, 0, true));
        String MOD = "";

        errors_ = new String[]{PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_CONNECT_FAILED + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_TIMEOUT + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_SERVER_ERROR + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_UNKNOWN_HOST + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_UNKNOWN_ERROR + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_DNS_TIMEOUT + MOD),
                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_APPL_ERROR)
        };
    }

    public WhatIsMyIp()
    {
        init();
    }

    public void go()
    {
        EngineMessage msg = getMessage();
        EngineMessenger.SendEngineMessage(null, msg, this);
    }

    /**
     * return message for registering online game
     */
    private EngineMessage getMessage()
    {
        return new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                 EngineMessage.PLAYER_NOTDEFINED,
                                 EngineMessage.CAT_PUBLIC_IP);
    }

    public static void main(String args[])
    {

        new ConfigManager("poker", ApplicationType.COMMAND_LINE, false);
        WhatIsMyIp test = new WhatIsMyIp();
        test.go();
    }


    public void debugStep(int nStep, String sMsg)
    {

    }

    /**
     * Called when a message has been received
     */
    public void messageReceived(DDMessage message)
    {
        int nStatus = message.getStatus();

        if (nStatus == DDMessageListener.STATUS_OK)
        {
            System.out.println(message.getString(EngineMessage.PARAM_IP));
        }
        else
        {
            String appmsg = message.getApplicationErrorMessage();
            System.out.println("*** ERROR: " + errors_[nStatus] + (appmsg == null ? "" : (" - " + appmsg)));
        }
    }

    /**
     * Called at various times during the connection so the UI can update the
     * status
     */
    public void updateStep(int nStep)
    {
    }

}