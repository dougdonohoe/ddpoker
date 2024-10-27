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

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.BitSet;

/**
 * Adapted from java.net.URLEncoder, but defines instances for query string encoding versus URL path
 * component encoding. <p/> The difference is important because a space is encoded as a + in a query
 * string, but this is a valid value in a path component (and is therefore not decode back to a
 * space).
 *
 * @author Doug Donohoe
 * @see java.net.URLEncoder
 * @see {@link "http://www.ietf.org/rfc/rfc2396.txt"}
 */
public class WicketURLEncoder
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    /**
     * encoder types
     */
    public enum Type {
        /**
         * query type
         */
        QUERY, /**
         * path type
         */
        PATH;
    }

    // list of what not to decode
    protected BitSet dontNeedEncoding;

    // used in decoding
    protected static final int caseDiff = ('a' - 'A');

    /**
     * Encoder used to encode name or value components of a query string.<br/><br/>
     *
     * For example: http://org.acme/notthis/northis/oreventhis?buthis=isokay&asis=thispart
     */
    public static final WicketURLEncoder QUERY_INSTANCE = new WicketURLEncoder(Type.QUERY);

    /**
     * Encoder used to encode components of a path.<br/><br/>
     *
     * For example: http://org.acme/foo/thispart/orthispart?butnot=thispart
     */
    public static final WicketURLEncoder PATH_INSTANCE = new WicketURLEncoder(Type.PATH);

    /**
     * Allow subclass to call constructor.
     *
     * @param type
     *            encoder type
     */
    protected WicketURLEncoder(Type type)
    {
        /*
         * This note from java.net.URLEncoder ==================================
         *
         * The list of characters that are not encoded has been determined as follows:
         *
         * RFC 2396 states: ----- Data characters that are allowed in a URI but do not have a
         * reserved purpose are called unreserved. These include upper and lower case letters,
         * decimal digits, and a limited set of punctuation marks and symbols.
         *
         * unreserved = alphanum | mark
         *
         * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         *
         * Unreserved characters can be escaped without changing the semantics of the URI, but this
         * should not be done unless the URI is being used in a context that does not allow the
         * unescaped character to appear. -----
         *
         * It appears that both Netscape and Internet Explorer escape all special characters from
         * this list with the exception of "-", "_", ".", "*". While it is not clear why they are
         * escaping the other characters, perhaps it is safest to assume that there might be
         * contexts in which the others are unsafe if not escaped. Therefore, we will use the same
         * list. It is also noteworthy that this is consistent with O'Reilly's "HTML: The Definitive
         * Guide" (page 164).
         *
         * As a last note, Intenet Explorer does not encode the "@" character which is clearly not
         * unreserved according to the RFC. We are being consistent with the RFC in this matter, as
         * is Netscape.
         *
         * This bit added by Doug Donohoe ================================== RFC 3986 (2005) updates
         * this (http://tools.ietf.org/html/rfc3986):
         *
         * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
         *
         * pct-encoded = "%" HEXDIG HEXDIG
         *
         * reserved = gen-delims / sub-delims
         *
         * gen-delims = ":" / "/" / "?" / "#" / "[" / "]" / "@"
         *
         * sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "=" // -- PATH
         * COMPONENT -- //
         *
         * path = (see RFC for all variations) path-abempty = *( "/" segment ) segment = *pchar
         * pchar = unreserved / pct-encoded / sub-delims / ":" / "@" // -- QUERY COMPONENT -- //
         *
         * query = *( pchar / "/" / "?" )
         */

        // unreserved
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++)
        {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++)
        {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++)
        {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('~'); // tilde encoded by java.net.URLEncoder version, but RFC is
        // clear on this

        // sub-delims
        dontNeedEncoding.set('!');
        dontNeedEncoding.set('$');
        // "&" needs to be encoded for query stings
        dontNeedEncoding.set('\'');
        // "(" and ")" probably don't need encoding, but we'll be conservative
        dontNeedEncoding.set('*');
        // "+" needs to be encoded for query strings (since it means =
        dontNeedEncoding.set(',');
        // ";" encoded due to use in path and/or query as delim in some instances (e.g., jsessionid)
        // "=" needs to be encoded for query strings

        // pchar
        dontNeedEncoding.set(':'); // allowed and used in wicket interface params
        dontNeedEncoding.set('@');

        // encoding type-specific
        switch (type)
        {
            // this code consistent with java.net.URLEncoder version
            case QUERY :
                dontNeedEncoding.set(' '); /*
                 * encoding a space to a + is done in the encode()
                 * method
                 */
                dontNeedEncoding.set('/'); // to allow direct passing of URL in query
                dontNeedEncoding.set('?'); // to allow direct passing of URL in query
                break;

            // this added to deal with encoding a PATH component
            case PATH :
                // encode ' ' with a % instead of + in path portion

                // path component sub-delim values we do not need to escape
                dontNeedEncoding.set('&');
                dontNeedEncoding.set('=');
                dontNeedEncoding.set('+');
                break;
        }
    }

    /**
     * Calls encode with the application response request encoding as returned by
     * Application.get().getRequestCycleSettings().getResponseRequestEncoding()
     *
     * @param s
     *            Value to encode
     * @return String encoded using default Application request/respose encoding
     */
    public String encode(String s)
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
        return encode(s, app == null ? "UTF-8" : app.getRequestCycleSettings()
                .getResponseRequestEncoding());
    }

    /**
     * @param s
     *            string to encode
     * @param enc
     *            encoding to use
     * @return encoded string
     * @see java.net.URLEncoder#encode(String, String)
     */
    public String encode(String s, String enc)
    {
        boolean needToChange = false;
        StringBuffer out = new StringBuffer(s.length());
        Charset charset;
        CharArrayWriter charArrayWriter = new CharArrayWriter();

        if (enc == null)
            throw new NullPointerException("charsetName");

        try
        {
            charset = Charset.forName(enc);
        }
        catch (IllegalCharsetNameException e)
        {
            throw new WicketRuntimeException(new UnsupportedEncodingException(enc));
        }
        catch (UnsupportedCharsetException e)
        {
            throw new WicketRuntimeException(new UnsupportedEncodingException(enc));
        }

        for (int i = 0; i < s.length();)
        {
            int c = s.charAt(i);
            // System.out.println("Examining character: " + c);
            if (dontNeedEncoding.get(c))
            {
                if (c == ' ')
                {
                    c = '+';
                    needToChange = true;
                }
                // System.out.println("Storing: " + c);
                out.append((char)c);
                i++;
            }
            else
            {
                // convert to external encoding before hex conversion
                do
                {
                    charArrayWriter.write(c);
                    /*
                     * If this character represents the start of a Unicode surrogate pair, then pass
                     * in two characters. It's not clear what should be done if a bytes reserved in
                     * the surrogate pairs range occurs outside of a legal surrogate pair. For now,
                     * just treat it as if it were any other character.
                     */
                    if (c >= 0xD800 && c <= 0xDBFF)
                    {
                        /*
                         * System.out.println(Integer.toHexString(c) + " is high surrogate");
                         */
                        if ((i + 1) < s.length())
                        {
                            int d = s.charAt(i + 1);
                            /*
                             * System.out.println("\tExamining " + Integer.toHexString(d));
                             */
                            if (d >= 0xDC00 && d <= 0xDFFF)
                            {
                                /*
                                 * System.out.println("\t" + Integer.toHexString(d) + " is low
                                 * surrogate");
                                 */
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                }
                while (i < s.length() && !dontNeedEncoding.get((c = s.charAt(i))));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = new byte[0];
                try
                {
                    ba = str.getBytes(charset.name());
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new WicketRuntimeException(e);
                }
                for (int j = 0; j < ba.length; j++)
                {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch))
                    {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch))
                    {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }

        return (needToChange ? out.toString() : s);
    }
}