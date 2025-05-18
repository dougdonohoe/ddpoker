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
 * DDComboBox.java
 *
 * Created on November 12, 2002, 4:03 PM
 */

package com.donohoedigital.gui;

import com.donohoedigital.config.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/*    
*
* @author  Doug Donohoe
*/
public class DDComboBox extends JComboBox implements 
            DDTextVisibleComponent,DDExtendedComponent,DDListComponent 
{    
    static Logger logger = LogManager.getLogger(DDComboBox.class);

    DataElement elem_;
    DefaultComboBoxModel model_;
    boolean bShowNotSelectedChoice_ = false;
    Color cMouseOverForeground_;
    Color cSelectionForeground_;
    Color cSelectionBackground_;
    private DDVector values_;
    private boolean bRequired_ = true;

    /**
     * 
     */
    public DDComboBox()
    {
        super();
        init(GuiManager.DEFAULT, GuiManager.DEFAULT);
    }
    
    /**
     * New 
     */
    public DDComboBox(String sName)
    {
        super();
        setDataElement(sName);
        init(sName, GuiManager.DEFAULT);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDComboBox(String sName, String sStyleName)
    {
        super();
        setDataElement(sName);
        init(sName, sStyleName);
    }

    public DDComboBox(DataElement element)
    {
        super();
        setDataElement(element);
        init(element.getName(), GuiManager.DEFAULT);
    }

    /**
     * Creates a new instance of DDTextField, sets name to sName
     */
    public DDComboBox(DataElement element, String sStyleName)
    {
        super();
        setDataElement(element);
        init(element.getName(), sStyleName);
    }

    /**
     * set dd combobox ui
     */
    public void updateUI() {
        setUI(DDComboBoxUI.createUI(this));
    }
    
    /**
     * init gui
     */
    private void init(String sName, String sStyleName)
    {
        setRenderer(new DDComboBoxRenderer(this));
        GuiManager.init(this, sName, sStyleName);
        setOpaque(false);
    }

    /**
     *  Set whether this is required (a selection must be made)
     */
    public void setRequired(boolean b)
    {
        bRequired_ = b;
    }

    /**
     * is required? (true by default)
     */
    public boolean isRequired()
    {
        return bRequired_;
    }

    /**
     * Set foreground color to use when mouse over button
     */
    public void setMouseOverForeground(Color c)
    {
        cMouseOverForeground_ = c;
    }
    
    /**
     * Get foreground color to use when mouse over button
     */
    public Color getMouseOverForeground()
    {
        return cMouseOverForeground_;
    }
    
    /**
     * Set background used when an item is selected
     */
    public Color getSelectionBackground() {
        return cSelectionBackground_;
    }
    
    /**
     * Set foreground used when an item is selected
     */
    public Color getSelectionForeground() {
        return cSelectionForeground_;
    }
    
    /**
     * Get background for selected items
     */
    public void setSelectionBackground(Color c) {
        cSelectionBackground_ = c;
    }
    
    /**
     * Get foreground for selected items
     */
    public void setSelectionForeground(Color c) {
        cSelectionForeground_ = c;
    }
    
    
    /**
     * Override to remember last value before changing
     */
    public void setSelectedItem(Object o)
    {
        rememberLast();
        super.setSelectedItem(o);
    }
    
    /**
     * remember last selection
     */
    private Object oLast_ = null;
    private void rememberLast()
    {
        oLast_ = getSelectedValue();
    }
    
    /**
     * set data element
     */
    public void setDataElement(String sDataElement)
    {
        DataElement elem = DataElementConfig.getDataElement(sDataElement);

        if (elem == null)
        {
            logger.warn("DataElement not found for " + sDataElement);
            return;
        }

        setDataElement(elem);
    }

    public void setDataElement(DataElement elem)
    {

        if (!elem.isList())
        {
            logger.warn("DataElement " + elem.getName() + " is not a list");
            return;
        }

        // not the right time to do this, but replicates past behavior
        elem_ = elem;

        List<?> values = elem.getListValues();

        if (values == null || values.size() == 0)
        {
            logger.warn("DataElement " + elem.getName() + " has no list values");
            return;
        }

        resetValues();
    }
    
    /**
     * Get type
     */
    public String getType() {
        return "combobox";
    }
    
    /**
     * set whether this combo is showing the "not selected" value
     */
    public void setShowNotSelectedChoice(boolean b)
    {
        boolean bOld = bShowNotSelectedChoice_;
        bShowNotSelectedChoice_ = b;
        if (bOld != b)
        {
            resetValues();
        }
    }
    
    /**
     * Is this combo showing the "not selected" value?
     */
    public boolean isShowNotSelectedChoice()
    {
        return bShowNotSelectedChoice_;
    }
    
    /**
     * Set values for use in the combo box
     */
    public void setValues(List<?> values)
    {
        boolean sortable = true;

        values_ = new DDVector(values.size());
        for (int i = 0; i < values.size(); i++)
        {
            if (!(values.get(i) instanceof Comparable))
            {
                sortable = false;
            }
            values_.addElement(values.get(i));
        }
        if (sortable)
        {
            values_.sort();
        }
        model_ = new DefaultComboBoxModel(values_);
        if (bShowNotSelectedChoice_)
        {
            int nPos = 0;
            String sValue = getNotSelectedValue();
            model_.insertElementAt(sValue, nPos);
            model_.setSelectedItem(sValue);
        }
        setModel(model_);
    }
    
    /**
     * return "notselected" value
     */
    public String getNotSelectedValue()
    {
        return elem_.getNotSelectedValue();
    }
    
    /**
     * return current selected value.  If "notselected" value is chosen,
     * this returns null;
     */
    public Object getSelectedValue()
    {
        Object oValue = getSelectedItem();
        if (oValue == getNotSelectedValue()) return null;
        return oValue;
    }

    /**
     * is valid?  Returns true if an item is selected and
     * this is required.  If not required, always returns true.
     */
    public boolean isValidData()
    {
        if (bRequired_) return getSelectedValue() != null;
        return true;
    }


    /**
     * Return display value for given object
     */
    public String getDisplayValue(Object oValue)
    {
        if (oValue == null) return "";

        return elem_.getDisplayValue(oValue);
    }
    
    /**
     * Return selected display value
     */
    public String getSelectedDisplayValue()
    {
        return elem_.getDisplayValue(getSelectedItem());
    }
    
    /**
     * Return last selected value
     */
    public Object getLastSelectedValue()
    {
        return oLast_;
    }

    /**
     * Return values used by data element
     */
    public List<?> getDefaultValues()
    {
        return elem_.getListValues();
    }
    
    /**
     * Reset values to original data element values
     */
    public void resetValues()
    {
        setValues(getDefaultValues());
    }
    
    /**
     * Remove value
     */
    public void removeValue(String sValue)
    {
        model_.removeElement(sValue);
    }
    
    /**
     * Add value
     */
    public void addValue(String sValue)
    {
        model_.addElement(sValue);
        values_.sort();
    }

    /**
     * Swing doesn't exactly do semi-transparent correctly unless
     * you start with the hightest parent w/ no transparency
     */
    public void repaint(long tm, int x, int y, int width, int height)
    {
        Component foo = GuiUtils.getSolidRepaintComponent(this);
        if (foo != null && foo != this)
        {
            Point pRepaint = SwingUtilities.convertPoint(this, x, y, foo);
            foo.repaint(pRepaint.x, pRepaint.y, width, height);
            return;
        }

        super.repaint(tm, x, y, width, height);
    }
    
    /**
     * Add mouse listener to this and children
     */
    public void addMouseListener(MouseListener listener)
    {
        if (listener instanceof GuiManager || listener instanceof OptionCombo)
        {
            GuiUtils.addMouseListenerChildren(this, listener);
        }
        super.addMouseListener(listener);
    }   
    
    /**
     * Remove mouse listener from this and children
     */
    public void removeMouseListener(MouseListener listener)
    {
        if (listener instanceof GuiManager || listener instanceof OptionCombo)
        {
            GuiUtils.removeMouseListenerChildren(this, listener);
        }
        super.removeMouseListener(listener);
    } 
    
    /**
     * Sortable vector
     */
    private static class DDVector extends Vector
    {
        DDVector(int n)
        {
            super(n);
        }
        
        public void sort()
        {
            Object[] data = toArray();
            Arrays.sort(data);
            elementData = data;
            elementCount = data.length;
        }
    }
}
