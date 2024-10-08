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
 * Order.java
 *
 * Created on May 26, 2004, 1:42 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;

import java.awt.*;

/**
 *
 * @author  Doug Donohoe
 */
public class Order extends BasePhase 
{
    //static Logger logger = LogManager.getLogger(Order.class);
    
    public void start()
    {
        String sURL = gamephase_.getString("url", "http://www.donohoedigital.com/");
        String sDisplay = sURL;
        int nQuery = sDisplay.indexOf('?');
        if (nQuery != -1) sDisplay = sDisplay.substring(0,nQuery);
        String sMsg = Utils.fixHtmlTextFor15(PropertyConfig.getMessage("msg.order.confirm", sDisplay));
        if (EngineUtils.displayConfirmationDialog(context_, sMsg))
        {
            context_.getFrame().setExtendedState(Frame.ICONIFIED);
            Utils.openURL(sURL);
        }
    }
}
