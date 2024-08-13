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

import org.apache.wicket.*;
import org.apache.wicket.protocol.http.*;
import org.apache.wicket.spring.injection.annot.*;
import org.springframework.context.*;

import javax.servlet.http.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 17, 2008
 * Time: 12:59:24 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseWicketApplication extends WebApplication implements ApplicationContextAware
{
    private ApplicationContext applicationContext;

    /**
     * Get Spring ApplicationContext
     */
    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    /**
     * Set Spring ApplicationContext
     */
    public void setApplicationContext(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void init()
    {
        // initialize Spring
        addComponentInstantiationListener(new SpringComponentInjector(this, applicationContext));

        // remove default of <em> ... </em>
        getMarkupSettings().setDefaultBeforeDisabledLink(null);
        getMarkupSettings().setDefaultAfterDisabledLink(null);

        // remove wicket tags in development
        getMarkupSettings().setStripWicketTags(true);
    }

    /**
     * Our request cycle processor
     */
    @Override
    public RequestCycle newRequestCycle(Request request, Response response)
    {
        return new BaseRequestCycle(this, (WebRequest) request, response);
    }

    /**
     * Our web response
     */
    @Override
    protected WebResponse newWebResponse(HttpServletResponse servletResponse)
    {
        return (getRequestCycleSettings().getBufferResponse() ?
                new BaseBufferedWebResponse(servletResponse) :
                new WebResponse(servletResponse));
    }

    /**
     * subclass can implement to return its own error page for exceptions
     */
    protected abstract Page getExceptionPage(RuntimeException e);
}