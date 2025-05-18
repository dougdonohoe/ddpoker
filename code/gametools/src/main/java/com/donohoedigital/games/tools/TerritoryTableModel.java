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
/*
 * TerritoryTableModel.java
 *
 * Created on October 31, 2002, 3:07 PM
 */

package com.donohoedigital.games.tools;

import org.apache.logging.log4j.*;
import com.donohoedigital.games.config.*;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

/**
 *
 * @author  Doug Donohoe
 */
public class TerritoryTableModel extends AbstractTableModel 
{
    
    //static Logger logger = LogManager.getLogger(TerritoryTableModel.class);
    
    
    Territories territories_;
    /** 
     * Creates a new TerritoryTableModel given a list of territories
     */
    public TerritoryTableModel(Territories territories) 
    {
        territories_ = territories;
    }
    
    public Territory getTerritory(int rowIndex)
    {
        Collection ts = territories_.values();
        Iterator iter = ts.iterator();
        
        // skip over rowIndex - 1 to get to desired row
        for (int i = 0; i < rowIndex; i++)
        {
            iter.next();
        }
        return (Territory) iter.next();
    }
    
    /** Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     *
     */
    public int getColumnCount() 
    {
        return 1; // 1=name, 2=area, 3=type
    }
    
    public String getColumnName(int column) {
        switch (column) 
        {
            case 0:
                return "Territory";
            case 1: 
                return "Area";
            case 2:
                return "Type";
        }
        
        return null;
    }
    
    /** Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     *
     */
    public int getRowCount() 
    {
        return territories_.size();
    }
    
    /** Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     * @param	columnIndex 	the column whose value is to be queried
     * @return	the value Object at the specified cell
     *
     */
    public Object getValueAt(int rowIndex, int columnIndex) 
    {
        Territory t = getTerritory(rowIndex);

        switch (columnIndex) 
        {
            case 0:
                return t.getName();
            case 1: 
                return t.getArea();
            case 2:
                return t.getType();
        }
        
        return null;
    }
    
}
