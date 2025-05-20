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
package com.donohoedigital.games.poker.ai;


import com.donohoedigital.base.*;

import java.io.*;
import java.util.*;

public class Roster
{
    public static String getRoster(PlayerType playerType)
    {
        File f = getRosterFile(playerType);

        if (!f.exists()) return "";

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(f));

            StringBuilder buf = new StringBuilder();

            String line;

            while ((line = br.readLine()) != null)
            {
                buf.append(line);
                buf.append("\n");
            }

            return buf.toString().trim();
        }
        catch (IOException e)
        {
            throw new ApplicationError(e);
        }
    }

    public static List<String> getRosterNameList(PlayerType playerType)
    {
        String[] names = getRoster(playerType).replaceAll
                ("\\s*,\\s*", ",").replaceAll
                (",+", ",").replaceAll
                ("^,|,$", "").split(",");

        List<String> list = new ArrayList<String>(names.length);

        if (names[0].length() == 0) return list;

        for (String name : names)
        {
            if (!list.contains(name)) list.add(name);
        }

        return list;
    }

    public static void setRoster(PlayerType playerType, String roster)
    {
        File f = getRosterFile(playerType);

        try
        {
            FileWriter fw = new FileWriter(f);

            fw.write(roster);

            fw.close();
        }
        catch (IOException e)
        {
            throw new ApplicationError(e);
        }
    }

    private static File getRosterFile(PlayerType playerType)
    {
        return new File(playerType.getFile().getParentFile(),
                        playerType.getFileName().replaceFirst("\\.dat", ".roster"));
    }
}
