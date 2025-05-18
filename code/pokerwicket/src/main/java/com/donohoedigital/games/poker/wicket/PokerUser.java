/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
package com.donohoedigital.games.poker.wicket;

import com.donohoedigital.games.poker.model.OnlineProfile;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 21, 2008
 * Time: 10:49:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class PokerUser implements Serializable
{
    private static final long serialVersionUID = 42L;

    private Long id;
    private String name;
    private String licenseKey;
    private String email;
    private boolean retired;
    private boolean authenticated;


    public PokerUser(OnlineProfile profile)
    {
        this(profile.getId(), profile.getName(), profile.getLicenseKey(), profile.getEmail(), profile.isRetired());
    }

    public PokerUser(Long id, String name, String licenseKey, String email, boolean retired)
    {
        this.id = id;
        this.licenseKey = licenseKey;
        this.name = name;
        this.email = email;
        this.retired = retired;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getLicenseKey()
    {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey)
    {
        this.licenseKey = licenseKey;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public boolean isRetired()
    {
        return retired;
    }

    public void setRetired(boolean retired)
    {
        this.retired = retired;
    }

    public boolean isAuthenticated()
    {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated)
    {
        this.authenticated = authenticated;
    }

    public String getDisplayName()
    {
        if (name.equals(OnlineProfile.Dummy.AI_BEST.getName()))
        {
            return "DD Poker AI - Best Place Finishes"; // FIX: put in properties
        }
        return name;
    }

    public boolean isAdmin()
    {
        return name.equals("Doug Donohoe") || name.equals("Greg King") || name.equals("DDPoker Support");
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
