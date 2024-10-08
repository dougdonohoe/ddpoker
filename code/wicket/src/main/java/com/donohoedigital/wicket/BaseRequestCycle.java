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
package com.donohoedigital.wicket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.*;
import org.apache.wicket.protocol.http.*;

import javax.servlet.http.*;

/**
 * Our own request cycle - keeping in case we need to override something in the future
 *
 * @author Doug Donohoe
 */
public class BaseRequestCycle extends WebRequestCycle
{
    private Logger logger = LogManager.getLogger(BaseRequestCycle.class);
        
    /**
     * Constructor which simply passes arguments to superclass for storage there. This instance will
     * be set as the current one for this thread.
     *
     * @param application The application
     * @param request     The request
     * @param response    The response
     */
    public BaseRequestCycle(BaseWicketApplication application, WebRequest request, Response response)
    {
        super(application, request, response);
    }

    /**
     * Called when the request cycle object is beginning its response
     */
    @Override
    protected void onBeginRequest()
    {
        HttpServletRequest http = getWebRequest().getHttpServletRequest();
        logger.debug("Request: " + http.getRemoteAddr() +
                     ' ' + http.getMethod() +
                     ' ' + getWebRequest().getURL());
    }

    /**
     * Called when the request cycle object has finished its response
     * (request.getPage() only non-null for non-mounted pages it seems)
     */
    @Override
    protected void onEndRequest()
    {
    }

    /**
     * Template method that is called when a runtime exception is thrown, just before the actual
     * handling of the runtime exception. This is called by
     * {@link org.apache.wicket.request.AbstractRequestCycleProcessor#respond(RuntimeException, org.apache.wicket.RequestCycle)}.
     *
     * @param page Any page context where the exception was thrown
     * @param e    The exception
     * @return Any error page to redirect to
     */
    @Override
    public Page onRuntimeException(Page page, RuntimeException e)
    {
        if (e instanceof PageExpiredException) return null;
        
        return ((BaseWicketApplication) application).getExceptionPage(e);
    }
}
