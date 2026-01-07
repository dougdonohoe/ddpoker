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
/*
 * Perf.java
 *
 * Created on June 30, 2003, 4:24 PM
 */

package com.donohoedigital.config;

//import com.jprofiler.agent.*;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author  Doug Donohoe
 */
public class Perf 
{
    private static boolean JPROFILER = true;

    private static boolean ON = false;
    private static boolean MEM = false;
    private static boolean bRunning = true;
    private static Map count_ = new TreeMap();

    /**
     * turn on performance features (called from EngineInit)
     */
    public static void setOn(boolean b)
    {
        ON = b;
        System.out.println("Perf monitoring now " + (ON ? "enabled" : "disabled"));
        if (b)
        {
            //Audit.enableMemoryProfiler();
            // Jprofile bug? - cpu data must be on at start for it to work at all, so stop at beginning
//            Controller.stopCPURecording();
//            Controller.stopAllocRecording();
        }
    }

    /**
     * is performance tuning code on?
     */
    public static boolean isOn()
    {
        return ON;
    }
    
    /**
     * are stats being gathered?
     */
    public static boolean isStarted()
    {
        return bRunning;
    }

    /**
     * start gathering stats
     */
    public static void start()
    {
        if (!ON || bRunning) return;
        bRunning = true;
        System.out.println("Perf monitoring started");
        try {
//            if (true) // mem
//            {
//                if (JPROFILER) Controller.startAllocRecording(true);
//            }
//            else
//            {
//                if (JPROFILER) Controller.startCPURecording(true);
//                if (JPROFILER) Controller.startThreadProfiling();
//                if (JPROFILER) Controller.startVMTelemetryRecording();
//            }
            //Audit.enableCPUProfiler();
            //Audit.enableMemoryProfiler();

        } catch (Throwable t) {}
    }

    /**
     * stop gathering stats
     */
    public static void stop()
    {
        if (!ON || !bRunning) return;
        bRunning = false;
        try {
//            if (JPROFILER) Controller.stopCPURecording();
//            if (JPROFILER) Controller.stopAllocRecording();
//            if (JPROFILER) Controller.stopThreadProfiling();
//            if (JPROFILER) Controller.stopVMTelemetryRecording();
            //Audit.disableCPUProfiler();
            //Audit.disableMemoryProfiler();

        } catch (Throwable t) {}
        System.out.println("Perf monitoring stopped");
    }

    /**
     * Note construction of object
     */
    public static void construct(Object o, String sDesc)
    {
        if (!MEM || !ON) return;
        synchronized(count_)
        {
            String sName = o.getClass().getName();
            if (sDesc != null) sName += "-"+sDesc;
            Counter count = (Counter) count_.get(sName);
            if (count == null) {
                count = new Counter();
                count_.put(sName, count);
            }
            count.num++;
            System.out.println("MEMORY: construct " + sName + " count now " + count.num);
        }
    }

    /**
     * Display current count of objects
     */
    public static void displayCurrentCount()
    {
        if (!MEM || !ON) return;
        synchronized(count_)
        {
            System.out.println("MEMORY: *********************************************************************************");
            Iterator iter = count_.keySet().iterator();
            String sName;
            Counter count;
            while (iter.hasNext())
            {
                sName = (String) iter.next();
                count = (Counter) count_.get(sName);
                System.out.println("MEMORY: ******** current count for " + sName + ": " + count.num);
            }
        }
    }

    /**
     * counter class to avoid recreating Integers in map
     */
    private static class Counter
    {
        int num = 0;
    }
}
