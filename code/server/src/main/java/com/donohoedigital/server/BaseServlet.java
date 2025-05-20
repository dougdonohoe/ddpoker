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
package com.donohoedigital.server;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.DDByteArrayOutputStream;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DDMessenger;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Doug Donohoe
 */
public abstract class BaseServlet extends HttpServlet 
{
    protected static Logger logger = LogManager.getLogger(BaseServlet.class);

    // our server
    private GameServer server;
    
    // used to track current servlet context
    private static final ThreadLocal<ServletContext> local = new ThreadLocal<ServletContext>();

    // settings
    private boolean ddMessageHandler;

    /**
     * used for any subclass init, called from GameServer after config files have been loaded.
     */
    public void afterConfigInit()
    {
    }

    /**
     * Get GameServer that this servlet belongs to
     */
    public GameServer getServer()
    {
        return server;
    }

    /**
     * Set GameServer that owns this.
     * @throws ApplicationError if server already set (one
     */
    void setServer(GameServer server)
    {
        ApplicationError.assertNull(this.server, "Server already set - instance of BaseServlet cannot be shared", this.server);
        this.server = server;
    }

    /**
     * set thread data
     */
    public static void setThreadServletContext(ServletContext c)
    {
        local.set(c);
    }
    
    /** 
     * Get thread data
     */
    public static ServletContext getThreadServletContext()
    {
        return local.get();
    }
    
    /**
     * Handle post - same as get, so calls doGet()
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        doGet(request, response);
    }

    /**
     * Handle get
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        // if not valid user agent, return error
        String sUserAgent = request.getHeader("user-agent");
        if (sUserAgent == null || !sUserAgent.equals(DDMessenger.getUSERAGENT()))
        {
            //useful for testing content-type behavoir in browsers
            //ret = new DDMessage(DDMessage.CAT_TESTING, "Test message returning back at you.");
            //response.setContentType(DDMessage.getContentType());
            //returnMessage(response, ret);
            response.sendError(403, "Permission denied dude.");
            return;
        }
        
        //ServletDebug.debugGet(request, response);
        DDMessage ret = null;
        DDMessage received = createNewMessage();
        received.setFromIP(request.getRemoteAddr());
        
        try {
            
            // TODO: needed for email
            if (!(request instanceof GameServletRequest))
            {
                setThreadServletContext(getServletContext());
            }

            // create DDMessage from the data
            InputStream stream = null;
            if (request instanceof GameServletRequest)
            {
                stream = ((GameServletRequest) request).getInputStream2();
            }
            else
            {
                stream = request.getInputStream();
            }
            received.read(stream, request.getContentLength());
            stream.close();
        
            // send to subclass for processing
            ret = processMessage(request, response, received);
        }
        catch (ApplicationError ae)
        {
            String s = Utils.formatExceptionText(ae);
            logger.error("Error processing message: " + received.toString(), ae);
            Throwable orig = ae.getException();
            
            ret = createNewMessage();
            ret.setCategory(DDMessage.CAT_ERROR);
            
            if (orig != null)
            {
                ret.setException(Utils.getExceptionMessage(orig));
                ret.setDDException(ae.toStringNoStackTrace());
            }
            else
            {
                ret.setException(ae.toStringNoStackTrace());
            }
            ret.addData(s);
        }
        catch (Throwable t)
        {
            String s = Utils.formatExceptionText(t);
            logger.error("Error processing message: " + received.toString(), t);
            
            ret = createNewMessage();
            ret.setCategory(DDMessage.CAT_ERROR);
            ret.setException(Utils.getExceptionMessage(t));
            ret.addData(s);
        }

        if (ret != null)
        {
            returnMessage(response, ret);
        }
    }
    
    /**
     * Return the message on the output stream (only one can be returned)
     */
    protected void returnMessage(HttpServletResponse response, DDMessage ret)
                                    throws IOException
    {
        response.setContentType(DDMessage.CONTENT_TYPE);
        OutputStream out;
        if (response instanceof GameServletResponse)
        {
            out = ((GameServletResponse)response).getOutputStream2();
        }
        else
        {
            out = response.getOutputStream();
        }
        
        ret.write(out);
        
        if (response instanceof GameServletResponse)
        {
            ((GameServletResponse)response).finishResponse();
        }
        else
        {
            out.close();
        }
    }
    
    /**
     * Return the given data (assumes a DDMessage has been written to a string)
     */
    protected void returnMessage(HttpServletResponse response, DDByteArrayOutputStream data)
                                    throws IOException
    {
        response.setContentType(DDMessage.CONTENT_TYPE);
        OutputStream out;
        if (response instanceof GameServletResponse)
        {
            out = ((GameServletResponse)response).getOutputStream2();
        }
        else
        {
            out = response.getOutputStream();
        }
        
        out.write(data.getBuffer(), 0, data.size());
        
        if (response instanceof GameServletResponse)
        {
            ((GameServletResponse)response).finishResponse();
        }
        else
        {
            out.close();
        }
    }
    
    /**
     * Subclasses implement this to process message received.  If desired,
     * can call returnMessage() to send the message back over the stream.  If this
     * is called, you must return null (or the message will get returned twice).
     * This is provided if a message needs to be returned within a lock, for example.
     * If returnMessage() is not called, then the caller of this will invoke it
     * automatically
     */
    public abstract DDMessage processMessage(HttpServletRequest request, HttpServletResponse response, DDMessage ddreceived) 
                        throws IOException;
    
    /**
     * Return new DDMessage for use with received messages.  Can
     * be overriden to return a subclass of DDMessage
     */
    public DDMessage createNewMessage()
    {
        return new DDMessage();
    }
    
    /**
     * Does this servlet expect dd messages in and out?
     * Default is true.
     */
    public final boolean isDDMessageHandler()
    {
        return ddMessageHandler;
    }

    /**
     * Set whether we expect dd messages.
     */
    public final void setDDMessageHandler(boolean b)
    {
        ddMessageHandler = b;
    }
    
    /**
     * debugging - override in subclass
     */
    public boolean isDebugOn()
    {
        return false;
    }
}





