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
package com.donohoedigital.games.tools;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 12, 2006
 * Time: 8:39:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChatValidate
{
    public static void main(String argv[])
    {
        if (argv.length == 0)
        {
            System.out.println("Usage:  chatvalidate [filename]");
            System.exit(0);
        }

        // read file
        String chat = ConfigUtils.readFile(new File(argv[0]));

        // get last comment in file (should match ChatPanel)
        int index = chat.lastIndexOf("<!-- ");
        if (index == -1) invalid();

        // get all data before comment (that is what the hash is on)
        String validateThis = chat.substring(0, index);
        String sHash = SecurityUtils.getMD5Hash(validateThis, PokerConstants.CHAT_BYTES);

        // parse the comment for the hash stored therein
        String comment = chat.substring(index + 5);
        StringTokenizer token = new StringTokenizer(comment);
        String sFileHash = token.nextToken();

        // compare
        if (sHash.equals(sFileHash)) valid();

        invalid();

    }

    private static void invalid()
    {
        System.out.println("File is invalid");
        System.exit(0);
    }

    private static void valid()
    {
        System.out.println("File is valid");
        System.exit(0);
    }

}
