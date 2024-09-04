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
/*
 * XMLConfigFileLoader.java
 *
 * Created on October 11, 2002, 6:02 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.log4j.*;
import org.jdom2.*;
import org.jdom2.input.*;
import org.xml.sax.*;

import java.net.*;
import java.util.*;

/**
 * @author donohoe
 */
@SuppressWarnings({"unchecked"})
public class XMLConfigFileLoader implements ErrorHandler
{
    static Logger logger = Logger.getLogger(XMLConfigFileLoader.class);

    /**
     * Standard Donohoe Digital name space - http://www.donohoedigital.com
     */
    public static final String DDNAMESPACE = "http://www.donohoedigital.com";
    protected Namespace ns_ = Namespace.getNamespace(DDNAMESPACE);
    private int nWarn_ = 0;
    private int nError_ = 0;
    private int nFatal_ = 0;

    /**
     * Creates a new instance of XMLConfigFileLoader
     */
    public XMLConfigFileLoader()
    {
        ns_ = Namespace.getNamespace(XMLConfigFileLoader.DDNAMESPACE);
    }

    /**
     * get namespace
     */
    public Namespace getNamespace()
    {
        return ns_;
    }

    /**
     * Load document and validate against given schema in the DDNAMESPACE name space
     */
    public Document loadXMLUrl(URL url, String sValidationSchema)
    {
        return loadXMLUrl(url, DDNAMESPACE, sValidationSchema, null, null);
    }

    /**
     * Load document and validate against given schema in the DDNAMESPACE name space
     */
    public Document loadXMLUrl(URL url, String sValidationSchema, String sAppName)
    {
        return loadXMLUrl(url, DDNAMESPACE, sValidationSchema, sAppName, null);
    }

    /**
     * Load document and validate against given schema in the DDNAMESPACE name space
     */
    public Document loadXMLUrl(URL url, String sValidationSchema, String sAppName, String sModule)
    {
        return loadXMLUrl(url, DDNAMESPACE, sValidationSchema, sAppName, sModule);
    }

    /**
     * Load document and validate against given schema in given namespace using
     * http://apache.org/xml/properties/schema/external-schemaLocation.
     * If sNameSpace
     * is null, then parser configured to use
     * http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation with
     * given schema.
     */
    public Document loadXMLUrl(URL url, String sNameSpace, String sValidationSchema, String sAppName, String sModule)
    {
        SAXBuilder parser = createParser(sNameSpace, sValidationSchema, sAppName, sModule);
        Document doc = null;
        JDOMException jde = null;
        Exception e = null;

        try
        {
            doc = parser.build(url);
        }
        catch (JDOMException e1)
        {
            jde = e1;
        }
        catch (Exception e2)
        {
            e = e2;
        }

        if (nWarn_ > 0)
        {
            logger.warn("Summary: " + nWarn_ + " warnings found loading document");
        }

        if (nError_ > 0)
        {
            logger.error("Summary: " + nError_ + " errors found loading document");
        }

        if (nFatal_ > 0)
        {
            logger.fatal("Summary: " + nFatal_ + " fatal errors found loading document");
        }

        if (nError_ > 0 || nFatal_ > 0 || jde != null || e != null)
        {
            // if no errors recorded, but we have an exception, throw that up the chain
            if (nError_ == 0 && nFatal_ == 0)
            {
                if (e != null) throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, e);
                else throw new ApplicationError(ErrorCodes.ERROR_JDOM_PARSE_FAILED, jde.getMessage(), "");
            }
            // otherwise, just record errors
            else
            {
                throw new ApplicationError(ErrorCodes.ERROR_JDOM_PARSE_FAILED,
                                           nError_ + " error(s) and " + nFatal_ + " fatal error(s) loading document.",
                                           "Resolve ERROR and/or FATAL issues indicated in log output");
            }
        }

        return doc;
    }

    /**
     * Create the parser according to the given parameters.
     */
    private SAXBuilder createParser(String sNameSpace, String sValidationSchema, String sAppName, String sModule)
    {
        SAXBuilder parser = new SAXBuilder(true);
        parser.setErrorHandler(this);
        parser.setEntityResolver(CachedEntityResolver.instance());

        // see bottom of file parser.setFactory(new MyFactory());
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setFeature("http://apache.org/xml/features/validation/schema", true);
        parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

        // If validation schema passed in, set it.  This is used to ensure correct schema is
        // used to validate and avoid catch wrong xml file type

        if (sValidationSchema != null)
        {
            String base = "classpath*:config/";
            String valsubpath = "xml-schema/" + sValidationSchema;
            URL schemaurl = null;

            // first look in module passed in for validation schema (unique case)            
            if (sModule != null)
            {
                schemaurl = new MatchingResources(base + sModule + "/" + valsubpath).getSingleResourceURL();
            }

            // if not there, then look in config/appname/xml-schema dir (app specific)
            if (schemaurl == null && sAppName != null)
            {
                schemaurl = new MatchingResources(base + sAppName + "/" + valsubpath).getSingleResourceURL();
            }

            // if not there, then look in config/xml-schema dir (global)
            if (schemaurl == null)
            {
                schemaurl = new MatchingResources(base + valsubpath).getSingleResourceURL();
            }

            if (schemaurl == null)
            {
                logger.warn("XML Schema file " + valsubpath +
                            " could not be found in classpath.  Defaulting to that specified in the document");
            }
            else
            {
                String sURL = schemaurl.toString();
                if (sNameSpace == null)
                {
                    parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
                                       sURL);
                    //logger.debug("Using XML Schema " + sURL + " to validate (no namespace)");
                }
                else
                {
                    parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                                       sNameSpace + " " + sURL);
                    //logger.debug("Using XML Schema " + sURL + " to validate (ns: " + sNameSpace + ")");
                }
            }
        }

        return parser;
    }

    /**
     * Handle SAX Warning
     */
    public void warning(SAXParseException ex)
    {
        nWarn_++;
        logger.warn(getLocationString(ex) + "\n" +
                    ex.getMessage());
        //logger.warn(Utils.formatExceptionText(ex));
    }

    /**
     * Handle SAX Error
     */
    public void error(SAXParseException ex)
    {
        nError_++;
        logger.error(getLocationString(ex) + "\n" +
                     ex.getMessage());
    }

    /**
     * Handle SAX Fatal Error
     */
    public void fatalError(SAXParseException ex) throws SAXException
    {
        nFatal_++;
        logger.fatal(getLocationString(ex) + "\n" +
                     ex.getMessage());
    }

    /**
     * Returns a string of the location of an error
     */
    private String getLocationString(SAXParseException ex)
    {
        StringBuilder str = new StringBuilder();

        String systemId = ex.getSystemId();
        if (systemId != null)
        {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            str.append(systemId);
        }
        str.append(", line ");
        str.append(ex.getLineNumber());
        // leave column info off as it is generally inaccurate
        //str.append(", col ");
        //str.append(ex.getColumnNumber());

        return str.toString();
    }

    ///
    /// Convienence functions for parsing documents
    ///

    /**
     * Return String array of contents of children with given name
     */
    public static String[] getChildStringArray(Element parent, String sChildName, Namespace ns,
                                               boolean bRequired, String sErrLocation)
    {
        List<Element> children = getChildren(parent, sChildName, ns, bRequired, sErrLocation);
        int nSize = children.size();
        if (nSize == 0) return null;

        String[] values = new String[nSize];
        for (int i = 0; i < nSize; i++)
        {
            values[i] = children.get(i).getTextTrim();
        }
        return values;
    }

    /**
     * Return Boolean array of contents of children with given name
     */
    public static Boolean[] getChildBooleanArray(Element parent, String sChildName, Namespace ns,
                                                 boolean bRequired, String sErrLocation)
    {
        List<Element> children = getChildren(parent, sChildName, ns, bRequired, sErrLocation);
        int nSize = children.size();
        if (nSize == 0) return null;

        Boolean[] values = new Boolean[nSize];
        Boolean value;
        String sValue = null;
        for (int i = 0; i < nSize; i++)
        {
            sValue = children.get(i).getTextTrim();
            value = Utils.parseBoolean(sValue);

            if (value == null)
            {
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                           "Element " + sChildName + " does not contain a valid boolean value in " + parent.getName(),
                                           "Value=" + sValue,
                                           "Make sure value is an boolean (true/1 or false/0) at " + sErrLocation);
            }
            values[i] = value;
        }
        return values;
    }

    /**
     * Return Integer array of contents of children with given name
     */
    public static Integer[] getChildIntegerArray(Element parent, String sChildName, Namespace ns,
                                                 boolean bRequired, String sErrLocation)
    {
        List<Element> children = getChildren(parent, sChildName, ns, bRequired, sErrLocation);
        int nSize = children.size();
        if (nSize == 0) return null;

        Integer[] values = new Integer[nSize];
        String sValue = null;
        for (int i = 0; i < nSize; i++)
        {
            try
            {
                sValue = children.get(i).getTextTrim();
                values[i] = Integer.valueOf(sValue);
            }
            catch (NumberFormatException ne)
            {
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                           "Element " + sChildName + " does not contain a valid integer value in " + parent.getName(),
                                           "Value=" + sValue,
                                           "Make sure value is an integer at " + sErrLocation);
            }
        }
        return values;
    }

    /**
     * Return Double array of contents of children with given name
     */
    public static Double[] getChildDoubleArray(Element parent, String sChildName, Namespace ns,
                                               boolean bRequired, String sErrLocation)
    {
        List<Element> children = getChildren(parent, sChildName, ns, bRequired, sErrLocation);
        int nSize = children.size();
        if (nSize == 0) return null;

        Double[] values = new Double[nSize];
        String sValue = null;
        for (int i = 0; i < nSize; i++)
        {
            try
            {
                sValue = (children.get(i)).getTextTrim();
                values[i] = Double.valueOf(sValue);
            }
            catch (NumberFormatException ne)
            {
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                           "Element " + sChildName + " does not contain a valid double value in " + parent.getName(),
                                           "Value=" + sValue,
                                           "Make sure value is an double at " + sErrLocation);
            }
        }
        return values;
    }

    /**
     * Return Integer array of attribute value of children with given name
     */
    public static Integer[] getChildAttributeIntegerArray(Element parent, String sChildName,
                                                          String sAttrName, Namespace ns,
                                                          boolean bRequired, String sErrLocation)
    {
        List<Element> children = getChildren(parent, sChildName, ns, bRequired, sErrLocation);
        int nSize = children.size();
        if (nSize == 0) return null;

        Integer[] values = new Integer[nSize];
        String sValue = null;
        for (int i = 0; i < nSize; i++)
        {
            try
            {
                values[i] = getIntegerAttributeValue((children.get(i)),
                                                     sAttrName, bRequired, sErrLocation +
                                                                           " (" + sChildName + " #" + i + ")");
            }
            catch (NumberFormatException ne)
            {
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                           "Element " + sChildName + " #" + i +
                                           " does not contain a valid integer value in " + parent.getName(),
                                           "Value=" + sValue,
                                           "Make sure value is an integer at " + sErrLocation);
            }
        }
        return values;
    }

    /**
     * Given an element, return a boolean from the given attribute's value
     */
    public static Boolean getBooleanAttributeValue(Element element, String sAttrName,
                                                   boolean bRequired, String sErrLocation,
                                                   Boolean bDefault)
    {
        String sValue = element.getAttributeValue(sAttrName);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sAttrName, "attribute",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return bDefault;

        Boolean value = Utils.parseBoolean(sValue);
        if (value == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                       "Attribute " + sAttrName + " does not contain a valid boolean value.",
                                       "Value=" + sValue,
                                       "Make sure value is 0, 1, true or false");
        }
        return value;
    }

    /**
     * Pass null for default
     */
    public static Boolean getBooleanAttributeValue(Element element, String sAttrName,
                                                   boolean bRequired, String sErrLocation)
    {
        return getBooleanAttributeValue(element, sAttrName, bRequired, sErrLocation, null);
    }

    /**
     * Given an element, return an integer from the given attribute's value
     */
    public static Integer getIntegerAttributeValue(Element element, String sAttrName,
                                                   boolean bRequired, String sErrLocation,
                                                   Integer nDefault)
    {
        String sValue = element.getAttributeValue(sAttrName);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sAttrName, "attribute",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return nDefault;

        try
        {
            return Integer.valueOf(sValue);
        }
        catch (NumberFormatException ne)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                       "Attribute " + sAttrName + " does not contain a valid integer value.",
                                       "Value=" + sValue,
                                       "Make sure value is an integer");
        }
    }

    /**
     * Pass null for default
     */
    public static Integer getIntegerAttributeValue(Element element, String sAttrName,
                                                   boolean bRequired, String sErrLocation)

    {
        return getIntegerAttributeValue(element, sAttrName, bRequired, sErrLocation, null);
    }

    /**
     * Given an element, return a double from the given attribute's value
     */
    public static Double getDoubleAttributeValue(Element element, String sAttrName,
                                                 boolean bRequired, String sErrLocation,
                                                 Double dDefault)
    {
        String sValue = element.getAttributeValue(sAttrName);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sAttrName, "attribute",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return dDefault;

        try
        {
            return Double.valueOf(sValue);
        }
        catch (NumberFormatException ne)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                       "Attribute " + sAttrName + " does not contain a valid double value.",
                                       "Value=" + sValue,
                                       "Make sure value is an double");
        }
    }

    /**
     * Pass null for default
     */
    public static Double getDoubleAttributeValue(Element element, String sAttrName,
                                                 boolean bRequired, String sErrLocation)
    {
        return getDoubleAttributeValue(element, sAttrName, bRequired, sErrLocation, null);
    }

    /**
     * Return string attribute value
     */
    public static String getStringAttributeValue(Element element, String sAttrName,
                                                 boolean bRequired, String sErrLocation,
                                                 String sDefault)
    {
        String sValue = element.getAttributeValue(sAttrName);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sAttrName, "attribute",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return sDefault;

        return sValue;
    }

    /**
     * Pass null for default
     */
    public static String getStringAttributeValue(Element element, String sAttrName,
                                                 boolean bRequired, String sErrLocation)
    {
        return getStringAttributeValue(element, sAttrName, bRequired, sErrLocation, null);
    }

    /**
     * Return boolean value of child from given element
     */
    public static Boolean getChildBooleanValue(Element element, String sChildName,
                                               Namespace ns, boolean bRequired, String sErrLocation,
                                               Boolean bDefault)
    {
        String sValue = element.getChildTextTrim(sChildName, ns);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sChildName, "element",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return bDefault;

        Boolean value = Utils.parseBoolean(sValue);
        if (value == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                       "Element " + sChildName + " does not contain a valid boolean value.",
                                       "Value=" + sValue,
                                       "Make sure value is 0, 1, true or false");
        }
        return value;
    }

    /**
     * Pass null for default
     */
    public static Boolean getChildBooleanValue(Element element, String sChildName,
                                               Namespace ns, boolean bRequired, String sErrLocation)
    {
        return getChildBooleanValue(element, sChildName, ns, bRequired, sErrLocation, null);
    }

    /**
     * Return integer value of child from given element
     */
    public static Integer getChildIntegerValue(Element element, String sChildName,
                                               Namespace ns, boolean bRequired, String sErrLocation,
                                               Integer nDefault)
    {
        String sValue = element.getChildTextTrim(sChildName, ns);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sChildName, "element",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return nDefault;

        try
        {
            return Integer.valueOf(sValue);
        }
        catch (NumberFormatException ne)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                       "Element " + sChildName + " does not contain a valid integer value.",
                                       "Value=" + sValue,
                                       "Make sure value is a integer");
        }
    }

    /**
     * Pass null for default
     */
    public static Integer getChildIntegerValue(Element element, String sChildName,
                                               Namespace ns, boolean bRequired, String sErrLocation)
    {
        return getChildIntegerValue(element, sChildName, ns, bRequired, sErrLocation, null);
    }

    /**
     * Return double value of child from given element
     */
    public static Double getChildDoubleValue(Element element, String sChildName,
                                             Namespace ns, boolean bRequired, String sErrLocation,
                                             Double dDefault)
    {
        String sValue = element.getChildTextTrim(sChildName, ns);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sChildName, "element",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return dDefault;

        try
        {
            return Double.valueOf(sValue);
        }
        catch (NumberFormatException ne)
        {
            throw new ApplicationError(ErrorCodes.ERROR_VALIDATION,
                                       "Element " + sChildName + " does not contain a valid double value.",
                                       "Value=" + sValue,
                                       "Make sure value is a double");
        }
    }

    /**
     * Pass null for default
     */
    public static Double getChildDoubleValue(Element element, String sChildName,
                                             Namespace ns, boolean bRequired, String sErrLocation)
    {
        return getChildDoubleValue(element, sChildName, ns, bRequired, sErrLocation, null);
    }

    /**
     * Return string value of child (trimmed) from given element
     */
    public static String getChildStringValueTrimmed(Element element, String sChildName,
                                                    Namespace ns, boolean bRequired, String sErrLocation,
                                                    String sDefault)
    {
        String sValue = element.getChildTextTrim(sChildName, ns);

        if (bRequired && sValue == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sChildName, "element",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        if (sValue == null) return sDefault;

        return sValue;
    }

    /**
     * Pass null for default
     */
    public static String getChildStringValueTrimmed(Element element, String sChildName,
                                                    Namespace ns, boolean bRequired, String sErrLocation)
    {
        return getChildStringValueTrimmed(element, sChildName, ns, bRequired, sErrLocation, null);
    }

    /**
     * Return child of given element
     */
    protected static Element getChild(Element element, String sChildName,
                                      Namespace ns, boolean bRequired, String sErrLocation)
    {
        // always returns non-null (if none, size=0)
        Element child = element.getChild(sChildName, ns);

        if (bRequired && child == null)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sChildName, "element",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        return child;
    }

    /**
     * Return children of given element
     */
    public static List<Element> getChildren(Element element, String sChildName,
                                            Namespace ns, boolean bRequired, String sErrLocation)
    {
        // always returns non-null (if none, size=0)
        List<Element> children = element.getChildren(sChildName, ns);

        if (bRequired && children.size() <= 0)
        {
            throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                       getErrorMessage(sChildName, "element(s)",
                                                       element.getName(), sErrLocation),
                                       null);
        }

        return children;
    }

    /**
     * Loads &lt;param&gt; and &lt;paramlist&gt; children into the given
     * TypedHashMap
     */
    public static void loadParams(Element element, Namespace ns, TypedHashMap map,
                                  boolean bParamRequired, boolean bParamListRequired,
                                  String sErrLocation)
    {
        String sAttrErrorDesc;
        Element param;
        String sName;
        Object oValue;

        // <param>
        List<Element> params = XMLConfigFileLoader.getChildren(element, "param", ns, bParamRequired, sErrLocation);

        int nNumParams = params.size();
        for (int i = 0; i < nNumParams; i++)
        {
            sAttrErrorDesc = "Param #" + (i + 1) + " of " + sErrLocation;
            param = params.get(i);

            sName = getStringAttributeValue(param, "name", true, sAttrErrorDesc);
            oValue = getStringAttributeValue(param, "strvalue", false, sAttrErrorDesc);

            if (oValue == null)
            {
                oValue = getIntegerAttributeValue(param, "intvalue", false, sAttrErrorDesc);
            }

            if (oValue == null)
            {
                oValue = getDoubleAttributeValue(param, "dblvalue", false, sAttrErrorDesc);
            }

            if (oValue == null)
            {
                oValue = getBooleanAttributeValue(param, "boolvalue", false, sAttrErrorDesc);
            }

            if (oValue == null)
            {
                throw new ApplicationError(ErrorCodes.ERROR_NOT_FOUND,
                                           "Param " + sName + " has no defined values in " + sAttrErrorDesc, null);
            }

            map.put(sName, oValue);
        }

        // <paramlist>
        List<Element> paramlist = XMLConfigFileLoader.getChildren(element, "paramlist", ns, bParamListRequired, sErrLocation);
        nNumParams = paramlist.size();
        Object[] values;
        List<Object> list;
        for (int i = 0; i < nNumParams; i++)
        {
            sAttrErrorDesc = "Paramlist #" + (i + 1) + " of " + sErrLocation;
            param = paramlist.get(i);
            list = new ArrayList<Object>();

            sName = getStringAttributeValue(param, "name", true, sAttrErrorDesc);

            values = getChildBooleanArray(param, "boolvalue", ns, false, sAttrErrorDesc);
            for (int j = 0; values != null && j < values.length; j++)
            {
                list.add(values[j]);
            }

            values = getChildStringArray(param, "strvalue", ns, false, sAttrErrorDesc);
            for (int j = 0; values != null && j < values.length; j++)
            {
                list.add(values[j]);
            }

            values = getChildIntegerArray(param, "intvalue", ns, false, sAttrErrorDesc);
            for (int j = 0; values != null && j < values.length; j++)
            {
                list.add(values[j]);
            }

            values = getChildDoubleArray(param, "dblvalue", ns, false, sAttrErrorDesc);
            for (int j = 0; values != null && j < values.length; j++)
            {
                list.add(values[j]);
            }

            map.put(sName, list);
        }
    }

    /**
     * Generate error message when lookup fails
     */
    private static String getErrorMessage(String sName, String sType, String sElementName, String sErrLocation)
    {
        String sLocation = "";
        if (sErrLocation != null) sLocation = " in " + sErrLocation;

        return sName + " " + sType + " missing in " + sElementName + sLocation;
    }

    ///
    /// Debugging functions
    ///

    /**
     * Print Jdom element and its children
     */
    public void debugListChildren(Element current, int depth)
    {
        logger.debug(genSpaces(depth) + current.getName() + " (" + current.getClass().getName() + ") - " +
                     current.getTextTrim());
        List<Element> children = current.getChildren();
        for (Element child : children)
        {
            debugListChildren(child, depth + 1);
        }

    }

    /**
     * Used to generate spaces for output
     */
    private String genSpaces(int n)
    {
        StringBuilder spaces = new StringBuilder();
        spaces.setLength(n * 3);

        for (int i = 0; i < spaces.length(); i++)
        {
            spaces.setCharAt(i, ' ');
        }
        return spaces.toString();
    }
}
