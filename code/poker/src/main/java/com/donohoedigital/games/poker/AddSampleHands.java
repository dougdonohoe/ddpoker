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
import com.donohoedigital.config.Activation;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.config.Prefs;
import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;

import java.util.List;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 * Plays the {@link SampleHands} into a player profile's hand-history database, as a new
 * "Sample Hands" tournament, so the hand history and statistics screens have every kind of hand to show.
 * <p>
 * The database is the one the game opens for that profile, whose name includes the game's license
 * key: the stored one, or the one given with -key (as the player1/player2 aliases do).
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class AddSampleHands
{
    private static final String OPTION_NAME = "name";
    private static final String OPTION_KEY = "key";

    static void main(String[] args)
    {
        // picks ~/.dd-poker3, so set before logging and config init (as BaseApp does)
        String version = PokerConstants.VERSION.getMajorAsString();
        Utils.setVersionString(version);

        // the game's prefs root (BaseApp sets it before ConfigManager can, and the first one sticks)
        Prefs.setRootNodeName("poker" + version);

        // parse directly rather than with BaseCommandLineApp, whose command-line logging setup
        // would clash with the headless engine's below
        CommandLine.setUsage(AddSampleHands.class.getName() + " [options]");
        CommandLine.addStringOption(OPTION_NAME, null);
        CommandLine.setDescription(OPTION_NAME, "profile name (exact match)", "name");
        CommandLine.setRequired(OPTION_NAME);
        CommandLine.addStringOption(OPTION_KEY, null);
        CommandLine.setDescription(OPTION_KEY, "activation key the game was started with (testing)", "key");
        CommandLine.parseArgs(args);
        TypedHashMap htOptions = CommandLine.getOptions();
        String name = htOptions.getString(OPTION_NAME);

        // a headless engine (it sets up logging), which storing hands needs to tell whether a
        // player is local, and the poker config, as the headless client loads it
        new PokerMain("poker", "poker", new String[0], true /* headless */, false /* loadNames */);
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // find the profile
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
        if (profile == null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("No profile named '").append(name).append("' in ")
              .append(PlayerProfile.getProfileDir(BaseProfile.PROFILE_DIR)).append(". Available:");
            for (BaseProfile p : profiles)
            {
                sb.append("\n  ").append(p.getName()).append(" (").append(p.getFileName()).append(")");
            }
            CommandLine.exitWithError(sb.toString());
            return;
        }

        // the game's license key (headless, this engine has its own) - like the game, -key only
        // counts with settings.debug.override.key on (GameEngine warns otherwise)
        String key = TESTING(EngineConstants.TESTING_OVERRIDE_KEY) ? htOptions.getString(OPTION_KEY, null) : null;
        if (key == null)
        {
            key = Prefs.getUserPrefs(GameEngine.getKeyNodeName("poker", PokerConstants.VERSION)).get(Activation.REGKEY, null);
            if (key == null)
            {
                CommandLine.exitWithError("No stored license key - run the game once first, or pass -key");
                return;
            }
        }

        // an old database has to be upgraded by the game first
        if (PokerDatabaseMigrator.needsMigration(PokerDatabase.getDatabaseDirectory()))
        {
            CommandLine.exitWithError("Hand-history databases need upgrading - run the game once first");
            return;
        }

        String database = PokerDatabase.getDatabaseName(profile, GameEngine.getPublicUseKey(key));
        PokerDatabase.init(profile, null, database);
        try
        {
            PokerGame game = new PokerGame(null);
            game.setProfile(new TournamentProfile("Sample Hands"));
            List<Integer> ids = SampleHands.playAll(game, profile);
            System.out.println("Added " + ids.size() + " sample hands for '" + name + "' to " +
                               PokerDatabase.getDatabaseDirectory() + "/" + database);
        }
        finally
        {
            PokerDatabase.init(null);
        }
    }
}
