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
 * ProxySocketThread.java
 *
 * Created on October 19, 2004, 4:50 PM
 */

package com.donohoedigital.tools;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.*;
import com.donohoedigital.server.SocketThread;
import jakarta.servlet.ServletException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class is for testing registrations through a proxy.  We configure
 * the client (e.g. DD Poker) to register to the address/port that this
 * program is running on.  Then, that request is intercepted and passed
 * to the proxy we configure and the results are passed back.  This is
 * actually simulating a chained proxy also.
 * 
 * We support two types of proxies: Normal proxies where you pass-thru
 * the request, changing GET foo HTTP/1.0 to GET http://server/foo HTTP/1.0.
 *
 * The other is the CONNECT method, where we pass CONNECT server HTTP/1.0
 * and then the original headers/post data from the client through after that.
 *
 * @author  donohoe
 */
public class ProxySocketThread extends SocketThread implements PostWriter, DDMessageListener
{
    static Logger logger = LogManager.getLogger(ProxySocketThread.class);
    
    private DDHttpClient.HttpOptions options_;
    private URL proxy_;
    private String sDestHost_ = "tbd.com";
    private int nDestPort_ = 8877;
    private String sDestHostPort_ = sDestHost_ + ":" + nDestPort_;
    
    /** 
     * Creates a new instance of ProxySocketThread 
     */
    public ProxySocketThread() 
    {
        options_ = new DDHttpClient.HttpOptions();
        
        try {
            // CONNECT method proxies
            //options_.sConnectDestViaProxy = sDestHostPort_;
            
            // Regular proxies
            proxy_ = new URL("http://81.192.163.131:80"); // working
            options_.bProxyPassThru = true;
            
        } catch (MalformedURLException mfe)
        {
            throw new ApplicationError(mfe);
        }
    }
   
    /**
     * process data that was read (in buffer_)
     */
    protected void process() throws IOException, ServletException
    {
        // Get data from proxy (sent via write() method of PostWriter)
        // Data sent is controlled via options_ use in DDMessenger and
        // cooresponding methods in DDHttpClient
        DDMessenger msg = new DDMessenger();
        proxy_ = new URL(request_.getRequestURI());
        DDMessenger.ReturnData data = msg.getURL(proxy_, this, null, null, this, options_);
        
        // debug output
        logger.debug("DATA: " + data.getOut().size() + " bytes ->"+
                    "\n========= OUTPUT =========\n" + Utils.decodeBasic(data.getOut().getBuffer(), 0, data.getOut().size()) +
                      "=========  END   =========");
        
        // return results to client
        OutputStream out = response_.getOutputStream3();
        out.write(data.getOut().getBuffer(), 0, data.getOut().size());
    }
    
    /** 
     * PostWriter - output post data 
     **/
    public void write(OutputStream writer) throws IOException 
    {
        // convert to string - sufficient for this test proxy since we
        // aren't dealing with binary data
        String sData = Utils.decodeBasic(buffer_.array(), 0, buffer_.limit());
        
        // if sending to a proxy (non-connect), fix the output POST and Host
        if (options_.bProxyPassThru)
        {
            sData = sData.replaceFirst("POST ", "POST http://" + sDestHostPort_);
            sData = sData.replaceFirst("[hH][oO][sS][tT]:[\\.a-zA-Z0-9\\-\\_ ]*", "Host: " + sDestHost_);
        }
        
        // we send the entire buffer
        logger.debug("Sending DATA to " + proxy_ + " ->\n========= INPUT =========\n" + sData +
                       "=========  END  =========");
        writer.write(sData.getBytes());
    }

    /** DDMessageListener - output progress **/
    public void updateStep(int nStep) {
        logger.debug("Step: " + ProxyServlet.getSteps()[nStep]);
    }
    
    /** DDMessageListener not used **/
    public void debugStep(int nStep, String sMsg) {
    }
    /** DDMessageListener not used **/
    public void messageReceived(DDMessage message) {
    }
    

}
