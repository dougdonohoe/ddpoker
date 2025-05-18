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
package com.donohoedigital.games.server.model;

import com.donohoedigital.db.model.BaseModel;
import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 15, 2008
 * Time: 4:43:04 PM
 * <p/>
 * A banned key where key is a license key, email or any other text string we want to use as a key for banning.
 */
@Entity
@Table(name = "banned_key")
public class BannedKey implements BaseModel<Long>, Comparable<BannedKey>
{
    public static final Date DEFAULT_UNTIL = new GregorianCalendar(2099, Calendar.DECEMBER, 31).getTime();  // 12/31/2099

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ban_id", nullable = false)
    private Long id;

    @Column(name = "ban_key", nullable = false)
    private String key;

    @Column(name = "ban_until", nullable = false)
    private Date until;

    @Column(name = "ban_comment", nullable = true)
    private String comment;

    @Column(name = "ban_create_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
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

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    /**
     * Auto set create/modify date on insert
     */
    @PrePersist
    private void onInsert()
    {
        setCreateDate(new Date());
        if (until == null) until = DEFAULT_UNTIL;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BannedKey bannedKey = (BannedKey) o;

        if (key != null ? !key.equals(bannedKey.key) : bannedKey.key != null) return false;
        if (until != null ? !until.equals(bannedKey.until) : bannedKey.until != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (until != null ? until.hashCode() : 0);
        return result;
    }

    /**
     * String representation
     */
    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE, false);
    }

    /**
     * Sort by license key
     */
    public int compareTo(BannedKey k)
    {
        return this.getKey().compareTo(k.getKey());
    }
}
