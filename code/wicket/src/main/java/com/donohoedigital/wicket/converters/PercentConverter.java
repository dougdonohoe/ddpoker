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
package com.donohoedigital.wicket.converters;

import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.AbstractConverter;
import org.apache.wicket.util.convert.converter.AbstractDecimalConverter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 23, 2008
 * Time: 2:58:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class PercentConverter extends AbstractDecimalConverter<Double>
{
	private static final long serialVersionUID = 42L;

    private final int fractionDigits;

    public PercentConverter(int fractionDigits)
    {
        this.fractionDigits = Math.max(0, fractionDigits);
    }

    /**
	 * @see AbstractConverter#getTargetType()
	 */
	@Override
    protected Class<Double> getTargetType()
	{
		return Double.class;
	}

    /**
	 * @see IConverter#convertToObject(String, Locale)
	 */
	public Double convertToObject(final String value, Locale locale)
	{
		final Number number = parse(value, -Double.MAX_VALUE, Double.MAX_VALUE, locale);
		// Double.MIN is the smallest nonzero positive number, not the largest
		// negative number

		if (number == null)
		{
			return null;
		}

		return number.doubleValue();
	}

    @Override
    public NumberFormat getNumberFormat(Locale locale)
    {
        NumberFormat format = DecimalFormat.getPercentInstance(locale); // FIX: cache like superclass does?
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        return format;
    }

}