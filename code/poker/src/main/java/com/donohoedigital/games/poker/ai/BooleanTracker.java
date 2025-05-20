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
package com.donohoedigital.games.poker.ai;

public class BooleanTracker
{
    private int min_;
    private int next_;
    private boolean full_;
    private int countTrue_;
    private boolean[] entries_;

    public BooleanTracker(int length, int min)
    {
        entries_ = new boolean[length];
        min_ = min;
    }

    public void addEntry(boolean b)
    {
        if (entries_[next_] != b)
        {
            entries_[next_] = b;

            countTrue_ += (b ? 1 : -1);
        }

        next_ = (next_ + 1) % entries_.length;

        if (next_ == 0)
        {
            full_ = true;
        }
    }

    public void addEntry(Boolean b)
    {
        if (b == null) return;

        boolean bv = b.booleanValue();

        if (entries_[next_] != bv)
        {
            entries_[next_] = bv;

            countTrue_ += (bv ? 1 : -1);
        }

        next_ = (next_ + 1) % entries_.length;

        if (next_ == 0)
        {
            full_ = true;
        }
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
        return entries_.length;
    }

    public int getCountTrue()
    {
        return countTrue_;
    }

    public float getPercentTrue(float defPercent)
    {
        if (full_)
        {
            return ((float)countTrue_ / (float)entries_.length);
        }
        else
        {
            if (next_ >= min_)
            {
                return ((float)countTrue_ / (float)next_);
            }
            else
            {
                return defPercent;
            }
        }
    }

    public float getWeightedPercentTrue(float defPercent)
    {
        float sum = 0.0f;
        float div = 0.0f;

        if (!full_ && next_ < min_) return defPercent;

        int index = (next_ + entries_.length - 1) % entries_.length;

        for (int i = (full_ ? entries_.length : next_) - 1; i >= 0; --i)
        {
            if (entries_[(index + entries_.length - i) % entries_.length])
            {
                sum += ((float)entries_.length - i) / (float)entries_.length;
            }

            div += ((float)entries_.length - i) / (float)entries_.length;
        }

        return sum / div;
    }

    public void clear()
    {
        entries_ = new boolean[entries_.length];
        next_ = 0;
        full_ = false;
        countTrue_ = 0;
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
        buf.append(countTrue_);
        buf.append(",");
        buf.append(entries_.length);
        buf.append(",");

        for (int i = 0; i < entries_.length; ++i)
        {
            buf.append(entries_[i] ? "T":"F");
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
        countTrue_ = Integer.parseInt(a[3]);
        entries_ = new boolean[Integer.parseInt(a[4])];

        for (int i = 0; i < entries_.length && i < a[5].length(); ++i)
        {
            entries_[i] = (a[5].charAt(i) == 'T');
        }
    }

    public int getConsecutive(boolean value)
    {
        int count = 0;

        for (int i = 0; (full_ && i < entries_.length) || i < next_; ++i)
        {
            if (entries_[i] == value) ++count;
            else return count;
        }

        return count;
    }
}
