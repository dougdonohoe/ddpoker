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
 * DisplayMessage.java
 *
 * Created on December 27, 2002, 4:05 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Class to show a message.  It first calls getMessage() - this
 * is used to enable subclasses.  If that returns null, it looks
 * for a "msgkey" param, which it looks up in the PropertyConfig.
 * If there is no message key, it looks for a "msg" param and
 * displays that.
 *
 * @author Doug Donohoe
 */
public class DisplayMessage extends DialogPhase
{
    public static final String PARAM_MESSAGE = "msg";
    public static final String PARAM_MESSAGE_KEY = "msgkey";

    /**
     * create UI to show message
     */
    @Override
    public JComponent createDialogContents()
    {
        DDPanel info = new DDPanel();

        String sMsgKey = gamephase_.getString(PARAM_MESSAGE_KEY, null);
        String sMsg = getMessage();
        if (sMsg == null)
        {
            if (sMsgKey != null)
            {
                sMsg = PropertyConfig.getMessage(sMsgKey);
            }
            else if (sMsg == null)
            {
                sMsg = gamephase_.getString(PARAM_MESSAGE, "No message (msg) or message key (msgkey) found");
            }
        }

        // message area
        DDHtmlArea label = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        label.addHyperlinkListener(GuiUtils.HYPERLINK_HANDLER);
        label.setText(sMsg);
        label.setBorder(EngineUtils.getStandardMessageBorder());
        info.add(label, BorderLayout.CENTER);

        return info;
    }

    public String getMessage()
    {
        return null;
    }
}
