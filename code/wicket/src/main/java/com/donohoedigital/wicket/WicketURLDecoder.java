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
package com.donohoedigital.wicket;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Adapted from java.net.URLDecoder, but defines instances for query string decoding versus URL path
 * component decoding. <p/> The difference is important because a space is encoded as a + in a query
 * string, but this is a valid value in a path component (and is therefore not decode back to a
 * space).
 *
 * @author Doug Donohoe
 * @see java.net.URLDecoder
 * @see {@link "http://www.ietf.org/rfc/rfc2396.txt"}
 */
public class WicketURLDecoder
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final boolean decodePlus;

    /**
     * Encoder used to decode name or value components of a query string.<br/><br/>
     *
     * For example: http://org.acme/notthis/northis/oreventhis?buthis=isokay&asis=thispart
     */
    public static final WicketURLDecoder QUERY_INSTANCE = new WicketURLDecoder(true);

    /**
     * Encoder used to decode components of a path.<br/><br/>
     *
     * For example: http://org.acme/foo/thispart/orthispart?butnot=thispart
     */
    public static final WicketURLDecoder PATH_INSTANCE = new WicketURLDecoder(false);

    /**
     * Create decoder
     *
     * @param decodePlus -
     *            whether to decode + to space
     */
    private WicketURLDecoder(boolean decodePlus)
    {
        this.decodePlus = decodePlus;
    }

    /**
     * Calls decode with the application response request encoding as returned by
     * Application.get().getRequestCycleSettings().getResponseRequestEncoding()
     *
     * @param s
     *            Value to encode
     * @return String encoded using default Application request/respose encoding
     */
    public String decode(String s)
    {
        Application app = null;

        try
        {
            app = Application.get();
        }
        catch (WicketRuntimeException ignored)
        {
            log.warn("No current Application found - defaulting encoding to UTF-8");
        }
        return decode(s, app == null ? "UTF-8" : app.getRequestCycleSettings()
                .getResponseRequestEncoding());
    }

    /**
     * @param s
     *            string to decode
     * @param enc
     *            encoding to decode with
     * @return decoded string
     * @see java.net.URLDecoder#decode(String, String)
     */
    public String decode(String s, String enc)
    {
        boolean needToChange = false;
        int numChars = s.length();
        StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        if (enc.length() == 0)
        {
            throw new WicketRuntimeException(new UnsupportedEncodingException(
                    "URLDecoder: empty string enc parameter"));
        }

        char c;
        byte[] bytes = null;
        while (i < numChars)
        {
            c = s.charAt(i);
            switch (c)
            {
                case '+' :
                    sb.append(decodePlus ? ' ' : '+');
                    i++;
                    needToChange = true;
                    break;

                case '%' :
                    /*
                     * Starting with this instance of %, process all consecutive substrings of the
                     * form %xy. Each substring %xy will yield a byte. Convert all consecutive bytes
                     * obtained this way to whatever character(s) they represent in the provided
                     * encoding.
                     */
                    try
                    {
                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        if (bytes == null)
                            bytes = new byte[(numChars - i) / 3];
                        int pos = 0;

                        while (((i + 2) < numChars) && (c == '%'))
                        {
                            bytes[pos++] = (byte)Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            i += 3;
                            if (i < numChars)
                                c = s.charAt(i);
                        }

                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown
                        if ((i < numChars) && (c == '%'))
                            throw new IllegalArgumentException(
                                    "URLDecoder: Incomplete trailing escape (%) pattern");

                        try
                        {
                            sb.append(new String(bytes, 0, pos, enc));
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            throw new WicketRuntimeException(e);
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IllegalArgumentException(
                                "URLDecoder: Illegal hex characters in escape (%) pattern - " +
                                        e.getMessage());
                    }
                    needToChange = true;
                    break;

                default :
                    sb.append(c);
                    i++;
                    break;
            }
        }

        return (needToChange ? sb.toString() : s);
    }
}