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
 * EngineMessenger.java
 *
 * Created on March 6, 2003, 7:24 PM
 */

package com.donohoedigital.games.comms;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.comms.DDMessenger;
import com.donohoedigital.config.PropertyConfig;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author  Doug Donohoe
 */
public class EngineMessenger extends DDMessenger
{
    //static Logger logger = Logger.getLogger(EngineMessenger.class);
    
    /**
     * Default instance to use for messaging
     */
    private static final EngineMessenger EMESSENGER;

    // Create sole engine messenger
    static {
        EMESSENGER = new EngineMessenger();
    }

    // url we send to
    private final String serverurl_;
    
    /**
     * Create the messenger, get url
     */
    protected EngineMessenger()
    {
        serverurl_ = PropertyConfig.getStringProperty("settings.online.server", "missing.ddpoker.com", false);
    }
    
    /**
     * Get URL to connect to for sending this message
     * and set version in message
     */
    private final StringBuilder sb_ = new StringBuilder(100); // reuse
    private URL getServerUrl(String sURL, EngineMessage msg)
    {
        URL url;
        sb_.setLength(0);
        try {
            if (sURL == null) sURL = serverurl_;
            sb_.append(sURL);
            sb_.append(msg.getGameID());
            sb_.append("/v");
            sb_.append(msg.getVersion());
            sb_.append("/cat.");
            sb_.append(msg.getCategory());
            sb_.append("/key.");
            sb_.append(msg.getKey());
            url = new URL(sb_.toString());
        }
        catch (MalformedURLException me)
        {
            throw new ApplicationError(me);
        }
        return url;
    }
    
    /**
     * Send engine message and receive one in return
     */
    public EngineMessage sendEngineMessage(String url, EngineMessage send, DDMessageListener listener)
    {
        return (EngineMessage) super.sendMessage(getServerUrl(url, send), send, listener);
    }
    
    /**
     * Static version of sendEngineMethod (note case difference) - uses
     * EMESSENGER
     */
    public static EngineMessage SendEngineMessage(String url, EngineMessage send, DDMessageListener listener)
    {
        return EMESSENGER.sendEngineMessage(url, send, listener);
    }
    
    /**
     * Send enginer message and receive one in return (via DDMessageListener)
     */
    public void sendEngineMessageAsync(String url, EngineMessage send, DDMessageListener listener)
    {
        super.sendMessageAsync(getServerUrl(url, send), send, listener);
    }
    
    /**
     * Static version of sendEngineMethod (note case difference) - uses
     * EMESSENGER
     */
    public static void SendEngineMessageAsync(String url, EngineMessage send, DDMessageListener listener)
    {
        EMESSENGER.sendEngineMessageAsync(url, send, listener);
    }
    
    /**
     * Overridden to return EngineMessage
     */
    @Override
    public DDMessage createNewMessage()
    {
        return new EngineMessage();
    }
}
