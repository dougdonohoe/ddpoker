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
package com.donohoedigital.config;

import org.apache.logging.log4j.*;

import java.io.*;
import java.util.*;

/**
 * OutputStream implementation for our redirect to Log4j.
 */
public class RedirectStream
{
    private static final String LINE_SEP = System.getProperty("line.separator");

    private final RedirectOutput stream;
    private final boolean error;
    private final Logger myLogger;

    private final Set<StreamSniffer> sniffers = Collections.synchronizedSet(new HashSet<StreamSniffer>());

    /**
     * Default constructor.
     */
    RedirectStream(String type, boolean error)
    {
        stream = new RedirectOutput();
        this.error = error;
        this.myLogger = LogManager.getLogger("com.donohoedigital." + type);
    }

    /**
     * Stream
     */
    ByteArrayOutputStream getStream()
    {
        return stream;
    }

    /**
     * Override flush
     */
    private class RedirectOutput extends ByteArrayOutputStream
    {
        public RedirectOutput()
        {
            super();
        }

        /**
         * override to get message and send to log4j
         */
        @Override
        public void flush() throws IOException
        {
            //	Get a String representation of the buffer
            String message = toString();

            //	skip if empty or only a new line
            if (!LINE_SEP.equals(message) && !"".equals(message))
            {
                notifySniffers(message);
                if (error)
                {
                    myLogger.error(message);
                }
                else
                {
                    myLogger.debug(message);
                }
            }

            //	Lastly we reset the underlying buffer so that it can be used again.
            reset();
        }
    }

    /**
     * Add sniffer
     */
    public void addSniffer(StreamSniffer sniffer)
    {
        sniffers.add(sniffer);
    }

    /**
     * Remove sniffer
     */
    public void removeSniffer(StreamSniffer sniffer)
    {
        sniffers.remove(sniffer);
    }

    /**
     * Notify sniffers
     */
    private void notifySniffers(String s)
    {
        synchronized (sniffers)
        {
            for (StreamSniffer sniffer : sniffers)
            {
                sniffer.sniff(s);
            }
        }
    }
}
