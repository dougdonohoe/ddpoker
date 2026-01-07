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
package com.donohoedigital.config;

import org.springframework.core.io.*;
import org.springframework.core.type.classreading.*;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Nov 6, 2008
 * Time: 12:40:35 PM
 * To change this template use File | Settings | File Templates.
 */
class CustomCachingMetadataReaderFactory extends SimpleMetadataReaderFactory
{

    private final Map<Resource, MetadataReader> classReaderCache = new HashMap<Resource, MetadataReader>();


    /**
     * Create a new CachingMetadataReaderFactory for the default class loader.
     */
    public CustomCachingMetadataReaderFactory()
    {
        super();
    }

    /**
     * Create a new CachingMetadataReaderFactory for the given resource loader.
     *
     * @param resourceLoader the Spring ResourceLoader to use
     *                       (also determines the ClassLoader to use)
     */
    public CustomCachingMetadataReaderFactory(ResourceLoader resourceLoader)
    {
        super(resourceLoader);
    }

    /**
     * Create a new CachingMetadataReaderFactory for the given class loader.
     *
     * @param classLoader the ClassLoader to use
     */
    public CustomCachingMetadataReaderFactory(ClassLoader classLoader)
    {
        super(classLoader);
    }

    /**
     * Override to (a) get MetadataReader wrapped in CachingMetadataReader and (b) cache it
     *
     * @param resource
     * @return
     * @throws IOException
     */
    @Override
    public MetadataReader getMetadataReader(Resource resource) throws IOException
    {
        synchronized (this.classReaderCache)
        {
            MetadataReader metadataReader = this.classReaderCache.get(resource);
            if (metadataReader == null)
            {
                metadataReader = new CachingMetadataReader(super.getMetadataReader(resource));
                this.classReaderCache.put(resource, metadataReader);
            }
            return metadataReader;
        }
    }
}