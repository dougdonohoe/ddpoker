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

import com.donohoedigital.wicket.common.CountPageable;
import com.donohoedigital.wicket.components.VoidContainer;
import com.donohoedigital.wicket.components.VoidPanel;
import com.donohoedigital.wicket.labels.GroupingIntegerLabel;
import com.donohoedigital.wicket.labels.PluralLabel;
import com.donohoedigital.wicket.labels.PluralLabelProvider;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * @author Doug Donohoe
 */
public class BoxPagingNavigator extends VoidPanel
{
    private static final long serialVersionUID = 42L;

    protected CountPageable pageable;
    protected boolean showAnchors;
    protected int padding;
    protected PluralLabelProvider itemName;

    public BoxPagingNavigator(String id, CountPageable pageable, PluralLabelProvider itemName)
    {
        this(id, pageable, 3, true, itemName);
    }

    public BoxPagingNavigator(String id, CountPageable pageable, int padding, boolean showAnchors,
                              PluralLabelProvider itemName)
    {
        super(id);
        this.showAnchors = showAnchors;
        this.itemName = itemName;
        this.pageable = pageable;
        this.padding = padding;
    }

    @Override
    protected void onBeforeRender()
    {
        // remove all children
        removeAll();

        // set pageable to page that should be displayed (as retrieved from params)
        int current = getCurrentPage();

        // get counts
        int pageCount = (int) pageable.getPageCount();
        int totalItemCount = pageable.getTotalItemCount();
        int rowsPerPage = pageable.getPageSize();

        int start = ((current - 1) * rowsPerPage) + 1;
        int end = start + rowsPerPage - 1;
        if (end > totalItemCount) end = totalItemCount;


        // if more than one page, display links
        if (pageCount > 1)
        {
            // links are in a fragment
            Fragment pages = new Fragment("pages", "pageLinks", this);
            add(pages);

            // always show prev/next
            pages.add(getLink("prev", validatePageNumber(current - 1)));
            pages.add(getLink("next", validatePageNumber(current + 1)));

            // hard work
            addPageLinks(pages);
        }
        else
        {
            // no pages, hide
            add(new VoidContainer("pages").setVisible(false));
        }

        // display size info
        add(new GroupingIntegerLabel("start", start).setVisible(pageCount > 1));
        add(new GroupingIntegerLabel("end", end));
        add(new GroupingIntegerLabel("size", totalItemCount));
        add(new PluralLabel("sizeLabel", totalItemCount, itemName));

        super.onBeforeRender();
    }

    private void addPageLinks(WebMarkupContainer pages)
    {
        int visible = (padding * 2) + 1;
        if (showAnchors) visible += 4; // 2 for anchors, 2 for '...'

        int current = getCurrentPage();
        int end = (int) pageable.getPageCount();
        int begin = 1;

        int first = validatePageNumber(current - padding);
        int last = validatePageNumber(current + padding);

        // adjust so we always show same number of spots
        // NOTE: this logic is ugly, sure, but it works!
        if (showAnchors)
        {
            if (last < (visible - 2) && first <= 2) last = (visible - 2);
            if (first > (end - (visible - 3))) first = end - (visible - 3);

            // if first is 3, set to 2 so we show [2]
            if (first == 3) first = 2;

            // likewise for the other end
            if (last == (end - 2)) last = end - 1;

            // make sure we don't exceed the bounds
            if (last > end) last = end;
            if (first < 2) first = 2;
        }
        else
        {
            if (last < (first + visible - 1)) last = first + visible - 1;
            if (last > end) last = end;

            if (first > (last - visible + 1)) first = last - visible + 1;
            if (first < 1) first = 1;
        }

        // first page anchor
        Link<?> firstLink = getLink("first", begin);
        firstLink.setVisible(showAnchors);
        pages.add(firstLink);

        // dots
        VoidContainer leftDots = new VoidContainer("leftDots");
        leftDots.setVisible(showAnchors && (first > begin + 1));
        pages.add(leftDots);

        // page links
        RepeatingView rv = new RepeatingView("pageLinks");
        pages.add(rv);

        for (int i = first; i <= last; i++)
        {
            Link<?> link = getLink(String.valueOf(i), i);
            link.add(new GroupingIntegerLabel("pageLabel", i));
            rv.add(link);
        }

        // dots
        VoidContainer rightDots = new VoidContainer("rightDots");
        rightDots.setVisible(showAnchors && (end > last + 1));
        pages.add(rightDots);

        // last page anchor
        Link<?> lastLink = getLink("last", end);
        lastLink.add(new GroupingIntegerLabel("lastLabel", end));
        lastLink.setVisible(showAnchors && last != end);
        pages.add(lastLink);

    }

    /**
     * validate page and set it
     */
    protected void setCurrentPageValidated(int pg)
    {
        setCurrentPage(validatePageNumber(pg));
    }

    /**
     * convert our page to the pageable 'page space'
     */
    private void setCurrentPage(int page)
    {
        pageable.setCurrentPage(page - 1);
    }

    /**
     * get current page in our 'page space'
     */
    protected int getCurrentPage()
    {
        return (int) pageable.getCurrentPage() + 1;
    }

    /**
     * validate page number, returning corrected one (if necessary)
     */
    private int validatePageNumber(int pg)
    {
        int count = (int) pageable.getPageCount();
        if (pg < 1 || count <= 1)
        {
            pg = 1;
        }
        else
        {
            if (pg > count) pg = count;
        }
        return pg;
    }

    /**
     * bookmarkable link to given page
     */
    protected Link<?> getLink(String id, final int pageNum)
    {
        Link<?> link = new Link<Page>(id)
        {
            private static final long serialVersionUID = 42L;

            @Override
            public void onClick()
            {
                setCurrentPage(pageNum);
            }
        };
        link.setEnabled(pageNum != getCurrentPage());
        return link;
    }
}
