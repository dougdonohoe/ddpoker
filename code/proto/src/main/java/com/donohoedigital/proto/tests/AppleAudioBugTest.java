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
package com.donohoedigital.proto.tests;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Apr 7, 2008
 * Time: 9:49:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class AppleAudioBugTest
{
    public static void main(String[] args)
    {
        new AppleAudioBugTest().testAppleBug();
    }

    public void testAppleBug()
    {
        AudioInputStream stream;
        URL url = null;

        try
        {
            //url = new URL("file:/Users/donohoe/work/ddpoker/code/common/target/test-classes/config/testapp/audio/bell.wav");
            url = new URL("file:/Users/donohoe/work/ddpoker/code/poker/src/main/resources/config/poker/audio/bell.wav");
            stream = AudioSystem.getAudioInputStream(url);
        }
        catch (UnsupportedAudioFileException | IOException uafe)
        {
            throw new RuntimeException(uafe);
        }

        DataLine dataline = null;
        try
        {
            DataLine.Info info;
            AudioFormat format = stream.getFormat();
            long nSize = stream.getFrameLength() * format.getFrameSize();
            info = new DataLine.Info(
                    Clip.class,
                    stream.getFormat(),
                    (int) nSize);

            dataline = (DataLine) AudioSystem.getLine(info);
            Clip clip = (Clip) dataline;
            clip.open(stream);
            clip.start();
            Thread.sleep(2000); // allow time to play
            clip.stop();
            clip.close();
            stream.close();
            System.exit(1); // hangs otherwise

        }
        catch (LineUnavailableException lue)
        {
            dataline = null;
            lue.printStackTrace();
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}