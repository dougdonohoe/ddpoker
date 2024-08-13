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
 * AudioPlayer.java
 *
 * Created on June 04, 2003, 2:26 PM
 */

package com.donohoedigital.config;

import com.donohoedigital.base.*;
import org.apache.log4j.*;

import javax.sound.sampled.*;
import java.io.*;

/**
 *
 * @author  donohoe
 */
public class AudioPlayer implements Runnable, LineListener
{
    static Logger logger = Logger.getLogger(AudioPlayer.class);
    
    private AudioDef def_;
    private AudioDef loopdef_;
    private double dSleepSecs_ = 0.0f;
    private float fGain_ = 0.99f;
    private float fPan_ = 0.0f;
    private Thread thread_;
    private DataLine dataline_;
    private MyStream stream_;
    private MyStream loopstream_;
    private boolean bStopPlayback_ = false;
    private boolean bLoop_ = false;
    
    /**
     * New audio definition from name and its file
     */
    public AudioPlayer(AudioDef def, float fGain, double sleepSecs, boolean bLoop)
    {
        def_ = def;
        dSleepSecs_ = sleepSecs;
        fGain_ = fGain;
        bLoop_ = bLoop;
    }
    
    
    /**
     * New audio definition from name and its file
     */
    public AudioPlayer(AudioDef startdef, AudioDef loopdef, float fGain, double sleepSecs)
    {
        def_ = startdef;
        loopdef_ = loopdef;
        dSleepSecs_ = sleepSecs;
        fGain_ = fGain;
        bLoop_ = true;
    }
    
    /**
     * debug
     */
    public String toString()
    {
        return "[start: " + getStartName() + " loop: " + getLoopName() + " isPaused: " + isPaused() + " isPlaying: " + isPlaying() + "]";
    }
    
    /**
     * Is currently playing?
     */
    public synchronized boolean isPlaying()
    {
        return dataline_ != null;
    }
    
    /**
     * Return name of start music (also name of music in non-looping mode)
     */
    public String getStartName()
    {
        return def_.getName();
    }
    
    /**
     * return name of looping music
     */
    public String getLoopName()
    {
        if (loopdef_ != null) return loopdef_.getName();
        return null;
    }


    /**
     * play this audio
     */
    public synchronized void play()
    {
        if (thread_ != null) return;
        thread_ = new Thread(this);
        thread_.setName("Audio-" + def_.getName());
        thread_.start();
    }

    /**
     * Stop current audio if playing (player an be restarted with play())
     */
    public synchronized void stop() 
    {
       
        if (dataline_ != null)
        {
            try {
                mute();
                bStopPlayback_ = true;
                dataline_.flush();
            } catch (Throwable e) 
            {
                logger.warn("Exception caught flushing " + def_.getName() +": " + Utils.formatExceptionText(e));
            }
        }
    }
    
    /**
     * Gradually reduce gain to 0
     */
    private void mute()
    {
        // ramp gain down when stopping so we
        // don't get a clicking sound
        for (float f = fGain_; f >= 0.0f; f -= .05f)
        {
            setGain(f, false);
            //logger.debug("STOP, set gain on " + def_.sName_ +": " + f);
            Utils.sleepMillis(20);
        }
    }

    /**
     * Runnable for playing sound
     */
    public void run()
    {
        bStopPlayback_ = false;
        Utils.sleepSeconds(dSleepSecs_);
        //logger.debug("Start playing sound: " + def_.sName_);
        stream_ = new MyStream(def_);
        loopstream_ = loopdef_ == null ? null :  new MyStream(loopdef_);
        dataline_ = stream_.getDataLine();
        playSound();
    }
    
    /**
     * cleanup when done
     */
    private synchronized void cleanup()
    {
        try {
            if (dataline_ != null) dataline_.close();
        } catch (Exception e) {}
        thread_ = null;
        dataline_ = null;
        stream_ = null;
        loopstream_ = null;
        
        //logger.debug("Cleaned up " + def_.sName_);
    }
    
    
    //////
    ////// Playback logic
    //////

    private class MyStream
    {
        AudioDef def;
        AudioInputStream stream;

        public MyStream(AudioDef def)
        {
            this.def = def;
            initStream();
        }

        public void initStream()
        {
            if (def == null)
            {
                stream = null;
                return;
            }

            try {
                stream = AudioSystem.getAudioInputStream(def.getURL());
                AudioFormat format = stream.getFormat();
                /**
                 * we can't yet open the device for ALAW/ULAW playback,
                 * convert ALAW/ULAW to PCM
                 */
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW) ||
                    (format.getEncoding() == AudioFormat.Encoding.ALAW))
                {
                    AudioFormat tmp = new AudioFormat(
                                              AudioFormat.Encoding.PCM_SIGNED,
                                              format.getSampleRate(),
                                              format.getSampleSizeInBits() * 2,
                                              format.getChannels(),
                                              format.getFrameSize() * 2,
                                              format.getFrameRate(),
                                              true);
                    stream = AudioSystem.getAudioInputStream(tmp, stream);
                }
            }
            catch (UnsupportedAudioFileException uafe)
            {
                throw new ApplicationError(uafe);
            }
            catch(IOException ioe)
            {
                throw new ApplicationError(ioe);
            }
        }

        public void reset() throws IOException
        {
            if (stream.markSupported())
            {
                stream.reset();
            }
            else
            {
                initStream();
            }
        }

        /**
         * Load the sound and prep for playing
         */
        private DataLine getDataLine()
        {
            DataLine dataline = null;
            try {
                DataLine.Info info;
                AudioFormat format = stream.getFormat();
                long nSize = stream.getFrameLength() * format.getFrameSize();


                if (nSize > 250000) {
                    //logger.debug("SourceDataLine created");
                    info = new DataLine.Info(
                                          SourceDataLine.class,
                                          stream.getFormat()
                                          );
                }
                else
                {
                    //logger.debug("Clip created, size=" + nSize);
                    info = new DataLine.Info(
                                          Clip.class,
                                          stream.getFormat(),
                                          (int)nSize);
                }

                dataline = (DataLine) AudioSystem.getLine(info);
                dataline.addLineListener(AudioPlayer.this);
                //logger.debug("Buffer size: " + dataline.getBufferSize());

            }
            catch (LineUnavailableException lue)
            {
                dataline = null;
                logger.error(def.getName() +": Caught exception loading sound: " + Utils.formatExceptionText(lue));
            }

            return dataline;
        }
    }

    /**
     * Play sound loaded previously
     */
    private void playSound() 
    {
        if (dataline_ == null) return;
        
        if (dataline_ instanceof Clip)
        {
            Clip clip = (Clip) dataline_;
            
            try {
                clip.open(stream_.stream);
                setGain(fGain_);
                setPan(fPan_);
                clip.start();
            }
            catch (LineUnavailableException lue)
            {
                logger.error(stream_.def.getName() +": Caught exception playing sound: " + Utils.formatExceptionText(lue));
                return;
            }
            catch (IOException ioe)
            {
                logger.error(stream_.def.getName() +": Caught exception playing sound: " + Utils.formatExceptionText(ioe));
                return;
            }
        }
        else
        {
            SourceDataLine line = (SourceDataLine) dataline_;
            byte tempBuffer[] = new byte[25000 * 4];
            int cnt = 0;

            try 
            {
                // prepare line
                line.open(stream_.stream.getFormat());
                setGain(fGain_);
                setPan(fPan_);
                line.start();
                
                // mark streams for looping
                stream_.stream.mark(Integer.MAX_VALUE);
                if (loopstream_ != null) loopstream_.stream.mark(Integer.MAX_VALUE);

                // play stream
                MyStream localstream = stream_;
                boolean bLooping = true; // always play once
                while (bLooping && !bStopPlayback_)
                {
                    bLooping = bLoop_;
                    // Keep looping until the input read method
                    // returns -1 for empty stream
                    while(!bStopPlayback_ &&
                                (bPaused_ ||
                                  (cnt = localstream.stream.read(tempBuffer, 0,
                                   Math.min(line.available(), tempBuffer.length))) != -1
                                 ))
                    {
                        if (cnt > 0)
                        {
                            // Write data to the internal buffer of
                            // the data line where it will be
                            // delivered to the speaker.
                            line.write(tempBuffer, 0, cnt);
                        }

                        // sleep if none available to avoid sucking all CPU time
                        if (!bStopPlayback_ && (bPaused_ || line.available() == 0)) Utils.sleepMillis(99);

                        //logger.debug("Available: " + line.available());
                        cnt = 0;
                    }

                    // if we have a loop stream start playing that now
                    if (loopstream_ != null) localstream = loopstream_;

                    // reset for looping
                    if (bLooping)
                    {
                        localstream.reset();
                    }
                }
            }
            catch (LineUnavailableException lue)
            {
                logger.error(stream_.def.getName() +": Caught exception playing sound: " + Utils.formatExceptionText(lue));
                // fall through to cleanup
            }
            catch (IOException ioe)
            {
                logger.error(stream_.def.getName() +": Caught exception playing sound: " + Utils.formatExceptionText(ioe));
                // fall through to cleanup
            }

            // Block and wait for internal buffer of the
            // data line to empty.
            try {
                dataline_.drain();
                dataline_.stop();
            } catch (Throwable e) 
            {
                logger.warn(stream_.def.getName() +": Exception caught draining/stopping: " + Utils.formatExceptionText(e));
            }
            
            cleanup();
        }
        
        // this sleep stuff was causing sound to stop early on windows,
        // so why bother!  it finishes on its own
//        Utils.sleepMillis(99);
//
//        while ((paused_ || clip_.isActive()) && thread_ != null) {
//            Utils.sleepMillis(99);
//        }
//        
//        clip_.stop();
//        clip_.close();
//        clip_ = null;
    }

    private boolean bPaused_ = false;
    
    /**
     * pause playback
     */
    public synchronized void pause()
    {
        bPaused_ = true;
        mute();
    }
    
    /**
     * Is loop paused?
     */
    public synchronized boolean isPaused()
    {
        return bPaused_;
    }
    
    /**
     * resume playback
     */
    public synchronized void resume()
    {
        setGain(fGain_);
        bPaused_ = false;
    }
    
    /**
     * Set pan (value -1 to 1)
     */
    synchronized void setPan(float value)
    {
        fPan_ = value;
        if (dataline_ == null) return;
        if (!dataline_.isControlSupported(FloatControl.Type.PAN)) return;

        try {

            FloatControl panControl =
                (FloatControl) dataline_.getControl(FloatControl.Type.PAN);
            panControl.setValue(value);
        } catch (Exception ex) {
            throw new ApplicationError(ex);
        }
    }

    /**
     * Set gain (value 0 to 1)
     */
    synchronized void setGain(float value) 
    {
        setGain(value, true);
    }
    
    /**
     * Set gain, remember permanent if true
     */
    private synchronized void setGain(float value, boolean bRemember)
    {
        if (bRemember) fGain_ = value;
        if (dataline_ == null) return;
        
        try {
            FloatControl gainControl = (FloatControl) dataline_.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(value==0.0?0.0001:value)/Math.log(10.0)*20.0);
            if (dB > gainControl.getMaximum()) dB = gainControl.getMaximum();
            if (dB < gainControl.getMinimum()) dB = gainControl.getMinimum();
            gainControl.setValue(dB);
        } catch (Exception ex) {
            throw new ApplicationError(ex);
        }
    }
    
    /**
     * Return length of current clip, (0 if no clip loaded)
     */
    public synchronized double getDuration() 
    {       
        if (dataline_ == null) return 0.0;
        
        double duration = dataline_.getBufferSize() / 
                (dataline_.getFormat().getFrameSize() * dataline_.getFormat().getFrameRate()); 
        
        return duration;
    }

    /**
     * Return seconds played of current clip (0 if no clip loaded)
     */
    public synchronized double getSeconds() 
    {
        if (dataline_ == null) return 0.0;
        
        return dataline_.getFramePosition() / dataline_.getFormat().getFrameRate();
    }
    
    public void update(javax.sound.sampled.LineEvent lineEvent) 
    {
        //logger.debug(def_.sName_ + " Update " + lineEvent);
        if (lineEvent.getType() == LineEvent.Type.STOP)
        {            
            if (dataline_ instanceof Clip) 
            {
                Utils.sleepMillis(200); // BUG 163 - clipping last 1/6th second or so, therefore wait
                cleanup();
            }

        }
    }
    
}
    
