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
package com.donohoedigital.wicket.annotations;

//import org.apache.wicket.request.target.coding.*;

/**
 * Override to deal with pre-fetch browser/toolbar (?) bug that tries to get resources
 * incorrectly (e.g., /home/resource/foo instead of /resources/foo).  This fixes these errors:
 * ERROR URL fragment has unmatched key/value pair: resources/org.wicketstuff.prototype.PrototypeResourceReference/prototype.js
 *
 * @author Doug Donohoe
 */
public class BookmarkablePage //extends BookmarkablePageRequestTargetUrlCodingStrategy
{
//    /**
//     * Construct.
//     */
//    public <C extends org.apache.wicket.Page> BookmarkablePage(String mountPath, Class<C> bookmarkablePageClass, String pageMapName)
//    {
//        super(mountPath, bookmarkablePageClass, pageMapName);
//    }
//
//    /**
//     * @see org.apache.wicket.request.target.coding.IRequestTargetUrlCodingStrategy#matches(org.apache.wicket.IRequestTarget)
//     */
//    @Override
//    public boolean matches(String path)
//    {
//        // mount path should not contain /resources/ (ends up returning a 404).
//
//        if (path.contains("/resources/")) return false;
//
//        return super.matches(path);
//    }
}
