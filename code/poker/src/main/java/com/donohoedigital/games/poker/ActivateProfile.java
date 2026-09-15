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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.CommandLine;
import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.BaseCommandLineApp;
import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.poker.engine.PokerConstants;

import java.util.List;

/**
 * Marks a local player profile as online-activated with the given email and
 * password, doing client-side what PlayerProfileDialog does after the server
 * confirms activation.  Skips the server and the activation email, for testing.
 * <p>
 * With -create, creates the profile the way ProfileList.addProfile() does if it doesn't
 * exist, and leaves it alone if it does.  Without it, the profile must exist and its
 * email and password are updated.
 * <p>
 * This only updates the local .dat file.  Joining the lobby also needs a matching
 * activated row in the server's wan_profile table (see ChatServer.java:219), which is
 * ActivateOnlineProfile in pokerserver.  The activateprofile script runs both.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class ActivateProfile
{
    // option names - keep in sync with ActivateOnlineProfile
    public static final String OPTION_NAME = "name";
    public static final String OPTION_EMAIL = "email";
    public static final String OPTION_PASSWORD = "password";
    public static final String OPTION_CREATE = "create";

    /**
     * Implements command line application interface.
     */
    private static final class ActivateProfileApp extends BaseCommandLineApp
    {
        private ActivateProfileApp(String sConfigName, String[] args)
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

    static void main(String[] args)
    {
        // picks ~/.dd-poker3, so set before logging and config init (as BaseApp does)
        Utils.setVersionString(PokerConstants.VERSION.getMajorAsString());

        ActivateProfileApp app = new ActivateProfileApp("plain", args);
        TypedHashMap htOptions = app.getCommandLineOptions();
        String name = htOptions.getString(OPTION_NAME);
        String email = htOptions.getString(OPTION_EMAIL);
        String password = htOptions.getString(OPTION_PASSWORD);
        boolean bCreate = htOptions.getBoolean(OPTION_CREATE, false);

        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        PlayerProfile profile = null;
        for (BaseProfile p : profiles)
        {
            if (p.getName().equals(name))
            {
                profile = new PlayerProfile(p.getFile(), true);
                break;
            }
        }

        if (bCreate)
        {
            if (profile != null)
            {
                System.out.println("Profile '" + name + "' already exists in " + profile.getFile() + ", skipping");
                return;
            }

            // same limit the profile dialog enforces
            if (name.length() > PlayerProfileDialog.PLAYER_NAME_LIMIT)
            {
                CommandLine.exitWithError("Name longer than " + PlayerProfileDialog.PLAYER_NAME_LIMIT + " characters: " + name);
            }

            // as PlayerProfileOptions.createEmptyProfile() and ProfileList.addProfile()
            profile = new PlayerProfile(name);
            profile.initCheck();
            profile.initFile();
            profile.setCreateDate();
        }
        else if (profile == null)
        {
            // fail hard on no match - don't fall through to some other profile
            StringBuilder sb = new StringBuilder();
            sb.append("No profile named '").append(name).append("' in ")
              .append(PlayerProfile.getProfileDir(BaseProfile.PROFILE_DIR)).append(". Available:");
            for (BaseProfile p : profiles)
            {
                sb.append("\n  ").append(p.getName()).append(" (").append(p.getFileName()).append(")");
            }
            CommandLine.exitWithError(sb.toString());
        }

        // Exit happens above if 'profile' is null
        //noinspection DataFlowIssue
        profile.setActivated(true);
        profile.setEmail(email);
        profile.setPassword(password);
        profile.save();

        System.out.println((bCreate ? "Created" : "Updated") + " activated profile '" + name + "' (" + email + ") in " + profile.getFile());
    }
}
