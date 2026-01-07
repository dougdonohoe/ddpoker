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
package com.donohoedigital.games.poker.wicket.panels;

import com.donohoedigital.games.poker.wicket.util.LoginUtils;
import com.donohoedigital.wicket.behaviors.DefaultFocus;
import com.donohoedigital.wicket.common.JavascriptHideable;
import com.donohoedigital.wicket.components.VoidPanel;
import com.donohoedigital.wicket.pages.BasePage;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.CompoundPropertyModel;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 18, 2008
 * Time: 12:21:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class Login extends VoidPanel implements JavascriptHideable
{
    private static final long serialVersionUID = 42L;

    private String name;
    private String password;
    private boolean remember = true;  // default to true
    private boolean visibleToUser;

    public Login(String id, boolean visibleToUser)
    {
        super(id);

        this.visibleToUser = visibleToUser;

        Form<Login> form = new StatelessForm<Login>("login", new CompoundPropertyModel<>(this))
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void onSubmit()
            {
                Login.this.visibleToUser = !new LoginUtils((BasePage<?>) getPage()).loginFromPage(name, password, remember);
            }
        };
        add(form);

        TextField<String> nameText = new TextField<>("name");
        nameText.setRequired(true);
        if (visibleToUser) nameText.add(new DefaultFocus());

        form.add(nameText);
        form.add(new PasswordTextField("password").setRequired(true));
        form.add(new CheckBox("remember"));

        add(new FormFeedbackPanel());
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public boolean isRemember()
    {
        return remember;
    }

    public void setRemember(boolean remember)
    {
        this.remember = remember;
    }

    public boolean isVisibleToUser()
    {
        return visibleToUser;
    }
}
