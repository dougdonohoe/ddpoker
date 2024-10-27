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
package com.donohoedigital.wicket.panels;

import com.donohoedigital.wicket.WicketUtils;
import com.donohoedigital.wicket.common.CountPageable;
import com.donohoedigital.wicket.labels.PluralLabelProvider;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 24, 2008
 * Time: 9:04:07 AM
 * Bookmarkable paging navigator.  Numbers pages starting at 1 so that
 * bookmarked links 'page=x' matches the display values to the user.
 */
public class BookmarkablePagingNavigator extends BoxPagingNavigator
{
    private static final long serialVersionUID = 42L;

    //private static Logger logger = LogManager.getLogger(BookmarkablePagingNavigator.class);

    protected String pageParamName;
    protected PageParameters linkToParams;
    protected Class<? extends Page> linkTo;



    public BookmarkablePagingNavigator(String id, CountPageable pageable, PluralLabelProvider itemName,
                                       Class<? extends Page> linkTo,
                                       PageParameters linkToParams,
                                       String pageParamName)
    {
        this(id, pageable, 3, true, itemName, linkTo, linkToParams, pageParamName);
    }

    public BookmarkablePagingNavigator(String id, CountPageable pageable, int padding, boolean showAnchors, PluralLabelProvider itemName,
                                       Class<? extends Page> linkTo,
                                       PageParameters linkToParams,
                                       String pageParamName)
    {
        super(id, pageable, padding, showAnchors, itemName);
        this.pageParamName = pageParamName;
        this.linkTo = linkTo;
        this.linkToParams = linkToParams;
        setCurrentPageValidated(WicketUtils.getAsInt(linkToParams, pageParamName, 1));
    }

    /**
     * bookmarkable link to given page
     */
    @Override
    protected Link<?> getLink(String id, int pageNum)
    {
        PageParameters linkParams = new PageParameters(linkToParams);
        linkParams.set(pageParamName, pageNum);
        BookmarkablePageLink link = new BookmarkablePageLink(id, linkTo, linkParams);
        link.setEnabled(pageNum != getCurrentPage());
        return link;
    }

}
