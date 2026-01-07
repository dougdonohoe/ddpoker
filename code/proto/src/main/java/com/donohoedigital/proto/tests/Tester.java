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
package com.donohoedigital.proto.tests;



/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jun 23, 2006
 * Time: 10:15:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class Tester
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

//        536870912 SEEDADJ: 592130587
//        11:24.158 [TournamentDirector-0] DEBUG SEED: 1073741824 SEEDADJ: 597617150
//        11:26.922 [TournamentDirector-0] DEBUG SEED: 1073741824 SEEDADJ: 605887395
//        11:29.840 [TournamentDirector-0] DEBUG SEED: 1073741824 SEEDADJ: 614199865
//        11:32.614 [TournamentDirector-0] DEBUG SEED: 1073741824 SEEDADJ: 622555383
//
        int a = 1073741824;
        int b = 597617150;
        int m = a * b;
        long l = (long) a *  (long) b;
        int x = (int) (l % Integer.MAX_VALUE);
        System.out.println("MIN: "+ Integer.MIN_VALUE);
        System.out.println("a: " + a +  " b: "+ b+ "  mult: " + m + " lmult: "+l+ " x="+x);

        for (int port = 0; port <= 0xFFFF; port++)
        {
            short sport = (short) port;
            int iport = 0xFFFF & sport;

            System.out.println("port: "+ port + " sport: "+sport + " iport:"+iport);
        }

        new Tester();
        new Tester("blah");
    }

    public Tester()
    {
        System.out.println("Tester() called");
    }

    public Tester(String s)
    {
        this();
        System.out.println("Tester(s) called: " + s);
    }
}
