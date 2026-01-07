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
/*
 * DataElement.java
 *
 * Created on November 11, 2002, 7:57 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import org.apache.xerces.impl.dv.ValidatedInfo;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.validation.ValidationState;
import org.apache.xerces.xs.StringList;

import java.util.*;

/**
 * @author Doug Donohoe
 */
public class DataElement
{

    static Logger logger = LogManager.getLogger(DataElement.class);

    XSSimpleType xsType_;
    List<?> values_;
    List<String> displayValues_;
    protected boolean bList_ = false;
    String sName_;

    /**
     * Creates a new instance of DataElement
     */
    public DataElement(String sName, List<?> values, List<String> displayValues)
    {
        sName_ = sName;
        values_ = values;
        displayValues_ = displayValues;
        bList_ = true;
    }

    /**
     * Creates a new instance of DataElement
     */
    public DataElement(String sName, XSSimpleType xsType)
    {
        sName_ = sName;

        if ((xsType != null) && xsType.isDefinedFacet(XSSimpleType.FACET_ENUMERATION))
        {
            bList_ = true;
            StringList lexi = xsType.getLexicalEnumeration();
            int nLen = lexi.getLength();
            List<String> values = new ArrayList<>(nLen);
            values_ = values;
            displayValues_ = new ArrayList<>(nLen);
            for (int i = 0; i < nLen; i++)
            {
                values.add(lexi.item(i));
                displayValues_.add(fetchDisplayValue(values_.get(i)));
            }
        }
    }

    /**
     * Get display value for given list value
     */
    private String fetchDisplayValue(Object oValue)
    {
        return getDisplayValue(getName(), oValue);
    }

    /**
     * Display value
     */
    public static String getDisplayValue(String sDataElement, Object oValue)
    {
        return PropertyConfig.getStringProperty("list." + sDataElement + "." + oValue, (String) oValue);
    }

    /**
     * Get name of this data element
     */
    public String getName()
    {
        return sName_;
    }

    /**
     * Is list type?
     */
    public boolean isList()
    {
        return bList_;
    }

    /**
     * Get list values.  Do not modify the returned object.
     */
    public List<?> getListValues()
    {
        return values_;
    }

    /**
     * Get string value used to represent the "notselected" value when
     * displaying this list in a combobox or similar.  Equals "0notselected",
     * which is used to lookup display value for not-selected choice.
     * Zero in front to sort properly
     */
    public String getNotSelectedValue()
    {
        return "0notselected";
    }

    private String notSelectedDisplayValue_ = null;

    /**
     * Get display value for given value
     */
    public String getDisplayValue(Object oValue)
    {
        if (getNotSelectedValue().equals(oValue))
        {
            if (notSelectedDisplayValue_ == null)
            {
                notSelectedDisplayValue_ = fetchDisplayValue(getNotSelectedValue());
            }
            return notSelectedDisplayValue_;
        }

        int index = values_.indexOf(oValue);

        String sDisplayValue = null;

        if ((displayValues_ != null) && (displayValues_.size() > index))
        {
            sDisplayValue = displayValues_.get(index);
        }

        if ((sDisplayValue == null) && (oValue instanceof NamedObject))
        {
            sDisplayValue = ((NamedObject) oValue).getName();
        }

        if (sDisplayValue == null)
        {
            logger.warn("WARNING: No display value for " + sName_ + "." + oValue);

            return oValue.toString();
        }
        else
        {
            return sDisplayValue;
        }
    }

    /**
     * validate
     */
    public boolean isValid(String sValue)
    {
        try
        {
            ValidationState vstate = new ValidationState();
            ValidatedInfo info = new ValidatedInfo();

            vstate.setExtraChecking(false);
            vstate.setFacetChecking(true);

            xsType_.validate(sValue, vstate, info);

            return true;
        }
        catch (Exception e)
        {
            // invalid if exception thrown
        }
        return false;
    }

}
