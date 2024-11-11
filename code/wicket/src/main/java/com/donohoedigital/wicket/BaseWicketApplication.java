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

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.resource.JQueryResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 17, 2008
 * Time: 12:59:24 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseWicketApplication extends WebApplication implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    /**
     * Set Spring ApplicationContext
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void init() {
        // set cycle provider and listener
        setRequestCycleProvider(new BaseRequestCycleProvider());
        getRequestCycleListeners().add(new BaseRequestCycleListener(this));

        // JQuery - ensure same version used everywhere
        getJavaScriptLibrarySettings().setJQueryReference(JQueryResourceReference.getV3());

        // initialize Spring
        getComponentInstantiationListeners().add(new SpringComponentInjector(this, applicationContext));

        // remove wicket tags in development
        getMarkupSettings().setStripWicketTags(true);

        // Wicket 9: disable CSP
        getCspSettings().blocking().disabled();
    }

    /**
     * subclass can implement to return its own error page for exceptions
     */
    protected abstract Page getExceptionPage(Exception e);
}