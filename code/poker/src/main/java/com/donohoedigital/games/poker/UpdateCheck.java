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
/*
 * UpdateCheck.java
 *
 * Created on September 9, 2026
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.comms.Version;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.gui.DDOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.SwingWorker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Tells the user when a newer DD Poker has been released.  There is no in-app updater - the game
 * is installed from an installer on GitHub Releases - so all this does is notice and point at the
 * download links in the README.
 *
 * <p>How it asks: {@code github.com/.../releases/latest} answers a bare {@code HEAD} with a 302 to
 * {@code .../releases/tag/<tag>}, and the tags this project publishes are exactly
 * {@link Version#toString} output (3.1.8, 3.1, 2.0b6.4).  So the whole check is one request with
 * redirects turned off, and the last path segment of the {@code Location} header handed to
 * {@link Version#parse}.  That is smaller than the JSON API, needs no JSON parser (none is on the
 * classpath), and has no unauthenticated rate limit; like the API, it already skips drafts and
 * prereleases.  This is unrelated to {@code DDMessageCheck}, which asks the DD Poker server for
 * messages and does no version comparison at all.
 *
 * <p>Nothing here is allowed to get in the user's way.  The request runs on a
 * {@link SwingWorker} so startup never waits on it, and the automatic check says nothing at all
 * when it fails - an offline laptop is not an error the user needs a dialog about.  The
 * <i>Check for Updates</i> button in Options does report a failure, because there someone asked.
 *
 * @author Doug Donohoe
 */
public final class UpdateCheck
{
    private static final Logger logger = LogManager.getLogger(UpdateCheck.class);

    /** Redirects to the newest non-draft, non-prerelease release's tag page. */
    private static final String LATEST_URL = "https://github.com/dougdonohoe/ddpoker/releases/latest";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    /** Prefs node and key holding the version we last told the user about. */
    private static final String PREFS_NODE = "/update";
    private static final String PREFS_KEY_NOTIFIED = "notified.version";

    // Both flags are read and written on the EDT only - checkAtStartup is called from there, and
    // SwingWorker.done() runs there too.

    /**
     * The automatic check runs once per launch.  The start menu is re-entered every time the user
     * leaves a game, which would otherwise ask again.
     */
    private static boolean checkedThisSession_;

    /** Set while a check is in flight, so nothing stacks up workers. */
    private static boolean running_;

    private UpdateCheck()
    {
    }

    /**
     * The check made when the start menu opens.  Silent unless there is something to say: a newer
     * release the user has not already been told about.
     *
     * @param stillShowing false when the user has moved on from the start menu.  Checked when the
     *                     answer comes back, not when the request goes out - the user can start a
     *                     game while it is in flight, and nothing may interrupt that with a
     *                     dialog.  Deliberately without recording the version in that case, so
     *                     the next launch offers the news again.
     */
    public static void checkAtStartup(GameContext context, BooleanSupplier stillShowing)
    {
        if (checkedThisSession_) return;
        checkedThisSession_ = true;

        fetchAsync(latest -> {
            if (latest == null) return; // offline, firewalled, GitHub down - not worth a dialog
            if (!stillShowing.getAsBoolean()) return;

            if (!latest.isNewerThan(PokerConstants.VERSION)) return;
            if (latest.toString().equals(lastNotified())) return; // already said so, once is enough

            setLastNotified(latest);
            showAvailable(context, latest);
        });
    }

    /**
     * The check behind the <i>Check for Updates</i> button in Options.  Always reports, and
     * ignores both the once-per-launch and once-per-version guards - someone asked, so answer.
     */
    public static void checkNow(GameContext context)
    {
        fetchAsync(latest -> {
            if (latest == null)
            {
                PokerUtils.displayInformationDialog(context,
                                                    PropertyConfig.getMessage("msg.update.failed"),
                                                    "msg.windowtitle.checkUpdate", null);
            }
            else if (latest.isNewerThan(PokerConstants.VERSION))
            {
                // Record it here too: having been shown the news, the user does not need it
                // repeated at the next launch.
                setLastNotified(latest);
                showAvailable(context, latest);
            }
            else
            {
                PokerUtils.displayInformationDialog(context,
                                                    PropertyConfig.getMessage("msg.update.none",
                                                                              PokerConstants.VERSION),
                                                    "msg.windowtitle.checkUpdate", null);
            }
        });
    }

    private static void showAvailable(GameContext context, Version latest)
    {
        PokerUtils.displayInformationDialog(context,
                                            PropertyConfig.getMessage("msg.update.available",
                                                                      latest, PokerConstants.VERSION),
                                            "msg.windowtitle.updateAvailable", null);
    }

    /**
     * Runs {@link #fetchLatest} off the EDT and hands the result (possibly null) back on it.
     */
    private static void fetchAsync(Consumer<Version> onResult)
    {
        if (running_) return;
        running_ = true;

        new SwingWorker<Version, Void>()
        {
            @Override
            protected Version doInBackground()
            {
                return fetchLatest();
            }

            @Override
            protected void done()
            {
                running_ = false;
                Version latest;
                try
                {
                    latest = get();
                }
                catch (Exception e)
                {
                    // fetchLatest swallows its own failures, so this is the worker itself going
                    // wrong (interrupted, say) - same outcome either way.
                    logger.debug("update check worker failed: {}", e.toString());
                    latest = null;
                }
                onResult.accept(latest);
            }
        }.execute();
    }

    /**
     * Asks GitHub for the newest release and returns its version.
     *
     * @return the latest released version, or null if it could not be determined for any reason -
     * no network, an unexpected response, a tag that is not a version.  Never throws.
     */
    static Version fetchLatest()
    {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER) // the redirect *is* the answer
                .connectTimeout(CONNECT_TIMEOUT)
                .build())
        {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_URL))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "DD Poker/" + PokerConstants.VERSION)
                    .timeout(REQUEST_TIMEOUT)
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            String location = response.headers().firstValue("location").orElse(null);
            if (location == null)
            {
                logger.info("update check: no redirect from {} (status {})", LATEST_URL, response.statusCode());
                return null;
            }

            Version latest = Version.parse(tagFrom(location));
            if (latest == null) logger.info("update check: unrecognized release tag in {}", location);
            return latest;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (Exception e)
        {
            // Offline is the common case and is not worth a stack trace in the user's log.
            logger.debug("update check failed: {}", e.toString());
            return null;
        }
    }

    /**
     * The tag out of a {@code .../releases/tag/3.1.8} redirect target.
     */
    private static String tagFrom(String location)
    {
        int slash = location.lastIndexOf('/');
        return slash < 0 ? location : location.substring(slash + 1);
    }

    private static String lastNotified()
    {
        return prefs().get(PREFS_KEY_NOTIFIED, null);
    }

    private static void setLastNotified(Version v)
    {
        prefs().put(PREFS_KEY_NOTIFIED, v.toString());
    }

    private static Preferences prefs()
    {
        return DDOption.getOptionPrefs(GameEngine.getGameEngine().getPrefsNodeName() + PREFS_NODE);
    }
}
