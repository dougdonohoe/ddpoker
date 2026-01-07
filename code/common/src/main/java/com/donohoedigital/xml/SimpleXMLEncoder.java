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
package com.donohoedigital.xml;

import com.donohoedigital.base.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;

/**
 * @author Doug Donohoe
 */
public class SimpleXMLEncoder
{
    //private static Logger logger = LogManager.getLogger(SimpleXMLEncoder.class);

    SimpleDateFormat format = Utils.getRFC822();

    private StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    private Stack<EncoderObject> currentObject = new Stack<EncoderObject>();

    /**
     * Set current object null and start new tag with name "alias".
     * Used for manually creating entries like lists.
     */
    public SimpleXMLEncoder setCurrentObject(String alias)
    {
        setCurrentObject(null, alias);
        return this;
    }

    /**
     * Set current object - starts new tag with name "alias"
     */
    public SimpleXMLEncoder setCurrentObject(Object clazz, String alias)
    {
        EncoderObject current = new EncoderObject(clazz, alias);
        startTag(current.getAlias(), true);
        currentObject.push(current);
        return this;
    }

    /**
     * Close tag on current object
     */
    public SimpleXMLEncoder finishCurrentObject()
    {
        EncoderObject current = currentObject.pop();
        endTag(current.getAlias(), true);
        return this;
    }

    /**
     * Return current object/alias at top of stack
     */
    public EncoderObject getCurrentObject()
    {
        return currentObject.peek();
    }

    /**
     * Indent spaces based on number of objects on stack
     */
    private void indent()
    {
        int num = currentObject.size();
        for (int i = 0; i < num; i++)
        {
            xml.append("  ");
        }
    }

    /**
     * Start an XML tag
     */
    private void startTag(String name, boolean newLineAtEnd)
    {
        indent();
        xml.append('<').append(name).append('>');
        if (newLineAtEnd) xml.append('\n');
    }

    /**
     * End an XML tag
     */
    private void endTag(String name, boolean indent)
    {
        if (indent) indent();
        xml.append("</").append(name).append('>');
        xml.append('\n');
    }

    /**
     * Add a comment
     */
    public void addComment(String comment, boolean bEscapeXML)
    {
        xml.append("<!--\n");
        xml.append(bEscapeXML ? Utils.encodeXML(comment) : comment);
        xml.append(" -->\n");
    }

    public SimpleXMLEncoder add(SimpleXMLEncodable encodable)
    {
        encodable.encodeXML(this);
        return this;
    }

    /**
     * Add tags by introspection of current object, looking for getters matching names
     */
    public SimpleXMLEncoder addTags(String... names)
    {
        EncoderObject encoderObject = getCurrentObject();
        for (String name : names)
        {
            addTag(name, getData(encoderObject.getObject(), name));
        }
        return this;
    }

    /**
     * Add tag with given name and value
     */
    public SimpleXMLEncoder addTag(String name, Object value)
    {
        if (value == null) return this;

        startTag(name, false);

        String svalue = null;
        if (value instanceof Date)
        {
            svalue = format.format((Date) value);   
        }
        else
        {
            svalue = Utils.encodeXML(value.toString());
        }
        xml.append(svalue);

        endTag(name, false);

        return this;
    }

    /**
     * Add all public getters from current object except those matching given names
     */
    public SimpleXMLEncoder addAllTagsExcept(String... names)
    {
        List<String> include = new ArrayList<String>();
        List<String> except = new ArrayList<String>();
        except.add("class");
        except.addAll(Arrays.asList(names));

        Class<?> clazz = getCurrentObject().getObject().getClass();
        Method[] methods = clazz.getMethods();

        for (Method method : methods)
        {
            if (method.getParameterTypes().length > 0) continue;
            if (Modifier.isStatic(method.getModifiers())) continue;

            String methodName = method.getName();
            if (methodName.startsWith("get") ||
                methodName.startsWith("is"))
            {
                String name;
                if (methodName.startsWith("get"))
                {
                    name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                }
                else
                {
                    name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                }

                if (!except.contains(name))
                {
                    include.add(name);
                }
            }
        }

        Collections.sort(include);
        String[] attriubutes = include.toArray(new String[include.size()]);
        return addTags(attriubutes);
    }

    /**
     * Execute getter and return value
     */
    private Object getData(Object object, String name)
    {
        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getMethods();

        Method found = null;
        for (Method method : methods)
        {
            if (method.getParameterTypes().length > 0) continue;

            if (method.getName().equalsIgnoreCase("get"+name) ||
                method.getName().equalsIgnoreCase("is"+name))
            {
                found = method;
                break;
            }
        }

        if (found == null)
        {
            throw new RuntimeException("No getter found for attribute: " + name);
        }

        try
        {
            return found.invoke(object);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return current buffer as string
     */
    @Override
    public String toString()
    {
        return xml.toString();
    }
}
