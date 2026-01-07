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
package com.donohoedigital.html;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 15, 2006
 * Time: 9:24:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class Table
{
    private ArrayList<TableColumn> cols_ = new ArrayList<TableColumn>();
    private ArrayList<TableRow> rows_ = new ArrayList<TableRow>();

    private int CELLPADDING, CELLSPACING;

    public Table(int CELLSPACING, int CELLPADDING)
    {
        this.CELLPADDING = CELLPADDING;
        this.CELLSPACING = CELLSPACING;
    }

    public void addColumn(TableColumn c)
    {
        cols_.add(c);
    }

    public TableColumn getColumn(int i)
    {
        return cols_.get(i);
    }

    public void addRow(TableRow r)
    {
        rows_.add(r);
    }

    public String toString()
    {
        return toStringBuilder().toString();
    }

    public StringBuilder toStringBuilder()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<TABLE");
        sb.append(" CELLSPACING=").append(CELLSPACING);
        sb.append(" CELLPADDING=").append(CELLPADDING);
        sb.append(">");

        // header
        sb.append("<TR>");
        for (int i = 0; i < cols_.size(); i++)
        {
            sb.append(cols_.get(i).toString());
        }
        sb.append("</TR>");

        // rows
        for (int i = 0; i < rows_.size(); i++)
        {
            sb.append(rows_.get(i).toString(this));
        }
        sb.append("</TABLE>");

        return sb;
    }
}
