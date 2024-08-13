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
package com.donohoedigital.games.server.model;

import com.donohoedigital.db.model.*;

import javax.persistence.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 15, 2008
 * Time: 4:43:04 PM
 * <p/>
 * An upgraded key
 */
@Entity
@Table(name = "upgraded_key")
public class UpgradedKey implements BaseModel<Long>
{
    @Id()
    @GeneratedValue
    @Column(name = "upg_id", nullable = false)
    private Long id;

    @Column(name = "upg_license_key", nullable = false)
    private String licenseKey;

    @Column(name = "upg_count", nullable = false)
    private int count;

    @Column(name = "upg_create_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    @Column(name = "upg_modify_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifyDate;

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

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public Date getModifyDate()
    {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate)
    {
        this.modifyDate = modifyDate;
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

    /**
     * override equals - uses license key for equality
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof UpgradedKey)) return false;
        final UpgradedKey other = (UpgradedKey) o;
        return this.getLicenseKey().equals(other.getLicenseKey());
    }

    /**
     * override hashcode
     */
    @Override
    public int hashCode()
    {
        String sKey = getLicenseKey();
        return sKey == null ? super.hashCode() : sKey.hashCode();
    }
}