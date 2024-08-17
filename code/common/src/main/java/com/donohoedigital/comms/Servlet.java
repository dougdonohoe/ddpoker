package com.donohoedigital.comms;

public class Servlet {

    /**
     * Utility to build URI part of a URL to keep usages clear.
     * Also see .properties entries "settings.online.server"
     */
    public static String ServletUri(String appName) {
        return '/' + appName + "/servlet/";
    }
}
