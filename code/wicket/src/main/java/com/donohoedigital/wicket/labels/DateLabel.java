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
package com.donohoedigital.wicket.labels;

import com.donohoedigital.wicket.converters.ParamDateConverter;
import org.apache.wicket.IGenericComponent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import java.util.Date;

public class DateLabel extends Label implements IGenericComponent<Date, DateLabel>
{
    private static final long serialVersionUID = 1L;

    private final ParamDateConverter converter;

    public static DateLabel forDatePattern(String id, IModel<Date> model, String datePattern)
    {
        return new DateLabel(id, model, new ParamDateConverter(datePattern));
    }

    public static DateLabel forDatePattern(String id, String datePattern)
    {
        return forDatePattern(id, null, datePattern);
    }

    private DateLabel(String id, IModel<Date> model, ParamDateConverter converter)
    {
        super(id, model);
        this.converter = converter;
    }

    @Override
    protected IConverter<?> createConverter(Class<?> type)
    {
        if (Date.class.isAssignableFrom(type))
        {
            return converter;
        }
        return null;
    }

    @Override
    public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
    {
        String s = getDefaultModelObjectAsString();
        replaceComponentTagBody(markupStream, openTag, s);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IModel<Date> getModel() {
        return (IModel<Date>) super.getDefaultModel();
    }

    @Override
    public DateLabel setModel(IModel<Date> model) {
        super.setDefaultModel(model);
        return this;
    }

    @Override
    public DateLabel setModelObject(Date object) {
        super.setDefaultModelObject(object);
        return this;
    }

    @Override
    public Date getModelObject() {
        return (Date) super.getDefaultModelObject();
    }
}
