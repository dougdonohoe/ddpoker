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
package com.donohoedigital.wicket.annotations;

import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MixedParamEncoderTest {

    @Test
    public void testEncodeDecode() {
        MixedParamEncoder encoder = new MixedParamEncoder(new String[] {"x", "y", "a", "b", "c", "d", "e"});
        PageParameters params = new PageParameters();
        params.add("y", "-");
        params.add("a", ".");
        params.add("b", "..");
        params.add("c", "space between");
        params.add("d", "slash/and\\backslash");
        params.add("e", "a:colon");
        Url encoded = encoder.encodePageParameters(params);
        assertEquals("-/:-/:d/:d:d/space%20between/slash:sand:bbackslash/a::colon", encoded.toString());
        PageParameters decoded = encoder.decodePageParameters(encoded);
        assertEquals(params, decoded);

        params = new PageParameters();
        params.add("x", "shorter");
        params.add("y", "path");
        encoded = encoder.encodePageParameters(params);
        assertEquals("shorter/path", encoded.toString());
        decoded = encoder.decodePageParameters(encoded);
        assertEquals(params, decoded);

        params.add("e", "gaps");
        encoded = encoder.encodePageParameters(params);
        assertEquals("shorter/path/-/-/-/-/gaps", encoded.toString());
        decoded = encoder.decodePageParameters(encoded);
        assertEquals(params, decoded);
    }

}