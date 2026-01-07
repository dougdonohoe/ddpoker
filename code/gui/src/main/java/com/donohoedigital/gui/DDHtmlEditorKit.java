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
 * DDHtmlEditorKit.java
 *
 * Created on March 29, 2003, 3:32 PM
 */
package com.donohoedigital.gui;

import com.donohoedigital.config.*;

import javax.swing.text.*;
import javax.swing.text.html.*;
import java.util.*;

public class DDHtmlEditorKit extends HTMLEditorKit 
{
    private StyleSheet sheet_ = null;

    private static HashMap hmTagViewClasses_ = new HashMap();

    static
    {
        registerTagViewClass("img", DDImageView.class);
        registerTagViewClass("ddimg", DDImageView.class);
    }

    public DDHtmlEditorKit(StyleSheet proto)
    {
        sheet_ = proto;
    }

    public static void registerTagViewClass(String tagName, Class viewClass)
    {
        hmTagViewClasses_.put(tagName, viewClass);
    }

    public ViewFactory getViewFactory()
    {
        return new HTMLFactoryX();
    }
    
    public static class HTMLFactoryX extends HTMLFactory
    {
        Class ctorArgs_[] = new Class[] { Element.class };

        public View create(Element elem)
        {
            Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);

//            if (o instanceof HTML.Tag)
//            {
                Class clazz = (Class)hmTagViewClasses_.get(o.toString());

                if (clazz != null)
                {
                    return (View)ConfigUtils.newInstance(clazz, ctorArgs_, new Object[] { elem });
                }
                //System.out.print("TAG " + sName +" ");
//            }
            
            View view = super.create(elem);
            //System.out.println(view.getClass().getName());
            return view;
        }
    }

    /**
     * Override to return our own style sheet, instead of global sheet
     */
    public StyleSheet getStyleSheet()
    {
        if (sheet_ == null)
        {
            // copy parent's style sheet so we can add our own values
            sheet_ = new StyleSheet();
            sheet_.addStyleSheet(super.getStyleSheet());
        }
        return sheet_;
    }
}
