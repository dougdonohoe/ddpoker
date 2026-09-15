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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.CommandLine;
import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.BaseCommandLineApp;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Marks the wan_profile row for the given name as activated, with the given email and
 * password.  This is the server half of what online activation does, skipping the
 * generated password and the email, for testing.
 * <p>
 * With -create, inserts the row if it doesn't exist, and leaves it alone if it does.
 * Without it, the row must exist and its email and password are updated.
 * <p>
 * The client half (the local profile .dat file) is ActivateProfile in the poker module,
 * which this module can't see.  The activateprofile script runs both.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class ActivateOnlineProfile
{
    // option names - keep in sync with ActivateProfile
    public static final String OPTION_NAME = "name";
    public static final String OPTION_EMAIL = "email";
    public static final String OPTION_PASSWORD = "password";
    public static final String OPTION_CREATE = "create";

    // only used on insert; the lobby authenticates with the client's own key, not this one
    private static final String LICENSE_KEY = "KEY-TEST-ACTIVATEPROFILE";

    /**
     * Implements command line application interface.
     */
    private static final class ActivateOnlineProfileApp extends BaseCommandLineApp
    {
        private ActivateOnlineProfileApp(String sConfigName, String[] args)
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
            CommandLine.addStringOption(OPTION_NAME, null);
            CommandLine.setDescription(OPTION_NAME, "profile name (exact match)", "name");
            CommandLine.setRequired(OPTION_NAME);

            CommandLine.addStringOption(OPTION_EMAIL, null);
            CommandLine.setDescription(OPTION_EMAIL, "online email", "email");
            CommandLine.setRequired(OPTION_EMAIL);

            CommandLine.addStringOption(OPTION_PASSWORD, null);
            CommandLine.setDescription(OPTION_PASSWORD, "online password", "password");
            CommandLine.setRequired(OPTION_PASSWORD);

            CommandLine.addFlagOption(OPTION_CREATE);
            CommandLine.setDescription(OPTION_CREATE, "create profile if missing, skip if it exists");
        }
    }

    /**
     * Run activation.
     */
    static void main(String[] args)
    {
        try
        {
            // Create app to parse command line options
            ActivateOnlineProfileApp app = new ActivateOnlineProfileApp("plain", args);

            // get the service from spring
            ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-testingtools.xml");
            OnlineProfileService service = (OnlineProfileService) ctx.getBean("onlineProfileService");

            activate(service, app.getCommandLineOptions());
        }
        catch (ApplicationError ae)
        {
            System.err.println("ActivateOnlineProfile ending due to ApplicationError: " + ae);
            System.exit(1);
        }
        catch (Throwable t)
        {
            System.err.println(Utils.formatExceptionText(t));
            System.exit(1);
        }

        System.exit(0);
    }

    /**
     * Insert or update the profile
     */
    private static void activate(OnlineProfileService service, TypedHashMap htOptions)
    {
        String name = htOptions.getString(OPTION_NAME);
        String email = htOptions.getString(OPTION_EMAIL);
        String password = htOptions.getString(OPTION_PASSWORD);
        boolean bCreate = htOptions.getBoolean(OPTION_CREATE, false);

        OnlineProfile profile = service.getOnlineProfileByName(name);
        if (bCreate)
        {
            if (profile != null)
            {
                System.out.println("wan_profile '" + name + "' already exists, skipping");
                return;
            }

            if (!service.isNameValid(name))
            {
                CommandLine.exitWithError("Name not allowed: " + name);
            }

            profile = new OnlineProfile(name);
            profile.setLicenseKey(LICENSE_KEY);
        }
        else if (profile == null)
        {
            CommandLine.exitWithError("No wan_profile named '" + name + "' (use -create to add it)");
        }

        // Exit happens above if 'profile' is null
        //noinspection DataFlowIssue
        profile.setEmail(email);
        profile.setPassword(password);
        profile.setActivated(true);
        profile.setRetired(false);

        if (bCreate)
        {
            ApplicationError.assertTrue(service.saveOnlineProfile(profile), "Profile already exists: " + name);
        }
        else
        {
            service.updateOnlineProfile(profile);
        }

        System.out.println((bCreate ? "Inserted" : "Updated") + " wan_profile '" + name + "' (" + email + "), activated");
    }
}
