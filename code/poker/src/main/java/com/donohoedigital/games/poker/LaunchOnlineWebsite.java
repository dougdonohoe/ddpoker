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
 * LaunchOnlineWebsite.java
 *
 * Created on July 16, 2005, 1:02 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.games.engine.*;

/**
 * @author Doug Donohoe
 */
public class LaunchOnlineWebsite extends BasePhase
{
    @Override
    public void start()
    {
        // As of DD Poker 3, turned off version info and confirmation
        String sURL = gamephase_.getString("url", "http://www.donohoedigital.com/");
//        String sKey = gamephase_.getString("key", "msg.launchonline.confirm");
//        boolean bVers = gamephase_.getBoolean("showvers", false);
//        String sDisplay = sURL;
//        int nQuery = sDisplay.indexOf('?');
//        if (nQuery != -1) sDisplay = sDisplay.substring(0,nQuery);
        //String sMsg = Utils.fixHtmlTextFor15(PropertyConfig.getMessage(sKey, sDisplay));
        //if (EngineUtils.displayConfirmationDialog(context_, sMsg))
        //{
//            if (bVers) sURL += "?v" + engine_.getVersion().toString();
//            context_.getFrame().setExtendedState(Frame.ICONIFIED);
        Utils.openURL(sURL);
        //}
    }
}
