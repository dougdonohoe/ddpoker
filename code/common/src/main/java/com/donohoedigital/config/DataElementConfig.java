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
 * DataElementConfig.java
 *
 * Created on November 11, 2002, 6:02 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads data-elements.xsd file defined in the appconfig file.
 * <p>
 * Only the named &lt;xsd:simpleType&gt; declarations are of interest: their
 * &lt;xsd:enumeration&gt; values become the list backing a {@link DataElement}
 * (and thus a combo box).  The file is read with JDOM rather than a schema
 * processor since no validation of the schema itself is required.
 *
 * @author donohoe
 */
public class DataElementConfig extends HashMap<String, DataElement>
{
    /**
     * The XML Schema namespace that data-elements.xsd files are written in
     */
    private static final Namespace XSD = Namespace.getNamespace("http://www.w3.org/2001/XMLSchema");

    private static DataElementConfig dataConfig = null;

    /**
     * DataElements for app unless elements from given module exist
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    public DataElementConfig(String sAppName, String sOverrideModule)
    {
        ApplicationError.warnNotNull(dataConfig, "DataElementConfig already created");
        dataConfig = this;

        String base = "classpath*:config/";
        String valsubpath = "xml-schema/data-elements.xsd";
        URL schema = null;

        // first look in module passed in for validation schema (unique case)
        if (sOverrideModule != null)
        {
            schema = new MatchingResources(base + sOverrideModule + "/" + valsubpath).getSingleResourceURL();
        }

        // if not there, then look in config/appname/xml-schema dir (app specific)
        if (schema == null)
        {
            schema = new MatchingResources(base + sAppName + "/" + valsubpath).getSingleResourceURL();
        }

        // if not there, then look in config/xml-schema dir (global)
        if (schema == null)
        {
            schema = new MatchingResources(base + valsubpath).getSingleResourceURL();
        }

        // load elements from the schema and everything it includes
        loadDataElements(schema, new HashSet<>());
    }

    /**
     * Load all data definitions from the given xsd (simple types) and store as
     * DataElement's, recursing into every &lt;xsd:include&gt; first so that a
     * type redefined locally wins over the one it was included from.
     */
    private void loadDataElements(URL url, Set<String> visited)
    {
        // guard against an include cycle or a diamond include
        if (url == null || !visited.add(url.toString())) return;

        Element schema = parse(url);

        // includes first, so locally declared types overwrite included ones
        for (Element include : schema.getChildren("include", XSD))
        {
            loadDataElements(resolveInclude(url, include.getAttributeValue("schemaLocation")), visited);
        }

        for (Element simpleType : schema.getChildren("simpleType", XSD))
        {
            String sName = simpleType.getAttributeValue("name");
            if (sName == null) continue; // anonymous type - not addressable, skip

            List<String> values = getEnumerationValues(simpleType);
            put(sName, values.isEmpty() ? new DataElement(sName) : new DataElement(sName, values));
        }
    }

    /**
     * Return the &lt;xsd:enumeration&gt; values of a simple type's restriction, in
     * document order.  Empty if the type is not an enumeration.
     */
    private static List<String> getEnumerationValues(Element simpleType)
    {
        List<String> values = new ArrayList<>();

        Element restriction = simpleType.getChild("restriction", XSD);
        if (restriction == null) return values;

        for (Element enumeration : restriction.getChildren("enumeration", XSD))
        {
            String sValue = enumeration.getAttributeValue("value");
            if (sValue != null) values.add(sValue);
        }

        return values;
    }

    /**
     * Resolve a schemaLocation.  "classpath:" (and "file:") locations go through
     * {@link CachedEntityResolver}; anything else is resolved relative to the
     * including document.
     */
    private static URL resolveInclude(URL parent, String sLocation)
    {
        if (sLocation == null) return null;

        try
        {
            URL url = CachedEntityResolver.instance().getMatch(sLocation);
            return url != null ? url : parent.toURI().resolve(sLocation).toURL();
        }
        catch (Exception e)
        {
            throw new ApplicationError(ErrorCodes.ERROR_XSD_PARSE_FAILED,
                                       "Unable to resolve include " + sLocation + " in " + parent, e,
                                       "Check the schemaLocation is a valid classpath: or relative reference");
        }
    }

    /**
     * Parse the given xsd and return its root &lt;xsd:schema&gt; element
     */
    private static Element parse(URL url)
    {
        try
        {
            // no validation - this is a schema, not an instance document
            Document doc = new SAXBuilder().build(url);
            return doc.getRootElement();
        }
        catch (JDOMException | IOException e)
        {
            throw new ApplicationError(ErrorCodes.ERROR_XSD_PARSE_FAILED,
                                       "Unable to parse " + url, e,
                                       "Resolve the XML error indicated above");
        }
    }

    /**
     * Return DataElement for request data element name from
     * global list of data elements
     */
    public static DataElement getDataElement(String sName)
    {
        return dataConfig.get(sName);
    }
}
