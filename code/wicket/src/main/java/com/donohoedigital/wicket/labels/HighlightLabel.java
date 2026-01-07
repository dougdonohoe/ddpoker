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
package com.donohoedigital.wicket.labels;

import com.donohoedigital.base.Utils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;

import java.util.regex.Pattern;

/**
 * class to replace text that matches highlight with &lt;span class=cssClass>highlight&lt;/span>
 */
public class HighlightLabel extends Label
{
    private static final long serialVersionUID = 42L;

    private Pattern pattern;
    private String cssClass;

    public HighlightLabel(String id, String highlight, String cssClass, boolean caseInsensitive)
    {
        this(id, new String[] {highlight}, cssClass, caseInsensitive);
    }

    public HighlightLabel(String id, String text, String highlight, String cssClass, boolean caseInsensitive)
    {
        this(id, text, new String[] {highlight}, cssClass, caseInsensitive);
    }

    public HighlightLabel(String id, String[] highlight, String cssClass, boolean caseInsensitive)
    {
        super(id);
        init(highlight, cssClass, caseInsensitive);
    }

    public HighlightLabel(String id, String text, String[] highlight, String cssClass, boolean caseInsensitive)
    {
        super(id, text);
        init(highlight, cssClass, caseInsensitive);
    }

    private void init(String[] h, String css, boolean caseInsensitive)
    {
        setEscapeModelStrings(false);
        cssClass = css;

        // create pattern
        if (h.length > 0)
        {
            StringBuilder sb = new StringBuilder();
            for (String pat : h)
            {
                if (pat == null || pat.length() == 0) continue;

                if (sb.length() > 0) sb.append('|');
                sb.append('(');
                sb.append(Pattern.quote(Utils.encodeHTML(pat, true, true)));
                sb.append(")+");
            }

            if (sb.length() > 0)
            {
                sb.insert(0, '(');
                sb.append(')');
                pattern = Pattern.compile(sb.toString(), caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
            }
        }
    }

    // do replacement
    private String getHighlightedText()
    {
        String text = Utils.encodeHTML(getDefaultModelObjectAsString(), true, true);

        if (pattern == null || text == null) return text;

        StringBuilder replacement = new StringBuilder()
                .append("<span class=\"").append(cssClass).append("\">")
                .append("$0")
                .append("</span>");
        return pattern.matcher(text).replaceAll(replacement.toString());
    }

    /**
	 * Do our magic
	 */
	@Override
	public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag)
	{
		replaceComponentTagBody(markupStream, openTag, getHighlightedText());
	}
}
