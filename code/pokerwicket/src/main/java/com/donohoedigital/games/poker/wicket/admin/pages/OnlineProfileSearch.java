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
package com.donohoedigital.games.poker.wicket.admin.pages;

import com.donohoedigital.db.DBUtils;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.wicket.PokerUser;
import com.donohoedigital.games.poker.wicket.PokerWicketApplication;
import com.donohoedigital.games.poker.wicket.pages.online.History;
import com.donohoedigital.games.poker.wicket.panels.Aliases;
import com.donohoedigital.wicket.annotations.MountPath;
import com.donohoedigital.wicket.behaviors.DefaultFocus;
import com.donohoedigital.wicket.common.PageableServiceProvider;
import com.donohoedigital.wicket.components.CountDataView;
import com.donohoedigital.wicket.labels.BasicPluralLabelProvider;
import com.donohoedigital.wicket.labels.HighlightLabel;
import com.donohoedigital.wicket.labels.StringLabel;
import com.donohoedigital.wicket.models.StringModel;
import com.donohoedigital.wicket.panels.BoxPagingNavigator;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;

import java.util.Iterator;

/**
 * @author Doug Donohoe
 */
@MountPath("admin/online-profile-search")
public class OnlineProfileSearch extends AdminPokerPage
{
    //private static Logger logger = LogManager.getLogger(Search.class);

    private static final long serialVersionUID = 42L;

    public static final int ITEMS_PER_PAGE = 10;

    @SpringBean
    private OnlineProfileService profileService;

    public OnlineProfileSearch()
    {
        super(null);

        // search data
        SearchData data = new SearchData();

        // data view (visible if user specified search terms)
        final GameListTableView dataView = new GameListTableView("row", data);
        add(dataView);

        // navigator
        add(new BoxPagingNavigator("navigator", dataView, new BasicPluralLabelProvider("player", "players")));

        // form data
        CompoundPropertyModel<SearchData> formData = new CompoundPropertyModel<SearchData>(data);

        // form
        Form<SearchData> form = new Form<SearchData>("form", formData)
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void onSubmit()
            {
                getModelObject().resetSize();
                dataView.setCurrentPage(0);
            }
        };
        add(form);

        TextField<String> nameText = new TextField<String>("name");
        nameText.add(new DefaultFocus());

        form.add(nameText);
        form.add(new TextField<String>("email"));
        form.add(new TextField<String>("key"));

        // no results found
        add(new WebMarkupContainer("no-match", formData)
        {
            private static final long serialVersionUID = 42L;

            @Override
            public boolean isVisible()
            {
                SearchData d = (SearchData) getDefaultModelObject();
                return !d.isSearchNull() && d.isEmpty();
            }
        }
        );
    }

    ////
    //// List
    ////

    class SearchData extends PageableServiceProvider<OnlineProfile>
    {
        private static final long serialVersionUID = 42L;

        private String name;
        private String email;
        private String key;

        @Override
        public Iterator<OnlineProfile> iterator(int first, int pagesize)
        {
            return profileService.getMatchingOnlineProfiles(size(), first, pagesize, name, email, key, true).iterator();
        }

        @Override
        public int calculateSize()
        {
            if (isSearchNull()) return 0;
            return profileService.getMatchingOnlineProfilesCount(name, email, key, true);
        }

        public String getName()
        {
            return name;
        }

        public void setName(String s)
        {
            name = s;
        }

        public String getEmail()
        {
            return email;
        }

        public void setEmail(String email)
        {
            if (email != null)
            {
                email = email.replaceAll("mailto:", DBUtils.SQL_EXACT_MATCH);
            }
            this.email = email;
        }

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String[] getAll()
        {
            return new String[]{name, email, key};
        }

        public boolean isSearchNull()
        {
            return Strings.isEmpty(name) && Strings.isEmpty(email) && Strings.isEmpty(key);
        }
    }

    /**
     * The leaderboard table
     */
    private class GameListTableView extends CountDataView<OnlineProfile>
    {
        private static final long serialVersionUID = 42L;

        private GameListTableView(String id, SearchData data)
        {
            super(id, data, ITEMS_PER_PAGE);
        }

        @Override
        protected void populateItem(Item<OnlineProfile> row)
        {
            OnlineProfile profile = row.getModelObject();
            final String email = profile.getEmail();
            final String key = profile.getLicenseKey();

            // CSS class
            row.add(new AttributeModifier("class", new StringModel(row.getIndex() % 2 == 0 ? "odd" : "even")));

            // link to player details
            Link<?> link = History.getHistoryLink("playerLink", profile.getName());
            row.add(link);

            // player name (in link)
            link.add(new HighlightLabel("name", getSearchData().getName(), PokerWicketApplication.SEARCH_HIGHLIGHT, true));

            // email
            Link<?> emailLink = new Link("emailLink")
            {
                private static final long serialVersionUID = 42L;

                @Override
                public void onClick()
                {
                    setResponsePage(new RegistrationSearch(null, DBUtils.sqlExactMatch(email)));
                }
            };
            row.add(emailLink);
            emailLink.add(new HighlightLabel("email", getSearchData().getEmail(), PokerWicketApplication.SEARCH_HIGHLIGHT, true));

            // key
            Link<?> keyLink = new Link("keyLink")
            {
                private static final long serialVersionUID = 42L;

                @Override
                public void onClick()
                {
                    setResponsePage(new RegistrationSearch(DBUtils.sqlExactMatch(key), null));
                }
            };
            row.add(keyLink);
            keyLink.add(new HighlightLabel("licenseKey", getSearchData().getKey(), PokerWicketApplication.SEARCH_HIGHLIGHT, true));

            // password, retired
            row.add(new StringLabel("password"));
            row.add(new StringLabel("retired"));

            // player list
            row.add(new Aliases("aliases", new PokerUser(profile)));
        }

        protected SearchData getSearchData()
        {
            return (SearchData) getDataProvider();
        }

        @Override
        public boolean isVisible()
        {
            return !getSearchData().isSearchNull();
        }
    }
}
