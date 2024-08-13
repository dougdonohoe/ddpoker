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
package com.donohoedigital.games.poker.wicket.pages.error;

import com.donohoedigital.base.*;
import com.donohoedigital.wicket.labels.*;
import org.wicketstuff.annotation.mount.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 19, 2008
 * Time: 2:48:39 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath(path = "error")
public class ErrorPage extends ErrorPokerPage
{
    private static final long serialVersionUID = 42L;

    public ErrorPage()
    {
        this("Nice try, pal", "");
    }

    public ErrorPage(String sMessage)
    {
        this("Nice try, pal", sMessage);
    }

    public ErrorPage(String title, String sMessage)
    {
        super(null);
        add(new StringLabel("title", title));
        add(new StringLabel("message", sMessage));
        add(new HiddenComponent("exception"));
    }

    public ErrorPage(Throwable e)
    {
        super(null);
        add(new StringLabel("title", "An Unexpected Error Occurred"));
        add(new StringLabel("message", "We are sorry, but we ran into a problem back on our server.  This problem " +
                                       "has been logged and DD Poker support has been notified.  It could be a " +
                                       "temporary problem, so please try again."));
        add(new StringLabel("exception", Utils.formatExceptionHTML(e)).setEscapeModelStrings(false));
    }
}