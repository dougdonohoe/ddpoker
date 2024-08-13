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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.service.helper.*;
import junit.framework.*;

import java.net.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jan 4, 2009
 * Time: 10:57:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class DisallowedManagerTest extends TestCase
{
    //private static final Logger logger = Logger.getLogger(DisallowedManagerTest.class);

    DisallowedManager manager = new DisallowedManager();

    public void testInvalid()
    {
        assertTrue(manager.isNameValid("Dexter"));
        assertTrue(manager.isNameValid(".Foo"));
        assertTrue(manager.isNameValid("<===A===>"));

        assertFalse(manager.isNameValid("bitch"));
        assertFalse(manager.isNameValid("BITCH"));
        assertFalse(manager.isNameValid("Hal is not a bitch"));
        assertFalse(manager.isNameValid(".?+-"));
        assertFalse(manager.isNameValid("????"));
        assertFalse(manager.isNameValid("??   ??"));
        assertFalse(manager.isNameValid("="));
        assertFalse(manager.isNameValid("_"));
        assertFalse(manager.isNameValid("<===+===>"));

    }

    public void testUTF8()
    {
        verifyFile("greek.utf8.txt");
        verifyFile("russian.utf8.txt");
        verifyFile("japanese.utf8.txt");
        verifyFile("chinese.utf8.txt");
        verifyFile("arabic.utf8.txt");
        verifyFile("swedish.utf8.txt");
    }

    private void verifyFile(String filename)
    {
        URL url = new MatchingResources("classpath:" + filename).getSingleRequiredResourceURL();
        String name = ConfigUtils.readURL(url).trim();
        assertTrue(manager.isNameValid(name));

    }

//    public void testRegexp()
//    {
//        match(".Foo", "^\\W+.*$");
//        match(".Foo", "\\W+.*");
//        match(".Foo", "^\\W+");
//        match("Dexter", "^\\W+.*$");
//    }
//
//    private void match(String name, String regex)
//    {
//        logger.debug(name + " matches: " + regex + ": " + name.matches(regex));
//        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(name);
//        logger.debug("  matcher matches: " + matcher.matches());
//        logger.debug("  matcher find: " + matcher.find());
//    }
}
