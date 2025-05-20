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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.SecurityUtils;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.db.model.BaseModel;
import jakarta.persistence.*;

import java.util.Date;

/**
 * Represents an online player profile.
 */
@Entity
@Table(name = "wan_profile")
public class OnlineProfile implements BaseModel<Long>
{
    private DMTypedHashMap data_;

    /**
     * Dummy types
     */
    public enum Dummy
    {
        HUMAN("__DUMMY__"), AI_BEST("__AIBEST__"), AI_REST("__AIREST__");

        // name and constructor for name
        private final String sName;

        private Dummy(String sName)
        {
            this.sName = sName;
        }

        /**
         * get type name for string
         */
        public String getName()
        {
            return sName;
        }
    }

    public static final String PROFILE_ID = "profileid";
    public static final String PROFILE_NAME = "profilename";
    public static final String PROFILE_LICENSE_KEY = "profilelicensekey";
    public static final String PROFILE_EMAIL = "profileemail";
    public static final String PROFILE_PASSWORD = "profilepassword";
    public static final String PROFILE_PASSWORD_IN_DB = "profilepasswordindb";
    public static final String PROFILE_ACTIVATED = "profileactivated";
    public static final String PROFILE_CREATE_DATE = "profilecreatedate";
    public static final String PROFILE_MODIFY_DATE = "profilemodifydate";
    public static final String PROFILE_RETIRED = "profileretired";

    // security key for encryption
    private static final byte[] SECURITY_KEY = new byte[]{(byte) 0x8c, (byte) 0xab, (byte) 0x4c, (byte) 0x92,
            (byte) 0xdc, (byte) 0x7a, (byte) 0x97, (byte) 0x68};

    /**
     * Creates an uninitialized instance of OnlineProfile
     */
    public OnlineProfile()
    {
        data_ = new DMTypedHashMap();
    }

    /**
     * Creates an a new instance of OnlineProfile
     * with the given name
     */
    public OnlineProfile(String name)
    {
        this();
        setName(name);
    }

    /**
     * Creates a new instance of OnlineProfile
     * with the given source data
     */
    public OnlineProfile(DMTypedHashMap data)
    {
        data_ = data;
    }

    @Transient
    public DMTypedHashMap getData()
    {
        return data_;
    }

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wpr_id", nullable = false)
    public Long getId()
    {
        return data_.getLong(PROFILE_ID);
    }

    public void setId(Long id)
    {
        data_.setLong(PROFILE_ID, id);
    }

    @Column(name = "wpr_name", unique = true, nullable = false)
    public String getName()
    {
        return data_.getString(PROFILE_NAME);
    }

    public void setName(String s)
    {
        data_.setString(PROFILE_NAME, s);
    }

    @Column(name = "wpr_license_key", nullable = false)
    public String getLicenseKey()
    {
        return data_.getString(PROFILE_LICENSE_KEY);
    }

    public void setLicenseKey(String s)
    {
        data_.setString(PROFILE_LICENSE_KEY, s);
    }

    @Column(name = "wpr_email", nullable = false)
    public String getEmail()
    {
        return data_.getString(PROFILE_EMAIL);
    }

    public void setEmail(String s)
    {
        data_.setString(PROFILE_EMAIL, s);
    }

    @Column(name = "wpr_password", nullable = false)
    public String getPasswordInDatabase()
    {
        return data_.getString(PROFILE_PASSWORD_IN_DB);
    }

    private void setPasswordInDatabase(String s)
    {
        data_.setString(PROFILE_PASSWORD_IN_DB, s);
    }

    @Transient
    public String getPassword()
    {
        String s = data_.getString(PROFILE_PASSWORD);
        return (s != null) ? decryptInternal(s) : null;
    }

    public void setPassword(String s)
    {
        setPasswordInDatabase(encryptToDatabase(s));

        String enc = null;
        if (s != null) enc = encryptInternal(s);
        data_.setString(PROFILE_PASSWORD, enc);
    }

    @Column(name = "wpr_is_activated", nullable = false)
    public boolean isActivated()
    {
        return data_.getBoolean(PROFILE_ACTIVATED, false);
    }

    public void setActivated(boolean b)
    {
        data_.setBoolean(PROFILE_ACTIVATED, b);
    }


    @Column(name = "wpr_is_retired", nullable = false)
    public boolean isRetired()
    {
        return data_.getBoolean(PROFILE_RETIRED, false);
    }

    public void setRetired(boolean b)
    {
        data_.setBoolean(PROFILE_RETIRED, b);
    }

    @Column(name = "wpr_create_date", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate()
    {
        return data_.getLongAsDate(PROFILE_CREATE_DATE);
    }

    public void setCreateDate(Date date)
    {
        data_.setLongFromDate(PROFILE_CREATE_DATE, date);
    }

    @Column(name = "wpr_modify_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModifyDate()
    {
        return data_.getLongAsDate(PROFILE_MODIFY_DATE);
    }

    public void setModifyDate(Date date)
    {
        data_.setLongFromDate(PROFILE_MODIFY_DATE, date);
    }

    /**
     * override equals - uses name for equality
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof OnlineProfile)) return false;
        final OnlineProfile other = (OnlineProfile) o;
        return getName().equals(other.getName());
    }

    /**
     * override hashcode
     */
    @Override
    public int hashCode()
    {
        String sName = getName();
        return sName == null ? super.hashCode() : sName.hashCode();
    }

    /**
     * Auto set create/modify date on insert
     */
    @PrePersist
    private void onInsert()
    {
        setCreateDate(new Date());
        setModifyDate(new Date());
    }

    /**
     * Auto set modify date on update
     */
    @PreUpdate
    private void onUpdate()
    {
        setModifyDate(new Date());
    }

    @PostLoad
    private void onLoad()
    {
        setPassword(decryptFromDatabase(getPasswordInDatabase()));
    }

    /**
     * Debug
     */
    @Override
    public String toString()
    {
        return "OnlineProfile: " + data_;
    }

    /**
     * encryption key (for storing password internally)
     */
    private byte[] k()
    {
        String s = "bb2a2ec9db88305e47cb19ab47a0fa50";
        byte[] k = new byte[s.length() / 2];
        int n = 0;

        for (int i = 0; i < k.length; ++i)
        {
            n = i * 2;
            k[i] = (byte) Integer.parseInt(s.substring(n, n + 2), 16);
        }

        return SecurityUtils.hashRaw(Utils.encode(getName()), k, null);
    }

    /**
     * encrypt given string to internal storage
     */
    private String encryptInternal(String s)
    {
        return SecurityUtils.encrypt(Utils.encode(s), k());
    }

    /**
     * Decrypt given string from internal storage
     */
    private String decryptInternal(String s)
    {
        return Utils.decode(SecurityUtils.decrypt(s, k()));
    }

    /**
     * Encrypt the given value for storage in the database.
     */
    private String encryptToDatabase(String value)
    {
        if (value == null)
        {
            return null;
        }

        return SecurityUtils.encrypt(Utils.encode(value), SECURITY_KEY);
    }

    /**
     * Decrypt the given value as retrieved from the database.
     */
    private String decryptFromDatabase(String value)
    {
        if (value == null)
        {
            return null;
        }

        try
        {
            return Utils.decode(SecurityUtils.decrypt(value, SECURITY_KEY));
        }
        catch (Throwable t)
        {
            throw new ApplicationError(ErrorCodes.ERROR_INVALID,
                                       "Unable to decrypt database value", t, value);
        }
    }
}
