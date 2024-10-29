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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.wicket.panels.FormFeedbackPanel;
import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.service.BannedKeyService;
import com.donohoedigital.wicket.annotations.MountPath;
import com.donohoedigital.wicket.behaviors.DefaultFocus;
import com.donohoedigital.wicket.common.PageableServiceProvider;
import com.donohoedigital.wicket.components.CountDataView;
import com.donohoedigital.wicket.labels.StringLabel;
import com.donohoedigital.wicket.models.StringModel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IErrorMessageSource;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.DateValidator;
import org.apache.wicket.validation.validator.StringValidator;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jan 5, 2009
 * Time: 10:39:35 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath("admin/ban-list")
public class BanList extends AdminPokerPage
{
    private static final long serialVersionUID = 42L;

    public static final int ITEMS_PER_PAGE = 10;

    @SpringBean
    private BannedKeyService banService;

    public BanList()
    {
        super(null);

        // search data
        BanData data = new BanData();

        // data view (visible if user specified search terms)
        BanListTableView dataView = new BanListTableView("row", data);
        add(dataView.setVisible(!data.isEmpty()));

        add(new WebMarkupContainer("none").setVisible(data.isEmpty()));

        // form data
        CompoundPropertyModel<BanData> formData = new CompoundPropertyModel<BanData>(data);

        // form
        Form<BanData> form = new Form<BanData>("form", formData)
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void onSubmit()
            {
                BanData ban = getModelObject();
                BannedKey key = new BannedKey();
                key.setKey(ban.getBan());
                key.setUntil(ban.getUntil());
                key.setComment(ban.getComment());
                ban.setBan(null);
                ban.setUntil(null);
                ban.setComment(null);
                banService.saveBannedKey(key);
                info("'" + key.getKey() + "' was banned");
            }
        };
        add(form);

        // name
        TextField<String> ban = new TextField<String>("ban");
        ban.add(new DefaultFocus());
        ban.setRequired(true);
        ban.add(new StringValidator.MaximumLengthValidator(255));
        ban.add(new CheckDup());
        form.add(ban);

        // until
        DateTextField until = new DateTextField("until");
        form.add(until.add(new DatePicker()));
        //noinspection unchecked
        until.add(DateValidator.minimum(new Date()));

        // comment
        TextField<String> comment = new TextField<String>("comment");
        comment.add(new StringValidator.MaximumLengthValidator(128));
        form.add(comment);

        // error / feedback
        form.add(new FormFeedbackPanel("form-style3"));
    }

    /**
     * Duplicate check
     */
    private class CheckDup implements IValidator<String>
    {
        public void validate(IValidatable<String> iValidatable)
        {
            final String ban = iValidatable.getValue();
            BannedKey bannedKey = banService.getIfBanned(ban);
            if (bannedKey != null)
            {
                // TODO: use wicket resources for this
                iValidatable.error(new IValidationError()
                {

                    public String getErrorMessage(IErrorMessageSource messageSource)
                    {
                        return "'" + ban + "' is already banned";
                    }
                });
            }
        }
    }

    ////
    //// List
    ////

    private class BanData extends PageableServiceProvider<BannedKey>
    {
        private static final long serialVersionUID = 42L;

        private transient List<BannedKey> banned;
        private String ban;
        private Date until;
        private String comment;

        @Override
        public Iterator<BannedKey> iterator(int first, int pagesize)
        {
            return getList().iterator();
        }

        @Override
        public int calculateSize()
        {
            return getList().size();
        }

        private List<BannedKey> getList()
        {
            if (banned == null)
            {
                banned = banService.getAllBannedKeys();
            }
            return banned;
        }

        @Override
        public void detach()
        {
            banned = null;
        }

        public String getBan()
        {
            return ban;
        }

        public void setBan(String ban)
        {
            this.ban = ban;
        }

        public Date getUntil()
        {
            return until;
        }

        public void setUntil(Date until)
        {
            this.until = until;
        }

        public String getComment()
        {
            return comment;
        }

        public void setComment(String comment)
        {
            this.comment = comment;
        }
    }

    /**
     * The leaderboard table
     */
    private class BanListTableView extends CountDataView<BannedKey>
    {
        private static final long serialVersionUID = 42L;

        private BanListTableView(String id, BanData data)
        {
            super(id, data, data.size() + 1); // add one in case 0
        }

        @Override
        protected void populateItem(Item<BannedKey> row)
        {
            BannedKey ban = row.getModelObject();

            // CSS class
            row.add(new AttributeModifier("class", new StringModel(row.getIndex() % 2 == 0 ? "odd" : "even")));

            // data
            row.add(new Label("id"));
            row.add(new StringLabel("key"));
            row.add(DateLabel.forDatePattern("until", PropertyConfig.getMessage("msg.format.date")));
            row.add(new StringLabel("comment"));
            row.add(DateLabel.forDatePattern("createDate", PropertyConfig.getMessage("msg.format.date")));

            // unban
            Form<String> form = new Form<String>("form", new StringModel(ban.getKey()))
            {
                @Override
                protected void onSubmit()
                {
                    String key = getModelObject();
                    BannedKey banned = banService.getIfBanned(key);
                    banService.deleteBannedKey(key);
                    BanData bdata = BanListTableView.this.getBanData();
                    bdata.setBan(banned.getKey());
                    bdata.setComment(banned.getComment());
                    bdata.setUntil(banned.getUntil());
                    info("'" + key + "' was unbanned");
                }
            };
            row.add(form);

        }

        protected BanData getBanData()
        {
            return (BanData) getDataProvider();
        }

    }
}
