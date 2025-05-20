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
 * CryptoTest.java
 *
 * Created on October 16, 2002, 10:52 PM
 */

package com.donohoedigital.proto.tests;


import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 *
 * @author  Doug Donohoe
 */
public class CryptoTest {
    
    /** Creates a new instance of CryptoTest */
    public CryptoTest() {
    }

    public static void main(String[] args) throws Exception {

        KeyGenerator kgen = KeyGenerator.getInstance("Blowfish");
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "Blowfish");

        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = 
            cipher.doFinal("This is just an example".getBytes());
        
        Cipher cipher2 = Cipher.getInstance("Blowfish");
        cipher2.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher2.doFinal(encrypted);
        String decrypteds = new String(decrypted);
        System.out.println("Decrypted: " + decrypteds);
        //String decrypted = c
    }
}
