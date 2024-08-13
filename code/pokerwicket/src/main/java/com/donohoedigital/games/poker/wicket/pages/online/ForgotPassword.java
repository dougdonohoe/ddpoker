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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.service.*;
import com.donohoedigital.games.poker.wicket.panels.*;
import com.donohoedigital.mail.*;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.spring.injection.annot.*;
import org.wicketstuff.annotation.mount.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 18, 2008
 * Time: 12:19:02 PM
 * To change this template use File | Settings | File Templates.
 */
@MountPath(path = "forgot")
public class ForgotPassword extends OnlinePokerPage
{
    private static final long serialVersionUID = 42L;

    @SpringBean
    private OnlineProfileService profileService;

    @SpringBean
    private DDPostalService postalService;

    private String name;

    /**
     * Use logged in user
     */
    public ForgotPassword()
    {
        super(null);

        // form data
        CompoundPropertyModel<ForgotPassword> formData = new CompoundPropertyModel<ForgotPassword>(this);

        // change password form
        Form<ForgotPassword> pwform = new Form<ForgotPassword>("form", formData)
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void onSubmit()
            {
                OnlineProfile profile = profileService.getOnlineProfileByName(name);
                if (profile == null)
                {
                    error("There is no profile named '" + name + "'");
                }
                else
                {
                    final String password = profile.getPassword();

                    String sSubject = PropertyConfig.getMessage("msg.email.forgot.subject", name);
                    String sPlainText = PropertyConfig.getMessage("msg.email.forgot.plain", name, password);
                    String sHtmlText = PropertyConfig.getMessage("msg.email.forgot.html", Utils.encodeHTML(name), Utils.encodeHTML(password));

                    // get results and send email
                    postalService.sendMail(profile.getEmail(), PropertyConfig.getRequiredStringProperty("settings.server.profilefrom"),
                                           null, sSubject,
                                           sPlainText, sHtmlText,
                                           null, null);

                    info("Your password has been sent to the email linked to '" + name + "'");
                    name = null;
                }
            }
        };
        add(pwform);

        TextField<String> nameText = new TextField<String>("name");
        pwform.add(nameText.setRequired(true));

        // error / feedback
        FormFeedbackPanel feedback = new FormFeedbackPanel("form-style2");
        pwform.add(feedback);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}