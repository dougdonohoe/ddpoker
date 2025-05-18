/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.base.*;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 20, 2006
 * Time: 8:09:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class Unhide
{
    public static void main(String[] args)
    {
        if (args.length == 0) System.out.println("Unhide [file]");
        unhide(ConfigUtils.getReader(new File(args[0])));
    }

    public static void unhide(Reader reader)
    {
        try {
            BufferedReader sreader = new BufferedReader(reader);
            String sLine;
            StringBuffer sbLine = new StringBuffer(80);
            int nEntry = -1;
            while ((sLine = sreader.readLine()) != null)
            {
                nEntry++;
                //logger.debug("Read: " + sLine);
                sbLine.setLength(0);
                sbLine.append(sLine);
                Hide.deobfuscate(sbLine, nEntry);
                System.out.println(sbLine);
            }
        }
        catch (Throwable e)
        {
            System.out.println(Utils.formatExceptionText(e));
        }
    }
}
