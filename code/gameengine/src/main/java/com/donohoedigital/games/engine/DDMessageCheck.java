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

import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.games.comms.EngineMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.SwingWorker;
import java.util.function.Consumer;

/**
 * Asks the DD Poker server whether there is a message for the user.  The request runs on a
 * {@link SwingWorker} so startup never waits on it - when the server is down, the connect
 * timeout would otherwise freeze the start menu.  Failures are silent.
 */
public final class DDMessageCheck
{
    private static final Logger logger = LogManager.getLogger(DDMessageCheck.class);

    private DDMessageCheck()
    {
    }

    /**
     * Send the check off the EDT and hand the reply back on it - null if the request failed for
     * any reason.
     */
    public static void checkUpdateAsync(long last, String playerProfileName, Consumer<EngineMessage> onResult)
    {
        EngineMessage msg = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                              EngineMessage.PLAYER_NOTDEFINED,
                                              EngineMessage.CAT_CHECK_DDMSG);

        msg.setString(EngineMessage.PARAM_PATCH_OS, Utils.OS);
        msg.setLong(EngineMessage.PARAM_DDMSG_ID, last);
        msg.setString(EngineMessage.PARAM_DDPROFILE, playerProfileName);

        new SwingWorker<EngineMessage, Void>()
        {
            @Override
            protected EngineMessage doInBackground()
            {
                EngineMessage reply = GameMessenger.SendEngineMessage(null, msg, null);
                return (reply != null && reply.getStatus() == DDMessageListener.STATUS_OK) ? reply : null;
            }

            @Override
            protected void done()
            {
                EngineMessage reply;
                try
                {
                    reply = get();
                }
                catch (Exception e)
                {
                    logger.debug("message check failed: {}", e.toString());
                    reply = null;
                }
                onResult.accept(reply);
            }
        }.execute();
    }
}
