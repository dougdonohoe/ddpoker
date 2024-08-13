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

import org.apache.wicket.*;
import org.apache.wicket.protocol.http.*;

import javax.servlet.http.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jan 25, 2009
 * Time: 5:22:12 PM
 * <p/>
 * Subclass to allow permanent moves
 */
public class BaseBufferedWebResponse extends BufferedWebResponse
{
    private final HttpServletResponse httpServletResponse;

    /**
     * Construct
     */
    public BaseBufferedWebResponse(HttpServletResponse httpServletResponse)
    {
        super(httpServletResponse);
        this.httpServletResponse = httpServletResponse;
    }

    // URL to redirect to when response is flushed, if any
    private String movedURL;

    /**
     * Flushes the response buffer by doing a redirect or writing out the buffer. NOTE: The servlet
     * container will close the response output stream.
     */
    public void close()
    {
        // If a redirection was specified
        if (movedURL != null)
        {
            // actually redirect
            httpServletResponse.setHeader("Location", movedURL);
            httpServletResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            redirect = true;
        }
        else
        {
            super.close();
        }
    }

    /**
     * @see org.apache.wicket.Response#reset()
     */
    public void reset()
    {
        movedURL = null;
        super.reset();
    }

    /**
     * permanent redirect
     */
    public final void moved(final String url)
    {
        if (movedURL != null)
        {
            throw new WicketRuntimeException("Already redirecting to '" + movedURL +
                                             "'. Cannot redirect more than once");
        }
        // encode to make sure no caller forgot this
        this.movedURL = encodeURL(url).toString();
    }
}
