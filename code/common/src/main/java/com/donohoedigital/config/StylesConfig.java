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
 * StylesConfig.java
 *
 * Created on November 09, 2002, 6:02 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.log4j.*;
import org.jdom2.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

/**
 * Loads styles.xml files in the module directories defined by
 * the appconfig.xml file.
 *
 * @author donohoe
 */
public class StylesConfig extends XMLConfigFileLoader
{
    private static Logger sLogger = Logger.getLogger(StylesConfig.class);

    private static final String STYLE_CONFIG = "styles.xml";
    private static final boolean DEBUG_FONT = false;

    private static StylesConfig stylesConfig = null;

    private Map<String, Color> colors_ = new HashMap<String, Color>();
    private Map<String, Font> fonts_ = new HashMap<String, Font>();
    private Map<String, Font> fontdefs_ = new HashMap<String, Font>();

    /**
     * Creates a new instance of StylesConfig from the Appconfig file
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    public StylesConfig(String[] modules) throws ApplicationError
    {
        ApplicationError.warnNotNull(stylesConfig, "StylesConfig is already initialized");
        stylesConfig = this;

        init(modules);
    }

    /**
     * Get current instance of StylesConfig
     */
    private static StylesConfig getConfig()
    {
        return stylesConfig;
    }

    /**
     * Return Color for requested color name
     */
    public static Color getColor(String sName)
    {
        return getConfig().colors_.get(sName);
    }

    /**
     * Return Color file for request color name.  Return cDefault if not found
     */
    public static Color getColor(String sName, Color cDefault)
    {
        Color c = getColor(sName);
        if (c == null)
        {
            if (!sName.contains("default"))
            {
                sLogger.warn("Color not found:" + sName);
            }
            return cDefault;
        }
        return c;
    }

    /**
     * Return Font for requested name
     */
    public static Font getFont(String sName)
    {
        return getConfig().fonts_.get(sName);
    }

    /**
     * Return Font for requested color name.  Return fDefault if not found
     */
    public static Font getFont(String sName, Font fDefault)
    {
        Font font = getConfig().fonts_.get(sName);
        if (font == null)
        {
            if (!sName.contains("default"))
            {
                sLogger.warn("Font not found:" + sName);
            }
            return fDefault;
        }
        return font;
    }

    private void init(String[] modules) throws ApplicationError
    {
        ApplicationError.assertNotNull(modules, "Modules list is null");

        Document doc;
        for (String module : modules)
        {
            // if styles file is missing, no big deal
            URL url = new MatchingResources("classpath*:config/" + module + "/" + STYLE_CONFIG).getSingleResourceURL();
            if (url != null)
            {
                doc = this.loadXMLUrl(url, "styles.xsd");
                init(doc);
            }
        }
    }

    /**
     * Initialize from JDOM doc
     */
    private void init(Document doc) throws ApplicationError
    {
        Element root = doc.getRootElement();

        // get list of colors
        List<Element> colors = getChildren(root, "color", ns_, false, STYLE_CONFIG);
        if (colors == null) return;

        // create color for each one
        Element color;
        String sAttrErrorDesc;
        for (int i = 0; i < colors.size(); i++)
        {
            sAttrErrorDesc = "Color #" + (i + 1) + " in " + STYLE_CONFIG;
            color = colors.get(i);
            initColor(color, sAttrErrorDesc);
        }

        // get list of fonts
        List<Element> fonts = getChildren(root, "font", ns_, false, STYLE_CONFIG);
        if (fonts == null) return;

        // create font for each one
        Element font;
        for (int i = 0; i < fonts.size(); i++)
        {
            sAttrErrorDesc = "Font #" + (i + 1) + " in " + STYLE_CONFIG;
            font = fonts.get(i);
            initFont(font, sAttrErrorDesc);
        }
    }

    /**
     * Read color info
     */
    private void initColor(Element color, String sAttrErrorDesc) throws ApplicationError
    {
        String sName = getStringAttributeValue(color, "name", true, sAttrErrorDesc);
        Integer r = getIntegerAttributeValue(color, "r", false, sAttrErrorDesc);
        Integer g = getIntegerAttributeValue(color, "g", false, sAttrErrorDesc);
        Integer b = getIntegerAttributeValue(color, "b", false, sAttrErrorDesc);
        Integer a = getIntegerAttributeValue(color, "a", false, sAttrErrorDesc);
        String sCopy = getStringAttributeValue(color, "copy", false, sAttrErrorDesc);

        if (sCopy != null)
        {
            ApplicationError.assertTrue(a == null, "'a' should not be defined in color entry " + sName + " (copy)");
            ApplicationError.assertTrue(r == null, "'r' should not be defined in color entry " + sName + " (copy)");
            ApplicationError.assertTrue(g == null, "'g' should not be defined in color entry " + sName + " (copy)");
            ApplicationError.assertTrue(b == null, "'b' should not be defined in color entry " + sName + " (copy)");

            Color copy = colors_.get(sCopy);
            if (copy == null)
            {
                String sMsg = sName + " copies " + sCopy + ", but that wasn't found.";
                sLogger.error(sMsg);
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg,
                                           "Make sure order is correct in " + STYLE_CONFIG +
                                           "(" + sCopy + " must appear before " + sName);
            }
            colors_.put(sName, copy);
        }
        else
        {
            ApplicationError.assertNotNull(r, "'r' missing in color entry " + sName);
            ApplicationError.assertNotNull(g, "'g' missing in color entry " + sName);
            ApplicationError.assertNotNull(b, "'b' missing in color entry " + sName);

            if (a != null)
            {
                colors_.put(sName, new Color(r, g, b, a));
            }
            else
            {
                colors_.put(sName, new Color(r, g, b));
            }
        }
    }

    /**
     * Read font info
     */
    private void initFont(Element font, String sAttrErrorDesc) throws ApplicationError
    {
        String sName = getStringAttributeValue(font, "name", true, sAttrErrorDesc);
        String sFontname = getStringAttributeValue(font, "fontname", true, sAttrErrorDesc);
        Integer nPointSize = getIntegerAttributeValue(font, "pointsize", true, sAttrErrorDesc);
        Boolean BItalic = getBooleanAttributeValue(font, "italic", false, sAttrErrorDesc);
        Boolean BBold = getBooleanAttributeValue(font, "bold", false, sAttrErrorDesc);
        String sCopy = getStringAttributeValue(font, "copy", false, sAttrErrorDesc);

        if (sCopy != null)
        {
            ApplicationError.assertTrue(sFontname == null, "'fontname' should not be defined in font entry " + sName + " (copy)");
            ApplicationError.assertTrue(nPointSize == null, "'pointsize' should not be defined in font entry " + sName + " (copy)");
            ApplicationError.assertTrue(BBold == null, "'bold' should not be defined in font entry " + sName + " (copy)");
            ApplicationError.assertTrue(BItalic == null, "'italic' should not be defined in font entry " + sName + " (copy)");

            Font copy = fonts_.get(sCopy);
            if (copy == null)
            {
                String sMsg = sName + " copies " + sCopy + ", but that wasn't found.";
                sLogger.error(sMsg);
                throw new ApplicationError(ErrorCodes.ERROR_VALIDATION, sMsg,
                                           "Make sure order is correct in " + STYLE_CONFIG +
                                           "(" + sCopy + " must appear before " + sName);
            }
            fonts_.put(sName, copy);
        }
        else
        {
            ApplicationError.assertNotNull(sFontname, "'fontname' missing in font entry " + sName);
            ApplicationError.assertNotNull(nPointSize, "'pointsize' missing in font entry " + sName);

            // Java 1.5.0_19 on Mac makes Lucida Sans look like crap.  A decent mac-specific replacement
            // is Lucida Grande per http://www.ampsoft.net/webdesign-l/WindowsMacFonts.html
            if (sFontname.equalsIgnoreCase("Lucida Sans Regular") && Utils.ISMAC)
            {
                sFontname = "Lucida Grande";
            }

            boolean bItalic = BItalic == null ? false : BItalic;
            boolean bBold = BBold == null ? false : BBold;

            int nStyle = Font.PLAIN;
            if (bItalic && bBold) nStyle = Font.BOLD & Font.ITALIC;
            else if (bItalic) nStyle = Font.ITALIC;
            else if (bBold) nStyle = Font.BOLD;

            Font newfont = null;
            // load directly from our font dir
            if (sFontname.endsWith(".ttf"))
            {
                URL fontUrl = new MatchingResources("classpath*:config/fonts/" + sFontname).getSingleResourceURL();
                Font basefont = fontdefs_.get(sFontname);
                if (basefont == null)
                {
                    if (DEBUG_FONT) sLogger.debug("Loading custom font: " + fontUrl);
                    InputStream is = null;
                    try
                    {
                        is = fontUrl.openStream();
                        basefont = Font.createFont(Font.TRUETYPE_FONT, is);
                        fontdefs_.put(sFontname, basefont);
                    }
                    catch (Exception e)
                    {
                        sLogger.error("Error processing font " + fontUrl);
                        sLogger.error(Utils.formatExceptionText(e));
                    }
                    finally
                    {
                        ConfigUtils.close(is);
                    }
                }
                if (basefont != null)
                {
                    newfont = basefont.deriveFont(nStyle, nPointSize);
                    if (DEBUG_FONT && !newfont.getFontName().equalsIgnoreCase(sFontname))
                    {
                        sLogger.debug(sName + " font mismatch, requested " +
                                      sFontname + ":" + nPointSize + " " +
                                      (bBold ? "(bold) " : " ") + (bItalic ? "(italic) " : " ") +
                                      "==>  font: " + newfont.getFontName() + " (" + newfont.getFamily() + ")"
                                      + " size: " + newfont.getSize());
                    }
                }
            }

            if (newfont == null)
            {
                newfont = new Font(sFontname, nStyle, nPointSize);
                if (DEBUG_FONT && !newfont.getFontName().equalsIgnoreCase(sFontname))
                {
                    sLogger.debug(sName + "  loaded 2, font mismatch, requested " +
                                  sFontname + ":" + nPointSize + " " +
                                  (bBold ? "(bold) " : " ") + (bItalic ? "(italic) " : " ") +
                                  "==> loaded font: " + newfont.getFontName() + " (" + newfont.getFamily() + ")"
                                  + " size: " + newfont.getSize());
                }
            }
            // TODO: check for matching fontname
            fonts_.put(sName, newfont);
        }
    }

//    /**
//     * Code to list all fonts
//     */
//    private void listAllFonts()
//    {
//        String[] fonts;
//        fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
//        for (int i = 0; i < fonts.length; i++)
//        {
//            logger.debug("Font " + i + ": " + fonts[i]);
//        }
//    }
}
