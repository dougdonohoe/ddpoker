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
package com.donohoedigital.base;

import java.util.*;
import java.security.*;

/**
 * Generate a random password.
 */
public class PasswordGenerator
{
    public static final int OPTION_INCLUDE_LETTERS = 0x001;
    public static final int OPTION_INCLUDE_MIXED_CASE = 0x002;
    public static final int OPTION_INCLUDE_NUMBERS = 0x004;
    public static final int OPTION_INCLUDE_PUNCTUATION = 0x008;
    public static final int OPTION_EXCLUDE_SIMILAR = 0x010;
    
    public static final int OPTION_ALL = (OPTION_INCLUDE_LETTERS |
                                    OPTION_INCLUDE_MIXED_CASE |
                                    OPTION_INCLUDE_NUMBERS |
                                    OPTION_INCLUDE_PUNCTUATION |
                                    OPTION_EXCLUDE_SIMILAR);

    private static final String excludedChars_ = "ILOUVilouv01";

    private String charPool_ = null;
    private boolean removeExcluded_ = false;

    /**
     * Create a generator with the given options.
     *
     * @param options options
     * @param chars additional characters
     */
    public PasswordGenerator(int options, char[] chars) {
        StringBuilder buffer = new StringBuilder();

        // Remove confusing charachters.
        removeExcluded_ = ((options & OPTION_EXCLUDE_SIMILAR) > 0);

        // Add lowercase letters to the pool.
        if ((options & OPTION_INCLUDE_LETTERS) > 0)
        {
            addPoolChars(buffer, 97, 122);
        }

        // Add uppercase letters to the pool.
        if ((options & OPTION_INCLUDE_MIXED_CASE) > 0)
        {
            addPoolChars(buffer, 65, 90);
        }

        // Add numbers to the pool.
        if ((options & OPTION_INCLUDE_NUMBERS) > 0)
        {
            addPoolChars(buffer, 48, 57);
        }

        // Add punctuation to the pool.
        if ((options & OPTION_INCLUDE_PUNCTUATION) > 0)
        {
            addPoolChars(buffer, 33, 47);
            addPoolChars(buffer, 58, 64);
        }

        // Add the given characters to the pool.
        if (chars != null)
        {
            buffer.append(chars);
        }

        charPool_ = buffer.toString();
    }

    /**
     * Generate a password of the given length using the current time as random number generator seed.
     */
    public String generatePassword(int length) {
        return generatePassword(length, System.currentTimeMillis());
    }

    /**
     * Generate a password of the given length using the given random number generator seed.
     */
    public String generatePassword(int length, long seed) {
        // Create the password by randomly retrieving the given number of characters.
        Random random = new SecureRandom();
        random.setSeed(seed);

        int randomEnd = charPool_.length();
        StringBuilder buffer = new StringBuilder(length);

        for (int i = 0; i < length; ++i)
        {
            buffer.append(charPool_.charAt(random.nextInt(randomEnd)));
        }

        return buffer.toString();
    }

    /**
     * Add characters in the given ASCII range to the given buffer.
     *
     * @param buffer buffer
     * @param begin first character in the range
     * @param end last character in the range (inclusive)
     */
    private void addPoolChars(StringBuilder buffer, int begin, int end) {
        int charCount = (end - begin) + 1;
        buffer.ensureCapacity(buffer.capacity() + charCount);
        char ch;

        for (int i = begin; i <= end; ++i)
        {
            ch = (char) i;

            if (removeExcluded_)
            {
                if (excludedChars_.indexOf(ch) < 0)
                {
                    buffer.append(ch);
                }
            }
            else
            {
                buffer.append(ch);
            }
        }
    }
}
