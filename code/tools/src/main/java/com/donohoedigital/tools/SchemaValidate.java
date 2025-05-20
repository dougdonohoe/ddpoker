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
package com.donohoedigital.tools;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;

public class SchemaValidate extends DefaultHandler {
    StringBuilder result = new StringBuilder();

    /** Warning. */
    public void warning(SAXParseException ex) {

        result.append("[Warning] "+
                           getLocationString(ex)+": "+
                           ex.getMessage() +  " \n");
    }

    /** Error. */
    public void error(SAXParseException ex) {

        result.append("[Error] "+
                           getLocationString(ex)+": "+
                           ex.getMessage() + " " + "\n");
//                           "\n at " + Utils.formatExceptionText(new Throwable()) + "\n");
    }

    /** Fatal error. */
    public void fatalError(SAXParseException ex) throws SAXException {

        result.append("[Fatal Error] "+
                           getLocationString(ex)+": "+
                           ex.getMessage() + " \n");
    }

    /** Returns a string of the location. */
    private String getLocationString(SAXParseException ex) {
        StringBuilder str = new StringBuilder();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            str.append(systemId);
        }
        str.append(':');
        str.append(ex.getLineNumber());
        str.append(':');
        str.append(ex.getColumnNumber());

        return str.toString();

    } // getLocationString(SAXParseException):String

    public static String process(String xmlFileURL) {
        try {
            SchemaValidate validate = new SchemaValidate();
            XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            parser.setContentHandler(validate);
            parser.setErrorHandler(validate);
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
            parser.parse(xmlFileURL);
            return validate.result.toString();
        }
        catch (Exception e) { e.printStackTrace(); }
        return null;
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java com.donohoedigital.tools.SchemaValidate <XML file>");
            return;
        }
        try {
            String validationResults = process(args[0]);
            if (validationResults.length() == 0)
            {
                System.out.println("Contents okay.");
            }
            else
            {
                System.out.println("Problems found:\n");
                System.out.println(validationResults);
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }
}

