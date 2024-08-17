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

    /**
     * Default instance to use for messaging
     */
    private static final GameMessenger GAME_MESSENGER;

    // Create sole engine messenger
    static {
        GAME_MESSENGER = new GameMessenger();
    }

    public GameMessenger() {
        super();
    }

    private static String getServerURL(String url) {
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
        return GAME_MESSENGER.sendEngineMessage(getServerURL(url), send, listener);
    }

    public static void SendEngineMessageAsync(String url, EngineMessage send, DDMessageListener listener) {
        GAME_MESSENGER.sendEngineMessageAsync(getServerURL(url), send, listener);
    }
}
