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
/*
 * Hide.java
 *
 * Created on August 30, 2003, 10:32 AM
 */

package com.donohoedigital.games.config;

/**
 *
 * @author  donohoe
 */
public class Hide
{
    /**
     * Obfuscate save line
     */
    public static void obfuscate(StringBuffer sb, int nEntry)
    {
        _fuscate(sb,nEntry,false);
    }
    
    /**
     * Deobfuscate save line
     */
    public static void deobfuscate(StringBuffer sb, int nEntry)
    {
        _fuscate(sb,nEntry,true);
    }
    
    /**
     * the logic of it all
     */
    private static void _fuscate(StringBuffer sb, int nEntry, boolean de)
    {
        char c,a,z;
        int n, rotate;
        for (int i = 0; i < sb.length(); i++)
        {
            rotate = nEntry * (i + 127);
            a = z ='\u0000';
            c = sb.charAt(i);
            n = 0;
            
            // do digits
            if (c >= '0' && c <= '9')
            {
                a = '0';
                z = '9';
                n = 10;
            }
            // lowercase ascii
            else if (c >= 'a' && c <= 'z')
            {
                a = 'a';
                z = 'z';
                n = 26;
            }
            // uppercase ascii
            else if (c >= 'A' && c <= 'Z')
            {
                a = 'A';
                z = 'Z';
                n = 26;
            }
            
            // rotate
            if (n != 0) {
                rotate = rotate % n;
                // leave entry 1 in clear (game info)
                if (nEntry > 0 && rotate == 0) rotate = 7;
                if (!de) {
                    c = (char)(c + rotate);
                    if (c > z) c = (char) (c-n);
                } else {
                    c = (char)(c - rotate);
                    if (c < a) c = (char) (c+n);
                }
                sb.setCharAt(i, c);
            }
        }
    }
}
