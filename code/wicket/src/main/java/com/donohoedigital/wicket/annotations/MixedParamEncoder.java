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
package com.donohoedigital.wicket.annotations;

import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.encoding.UrlDecoder;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MixedParamEncoder implements IPageParametersEncoder {

    private final String[] parameterNames;

    public MixedParamEncoder(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    @Override
    public Url encodePageParameters(PageParameters parameters) {
        Url url = new Url();
        Set<String> parameterNamesToAdd = new HashSet<>(parameters.getNamedKeys());

        // Find index of last specified parameter
        boolean foundParameter = false;
        int lastSpecifiedParameter = parameterNames.length;
        while (lastSpecifiedParameter != 0 && !foundParameter)
        {
            String param = getString(parameters, parameterNames[--lastSpecifiedParameter]);
            foundParameter = param != null && !param.isEmpty();
        }

        // append parameters we found
        if (foundParameter)
        {
            for (int i = 0; i <= lastSpecifiedParameter; i++)
            {
                // NOTE: adding to url segments will UrlEncode the value (which we need to reverse below)
                url.getSegments().add(urlEncodePathComponent(getString(parameters, parameterNames[i])));
                parameterNamesToAdd.remove(parameterNames[i]);
            }
        }

        // add remaining as query string
        if (!parameterNamesToAdd.isEmpty())
        {
            for (String parameterName : parameterNamesToAdd)
            {
                String param = getString(parameters, parameterName);
                if (param != null && !param.isEmpty())
                {
                    url.getQueryParameters().add(new Url.QueryParameter(parameterName, param));
                }
            }
        }
        return url;
    }

    private String getString(final PageParameters parameters, final String key)
    {
        final Object o = parameters.get(key);
        if (o == null)
        {
            return null;
        }
        else if (o.getClass().isArray() && Array.getLength(o) > 0)
        {
            // if it is an array just get the first value
            final Object arrayValue = Array.get(o, 0);
            if (arrayValue == null)
            {
                return null;
            }
            else
            {
                return arrayValue.toString();
            }

        }
        else
        {
            return o.toString();
        }
    }

    @Override
    public PageParameters decodePageParameters(Url url) {
        PageParameters params = new PageParameters();

        // add all url query parameters
        List<Url.QueryParameter> queryParameters = url.getQueryParameters();
        queryParameters.forEach(qp ->
                params.set(qp.getName(), qp.getValue()));

        // add path components
        String urlPath = url.getPath();
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }

        if (!urlPath.isEmpty())
        {
            String[] pathParts = urlPath.split("/");
            //noinspection StatementWithEmptyBody
            if (pathParts.length > parameterNames.length)
            {
                //logger.warn("Too many path parts: " + WicketUtils.getWebRequest().getURL());
            }

            for (int i = 0; i < pathParts.length && i < parameterNames.length; i++)
            {
                // only set parameter if it didn't come down in another form (e.g., query
                // string or posted form values).
                if (params.get(parameterNames[i]) != null)
                {
                    params.set(parameterNames[i], urlDecodePathComponent(pathParts[i]));
                }
            }
        }

        return params;
    }

    ////
    //// code below to allow any character in the path.  We deal with:
    ////
    //// . and .. which are interpreted by the servlet container
    //// / and \ which are also path components
    //// the null/empty param.
    ////
    //// All these characters are replaced with aliases - an escape char
    //// and a normal ascii char for the special character
    ////

    private static final char ESCAPE_CHAR = ':';
    private static final char NULL_CHAR = '-';

    private static final String NULL_CHAR_STRING = String.valueOf(NULL_CHAR);
    private static final String NULL_CHAR_ALIAS = String.valueOf(ESCAPE_CHAR) + NULL_CHAR;
    private static final String DOT_ALIAS = "" + ESCAPE_CHAR + 'd';
    private static final String DOTDOT_ALIAS = DOT_ALIAS + DOT_ALIAS;

    private static final char SLASH_ALIAS = 's';
    private static final char BACKSLASH_ALIAS = 'b';

    private String urlEncodePathComponent(String value)
    {
        String enc;
        if (value == null || value.isEmpty())
        {
            enc = NULL_CHAR_STRING;
        }
        else if (value.equals(NULL_CHAR_STRING))
        {
            enc = NULL_CHAR_ALIAS;
        }
        else if (value.equals("."))
        {
            enc = DOT_ALIAS;
        }
        else if (value.equals(".."))
        {
            enc = DOTDOT_ALIAS;
        }
        else
        {
            enc = encodeSpecial(value);
        }
        return enc;
    }

    private String urlDecodePathComponent(String value)
    {
        if (value == null || value.isEmpty() || value.equals(NULL_CHAR_STRING))
        {
            return null;
        }
        else if (value.equals(NULL_CHAR_ALIAS))
        {
            return NULL_CHAR_STRING;
        }
        else if (value.equals(DOT_ALIAS))
        {
            return ".";
        }
        else if (value.equals(DOTDOT_ALIAS))
        {
            return "..";
        }

        // Undo UrlEncode done above
        return UrlDecoder.PATH_INSTANCE.decode(decodeSpecial(value), StandardCharsets.UTF_8);
    }

    /**
     * Encode string to handle . / \
     */
    private String encodeSpecial(String value)
    {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        char c;

        for (int i = 0; i < value.length(); i++)
        {
            c = value.charAt(i);

            if (c == '/')
            {
                sb.append(ESCAPE_CHAR);
                sb.append(SLASH_ALIAS);
            }
            else if (c == '\\')
            {
                sb.append(ESCAPE_CHAR);
                sb.append(BACKSLASH_ALIAS);
            }
            else if (c == ESCAPE_CHAR)
            {
                sb.append(ESCAPE_CHAR);
                sb.append(ESCAPE_CHAR);
            }
            else
            {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Decode
     */
    private String decodeSpecial(String value)
    {
        StringBuilder sb = new StringBuilder(value.length());
        char c;

        boolean decode = false;

        for (int i = 0; i < value.length(); i++)
        {
            c = value.charAt(i);

            if (decode)
            {
                decode = false;

                switch (c)
                {
                    case SLASH_ALIAS:
                        c = '/';
                        break;

                    case BACKSLASH_ALIAS:
                        c = '\\';
                        break;

                    case ESCAPE_CHAR:
                        break;

                    default:
                        break; // just ignore unknown decode
                }
            }
            else if (c == ESCAPE_CHAR)
            {
                decode = true;
                continue;
            }

            sb.append(c);
        }

        return sb.toString();
    }
}
