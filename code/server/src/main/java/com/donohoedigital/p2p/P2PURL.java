/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
 * P2PURL.java
 *
 * Created on November 18, 2004, 1:59 PM
 */

package com.donohoedigital.p2p;

import com.donohoedigital.base.*;

/**
 * @author donohoe
 */
public class P2PURL
{
    private String sProtocol_;
    private String sHost_;
    private int nPort_;
    private String sURI_;

    public static final String PROTOCOL_DELIM = "://";
    public static final String PORT_DELIM = ":";
    public static final String URI_DELIM = "/";

    // sample
    // poker://192.211.1.110:11885/n-2/WXZ-741

    /**
     * Creates a new instance of P2PURL
     */
    public P2PURL(String spec)
    {
        // get first part

        // protocol
        int n = spec.indexOf(PROTOCOL_DELIM);
        ApplicationError.assertTrue(n >= 0, "Invalid P2PURL", spec);

        sProtocol_ = spec.substring(0, n);
        String rest = spec.substring(n + 3);

        // host
        n = rest.indexOf(PORT_DELIM);
        ApplicationError.assertTrue(n >= 0, "Invalid P2PURL", spec);

        sHost_ = rest.substring(0, n);
        rest = rest.substring(n + 1);

        // port
        n = rest.indexOf(URI_DELIM);
        ApplicationError.assertTrue(n >= 0, "Invalid P2PURL", spec);

        String sPort = rest.substring(0, n);
        try
        {
            nPort_ = Integer.parseInt(sPort);
        }
        catch (NumberFormatException nfe)
        {
            ApplicationError.assertTrue(false, "Invalid P2PURL", spec);
        }

        // URI
        sURI_ = rest.substring(n + 1);
    }

    /**
     * Retrurn string version of URL (should equal spec used to create this url)
     */
    public String toString()
    {
        return sProtocol_ + PROTOCOL_DELIM + sHost_ + PORT_DELIM + nPort_ + URI_DELIM + sURI_;
    }

    /**
     * get host
     */
    public String getHost()
    {
        return sHost_;
    }

    /**
     * Get port
     */
    public int getPort()
    {
        return nPort_;
    }

    /**
     * Get URI
     */
    public String getURI()
    {
        return sURI_;
    }

}
