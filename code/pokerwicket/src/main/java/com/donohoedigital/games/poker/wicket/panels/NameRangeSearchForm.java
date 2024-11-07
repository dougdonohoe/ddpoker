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
package com.donohoedigital.games.poker.wicket.panels;

import com.donohoedigital.games.poker.wicket.pages.BasePokerPage;
import com.donohoedigital.games.poker.wicket.util.NameRangeSearch;
import com.donohoedigital.wicket.WicketUtils;
import com.donohoedigital.wicket.behaviors.DefaultFocus;
import com.donohoedigital.wicket.components.DatePickerUtil;
import com.donohoedigital.wicket.components.VoidPanel;
import com.donohoedigital.wicket.converters.ParamDateConverter;
import com.donohoedigital.wicket.labels.StringLabel;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Date;

/**
 * @author Doug Donohoe
 */
public class NameRangeSearchForm extends VoidPanel
{
    private static final long serialVersionUID = 42L;

    private final ParamDateConverter CONVERTER = new ParamDateConverter();
    private final DateTextField beginT;
    private final DateTextField endT;

    protected Form<NameRangeSearch> form;

    /**
     * @see org.apache.wicket.Component#Component(String)
     */
    public NameRangeSearchForm(String id, PageParameters params, final Class<? extends BasePokerPage> clazz,
                               final NameRangeSearch data, final String paramName, final String paramBegin, final String paramEnd, String nameLabel)
    {
        super(id);

        String name = params.get(paramName).toString();
        Date begin = WicketUtils.getAsDate(params, paramBegin, null, CONVERTER);
        Date end = WicketUtils.getAsDate(params, paramEnd, null, CONVERTER);

        data.setBegin(begin);
        data.setEnd(end);
        data.setName(name);

        // form
        form = new StatelessForm<NameRangeSearch>("form", new CompoundPropertyModel<>(data))
        {
            private static final long serialVersionUID = 42L;

            @Override
            protected void onSubmit()
            {
                PageParameters p = new PageParameters();
                addCustomPageParameters(p);
                p.set(paramName, getModelObject().getName());
                p.set(paramBegin, toStringDate(getModelObject().getBegin()));
                p.set(paramEnd, toStringDate(getModelObject().getEnd()));
                setResponsePage(clazz, p);
            }
        };
        add(form);

        // entry fields
        beginT = DatePickerUtil.create("begin");
        endT = DatePickerUtil.create("end");
        TextField<String> nameText = new TextField<>("name");
        nameText.add(new DefaultFocus());

        form.add(beginT);
        form.add(endT);
        form.add(new StringLabel("nameLabel", nameLabel));
        form.add(nameText);
        form.add(new FormFeedbackPanel("form-style2"));
    }

    public String getBeginDateAsUserSeesIt()
    {
        return beginT.getDefaultModelObjectAsString();
    }

    public String getEndDateAsUserSeesIt()
    {
        return endT.getDefaultModelObjectAsString();
    }

    public String toStringDate(Date date)
    {
        return CONVERTER.convertToString(date);
    }

    /**
     * subclass can override to add custom page parameters
     */
    protected void addCustomPageParameters(PageParameters p)
    {
        // do nothing
    }
}
