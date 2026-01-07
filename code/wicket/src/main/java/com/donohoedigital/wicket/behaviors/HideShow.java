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
package com.donohoedigital.wicket.behaviors;

import com.donohoedigital.wicket.common.JavascriptHideable;
import com.donohoedigital.wicket.models.StringModel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;

/**
 * @author Doug Donohoe
 */
public class HideShow extends AbstractOnClickBehavior
{
    private static final long serialVersionUID = 42L;

    protected Component hideShow;
    protected boolean visibleAtStart;

    public HideShow(Component hideShow, boolean visibleAtStart)
    {
        super(true);

        this.hideShow = hideShow;
        this.visibleAtStart = visibleAtStart;

        hideShow.setOutputMarkupId(true);
        hideShow.add(new AttributeModifier("style", new HideShowModel()));
    }

    /**
     * Get javascript to render
     */
    @Override
    protected StringBuilder getJavascript()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(proto(hideShow)).append(".toggle();");
        return sb;
    }

    /**
     * at render time, decide if the thing we are toggling is visible
     */
    private class HideShowModel extends StringModel
    {
        private static final long serialVersionUID = 42L;

        @Override
        public String getObject()
        {
            boolean vis = visibleAtStart;
            if (hideShow instanceof JavascriptHideable)
            {
                vis = ((JavascriptHideable) hideShow).isVisibleToUser();
            }

            return vis ? "" : "display: none;";
        }
    }
}
