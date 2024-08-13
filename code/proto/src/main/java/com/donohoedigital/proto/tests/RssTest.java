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
package com.donohoedigital.proto.tests;

import com.sun.syndication.feed.rss.Category;
import com.sun.syndication.feed.rss.*;
import com.sun.syndication.io.*;
import junit.framework.*;
import org.apache.log4j.*;

import java.util.*;

/**
 * @author Doug Donohoe
 */
public class RssTest extends TestCase
{
    private static Logger logger = Logger.getLogger(RssTest.class);

    public void testRSS()
    {
        Channel channel = new Channel("rss_2.0");
        channel.setTtl(30);
        channel.setTitle("TEST");
        channel.setDescription("Test channel description");
        channel.setLink("http://www.ddpoker.com/");
        channel.setLastBuildDate(new Date());
        channel.setLanguage("en");
        channel.setCopyright("Copyright (c) 2004-2008.  Donohoe Digital LLC.");


        Category cat = new Category();
        cat.setValue("Available Games");
        List<Category> cats = new ArrayList<Category>();
        cats.add(cat);

        Item item = new Item();
        Description desc = new Description();
        desc.setType("hmmm");
        desc.setValue("what goes here?");
        item.setDescription(desc);
        item.setLink("http://www.ddpoker.com/foo");
        item.setCategories(cats);
        Source source = new Source();
        source.setUrl("http://www.ddpoker.com/rss/games/available");
        source.setValue("Available Games");
        item.setSource(source);

        List<Item> items = new ArrayList<Item>();
        items.add(item);
        channel.setItems(items);

        WireFeedOutput out = new WireFeedOutput();
        try
        {
            logger.debug("Feed: \n" + out.outputString(channel));
        }
        catch (FeedException e)
        {
            logger.error(e);
        }
    }

}
