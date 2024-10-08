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
 * RegAnalyzer.java
 *
 * Created on September 27, 2004, 3:47 PM
 */

package com.donohoedigital.games.server;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.server.model.*;
import com.donohoedigital.games.server.service.*;
import org.apache.logging.log4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

import java.text.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class Ban
{
    // logging
    private static Logger logger = LogManager.getLogger(Ban.class);

    // option names
    public static final String OPTION_KEY = "key";
    public static final String OPTION_COMMENT = "comment";
    public static final String OPTION_UNTIL = "until";
    // services
    private BannedKeyService bannedService;

    /**
     * Implements command line application interface.
     */
    private static class BanApp extends BaseCommandLineApp
    {
        private BanApp(String sConfigName, String[] args)
        {
            // init app (this parses the args)
            super(sConfigName, args);
        }

        /**
         * our specific options
         */
        @Override
        protected void setupApplicationCommandLineOptions()
        {
            CommandLine.addStringOption(OPTION_KEY, null);
            CommandLine.setDescription(OPTION_KEY, "item to ban", "key or email or profile name");
            CommandLine.setRequired(OPTION_KEY);

            CommandLine.addStringOption(OPTION_COMMENT, null);
            CommandLine.setDescription(OPTION_COMMENT, "comment for ban", "comment");

            CommandLine.addStringOption(OPTION_UNTIL, null);
            CommandLine.setDescription(OPTION_UNTIL, "until date", "2012-12-22");
        }
    }

    /**
     * Run analyzer
     */
    public static void main(String[] args)
    {
        try
        {
            // Create app to parse command line options
            BanApp info = new BanApp("servertools", args);

            logger.info("Ban initializing, params: " + Utils.toString(args, " "));

            // create application context
            ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-gameserver.xml");

            // get the analyzer bean and run it
            Ban app = (Ban) ctx.getBean("banApp");
            app.initAndRun(info.getCommandLineOptions());
        }
        catch (ApplicationError ae)
        {
            System.err.println("Ban ending due to ApplicationError: " + ae.toString());
        }
        catch (Throwable t)
        {
            System.err.println(Utils.formatExceptionText(t));
        }

        System.exit(0);
    }

    /**
     * Initialize options and then run the analyzer
     */
    public void initAndRun(TypedHashMap htOptions)
    {
        // get params
        String key = htOptions.getString(OPTION_KEY);
        String comment = htOptions.getString(OPTION_COMMENT);
        String date = htOptions.getString(OPTION_UNTIL);
        Date realDate = null;
        try
        {
            if (date != null)
            {
                realDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            }
        }
        catch (ParseException e)
        {
            CommandLine.exitWithError(e.getMessage());
        }

        // make sure not banned
        if (bannedService.isBanned(key))
        {
            logger.info("Key already banned: " + key);
            System.exit(0);
        }

        BannedKey banthis = new BannedKey();
        banthis.setKey(key);
        banthis.setComment(comment);
        if (realDate != null) banthis.setUntil(realDate);
        bannedService.saveBannedKey(banthis);

        logger.info(key + " banned, id=" + banthis.getId() + ", comment=" + comment + ", date=" + realDate);
    }

    public BannedKeyService getBannedService()
    {
        return bannedService;
    }

    @Autowired
    public void setBannedService(BannedKeyService bannedService)
    {
        this.bannedService = bannedService;
    }


}