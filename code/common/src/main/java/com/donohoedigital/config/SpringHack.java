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
package com.donohoedigital.config;

public class SpringHack {
    public static void initialize() {
        // The Spring we currently use only works up to Java 1.7 as seen in
        // org.springframework.core.JdkVersion.  Java 1.8 confuses it, causing
        // this:  java.lang.IllegalStateException: AnnotationTransactionAttributeSource
        //        is only available on Java 1.5 and higher
        // DD Poker was originally built on Java v1.5, so let's fake it out.  This calls needs to
        // happen before any spring contexts are loaded.  For unit tests, this is why we need
        // the HackRunner classes that extend SpringJUnit4ClassRunner
        // TODO: upgrade Spring to latest version and try to eliminate this.
        System.setProperty("java.version", "1.5.0_22");

        // if DB_HOST not defined, hard-code it to localhost.  We do this because apparently
        // current (ancient) version of spring doesn't support defaults like
        // ${DB_HOST:127.0.0.1} in properties files
        if (System.getenv("DB_HOST") == null) {
            System.setProperty("DB_HOST", "127.0.0.1");
        }
    }
}
