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
package com.donohoedigital.games.poker.wicket.panels;

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.wicket.PokerUser;
import com.donohoedigital.games.poker.wicket.pages.online.History;
import com.donohoedigital.wicket.components.VoidContainer;
import com.donohoedigital.wicket.components.VoidPanel;
import com.donohoedigital.wicket.labels.StringLabel;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 21, 2008
 * Time: 12:38:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class Aliases extends VoidPanel
{
    private static final long serialVersionUID = 42L;

    @SuppressWarnings({"NonSerializableFieldInSerializableClass"})
    @SpringBean
    private OnlineProfileService profileService;

    public Aliases(String id, PokerUser user)
    {
        this(id, user, null, null);
    }

    public Aliases(String id, PokerUser user, final Date begin, final Date end)
    {
        super(id);

        AliasModel alias = new AliasModel(user);

        // none label if list is empty
        add(new VoidContainer("none").setVisible(alias.isEmpty()));

        // list of aliases
        add(new ListView<OnlineProfile>("list", alias)
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void populateItem(ListItem<OnlineProfile> item)
            {
                OnlineProfile p = item.getModelObject();

                // link
                Link<?> link = History.getHistoryLink("link", p.getName(), begin, end);
                item.add(link);

                // display name with &nbsp; spaces so they don't wrap
                link.add(createLabel("name", p.getName()));
            }
        });
    }

    protected Component createLabel(String id, String name)
    {
        return new StringLabel(id, Utils.encodeHTMLWhitespace(name)).setEscapeModelStrings(false);
    }

    /**
     * DESIGN NOTE:  Decided to show aliases for retired players because someone may want to know
     * the aliases for a newly retired player (e.g., - where did "xyz" go?)
     */

    private class AliasModel extends LoadableDetachableModel<List<OnlineProfile>>
    {
        PokerUser user;
        private static final long serialVersionUID = 42L;

        private AliasModel(PokerUser user)
        {
            this.user = user;
        }

        public boolean isEmpty()
        {
            return getObject().isEmpty();
        }

        @Override
        protected List<OnlineProfile> load()
        {
            if (user == null) return new ArrayList<OnlineProfile>();

            return profileService.getAllOnlineProfilesForEmail(user.getEmail(), user.getName());
        }
    }
}
