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
package com.donohoedigital.config;

import org.apache.commons.io.output.TeeOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class TeePrintStream {
    private final PrintStream originalOut;
    private final ByteArrayOutputStream baos;

    public TeePrintStream() {
        originalOut = System.out;
        baos = new ByteArrayOutputStream();
        TeeOutputStream tee = new TeeOutputStream(System.out, baos);
        PrintStream ps = new PrintStream(tee);
        System.setOut(ps);
    }

    public String[] getCapturedLines() {
        String capturedOutput;
        try {
            capturedOutput = baos.toString(StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if (capturedOutput == null || capturedOutput.isEmpty()) {
            return new String[0];
        }
        return capturedOutput.split(System.lineSeparator());
    }

    public void restoreOriginal() {
        System.setOut(originalOut);
    }

    public static void main(String[] args) {
        TeePrintStream teePrintStream = new TeePrintStream();

        // Example usage
        System.out.println("Hello, World!");
        System.out.println("This is a test.");

        // Fetch captured lines
        String[] output = teePrintStream.getCapturedLines();
        System.out.println("Captured Line 2: " + output[1]);

        // Restore original System.out
        teePrintStream.restoreOriginal();
    }
}
