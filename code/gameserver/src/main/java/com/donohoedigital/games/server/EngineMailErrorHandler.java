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
 * EngineMailErrorHandler.java
 *
 * Created on March 13, 2003, 10:49 AM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.jsp.StringHttpServletRequest;
import com.donohoedigital.mail.DDMailErrorHandler;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 *
 * @author  donohoe
 */
public class EngineMailErrorHandler implements DDMailErrorHandler
{
    //static Logger logger = LogManager.getLogger(EngineMailErrorHandler.class);
    
    private EngineServlet servlet_;
    /** 
     * Creates a new instance of EngineMailErrorHandler 
     */
    public EngineMailErrorHandler(EngineServlet servlet)
    {
        servlet_ = servlet;
    }
    
    public void handleError(String sAppName, String sErrorDetails) throws IOException
    {
        EngineMailErrorInfo info = new EngineMailErrorInfo(sAppName, sErrorDetails);
        EngineMessage msg = new EngineMessage(info.getGameID(), EngineMessage.PLAYER_SERVER, 
                                              EngineMessage.CAT_ERROR_BAD_EMAIL, (String)null);
        msg.setInteger(EngineMessage.PARAM_REF_PLAYER_ID, info.getPlayerID());
        msg.setString(EngineMessage.PARAM_EMAIL_ID, info.getEmailID());

        HttpServletRequest request = new StringHttpServletRequest();
        
        servlet_.processMessage(request, null, msg);
    }
    
}
