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
package com.donohoedigital.games.poker.ai;

public class FloatTracker
{
    private int min_;
    private int next_;
    private boolean full_;
    private float weightedAverage_;
    private float[] entries_;

    public FloatTracker(int length, int min)
    {
        entries_ = new float[length];
        min_ = min;
    }

    public void addEntry(float f)
    {
        entries_[next_] = f;

        next_ = (next_ + 1) % entries_.length;

        if (next_ == 0)
        {
            full_ = true;
        }

        computeAverage();
    }

    public void addEntry(Float f)
    {
        if (f == null) return;

        float fv = f;

        entries_[next_] = fv;

        next_ = (next_ + 1) % entries_.length;

        if (next_ == 0)
        {
            full_ = true;
        }

        computeAverage();
    }

    public boolean isFull()
    {
        return full_;
    }

    public boolean isReady()
    {
        return full_ || next_ > min_;
    }

    public int getCount()
    {
        return (full_ ? entries_.length : next_);
    }

    public float getWeightedAverage(float defPercent)
    {
        if (!full_ && next_ < min_) return defPercent;

        else return weightedAverage_;
    }

    private void computeAverage()
    {
        float sum = 0.0f;
        float div = 0.0f;

        int count = getCount();

        int start = (full_ ? next_ : 0);


        for (int i = 0; i < count; ++i)
        {
            sum += entries_[(start + i) % entries_.length] * (i + 1);

            div += i+1;
        }

        weightedAverage_ = sum / div;

        /*
        System.out.println("count="+count);
        System.out.println("start="+start);
        System.out.println("sum="+sum);
        System.out.println("div="+div);
        System.out.println("avg="+weightedAverage_);
        */
    }

    public void clear()
    {
        entries_ = new float[entries_.length];
        next_ = 0;
        full_ = false;
        weightedAverage_ = 0;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        if (full_)
        {
            for (int i = next_; i < entries_.length; ++i)
            {
                if (i > next_)
                {
                    buf.append(",");
                }
                buf.append(entries_[i]);
            }
            for (int i = 0; i < next_; ++i)
            {
                buf.append(",");
                buf.append(entries_[i]);
            }
        }
        else
        {
            for (int i = 0; i < next_; ++i)
            {
                if (i > 0)
                {
                    buf.append(",");
                }
                buf.append(entries_[i]);
            }
        }
        buf.append("]");

        return buf.toString();
    }

    public Object encode()
    {
        StringBuilder buf = new StringBuilder();

        buf.append(min_);
        buf.append(",");
        buf.append(next_);
        buf.append(",");
        buf.append(full_);
        buf.append(",");
        buf.append(weightedAverage_);
        buf.append(",");
        buf.append(entries_.length);
        buf.append(",");

        for (int i = 0; i < entries_.length; ++i)
        {
            buf.append(entries_[i]);
            buf.append(":");
        }

        return buf.toString();
    }

    public void decode(Object o)
    {
        if (o == null) return;

        String s = (String)o;

        String[] a = s.split(",");

        min_ = Integer.parseInt(a[0]);
        next_ = Integer.parseInt(a[1]);
        full_ = Boolean.parseBoolean(a[2]);
        entries_ = new float[Integer.parseInt(a[4])];

        String[] v = a[5].split(":");

        for (int i = 0; i < entries_.length && i < v.length; ++i)
        {
            entries_[i] = Float.parseFloat(v[i]);
        }

        computeAverage();
    }
}
