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
 * XMLWriter.java
 *
 * Created on October 29, 2002, 1:46 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;

import java.io.*;

/**
 * Class for writing XML documents
 *
 * @author  Doug Donohoe
 */
public class XMLWriter extends PrintWriter {
    
    static Logger logger = LogManager.getLogger(XMLWriter.class);
    
    private int nIndentSize_ = 4;
    
    /** Creates a new instance of XMLWriter */
    public XMLWriter(OutputStream os) 
    {
        super(os);
    }
    
    public XMLWriter(OutputStream os, boolean bAutoFlush)
    {
        super(os, bAutoFlush);
    }
    
    public XMLWriter(Writer out)
    {
        super(out);
    }
    
    public XMLWriter(Writer out, boolean bAutoFlush)
    {
        super(out, bAutoFlush);
    }
    
    /**
     * Create new writer for f.  If bBackup passed,
     * file is saved to filename + ".bak".  If not, file is overwritten
     */
    public static XMLWriter CreateXMLWriter(File f, boolean bBackup)
                    throws ApplicationError
    {
        if (bBackup)
        {
            File backup = new File(f.getAbsolutePath() + ".bak");
            if (backup.exists())
            {
                backup.delete();
            }
            
            boolean brenamed = f.renameTo(backup);
            if (!brenamed)
            {
                String sMsg ="Unable to rename " + f.getAbsolutePath() + " to " + backup.getAbsolutePath(); 
                logger.fatal(sMsg);
                throw new ApplicationError(ErrorCodes.ERROR_RENAME, sMsg, 
                                "Check for correct write permissions on the files/directories");
            }
        }
        
        FileOutputStream os;
        try {
            os = new java.io.FileOutputStream(f);
        } catch (FileNotFoundException fnfe)
        {
            throw new ApplicationError(fnfe);
        }

        return new XMLWriter(os);
    }
    
    /**
     * Print XML Header
     * <PRE>
     * &lt;?xml version="1.0"?&gt;
     * </PRE>
     */
    public void printXMLHeaderLine()
    {
        // write XML Header
        print("<?xml version=\"1.0\"?>");
        printNewLine();
    }
    
    /**
     * Print the root element like this:
     * <PRE>
     * &lt;[sElementName] xmlns="[sNameSpace]"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation= "[sNameSpace]
                                [sSchemaLocation]"&gt;
     * </PRE>
     */
    public void printRootElementStartLine(String sElementName,
                                 String sNamespace,
                                 String sSchemaLocation,
                                 int nIndent)
    {
        // TODO: allow nonamespace schemas?
        int nSpaces = sElementName.length() + 2;
        StringBuilder sbSpaces = new StringBuilder(nSpaces);
        for (int i = 0; i < nSpaces; i++) {
            sbSpaces.append(' ');
        }

        printIndent(nIndent);
        print("<" + sElementName + " " + "xmlns=\"" + sNamespace + "\"");
        printIndent(nIndent);printNewLine();
        print(sbSpaces + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        // FIX: not needed with classpath resource lookup?
//        printIndent(nIndent);printNewLine();
//        print(sbSpaces + "xsi:schemaLocation= \"" + sNamespace);
//        printIndent(nIndent);printNewLine();
//        print(sbSpaces + "                     " + sSchemaLocation);
        print("\">");
        printIndent(nIndent);printNewLine();
    }
    
    /**
     * print start of element "&lt;name", but don't close it (allow for attributes)
     */
    public void printElementStartOpen(String sElementName)
    {
        print("<" + sElementName);
    }
    
    /**
     * print start tag element "&lt;name&gt;"
     */
    public void printElementStart(String sElementName)
    {
        print("<" + sElementName + ">");
    }
    
    /**
     * print start tag element "&lt;name&gt;" with newline w/ nIndent spaces
     */
    public void printElementStartLine(String sElementName, int nIndent)
    {
        printIndent(nIndent);
        printElementStart(sElementName);
        printNewLine();
    }
    
    /**
     * print closing element "/&gt;"
     */
    public void printElementEnd()
    {
        print("/>");
    }
    
    /**
     * print closing element "/&gt;" with newline
     */
    public void printElementEndLine()
    {
        print("/>");
        printNewLine();
    }
    
    /**
     * print closing element "&gt;" - use
     * printElementEnd(sElementName) to print matching closing tag
     */
    public void printElementClose()
    {
        print(">");
    }
    
    /**
     * print closing element "&gt;" with newline - use
     * printElementEnd(sElementName) to print matching closing tag
     */
    public void printElementCloseLine()
    {
        print(">");
        printNewLine();
    }
    
    /**
     * Print end tag element "&lt;/name&gt;"
     */
    public void printElementEnd(String sElementName)
    {
        print("</" + sElementName + ">");
    }
    
    /**
     * Print end tag element "&lt;/name&gt;" with newline w/ nIndent spaces
     */
    public void printElementEndLine(String sElementName, int nIndent)
    {
        printIndent(nIndent);
        printElementEnd(sElementName);
        printNewLine();
    }
    
    /**
     * Print element and its value (using toString) "&lt;name&gt;value&lt;/name&gt;"
     */
    public void printElement(String sElementName,
                                Object value)
    {
        printElementStart(sElementName);
        print(value.toString()); // TODO: escapes?
        printElementEnd(sElementName);
    }
    
    /**
     * Print element on line w/ nIndent spaces
     * before it and a newline after
     */
    public void printElementLine(String sElementName,
                                Object value,
                                int nIndent)
    {
        printIndent(nIndent);
        printElement(sElementName, value);
        printNewLine();
    }
    
    /**
     * print name="value"
     */
    public void printAttribute(String sAttrName, Object value)
    {
        print(" ");
        print(sAttrName);
        print("=\"");
        print(value.toString()); // TODO: escapes?
        print("\"");
    }
    
    /**
     * print "\n"
     */
    public void printNewLine()
    {
        print("\n");
    }
    
    /**
     * print nIndent * getIndentSize() spaces
     */
    public void printIndent(int nIndent)
    {
        for (int i = 0; i < (nIndent * nIndentSize_); i++)
        {
            print(' ');
        }
    }
    
    /**
     * Change indent size
     */
    public void setIndentSize(int nSize)
    {
        nIndentSize_ = nSize;
    }
    
    /**
     * Get number of spaces in an indent.  Default is 4
     */
    public int getIndentSize()
    {
        return nIndentSize_;
    }
}
