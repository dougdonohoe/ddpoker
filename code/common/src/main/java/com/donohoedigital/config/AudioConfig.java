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
 * AudioConfig.java
 *
 * Created on June 04, 2003, 2:26 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.logging.log4j.*;
import org.jdom2.*;

import javax.sound.midi.*;
import java.net.*;
import java.util.*;

/**
 * Loads audio.xml files in the module directories defined by
 * the appconfig.xml file.
 *
 * @author donohoe
 */
public class AudioConfig extends XMLConfigFileLoader
{
    private static Logger aLogger = LogManager.getLogger(AudioConfig.class);

    private static final String AUDIO_CONFIG = "audio.xml";

    private static AudioConfig audioConfig = null;

    private Map<String, AudioDef> audios_ = new HashMap<String, AudioDef>();

    private static boolean bMuteFX_ = false;
    private static float fFXGain_ = .8f;

    private static boolean bMuteMusic_ = false;
    private static float fMusicGain_ = .8f;

    private static boolean bMuteBGMusic_ = false;
    private static float fBGMusicGain_ = .8f;

    private static AudioPlayer lastMusic_ = null;

    /**
     * Creates a new instance of AudioConfig from the Appconfig file
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
    public AudioConfig(String[] modules) throws ApplicationError
    {
        ApplicationError.warnNotNull(audioConfig, "AudioConfig is already initialized");
        audioConfig = this;
        init(modules);
    }

    /**
     * set mute for fx
     */
    public static void setMuteFX(boolean b)
    {
        bMuteFX_ = b;
    }

    /**
     * set mute for music
     */
    public static void setMuteMusic(boolean b)
    {
        bMuteMusic_ = b;
        if (b)
        {
            if (lastMusic_ != null && lastMusic_.isPlaying())
            {
                lastMusic_.stop();
            }
        }
        else
        {
            if (lastMusic_ != null)
            {
                lastMusic_.play();
            }
        }
    }

    /**
     * set mute for music
     */
    public static void setMuteBGMusic(boolean b)
    {
        //logger.debug("Set mute BG music: " + b);
        bMuteBGMusic_ = b;
        if (b)
        {
            stopBackgroundMusic(false);
        }
        else
        {
            restartBackgroundMusic(false);
        }
    }

    /**
     * Set fx gain (1 - 100)
     */
    public static void setFXGain(int f)
    {
        fFXGain_ = f / 100.0f;
    }

    /**
     * Set music gain (1-100)
     */
    public static void setMusicGain(int f)
    {
        fMusicGain_ = f / 100.0f;
        if (lastMusic_ != null) lastMusic_.setGain(fMusicGain_);
    }

    /**
     * Set background music gain (1-100)
     */
    public static void setBGMusicGain(int f)
    {
        fBGMusicGain_ = f / 100.0f;
        if (bgMusic_ != null) bgMusic_.setGain(fBGMusicGain_);
    }

    /**
     * Play a sound FX
     */
    public static AudioPlayer playFX(String sName)
    {
        return playFX(sName, 0, false);
    }

    /**
     * Play a sound FX, with delay before playing
     */
    public static AudioPlayer playFX(String sName, double sleepSecs)
    {
        return playFX(sName, sleepSecs, false);
    }

    /**
     * Play a sound FX, with delay before playing
     */
    public static AudioPlayer playFX(String sName, double sleepSecs, boolean bLoop)
    {
        if (bMuteFX_) return null;
        AudioPlayer play = play(sName, fFXGain_, sleepSecs, bLoop, true, true);
        Utils.sleepMillis(75); // sleep calling thread so sound can play
        return play;
    }

    /**
     * Play music
     */
    public static AudioPlayer playMusic(String sName)
    {
        return playMusic(sName, 0, false, false);
    }

    /**
     * Play music, with delay before playing
     */
    public static AudioPlayer playMusic(String sName, double sleepSecs)
    {
        return playMusic(sName, sleepSecs, false, false);
    }

    /**
     * Play music, with delay before playing
     */
    public static AudioPlayer playMusic(String sName, double sleepSecs, boolean bLoop, boolean bRemember)
    {
        AudioPlayer player = play(sName, fMusicGain_, sleepSecs, bLoop, true, false);
        if (!bMuteMusic_) player.play();
        if (bRemember) lastMusic_ = player;
        return player;
    }

    /**
     * Play sound request audio.  If bReportMissing is true, log message
     * if audio not found.  Returns AudioDef which is playing the sound
     */
    private static AudioPlayer play(String sName, float fGain, double sleepSecs,
                                    boolean bLoop, boolean bReportMissing, boolean bPlay)
    {
        AudioDef audio = getAudioDef(sName, bReportMissing);
        if (audio == null)
        {
            aLogger.warn("Unable to play " + sName + " (not defined)");
            return null;
        }
        AudioPlayer player = new AudioPlayer(audio, fGain, sleepSecs, bLoop);
        if (bPlay) player.play();
        return player;
    }

    /**
     * Play first sound, then loop on the second
     */
    public static AudioPlayer playMusicLoop(String sStart, String sLoop)
    {
        AudioPlayer player = getMusicLoop(sStart, sLoop, fMusicGain_);
        if (!bMuteMusic_) player.play();
        return player;
    }

    /**
     * Return player for looping, but not started
     */
    private static AudioPlayer getMusicLoop(String sStart, String sLoop, float fGain)
    {
        AudioDef start = getAudioDef(sStart, false);
        ApplicationError.assertNotNull(start, "No audio found for " + sStart);
        AudioDef loop = getAudioDef(sLoop, false);
        ApplicationError.assertNotNull(loop, "No audio found for " + sLoop);

        return new AudioPlayer(start, loop, fGain, 0);
    }

    /**
     * Stop last music returned if still playing and forgets it
     */
    public static void stopLastMusic()
    {
        if (lastMusic_ != null)
        {
            if (lastMusic_.isPlaying()) lastMusic_.stop();
            lastMusic_ = null;
        }
    }

    //////
    ////// Background music
    //////

    private static AudioPlayer bgMusic_ = null;
    private static String sLoop_ = null;

    /**
     * Start background music - play sStart followed by endless sLoop
     */
    public static void startBackgroundMusic(String sStart, String sLoop, boolean bPlayStart)
    {
        //logger.debug("Start: " + sStart + " loop: " + sLoop + " bPlayStart: " + bPlayStart);
        if (bgMusic_ != null)
        {
            //logger.debug("non null bgMusic_: " + bgMusic_);
            if (!bPlayStart &&
                ((sStart.equals(bgMusic_.getStartName()) &&
                  sLoop.equals(bgMusic_.getLoopName())) ||
                 // if didn't play start, then the music name
                 // equals the loop
                 (sLoop.equals(bgMusic_.getStartName()) &&
                  bgMusic_.getLoopName() == null)))
            {
                //logger.debug("Already defined....");
                resumeBackgroundMusic();
                return; // bg music defined, just resume
            }
            else
            {
                //logger.debug("Stop....");
                bgMusic_.stop();
                bgMusic_ = null;
            }
        }

        sLoop_ = sLoop;
        bgMusic_ = getMusicLoop(sStart, sLoop, fBGMusicGain_);

        if (!bMuteBGMusic_)
        {
            if (bPlayStart)
            {
                //logger.debug("Play...."+bgMusic_);
                bgMusic_.play();
            }
            else
            {
                restartBackgroundMusic(true);
            }
        }
    }

    /**
     * Restart background music using sLoop_;
     */
    public static void restartBackgroundMusic(boolean bRestartIfNull)
    {
        //logger.debug("restartBackground music, bgMusic_ = " + bgMusic_ + " bMuteBGMusic_=" + bMuteBGMusic_
        //                            + "  sLoop_=" + sLoop_);
        if (bgMusic_ == null && !bRestartIfNull) return;
        if (bMuteBGMusic_) return;
        if (bgMusic_ != null && (bgMusic_.isPlaying() || bgMusic_.isPaused())) return;
        if (sLoop_ == null) return;
        bgMusic_ = play(sLoop_, fBGMusicGain_, 0, true, true, true);
    }

    /**
     * Stop background music from playing
     */
    public static void stopBackgroundMusic()
    {
        stopBackgroundMusic(true);
    }

    /**
     * stop background music, set to null if true
     */
    private static void stopBackgroundMusic(boolean bNull)
    {
        //logger.debug("Stopping background music, bgMusic_ = " + bgMusic_ + " null? " + bNull);
        if (bgMusic_ != null)
        {
            bgMusic_.stop();
            if (bNull) bgMusic_ = null;
        }
    }

    /**
     * Pause background music
     */
    public static void pauseBackgroundMusic()
    {
        if (bgMusic_ != null)
        {
            bgMusic_.pause();
        }
    }

    /**
     * Resume background music
     */
    public static void resumeBackgroundMusic()
    {
        if (bgMusic_ != null)
        {
            // if now off, stop it
            if (bMuteBGMusic_)
            {
                bgMusic_.stop();
            }
            else
            {
                bgMusic_.resume();
                if (!bgMusic_.isPlaying()) bgMusic_.play();
            }
        }
    }

    ///////
    /////// INIT - load config
    ///////

    /**
     * Get audio def of given name
     */
    public static AudioDef getAudioDef(String sName, boolean bReportMissing)
    {
        AudioDef audio = audioConfig.audios_.get(sName);
        if (audio == null)
        {
            if (bReportMissing)
            {
                aLogger.warn("No audio found for " + sName);
            }
            return null;
        }
        return audio;
    }

    /**
     * Load audios from modules
     */
    private void init(String[] modules) throws ApplicationError
    {
        ApplicationError.assertNotNull(modules, "Modules list is null");

        // on mac, this prevents multiple threads from being left around on Mac platform
        // very weird bug.  FIX: is this a 1.5 issue?
        if (Utils.ISMAC)
        {
            try
            {
                MidiSystem.getSequencer();
            }
            catch (MidiUnavailableException e)
            {
                // ignore
            }
        }

        Document doc;
        for (String module : modules)
        {
            // if audio file is missing, no big deal
            URL url = new MatchingResources("classpath*:config/" + module + "/" + AUDIO_CONFIG).getSingleResourceURL();
            if (url != null)
            {
                doc = this.loadXMLUrl(url, "audio.xsd");
                init(doc, module);
            }
        }
    }

    /**
     * Initialize from JDOM doc
     */
    private void init(Document doc, String module) throws ApplicationError
    {
        Element root = doc.getRootElement();

        // audiodir name
        String audiodir = getChildStringValueTrimmed(root, "audiodir", ns_, true, AUDIO_CONFIG);

        // get list of audios
        List<Element> audios = getChildren(root, "audio", ns_, false, AUDIO_CONFIG);
        if (audios == null) return;

        // create audiodef for each one
        for (Element audio : audios)
        {
            initAudio(audio, module, audiodir);
        }
    }

    /**
     * Read audio info
     */
    private void initAudio(Element audio, String module, String audiodir) throws ApplicationError
    {
        String sName = getStringAttributeValue(audio, "name", true, AUDIO_CONFIG);
        String sLocation = getStringAttributeValue(audio, "location", true, AUDIO_CONFIG);

        String location = module + "/" + audiodir + "/" + sLocation;
        URL url = new MatchingResources("classpath*:config/" + location).getSingleResourceURL();
        if (url == null)
        {
            aLogger.warn("Audio " + sName + " not found at " + location + ".  Skipping");
            return;
        }

        audios_.put(sName, new AudioDef(sName, url));
    }
}
