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
package com.donohoedigital.base;

import java.util.*;

public class CSVParser
{

    public CSVParser()
    {
    }

    /**
     * Given a CSV XLS format, returns a StringArray of the parsed values.
     * Note that the " is the
     * escape character in CSV files.  A CSV string of<BR>
     * <TT>"foo bar","he said ""hi there"" to me, Joe",doh<TT><BR>
     * Will return: <UL>
     * <LI>foo bar
     * <LI>he said "hi there" to me, Joe
     * <LI>doh
     * </UL>
     */
    public static String[] parseLine(String sLine)
    {
        Vector vValues = new Vector();
        int nLen = sLine.length();
        for (int i = 0; i < nLen; i++)
        {
            i = getNextString(sLine, i, nLen, vValues);

            // special case: if last comma is the last character, then
            // we need to add a blank entry - CSV lines don't end in a comma
            if (i == (nLen - 1))
            {
                vValues.add("");
            }
        }

        nLen = vValues.size();
        String[] sRet = new String[nLen];
        for (int j = 0; j < nLen; j++)
        {
            sRet[j] = (String) vValues.elementAt(j);
        }
        return sRet;
    }

    /**
     * Gets next string and puts it in the vector.
     * @returns index of the last comma or nLen (if end of line)
     */
    private static int getNextString(String sLine, int nStart, int nLen, Vector vValues)
    {
        boolean bDone = false;
        boolean bInsideQuote = false;
        if (sLine.charAt(nStart) == '"') {
            bInsideQuote = true;
            nStart++;
        }
        char c;
        int nEnd = nLen;
        StringBuilder sbValue = new StringBuilder("");

        for (int i = nStart; i < nLen && !bDone; i++)
        {
            c = sLine.charAt(i);
            if (c == ',' && !bInsideQuote)
            {
                nEnd = i;
                bDone = true;
            }
            else if (c == '"')
            {
                if (i == (nLen - 1)) // last char
                {
                    if (!bInsideQuote)
                    {
                        // TODO: ERROR CONDITION - what to do?
                    }
                    bInsideQuote = false;
                }
                // Quote after a quote - keep only one
                else if (sLine.charAt(i+1) == '"')
                {
                    sbValue.append(c);
                    i++; // advance past escape char
                }
                else if (bInsideQuote)
                {
                    bInsideQuote = false;
                }
            }
            else
            {
                sbValue.append(c);
            }
        }

        vValues.add(sbValue.toString());

        return nEnd;
    }
}