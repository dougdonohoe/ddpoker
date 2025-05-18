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
 * ProxyServlet.java
 *
 * Created on March 6, 2003, 6:23 PM
 */

package com.donohoedigital.tools;


import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.comms.DDMessenger;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.server.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 *
 * @author  donohoe
 */
public class ProxyServlet extends BaseServlet
{
    private static String[] steps_ = null;
    
    /**
     * init from gameserver
     */
    @Override
    public void afterConfigInit()
    {
        super.afterConfigInit();
        setDDMessageHandler(false);
        DDMessenger.setUSERAGENT("DD Proxy Server/1.0");
        setSteps(new String[] {
                 PropertyConfig.getMessage("msg.ddmessagelistener." + DDMessageListener.STEP_CONNECTING),
                 PropertyConfig.getMessage("msg.ddmessagelistener." + DDMessageListener.STEP_SENDING),
                 PropertyConfig.getMessage("msg.ddmessagelistener." + DDMessageListener.STEP_WAITING_FOR_REPLY),
                 PropertyConfig.getMessage("msg.ddmessagelistener." + DDMessageListener.STEP_RECEIVING),
                 PropertyConfig.getMessage("msg.ddmessagelistener." + DDMessageListener.STEP_DONE)
               });
    }
    
    /**
     * Setting DD Message Handler to false (above) means this is not called.
     * But we have to implement it anyhow.  Data handled in ProxySocketThread.
     */
    public DDMessage processMessage(HttpServletRequest request, HttpServletResponse response, 
                            DDMessage ddreceived) throws IOException 
    {
        return null;
    }

    static String[] getSteps()
    {
        return steps_;
    }

    private static void setSteps(String[] steps)
    {
        steps_ = steps;
    }
}
