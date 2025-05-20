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
 * MessageErrorDialog.java
 *
 * Created on June 24, 2003, 8:45 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;

import javax.swing.*;

/**
 *
 * @author  donohoe
 */
public class MessageErrorDialog extends SendMessageDialog 
{
    public static final String PARAM_MESSAGE = "Message";
    
    /**
     * Create UI
     */
    public JComponent createDialogContents()
    {
        JComponent comp = super.createDialogContents();
        
        // set title
        String sTitleKey = gamephase_.getString("dialog-windowtitle-prop");
        getDialog().setTitle(PropertyConfig.getMessage(sTitleKey, context_.getGame().getOnlineGameID()));
        nSleep_ = 500; // sleep longer when resume on success
        return comp;
    }
    
    /** 
     * no message here to send (only displaying error message)
     */
    protected EngineMessage getMessage() 
    {
        return null;
    }
    
    /**
     * key for status title
     */
    protected String getMessageKey()
    {
        return "msg.errorOnline";
    }
    
    /**
     * override to not touch okay buttons
     * and to call messageReceived with passed in EngineMessage that had error
     */
    protected void localStart()
    {
        messageReceived((DDMessage)gamephase_.getObject(PARAM_MESSAGE));
    }
    
    /**
     * Override to not send message, but show error
     */
    protected void opened()
    {
        // do nothing
    }
    
    /**
     * when the return message is received, this is called
     */
    public void messageReceived(DDMessage message) 
    {
        int nStatus = message.getStatus();
        
        if (nStatus == DDMessageListener.STATUS_OK)
        {
            _setStatusText(PropertyConfig.getMessage("msg.resume"));
        }
        super.messageReceived(message);
    }
}
