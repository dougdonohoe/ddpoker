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
package com.donohoedigital.games.poker.wicket.pages.online;

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.OnlineProfileSummary;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.wicket.PokerSession;
import com.donohoedigital.games.poker.wicket.PokerUser;
import com.donohoedigital.games.poker.wicket.panels.FormFeedbackPanel;
import com.donohoedigital.wicket.annotations.MountPath;
import com.donohoedigital.wicket.behaviors.DefaultFocus;
import com.donohoedigital.wicket.common.InfoOnlyFilter;
import com.donohoedigital.wicket.labels.GroupingIntegerLabel;
import com.donohoedigital.wicket.labels.HiddenComponent;
import com.donohoedigital.wicket.labels.StringLabel;
import com.donohoedigital.wicket.models.IntegerModel;
import com.donohoedigital.wicket.models.StringModel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 18, 2008
 * Time: 12:19:02 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath("myprofile")
public class MyProfile extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    @SpringBean
    private OnlineProfileService profileService;

    private PokerUser user;

    /**
     * Use logged in user
     */
    public MyProfile()
    {
        super(null);

        user = PokerSession.get().getLoggedInUser();
        boolean hasUser = user != null;

        // title
        add(new StringLabel("titleName", hasUser ? user.getDisplayName() : null).setVisible(hasUser).setRenderBodyOnly(true));
        add(new StringLabel("name", hasUser ? user.getDisplayName() : null).setVisible(hasUser));
        add(new StringLabel("email", hasUser ? user.getEmail() : null).setVisible(hasUser));

        // description and history
        if (hasUser)
        {
            // description and table
            add(new DescriptionFragment("description"));
            add(new AliasTable("history"));
        }
        else
        {
            // description
            Fragment desc = new Fragment("description", "notLoggedIn", this);
            desc.add(getCurrentProfile().getLoginLink("loginLink"));
            add(desc);

            // no table
            add(new HiddenComponent("history"));
        }

        // error / feedback
        FormFeedbackPanel feedback = new FormFeedbackPanel("form-style4");
        feedback.setFilter(new InfoOnlyFilter());
        add(feedback);
    }

    protected Component createLabel(String id, String name)
    {
        return new StringLabel(id, Utils.encodeHTMLWhitespace(name)).setEscapeModelStrings(false);
    }

    /**
     * Logged in description
     */
    private class DescriptionFragment extends Fragment
    {

        public DescriptionFragment(String id)
        {
            super(id, "loggedIn", MyProfile.this);
            add(createLabel("name", user.getDisplayName()));
        }
    }

    ////
    //// List
    ////

    private class AliasTable extends Fragment
    {
        private static final long serialVersionUID = 42L;

        private AliasTable(String id)
        {
            super(id, "table", MyProfile.this);

            IntegerModel max = new IntegerModel(PokerConstants.MAX_PROFILES_PER_EMAIL);
            add(new GroupingIntegerLabel("max", max));
            add(new GroupingIntegerLabel("max2", max));
            add(createLabel("name2", user.getDisplayName()));

            // form data
            CompoundPropertyModel<ChangePasswordData> formData = new CompoundPropertyModel<ChangePasswordData>(new ChangePasswordData());

            // change password form
            Form<ChangePasswordData> pwform = new Form<ChangePasswordData>("form", formData)
            {
                private static final long serialVersionUID = 42L;

                @Override
                protected void onSubmit()
                {
                    ChangePasswordData data = getModelObject();
                    OnlineProfile auth = new OnlineProfile();
                    auth.setName(user.getName());
                    auth.setPassword(data.getOld());
                    OnlineProfile profile = profileService.authenticateOnlineProfile(auth);
                    if (profile == null)
                    {
                        error("Existing password is incorrect.");
                    }
                    else
                    {
                        profile.setPassword(data.getNu());
                        profileService.updateOnlineProfile(profile);
                        data.setConfirm(null);
                        data.setNu(null);
                        data.setOld(null);
                        info("Your password has been changed.");
                    }
                }
            };
            add(pwform);

            PasswordTextField nameText = new PasswordTextField("old");
            PasswordTextField nu = new PasswordTextField("nu");
            PasswordTextField confirm = new PasswordTextField("confirm");
            nameText.add(new DefaultFocus());
            pwform.add(nameText.setResetPassword(false).setRequired(true));
            pwform.add(nu.setResetPassword(false).setRequired(true));
            pwform.add(confirm.setResetPassword(false).setRequired(true));
            pwform.add(new EqualPasswordInputValidator(nu, confirm));

            FormFeedbackPanel feedback = new FormFeedbackPanel("pwerror", "form-style2");
            feedback.setFilter(new ContainerFeedbackMessageFilter(pwform));
            pwform.add(feedback);

            // list of aliases
            OnlineProfileSummaryModel onlineProfileSummary = new OnlineProfileSummaryModel(user);
            add(new ListView<OnlineProfileSummary>("row", onlineProfileSummary)
            {
                private static final long serialVersionUID = 42L;

                @Override
                protected void populateItem(ListItem<OnlineProfileSummary> row)
                {
                    OnlineProfileSummary p = row.getModelObject();

                    // CSS class
                    row.add(new AttributeModifier("class", new StringModel(row.getIndex() % 2 == 0 ? "odd" : "even")));

                    // display name with &nbsp; spaces so they don't wrap
                    Link<?> link = History.getHistoryLink("playerLink", p.getName());
                    row.add(link);

                    // display name with &nbsp; spaces so they don't wrap
                    link.add(createLabel("alias", p.getName()));

                    row.add(new GroupingIntegerLabel("count", p.getCount()));

                    // retire
                    // unban
                    Form<String> form = new Form<String>("form", new StringModel(p.getName()))
                    {
                        @Override
                        protected void onSubmit()
                        {
                            String name = getModelObject();
                            profileService.retire(name);
                            info("'" + name + "' was retired");
                        }
                    };
                    Button button = new Button("button");
                    button.add(new SimpleAttributeModifier("onclick",
                                                           "return confirm('Are you sure you want to retire " +
                                                           Utils.encodeJavascript(p.getName()) + "?');"));
                    form.add(button);
                    final boolean currentUser = p.getName().equals(user.getName());
                    form.setVisible(!currentUser);

                    row.add(form);
                }
            });
        }
    }

    private class OnlineProfileSummaryModel extends LoadableDetachableModel<List<OnlineProfileSummary>>
    {
        PokerUser userinfo;

        private OnlineProfileSummaryModel(PokerUser userinfo)
        {
            this.userinfo = userinfo;
        }

        public boolean isEmpty()
        {
            return getObject().isEmpty();
        }

        @Override
        protected List<OnlineProfileSummary> load()
        {
            if (userinfo == null) return new ArrayList<OnlineProfileSummary>();

            return profileService.getOnlineProfileSummariesForEmail(userinfo.getEmail());
        }
    }

    private class ChangePasswordData implements Serializable
    {
        private String old;
        private String nu;
        private String confirm;

        public String getOld()
        {
            return old;
        }

        public void setOld(String old)
        {
            this.old = old;
        }

        public String getNu()
        {
            return nu;
        }

        public void setNu(String nu)
        {
            this.nu = nu;
        }

        public String getConfirm()
        {
            return confirm;
        }

        public void setConfirm(String confirm)
        {
            this.confirm = confirm;
        }
    }

}