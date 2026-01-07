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
package com.donohoedigital.games.engine;

import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.comms.Servlet;
import com.donohoedigital.games.comms.EngineMessage;
import com.donohoedigital.games.comms.EngineMessenger;
import com.donohoedigital.games.config.EngineConstants;

/**
 * GameMessenger wraps EngineMessenger, getting server URL from options (instead of .properties)
 */
public class GameMessenger extends EngineMessenger {

    // Lazy initialization via classloading magic means the messenger won't be created until
    // getGameMessengerInstance() is called
    private static class Holder {
        private static final GameMessenger INSTANCE = new GameMessenger();
    }

    private static GameMessenger getGameMessengerInstance() {
        return GameMessenger.Holder.INSTANCE;
    }

    public GameMessenger() {
        // parent doesn't need to lookup URL since we fetch from preferences
        super(false);
    }

    private static String getBaseServerUrl(String url) {
        if (url != null) return url;
        GameEngine engine = GameEngine.getGameEngine();
        EnginePrefs prefs = engine.getPrefsNode();
        String serverAndPort = prefs.getStringOption(EngineConstants.OPTION_ONLINE_SERVER);
        // Result is something like http://free.ddpoker.com:80/poker/servlet/
        return "http://" + serverAndPort + Servlet.ServletUri(engine.name());
    }

    @Override
    public boolean isDisabled(String url) {
        EnginePrefs prefs = GameEngine.getGameEngine().getPrefsNode();
        return !prefs.getBooleanOption(EngineConstants.OPTION_ONLINE_ENABLED);
    }

    public static EngineMessage SendEngineMessage(String url, EngineMessage send, DDMessageListener listener) {
        return getGameMessengerInstance().sendEngineMessage(getBaseServerUrl(url), send, listener);
    }

    public static void SendEngineMessageAsync(String url, EngineMessage send, DDMessageListener listener) {
        getGameMessengerInstance().sendEngineMessageAsync(getBaseServerUrl(url), send, listener);
    }
}
