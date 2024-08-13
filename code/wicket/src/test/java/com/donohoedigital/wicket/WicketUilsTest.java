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
package com.donohoedigital.wicket;

import com.donohoedigital.wicket.converters.*;
import junit.framework.*;
import org.apache.wicket.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 26, 2008
 * Time: 10:30:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class WicketUilsTest extends TestCase
{
	public void testDate()
	{
        ParamDateConverter conv = new ParamDateConverter();
        Date def = new Date();

        String GOOD = "good";
        String good = "2008-10-31";

        String BAD = "bad";
        String bad = "adsfkjadfs";

        PageParameters params = new PageParameters();
        params.put(GOOD, good);
        params.put(BAD, bad);


        Date d = WicketUtils.getAsDate(params, GOOD, null, conv);
        Calendar c = new GregorianCalendar();
        c.setTime(d);

        assertEquals(2008, c.get(Calendar.YEAR));
        assertEquals(10-1, c.get(Calendar.MONTH));
        assertEquals(31, c.get(Calendar.DAY_OF_MONTH));

        Date b = WicketUtils.getAsDate(params, BAD, def, conv);
        assertEquals(def, b);
    }
}
