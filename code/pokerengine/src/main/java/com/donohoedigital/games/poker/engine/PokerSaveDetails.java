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
 * PokerSaveDetails.java
 *
 * Created on January 20, 2005, 9:14 AM
 */

package com.donohoedigital.games.poker.engine;

import com.donohoedigital.comms.*;
import com.donohoedigital.games.config.*;

import java.util.*;

/**
 *
 * @author  donohoe
 */
@DataCoder('Y')
public class PokerSaveDetails implements DataMarshal
{
    private int nProfileData_;
    private int nTables_;
    
    // when saving for a particular player
    public static final int NO_OVERRIDE = -1;
    public static final int NO_PLAYER = -1;

    private int nPlayerID_ = NO_PLAYER;
    private boolean bHideOthersCards_ = false;
    private boolean bSetCurrentTableToLocal_ = false;
    private int nOverrideState_ = NO_OVERRIDE;
    private boolean bOtherTableUpdate_ = false;
    private int[] removed_ = null;

    // transient
    private List<BaseProfile> files_;

    /**
     * empty constructor for marshalling
     */
    public PokerSaveDetails() {
        this(SaveDetails.SAVE_ALL);
    }
    
    /** 
     * Creates a new instance of PokerSaveDetails 
     */
    public PokerSaveDetails(int nInit) {
        nProfileData_ = nInit;
        nTables_ = nInit;
    }

    public int getSaveProfileData()
    {
        return nProfileData_;
    }
    
    public void setSaveProfileData(int n)
    {
        nProfileData_ = n;
    }
    
    public int getSaveTables()
    {
        return nTables_;
    }
    
    public void setSaveTables(int n)
    {
        nTables_ = n;
    }

    public void setOtherTableUpdate(boolean b)
    {
        bOtherTableUpdate_ = b;
    }

    public boolean isOtherTableUpdate()
    {
        return bOtherTableUpdate_;
    }

    public int getPlayerID()
    {
        return nPlayerID_;
    }
    
    public void setPlayerID(int n)
    {
        nPlayerID_ = n;
    }

    public boolean isHideOthersCards()
    {
        return bHideOthersCards_;
    }
    
    public void setHideOthersCards(boolean b)
    {
        bHideOthersCards_ = b;
    }
    
    public boolean isSetCurrentTableToLocal()
    {
        return bSetCurrentTableToLocal_;
    }
    
    public void setSetCurrentTableToLocal(boolean b)
    {
        bSetCurrentTableToLocal_ = b;
    }

    public void setOverrideState(int n)
    {
        nOverrideState_ = n;
    }

    public int getOverrideState()
    {
        return nOverrideState_;
    }

    public void setRemovedTables(int[] removed)
    {
        removed_  = removed;
    }

    public int[] getRemovedTables()
    {
        return removed_;
    }

    // loading helper

    public void setPlayerTypeProfiles(List<BaseProfile> files)
    {
        files_ = files;
    }

    public List<BaseProfile> getPlayerTypeProfiles()
    {
        return files_;
    }

    ///
    /// save/load
    ///
    
    public void demarshal(MsgState state, String sData)
    {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
    	nProfileData_ = list.removeIntToken();
    	nTables_ = list.removeIntToken();
        bOtherTableUpdate_ = list.removeBooleanToken();
        nPlayerID_ = list.removeIntToken();
        bHideOthersCards_ = list.removeBooleanToken();
        bSetCurrentTableToLocal_ = list.removeBooleanToken();
        nOverrideState_ = list.removeIntToken();
        int nSize = list.removeIntToken();
        if (nSize > 0)
        {
            removed_ = new int[nSize];
            for (int i = 0; i < nSize; i++)
            {
                removed_[i] = list.removeIntToken();
            }
        }
    }
   
    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
    	list.addToken(nProfileData_);
    	list.addToken(nTables_);
        list.addToken(bOtherTableUpdate_);
        list.addToken(nPlayerID_);
        list.addToken(bHideOthersCards_);
        list.addToken(bSetCurrentTableToLocal_);
        list.addToken(nOverrideState_);
        list.addToken(removed_ == null ? 0 : removed_.length);
        if (removed_ != null)
        {
            for (int removed : removed_)
            {
                list.addToken(removed);
            }
        }
        return list.marshal(state);
    }
}
