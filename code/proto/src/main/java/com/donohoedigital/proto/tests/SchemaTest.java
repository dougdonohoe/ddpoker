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
 * SchemaTest.java
 *
 * Created on September 24, 2002, 2:44 PM
 */

package com.donohoedigital.proto.tests;

import com.donohoedigital.base.*;
import org.apache.xerces.impl.dv.ValidatedInfo;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.validation.ValidationState;
import org.apache.xerces.impl.xs.*;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xs.*;


/**
 *
 * @author  donohoe
 */
public class SchemaTest implements XMLErrorHandler
{
    
    String sFile_;
    SchemaGrammar sgrammar_;
    
    /** Creates a new instance of SchemaTest */
    public SchemaTest(String sFile) {
        sFile_ = sFile;
    }
    
    public void process()
    {
        System.out.println("File: " + sFile_);
        XMLSchemaLoader loader = new XMLSchemaLoader();
        loader.setErrorHandler(this);
        XMLInputSource source = new XMLInputSource("Donohoe Digital data-element", sFile_, null);
        
        try {
        
            Grammar grammar = loader.loadGrammar(source);
            // TODO: check for wrong class?
            sgrammar_ = (SchemaGrammar) grammar;
            
//            //printInfo(sgrammar_, XSConstants.TYPE_DEFINITION, "TYPE_DEFINITION");
//            printInfo(sgrammar_, XSConstants.ATTRIBUTE_GROUP, "ATTRIBUTE_GROUP");
//            printInfo(sgrammar_, XSConstants.MODEL_GROUP_DEFINITION, "MODEL_GROUP_DEFINITION");
//            printInfo(sgrammar_, XSConstants.NOTATION_DECLARATION, "NOTATION_DECLARATION");
//            printInfo(sgrammar_, XSConstants.ATTRIBUTE_DECLARATION, "ATTRIBUTE_DECLARATION");
//            printInfo(sgrammar_, XSTypeDefinition.COMPLEX_TYPE, "COMPLEX_TYPE");
//            printInfo(sgrammar_, XSTypeDefinition.SIMPLE_TYPE, "SIMPLE_TYPE");
//            printInfo(sgrammar_, XSConstants.ELEMENT_DECLARATION, "ELEMENT_DECLARATION");

            
//            validate("no-such-element", "blah");
//            validate("boolean", "false");
//            validate("boolean", "true");
//            validate("boolean", "1");
//            validate("boolean", "0");
//            validate("boolean", "2");
//            validate("boolean", "FALSE");
//            validate("boolean", "boolean");
//            validate("integer", "100");
//            validate("integer", "101");
//            validate("float", "100.01");
//            validate("float", "100.11");
            validate("list-states", "CO");
            validate("list-states", "XX");
            validate("list-numbers",  "2");
            validate("list-numbers", "33");
            validate("list-territory-point-types", "Label Location");
            validate("list-territory-point-types", "Foobar");
//            validate("ssn", "281-66-7215");
//            validate("ssn", "281-66-72155");
//            validate("ssn", "2281-66-7215");
//            validate("ssn", "33-343-2333");
//            validate("double", "100.1001");
//            validate("double", "110.10001");
//            validate("birthday", "--10-31");
//            validate("birthday", "10-31");
//            validate("birthday", "--07-18");
//            validate("date", "2001-09-11");
//            validate("date", "2002-10-31");
//            validate("url", "http://www.donohoedigital.com/");
//            validate("url", "0-  3 3 kdnutty/;shitty//blah.com");
//            validateElement("TEST-OBJECT");
            
            
        } catch (Exception e) {
            System.out.println("SchemaTest Exception: " + Utils.formatExceptionText(e));
        }    
    }
    
    private void validate(String sElement, String sData)
    {
            ValidationState vstate = new ValidationState();
            vstate.setExtraChecking(false);
            vstate.setFacetChecking(true);
            ValidatedInfo info = new ValidatedInfo();
            
            XSTypeDefinition edecl = sgrammar_.getGlobalTypeDecl(sElement);
            if (edecl != null)
            {
                XSSimpleType stype = (XSSimpleType) edecl;
                StringList lexi = stype.getLexicalEnumeration();
                for (int i = 0; i < lexi.getLength(); i++)
                {
                    System.out.println("Value " + i + ": " + lexi.item(i));
                }
//                short facet = stype.getDefinedFacets();
                //XSSimpleTypeDecl typedecl = (XSSimpleTypeDecl) stype;
//                System.out.println(sElement + "'s maxInclusive value is " +
//                                    typedecl.getLexicalFacetValue(stype.FACET_MAXINCLUSIVE));
                //System.out.println("Real value is " + typedecl.fMaxExclusive);
                try
                {
                    //System.out.println("STYPE: " + stype);
                    stype.validate(sData, vstate, info);
                    
                    System.out.println("VALID:   " + sData + " is a valid " + sElement);
                } catch (Exception e)
                {
                    System.out.println("INVALID: " + e.getLocalizedMessage());
                    System.out.println( Utils.formatExceptionText(e));
                }
            }
            else
            {           
                System.out.println("INVALID: " + sElement + " does not exist.");
            }
//            XSObject obj = sgrammar_.getAttributeDecl("speed");
//            if (obj != null)
//            {
//                XSAttributeDecl decl = (XSAttributeDecl) obj;
//                XSSimpleType stype = (XSSimpleType) decl.getTypeDefinition();
//                try {
//                    stype.validate("a334.00", vstate, info);
//                } catch (Exception e) {
//                    System.out.println("SchemaTest Exception: " + Utils.formatExceptionText(e));
//                }  
//            }
    }
    
    private void validateElement(String sName)
    {
            ValidationState vstate = new ValidationState();
            vstate.setExtraChecking(false);
            vstate.setFacetChecking(true);
            ValidatedInfo info = new ValidatedInfo();
            XSTypeDefinition elem = sgrammar_.getGlobalTypeDecl(sName);
            if (elem != null)
            {
//                    XSNamedMap map = elem.getIdentityConstraints();
//                    System.out.println("map size is " + map.getMapLength());
//                    for (int i = 0; i < map.getMapLength(); i++)
//                    {
//                        System.out.println(sName + "[" + i + "]=" + map.getItem(i));
//                    }
                    
                    XSComplexTypeDecl complex = (XSComplexTypeDecl) elem;
                    XSParticle particle = complex.getParticle();
                    //System.out.println("Particle: " + particle);
                    XSTerm term = particle.getTerm();
                    //System.out.println("Term: " + term.getClass());
                    XSModelGroupImpl model = (XSModelGroupImpl) term;
                    XSObjectList list = model.getParticles();
                    for (int j = 0; j < list.getLength(); j++)
                    {
                        //System.out.println(sName + " list[" + j + "]=" + list.getItem(j));
                        //System.out.println("==> class = " + list.getItem(j).getClass());
                        XSParticleDecl pd = (XSParticleDecl) list.item(j);
                        //System.out.println("==> Particle " + pd.getName() + " = " + pd.getTerm().getClass());
                        XSElementDecl edecl = (XSElementDecl) pd.getTerm();
                        //System.out.println("==> name " + edecl.getName());
                        XSSimpleType stype = (XSSimpleType) edecl.getTypeDefinition();
                        try
                        {
                            //System.out.println("STYPE: " + stype);
                            stype.validate("281-66-7215", vstate, info);
                            System.out.println("VALID:   " + " 281-66-7216" + " is a valid " + sName + ":" + edecl.getName());
                        } catch (Exception e)
                        {
                            System.out.println("INVALID: " + e.getLocalizedMessage());
                            //System.out.println( Utils.formatExceptionText(e));
                        }
                        
                    }
            }
            else
            {           
                System.out.println("INVALID: " + sName + " does not exist.");
            }
    }
    
    public void printInfo(SchemaGrammar sgrammar_, short type, String sMsg)
    {
            //if (true) return;
            XSNamedMap map = sgrammar_.getComponents(type);
            XSObject xsobj;
            System.out.println(sMsg + " size is " + map.getLength());
            for (int i = 0; i < map.getLength(); i++)
            {
                xsobj = map.item(i);
                System.out.println(sMsg + " [" + i + "] " + xsobj.getName() + " class " + xsobj.getClass());
                System.out.println("==> DETAILS " + xsobj.toString());
                if (xsobj instanceof XSElementDecl)
                {
                    XSElementDecl decl = (XSElementDecl) xsobj;
                    XSTypeDefinition xtype = decl.getTypeDefinition();
                    System.out.println("  ==> " + decl.getName() + " type is " + xtype);
                    System.out.println("  ==> " + decl.getName() + " type is " + xtype.getClass());
                    if (xtype.getTypeCategory() == xtype.COMPLEX_TYPE)
                    {
                        XSComplexTypeDecl complex = (XSComplexTypeDecl) xtype;
                        XSParticle particle = complex.getParticle();
                        System.out.println("  ==> Particle: " + particle);
                        XSTerm term = particle.getTerm();
                        System.out.println("  ==> Term: " + term.getClass());
                        XSModelGroupImpl model = (XSModelGroupImpl) term;
                        XSObjectList list = model.getParticles();
                        for (int j = 0; j < list.getLength(); j++)
                        {
                            System.out.println("   ==> list[" + j + "]=" + list.item(j));
                            System.out.println("   ==>        class = " + list.item(j).getClass());
                            XSParticleDecl pd = (XSParticleDecl) list.item(j);
                            System.out.println("   ==>        Particle " + pd.getName() + " = " + pd.getTerm().getClass());
                            XSElementDecl edecl = (XSElementDecl) pd.getTerm();
                            System.out.println("   ==>        particle element name " + edecl.getName());
                            //XSSimpleType stype = (XSSimpleType) edecl.getTypeDefinition();
                            //System.out.println("==> STYPE: " + stype);
                        }
                    }
                }
            }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java SchemaTest <XML file>");
            return;
        }
        try {
            for (int i = 0; i < args.length; i++)
            {
                SchemaTest test = new SchemaTest(args[i]);
                test.process();
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }
    
    public void warning(String domain, String key, 
                        XMLParseException exception) throws XNIException
    {
        printMessage("WARNING", domain, key, exception);
    }
   
    public void error(String domain, String key, 
                      XMLParseException exception) throws XNIException
    {
        printMessage("ERROR", domain, key, exception);
    }

    
    public void fatalError(String domain, String key, 
                           XMLParseException exception) throws XNIException
    {
        printMessage("FATAL ERROR", domain, key, exception);
    }
    
    private void printMessage(String msg, String domain, String key, XMLParseException e)
    {
        System.out.println("**** " + msg + ": " + domain + " [" + key + "] " +
                e.getMessage() + " at line " + e.getLineNumber() + " Column " + e.getColumnNumber());
        //System.out.println(Utils.formatExceptionText(e));
    }
    
}
