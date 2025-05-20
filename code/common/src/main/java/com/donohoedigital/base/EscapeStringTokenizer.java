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
 * EscapeStringTokenizer.java
 *
 * Created on February 8, 2003, 9:47 PM
 */

package com.donohoedigital.base;

import java.util.*;

/**
 * A class similar to StringTokenizer, with a few differences making it more
 * suitable.  The \ character is used as an escape character, allowing
 * delimiters to be escaped and \xxx decimal value to be used.  Quoted
 * strings (delimited by double quotes) are treated as one token.
 * Return (\n) can be escaped using the ESCAPED_RETURN value.
 */

public class EscapeStringTokenizer implements Enumeration 
{

    /**
     * If a function wishes to escape a carriage return '\n',
     * then they should replace each '\n' character with
     * escape-ESCAPED_RETURN.  ESCAPED_RETURN equals capital-N.
     */
    public static final char ESCAPED_RETURN = 'N';
    
    /**
     * '\n' - useful for self-documenting code
     */
    public static final char ACTUAL_RETURN = '\n';
    
    /**
     * '"' - double quote
     */
    public static final char DOUBLE_QUOTE = '"';
    
    /**
     * '\' - the escape character
     */
    public static final char ESCAPE = '\\';
    
    private char [] string_;
    private String delim_;
    private boolean returnTokens_;
    private int current_;
    private String putBack_;

    /** Creates a new instance of MyStringTokenizer.
     * @param s The string to be tokenized
     * @param delim A string containing all delimiters
     * @param returnTokens If true, return delimiters as tokens.  This
     * differs from StringTokenizer in that adjacent delimiters are returned
     * in the same token.
     */
    public EscapeStringTokenizer(String s, String delim, boolean returnTokens) 
    {
        string_ = new char[s.length()];
        s.getChars(0, s.length(), string_, 0);
        delim_ = delim;
        returnTokens_ = returnTokens;
        current_ = 0;
    }

    /** 
     * Creates a new instance of MyStringTokenizer.
     * @param s The string to be tokenized
     * @param delim A string containing all delimiters
     */
    public EscapeStringTokenizer(String s, String delim) 
    {
        this(s, delim, false);
    }

    /** 
     * Creates a new instance of MyStringTokenizer.
     * @param s The string to be tokenized
     * @param delim A char to use as the delim
     */
    public EscapeStringTokenizer(String s, char delim) 
    {
        this(s, "" + delim, false);
    }
    
    /** 
     * Creates a new instance of MyStringTokenizer, with whitespace delimiters
     * (space, tab, newline).
     * @param s The string to be tokenized
     */
    public EscapeStringTokenizer(String s) 
    {
        this(s, " \t\n\r", false);
    }

    private boolean isDelim(int i) 
    {
        return (delim_.indexOf(string_[i]) >= 0);
    }

    /** 
     * Are there any more tokens in the string 
     */
    public boolean hasMoreTokens() 
    {
        if (current_ >= string_.length)
            return false;
        
        if (!isDelim(current_) || returnTokens_)
            return true;
        
        int t = current_;
        while (t < string_.length && isDelim(t))
            t++;
        return (t < string_.length);
    }

    /** 
     * Are there any more tokens in the string 
     */
    public boolean hasMoreElements() 
    {
        return hasMoreTokens();
    }

    /**
     * Are there any more delimiters in the string?  This should only be called
     * if hasMoreTokens is false, to determine if the string contains trailing
     * delimiters.
     */
    public boolean hasMoreDelimiters() 
    {
        return (current_ < string_.length);
    }

    /** 
     * Returns the next token 
     */
    public String nextToken() 
    {
        if (putBack_ != null) {
            String s = putBack_;
            putBack_ = null;
            return s;
        }
        
        int start = current_;
        if (current_ >= string_.length)
            return null;
        
        if (isDelim(current_)) {
            /* This is whitespace */
            while (current_ < string_.length && isDelim(current_))
                current_++;
            if (returnTokens_)
                return new String(string_, start, current_ - start);
            else if (current_ >= string_.length)
                return null;
        }
        
        boolean quoted = false;
        boolean escaped = false;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (current_ == string_.length)
                break;
            if (escaped) {
                if (Character.digit(string_[current_], 10) >= 0) {
                    String s = new String(string_, current_, 3);
                    int i = Integer.parseInt(s);
                    sb.append((char)i);
                    current_ += 2;
                }
                // if we escaped the return character, append \n instead
                else if (string_[current_] == ESCAPED_RETURN)
                {
                    sb.append(ACTUAL_RETURN);
                }
                else 
                {
                    sb.append(string_[current_]);
                }
                escaped = false;
            }
            else if (quoted) {
                if (string_[current_] == DOUBLE_QUOTE) {
                    current_++;
                    break;
                }
                else
                    sb.append(string_[current_]);
            }
            else {
                if (string_[current_] == DOUBLE_QUOTE) 
                    quoted = true;
                else if (string_[current_] == ESCAPE)
                    escaped = true;
                else if (isDelim(current_)) {
                    break;
                }
                else
                    sb.append(string_[current_]);
            }
            current_++;
        }
        return sb.toString();
    }

    /** 
     * Returns the next token 
     */
    public Object nextElement() 
    {
        return nextToken();
    }

    /**
     * Specifies a string to be added to the MyStringTokenizer object.  The next
     * call to nextToken() will return this string.
     */
    public void putBackToken(String s) 
    {
        putBack_ = s;
    }

    /** 
     * Returns a concatenation of all remaining tokens 
     */
    public String remainingTokens() 
    {
        StringBuilder sb = new StringBuilder();
        while (hasMoreTokens())
            sb.append(nextToken());
        return sb.toString();
    }
}

