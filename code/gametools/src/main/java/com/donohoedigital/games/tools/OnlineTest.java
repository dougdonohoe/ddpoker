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
 * OnlineTest.java
 *
 * Created on April 5, 2004, 8:33 AM
 */

package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import org.apache.log4j.*;
import com.donohoedigital.p2p.*;

/**
 *
 * @author  donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class OnlineTest implements DDMessageListener
{
 
    static Logger logger = Logger.getLogger(OnlineTest.class);

    // display
    private String[] steps_;    
    private String[] errors_;
    
    // options
    private boolean bTestDDMessenger_ = false;
    private boolean bTestP2P_ = !bTestDDMessenger_;
    
    /**
     * Init
     */
    private void init()
    {
        DDMessage.setDefaultRealKey("2100-0005-5596-3554");
        DDMessage.setDefaultVersion(new Version(1, 2, 0, true));
        String MOD = "";
        steps_ = new String[] { PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_CONNECTING),
                                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_SENDING),
                                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_WAITING_FOR_REPLY),
                                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_RECEIVING),
                                PropertyConfig.getMessage("msg.sendmsgdialog." + DDMessageListener.STEP_DONE)
                            };
                            
    
        errors_ = new String[] {PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_CONNECT_FAILED +MOD),
                                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_TIMEOUT+MOD),
                                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_SERVER_ERROR+MOD),
                                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_UNKNOWN_HOST+MOD),
                                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_UNKNOWN_ERROR+MOD),
                                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_DNS_TIMEOUT+MOD),
                                PropertyConfig.getMessage("msg.msgerror." + DDMessageListener.STATUS_APPL_ERROR)
                            };
    }
    
    /** Creates a new instance of OnlineTest */
    public OnlineTest() {
        init();
    }
    
    private long start;
    
    public void go(int nNum)
    {
        EngineMessage msg = getMessage();
        Peer2PeerClient p2pClient = null;
        
        long finish;
        for (int i = 0; i < nNum; i++)
        {
            start = System.currentTimeMillis();
            
            try 
            {
                if (bTestDDMessenger_) EngineMessenger.SendEngineMessage(null, msg, this);

                if (bTestP2P_) {
                    Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_TEST, msg);
                    if (p2pClient == null) {
                        p2pClient = new Peer2PeerClient("192.168.1.100", 11885, null, this);
                        //p2pClient = new Peer2PeerClient("192.168.1.102", 11885, null, this);
                        p2pClient.connect();
                    }
                    Peer2PeerMessage reply = p2pClient.sendGetReply(p2p);
                    logger.debug("REPLY " +i+": " + reply);
        
                }
                
            } 
            catch (Exception e)
            {
                logger.debug("Exception: " + Utils.formatExceptionText(e));
                break;
            }
            finish = System.currentTimeMillis();
            logger.debug("#" + i + ": " + (finish-start) + " millis");
        }
        
        try {
            if (p2pClient != null) p2pClient.close();
        }
        catch (Exception e)
        {
            logger.debug("Exception: " + Utils.formatExceptionText(e));
        }
        
    }
    
    /** 
     * return message for registering online game
     */
    private EngineMessage getMessage()
    {
        return new RegistrationMessage(EngineMessage.CAT_TESTING
                                            //EngineMessage.CAT_VERIFY_KEY
                                            );
    }
    
    public static void main(String args[])
    {
        if (args.length == 0) usage();
        
        int num = 0;
        try {
            num = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe)
        {
            usage();
        }

        new ConfigManager("poker", ApplicationType.COMMAND_LINE);
        OnlineTest test = new OnlineTest();
        test.go(num);
    }
    
    private static void usage()
    {
        System.out.println("Usage:  OnlineTest <#>");
        System.exit(1);
    }
    
    public void debugStep(int nStep, String sMsg) {
        
    }
    
    /** Called when a message has been received
     *
     */
    public void messageReceived(DDMessage message) {
        int nStatus = message.getStatus();
        
        if (nStatus == DDMessageListener.STATUS_OK)
        {
            logger.debug("Message received: " + message);
        }
        else
        {
            logger.debug("*** ERROR: " + errors_[nStatus] + " - " + message.getApplicationErrorMessage());
        }
    }
    
    /** Called at various times during the connection so the UI can update the
     * status
     *
     */
    public void updateStep(int nStep) {
        long now = System.currentTimeMillis();
        logger.debug(steps_[nStep]+" - " + (now-start) + " millis");
    }
    
}
