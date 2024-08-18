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
