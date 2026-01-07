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
package com.donohoedigital.wicket;

import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * Not sure if needed, but here to start
 */
public class BaseRequestCycleListener implements IRequestCycleListener {

    private final BaseWicketApplication application;
    public BaseRequestCycleListener(BaseWicketApplication application) {
        this.application = application;
    }

    @Override
    public void onBeginRequest(RequestCycle cycle) {

    }

    @Override
    public void onEndRequest(RequestCycle cycle) {

    }

    @Override
    public void onDetach(RequestCycle cycle) {

    }

    @Override
    public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler) {

    }

    @Override
    public void onRequestHandlerScheduled(RequestCycle cycle, IRequestHandler handler) {

    }

    @Override
    public IRequestHandler onException(RequestCycle cycle, Exception ex) {
        if (ex instanceof PageExpiredException) return null;

        return new RenderPageRequestHandler(new PageProvider(application.getExceptionPage(ex)));
    }

    @Override
    public void onExceptionRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler, Exception exception) {

    }

    @Override
    public void onRequestHandlerExecuted(RequestCycle cycle, IRequestHandler handler) {

    }

    @Override
    public void onUrlMapped(RequestCycle cycle, IRequestHandler handler, Url url) {

    }
}
