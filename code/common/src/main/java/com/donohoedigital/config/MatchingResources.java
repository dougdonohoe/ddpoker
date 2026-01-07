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

import org.apache.logging.log4j.*;
import org.springframework.core.io.*;
import org.springframework.core.io.support.*;
import org.springframework.core.type.*;
import org.springframework.core.type.classreading.*;

import java.io.*;
import java.lang.annotation.*;
import java.net.*;
import java.util.*;

/**
 * Class to get matching resources - uses Spring's {@link PathMatchingResourcePatternResolver}.
 *
 * @author Doug Donohoe
 * @see PathMatchingResourcePatternResolver
 */
public class MatchingResources
{
    private final Resource[] resources;
    private final String pattern;

    /**
     * Initialize list of matching {@link Resource} as found by
     * {@link PathMatchingResourcePatternResolver#getResources(String)}.
     *
     * @param sPattern the pattern to search for
     * @see PathMatchingResourcePatternResolver
     */
    public MatchingResources(String sPattern)
    {
        pattern = sPattern.replace('\\', '/'); // fix DOS paths
        PathMatchingResourcePatternResolver match = new PathMatchingResourcePatternResolver();
        try
        {
            resources = match.getResources(pattern);
            // get on demand since used before LoggingConfig is run
            Logger logger = LogManager.getLogger(MatchingResources.class);
            logger.debug("Found {} resource(s) for: {}", resources.length, pattern);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all matching resources
     *
     * @return A {@link Resource} array of all matches.  If no matches are found this is a zero-length array.
     */
    public Resource[] getAllMatches()
    {
        return resources;
    }

    /**
     * Get all matching resources as URLs.
     *
     * @return {@link URL} array determined by calling {@link #getURL(Resource)} on each resource.
     */
    public URL[] getAllMatchesURL()
    {
        URL[] urls = new URL[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            urls[i] = getURL(resources[i]);
        }
        return urls;
    }

    /**
     * Get all matching classes that are annotated with the given Annotation.
     *
     * @param annotation an annotation class
     * @return Set of all classes that have the given annotation.  List is empty if non matches found.
     */
    public Set<Class<?>> getAnnotatedMatches(Class<? extends Annotation> annotation)
    {
        Set<Class<?>> matches = new HashSet<>();
        for (Resource r : resources)
        {
            MetadataReader meta = getMetadataReader(r);
            AnnotationMetadata anno = meta.getAnnotationMetadata();
            Set<String> types = anno.getAnnotationTypes();
            if (types.contains(annotation.getName()))
            {
                matches.add(ConfigUtils.getClass(anno.getClassName()));
            }
        }
        return matches;
    }

    /**
     * Get all matching classes that implement given interface
     *
     * @param iclass an interface class
     * @return Set of all classes that implement given interface.  List is empty if non matches found.
     */
    public Set<Class<?>> getImplementingMatches(Class<?> iclass)
    {
        Set<Class<?>> matches = new HashSet<>();
        for (Resource r : resources)
        {
            // Get meta data
            MetadataReader meta = getMetadataReader(r);
            ClassMetadata classmeta = meta.getClassMetadata();

            // get all interfaces this class implements
            String[] interfaces = classmeta.getInterfaceNames();
            for (String i : interfaces)
            {
                if (i.equals(iclass.getName()))
                {
                    Class<?> clazz = ConfigUtils.getClass(classmeta.getClassName());

                    // if class is an interface itself (meaning we extended an interface), look for that interface too
                    if (classmeta.isInterface())
                    {
                        matches.addAll(getImplementingMatches(clazz));
                    }
                    else
                    {
                        matches.add(clazz);
                        matches.addAll(getSubclasses(clazz));
                    }
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * Get all subclasses of given class
     *
     * @param clazz a class
     * @return Set of all classes that subclass given class.  List is empty if non matches found.
     */
    public Set<Class<?>> getSubclasses(Class<?> clazz)
    {
        Set<Class<?>> matches = new HashSet<>();
        for (Resource r : resources)
        {
            MetadataReader meta = getMetadataReader(r);
            ClassMetadata classmeta = meta.getClassMetadata();

            String superClassName = classmeta.getSuperClassName();
            if (superClassName != null && superClassName.equals(clazz.getName()))
            {
                Class<?> sub = ConfigUtils.getClass(classmeta.getClassName());
                matches.add(sub);
                matches.addAll(getSubclasses(sub));
            }
        }
        return matches;
    }

    private MetadataReaderFactory factory;

    /**
     * Get metadata reader for given resource using our factory (lazily created)
     */
    private MetadataReader getMetadataReader(Resource r)
    {
        if (factory == null) factory = new CustomCachingMetadataReaderFactory();
        MetadataReader meta;
        try
        {
            meta = factory.getMetadataReader(r);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to get MetadataReader for " + r, e);
        }
        return meta;
    }

    /**
     * Get a single matching resource.  Throws an exception if multiple are found.  This is useful if you
     * are expecting to find only one instance of an item on the classpath.
     *
     * @return The single matching {@link Resource}
     * @throws RuntimeException if more than one {@link Resource} was found
     */
    public Resource getSingleResource()
    {
        if (resources.length > 1)
        {
            throw new RuntimeException("Found more than one resource in classpath for " + pattern + ": " + this);
        }

        if (resources.length == 0) return null;
        return resources[0];
    }

    /**
     * Similar to {@link #getSingleResource()}, but returns result as an {@link URL}.
     *
     * @return The single matching {@link Resource} as an {@link URL}
     * @throws RuntimeException if more than one {@link Resource} was found
     */
    public URL getSingleResourceURL()
    {
        Resource r = getSingleResource();
        if (r == null) return null;
        return getURL(r);
    }

    /**
     * Get a single required matching resource.  Throws an exception if zero or multiple are found.  This is useful if you
     * are expecting to find one and only one instance of an item on the classpath.
     *
     * @return The single matching {@link Resource}
     * @throws RuntimeException if zero or more than one {@link Resource} was found
     */
    public Resource getSingleRequiredResource()
    {
        if (resources.length == 0)
        {
            throw new RuntimeException("Could not find required resource for " + pattern);
        }

        if (resources.length > 1)
        {
            throw new RuntimeException("Found more than one resource in classpath for " + pattern + ": " + this);
        }

        return resources[0];
    }

    /**
     * Similar to {@link #getSingleRequiredResource()}, but returns result as an {@link URL}.
     *
     * @return The single matching {@link Resource} as an {@link URL}
     * @throws RuntimeException if zero or more than one {@link Resource} was found
     */
    public URL getSingleRequiredResourceURL()
    {
        Resource r = getSingleRequiredResource();
        if (r == null) return null;
        return getURL(r);
    }

    /**
     * Get URL from resource.
     *
     * @param r a {@link Resource}
     * @return its URL
     * @throws RuntimeException if {@link Resource#getURL()} throws {@link IOException}
     */
    public URL getURL(Resource r)
    {
        if (r == null) return null;
        try
        {
            return r.getURL();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return string representing all matching resources as URLs
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Resource r : resources)
        {
            if (sb.length() > 0) sb.append('\n');
            sb.append(getURL(r).toString());
        }
        return sb.toString();
    }
}
