/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSTypeDefinition;

import java.net.URL;
import java.util.HashMap;

/**
 * Loads data-elements.xsd file defined in the appconfig file
 *
 * @author donohoe
 */
public class DataElementConfig extends HashMap<String, DataElement> implements XMLErrorHandler
{
    static Logger logger = LogManager.getLogger(DataElementConfig.class);

    private static DataElementConfig dataConfig = null;

    private final SchemaGrammar sgrammar_;
    private int nWarn_ = 0;
    private int nError_ = 0;
    private int nFatal_ = 0;

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

        // load grammar
        sgrammar_ = loadGrammar(schema);

        // load elements from grammar
        loadDataElements();
    }

    /**
     * Load all data definitions from xsd (simple types) and store as DataElement's
     */
    private void loadDataElements()
    {
        XSNamedMap map = sgrammar_.getComponents(XSTypeDefinition.SIMPLE_TYPE);
        XSObject obj;
        int nNum = map.getLength();
        for (int i = 0; i < nNum; i++)
        {
            obj = map.item(i);
            if (obj instanceof XSSimpleType)
            {
                put(obj.getName(), new DataElement(obj.getName(), (XSSimpleType) obj));
            }
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

    /**
     * load the grammar
     */
    private SchemaGrammar loadGrammar(URL url) throws ApplicationError
    {
        SchemaGrammar sgrammar = null;
        Exception error = null;
        String sURLpath = url.toString();
        XMLSchemaLoader loader = new XMLSchemaLoader();
        loader.setErrorHandler(this);
        loader.setEntityResolver(CachedEntityResolver.instance());
        XMLInputSource source = new XMLInputSource("Donohoe Digital data-elements", sURLpath, null);
        try
        {
            Grammar grammar = loader.loadGrammar(source);
            sgrammar = (SchemaGrammar) grammar;
        }
        catch (Exception e)
        {
            error = e;
        }

        if (nWarn_ > 0)
        {
            logger.warn("Summary: " + nWarn_ + " warnings found loading " + sURLpath);
        }

        if (nError_ > 0)
        {
            logger.error("Summary: " + nError_ + " errors found loading " + sURLpath);
        }

        if (nFatal_ > 0)
        {
            logger.fatal("Summary: " + nFatal_ + " fatal errors found loading " + sURLpath);
        }

        if (nError_ > 0 || nFatal_ > 0 || error != null)
        {
            // if no errors recorded, but we have an exception, throw that up the chain
            if (nError_ == 0 && nFatal_ == 0)
            {
                throw new ApplicationError(ErrorCodes.ERROR_XSD_PARSE_FAILED, error);
            }
            // otherwise, just record errors
            else
            {
                throw new ApplicationError(ErrorCodes.ERROR_XSD_PARSE_FAILED,
                                           nError_ + " error(s) and " + nFatal_ + " fatal error(s) loading " + sURLpath,
                                           "Resolve ERROR and/or FATAL issues indicated in log output");
            }
        }

        return sgrammar;
    }

    ///
    /// XMLErrorHandler methods
    ///
    public void warning(String domain, String key,
                        XMLParseException exception) throws XNIException
    {
        nWarn_++;
        logger.warn(getMessage(domain, key, exception));
    }

    public void error(String domain, String key,
                      XMLParseException exception) throws XNIException
    {
        nError_++;
        logger.error(getMessage(domain, key, exception));
    }


    public void fatalError(String domain, String key,
                           XMLParseException exception) throws XNIException
    {
        nFatal_++;
        logger.fatal(getMessage(domain, key, exception));
    }

    private String getMessage(String domain, String key, XMLParseException e)
    {
        return domain + " [" + key + "] " +
               e.getMessage() + " at line " + e.getLineNumber();
    }
}
