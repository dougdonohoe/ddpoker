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
/*
 * Messenger.java
 *
 * Created on March 5, 2003, 7:40 PM
 */

package com.donohoedigital.comms;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.DDByteArrayOutputStream;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 *
 * @author  Doug Donohoe
 */
public class DDMessenger
{
    //static Logger logger = LogManager.getLogger(DDMessenger.class);

    /**
     * User agent for our requests
     */
    private static final String USERAGENT = "DD/Java Arch 1.0";

    /**
     * Get user agent
     */
    public static String getUSERAGENT()
    {
        return USERAGENT;
    }

    /** 
     * Creates a new instance of Messenger 
     */
    public DDMessenger() 
    {
    }
    
    /**
     * Return new DDMessage for use with received messages.  Can
     * be overriden to return a subclass of DDMessage
     */
    public DDMessage createNewMessage()
    {
        return new DDMessage();
    }
    
    /**
     * Send a message and return response message
     */
    public DDMessage sendMessage(String sURL, DDMessage send)
    {
        URL url;
       
        try {
            url = URI.create(sURL).toURL();
            return sendMessage(url, send);
        }
        catch (MalformedURLException | IllegalArgumentException me)
        {
            // URI.create() throws IllegalArgumentException where new URL() threw
            // MalformedURLException - both mean the same thing to callers
            throw new ApplicationError(me);
        }
    }
        
    /**
     * Send a message & return response message
     */
    public DDMessage sendMessage(URL url, DDMessage send)
    {
        return sendMessage(url, send, null);
    }

    /**
     * Return true if should not attempt to contact given URL
     */
    public boolean isDisabled(String url) {
        return false;
    }

    /**
     * private version, takes DDMessageListener to pass to getURL, handles
     * exceptions and returns message with CAT_ERROR or CAT_APPL_ERROR.
     * If listener is not null, it is notified of the message
     */
    public DDMessage sendMessage(URL url, DDMessage send, DDMessageListener listener)
    {
        // when talking to server, override the public key to send the real key
        // so server can validate this as a valid client
        send.setKey(DDMessage.getDefaultRealKey());
        DDMessage ret = createNewMessage();
        int nStatus = DDMessageListener.STATUS_OK;
        
        try 
        {
            if (isDisabled(url.toString())) {
                nStatus = DDMessageListener.STATUS_DISABLED;
            } else {
                getURL(url, send, DDMessage.CONTENT_TYPE, ret, listener);
                if (ret.getCategory() == DDMessage.CAT_ERROR)
                {
                    nStatus = DDMessageListener.STATUS_SERVER_ERROR;
                }
                else if (ret.getCategory() == DDMessage.CAT_APPL_ERROR)
                {
                    nStatus = DDMessageListener.STATUS_APPL_ERROR;
                }
            }
        }
        // error handling closely linked to SendMessageDialog.java
        catch (ApplicationError ae)
        {
            Throwable e = ae.getException();
            if (e == null) e = ae;
            
            ret.setCategory(DDMessage.CAT_ERROR);
            ret.addData(Utils.formatExceptionText(e));

            nStatus = DDMessageListener.STATUS_UNKNOWN_ERROR;
            //noinspection ChainOfInstanceofChecks
            switch (e) {
                case ApplicationError ae2 -> {
                    if (ae2.getErrorCode() == ErrorCodes.ERROR_404 ||
                            ae2.getErrorCode() == ErrorCodes.ERROR_403 ||
                            ae2.getErrorCode() == ErrorCodes.ERROR_503) {
                        nStatus = DDMessageListener.STATUS_CONNECT_FAILED;
                    }
                }
                case java.net.SocketException _ -> nStatus = DDMessageListener.STATUS_CONNECT_FAILED;
                case java.net.UnknownHostException _ ->
                        nStatus = DDMessageListener.STATUS_UNKNOWN_HOST;
                case DNSTimeoutException _ ->
                        nStatus = DDMessageListener.STATUS_DNS_TIMEOUT;
                case java.net.SocketTimeoutException _ ->
                        nStatus = DDMessageListener.STATUS_TIMEOUT;
                default -> {
                }
            }

            ret.setString(DDMessage.PARAM_EXCEPTION_MESSAGE, Utils.getExceptionMessage(e));
        }
        
        ret.setStatus(nStatus);
        if (listener != null) listener.messageReceived(ret);
        return ret;
    }
    
    /**
     * Send a message in a thread and return response to the DDMessageListener
     */
    public void sendMessageAsync(URL url, DDMessage send, DDMessageListener listener)
    {
        URLThread urlthread = new URLThread(url, send, listener);
        Thread t = new Thread(urlthread, "URLThread");
        t.start();
    }

    /**
     * Class to request url in a thread and send result back to a listener
     */
    private class URLThread implements Runnable
    {
        URL url;
        DDMessage send;
        DDMessageListener listener;
        
        public URLThread(URL url, DDMessage send, DDMessageListener listener)
        {
            this.url = url;
            this.send = send;
            this.listener = listener;
        }
        
        public void run()
        {
            sendMessage(url, send, listener);
        }
    }
    
    /**
     * Connect to site and get content of the given URL.  If writer is not null, initiate
     * the request using POST method, calling writer.write() to write the data 
     * and sPostContentType as the content-type.  If reader is non-null, data is
     * expected to be read using the reader.  Return Data will contain no data, but
     * will contain content-type and length;
     */
    @SuppressWarnings("UnusedReturnValue")
    public ReturnData getURL(URL url, PostWriter writer, String sPostContentType,
                             PostReader reader, DDMessageListener listener)
    {
        DDHttpClient conn = null;

        int nRead;
        try 
        {    
            // create http client
            conn = new DDHttpClient(url, listener);

            // open connection to server
            conn.connect();

            // write request (POST if writer, otherwise GET)
            conn.write(writer, sPostContentType, USERAGENT);

            // start reading - which process headers to get content-type
            // length and response code.  Reading ends at end of header
            // data (two CrLf in a row).
            conn.startRead();
            
            // get data gleamed from headers
            int nLength = conn.getContentLength();
            int nResponseCode = conn.getResponseCode();
            DDByteArrayOutputStream ddbytes = null;
            
            // If error, get rest of data (ie error page) and return exception
            if (nResponseCode != 200)
            {
                ddbytes = conn.getHeaderBuffer();
                try {
                    InputStream is = conn.getInputStream();
                    byte[] bytes = new byte[1000];
                    while ((nRead = is.read(bytes)) != -1)
                    {
                        ddbytes.write(bytes, 0, nRead);
                    }
                }
                catch (IOException ignored)
                {
                }
                
                throw new ApplicationError(ErrorCodes.ERROR_BAD_RESPONSE + nResponseCode,
                            "Connect failed, response code = " + nResponseCode,
                        Utils.decodeBasic(ddbytes.getBuffer(), 0, ddbytes.size()), null);
            }
            // No error - read the "meat" of the reply
            else
            {
                InputStream is = conn.getInputStream();
                
                // use a reader if provided
                if (reader != null)
                {
                    reader.read(is, nLength);
                }
                // else read into a buffer and pass back with ReturnData
                else
                {
                    byte[] bytes = new byte[1000];
                    ddbytes = new DDByteArrayOutputStream(1000);
                    while ((nRead = is.read(bytes)) != -1)
                    {
                        ddbytes.write(bytes, 0, nRead);
                    }
                }
            }
            
            // finished - notify listener and return data
            if (listener != null) listener.updateStep(DDMessageListener.STEP_DONE);
            return new ReturnData(conn.getHeaderBuffer(), ddbytes);
        }
        catch (Exception io)
        {
            throw new ApplicationError(io);
        }
        finally
        {
            try {
                if (conn != null)
                {
                    conn.close();
                }
            }
            catch (Exception ignore) {}
        }
    }

    /**
     * Class to represent return data from getURL
     */
    public static final class ReturnData
    {
        private final DDByteArrayOutputStream headers;
        private final DDByteArrayOutputStream out;

        private ReturnData(DDByteArrayOutputStream headers, DDByteArrayOutputStream out)
        {
            this.headers = headers;
            this.out = out;
        }

        public DDByteArrayOutputStream getHeaders()
        {
            return headers;
        }

        public DDByteArrayOutputStream getOut()
        {
            return out;
        }
    }
}
