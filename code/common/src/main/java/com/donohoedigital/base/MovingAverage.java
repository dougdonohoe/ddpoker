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

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 3, 2006
 * Time: 8:40:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class MovingAverage
{
    private int cnt_;
    private int idx_;
    private int sum_;

    private long[] entries_;
    private long peak_;

    /**
     * Create a moving average tracker with given number of entries
     */
    public MovingAverage(int nNumEntries)
    {
        entries_ = new long[nNumEntries];
    }
    /**
     * store amount
     */
    public void record(long amount)
    {
        if (amount > peak_) peak_ = amount;
        sum_ -= entries_[idx_];
        entries_[idx_] = amount;
        sum_ += entries_[idx_];
        idx_++;
        if (idx_ >= entries_.length) idx_ = 0;
        if (cnt_ < entries_.length) cnt_++;
    }

    /**
     * reset average
     */
    public void reset()
    {
        cnt_ = 0;
        idx_ = 0;
        sum_ = 0;
        for (int i = 0; i < entries_.length; i++)
        {
            entries_[i] = 0;
        }
        peak_ = 0;
    }
    /**
     * Get average as long
     */
    public long getAverageLong()
    {
        return (long) getAverageDouble();
    }

    /**
     * Get average as a double
     */
    public double getAverageDouble()
    {
        if (cnt_ == 0) return 0;
        return (double) sum_ / (double) cnt_;
    }

    /**
     * Get peak
     */
    public long getPeak()
    {
        return peak_;
    }

    /**
     * Get high
     */
    public long getHigh()
    {
        long high = 0;
        for (long i : entries_)
        {
            if (i > high) high = i;
        }
        return high;
    }
}
