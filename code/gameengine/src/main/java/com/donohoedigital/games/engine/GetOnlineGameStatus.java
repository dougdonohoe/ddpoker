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
 * GetOnlineGameStatus.java
 *
 * Created on April 20, 2003, 5:10 PM
 */

package com.donohoedigital.games.engine;

import com.donohoedigital.gui.*;
import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.comms.*;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.text.*;

/**
 *
 * @author  Doug Donohoe
 */
public class GetOnlineGameStatus extends SendMessageDialog
{
    //static Logger logger = LogManager.getLogger(GetOnlineGameStatus.class);
    
    /** 
     * return message for registering online game
     */
    protected EngineMessage getMessage()
    {
        DMArrayList ids = (DMArrayList) gamephase_.getList(EngineMessage.PARAM_GAME_IDS);
        DMArrayList pass = (DMArrayList) gamephase_.getList(EngineMessage.PARAM_PASSWORDS);
        EngineMessage msg = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                            EngineMessage.PLAYER_NOTDEFINED, 
                                            EngineMessage.CAT_STATUS,
                                            (String) null);
        msg.setList(EngineMessage.PARAM_GAME_IDS, ids);
        msg.setList(EngineMessage.PARAM_PASSWORDS, pass);
        
        return msg;
    }
    
    protected String getMessageKey()
    {
        return "msg.getStatus";
    }
}
