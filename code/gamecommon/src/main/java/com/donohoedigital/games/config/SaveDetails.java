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
 * SaveDetails.java
 *
 * Created on January 19, 2005, 5:07 PM
 */

package com.donohoedigital.games.config;

import com.donohoedigital.comms.*;

/**
 *
 * @author  donohoe
 */
@DataCoder('S')
public class SaveDetails implements DataMarshal
{
    
    public static final int SAVE_ALL = 1;
    public static final int SAVE_DIRTY = 2;
    public static final int SAVE_NONE = 3;
    
    public static final int TERRITORY_OWNER_UNITS = 1;
    public static final int TERRITORY_ALL_UNITS = 2;
    
    private int nGameHashData_ = SAVE_ALL;
    private int nGameSubclassData_ = SAVE_ALL;
    private int nPlayers_ = SAVE_ALL;
    private int nAI_ = SAVE_ALL;
    private int nObservers_ = SAVE_ALL;
    private int nCurrentPhase_ = SAVE_ALL;
    private int nTerritories_ = SAVE_ALL;
    private int nTerritoryDirtyType_;  // if dirty load, specifies type
    private int nTerritoryUnitOwnerID_;// if owner unit load, specifies owner id
    private int nCustomSave_ = SAVE_ALL;
    
    private DataMarshal dCustomInfo_;

    
    /** 
     * create details set to SAVE_ALL
     */
    public SaveDetails() {
        this(SAVE_ALL);
    }
    
    /**
     * Create details with all types set to given value
     */
    public SaveDetails(int nInitialValue)
    {
        nGameHashData_ = nInitialValue;
        nGameSubclassData_ = nInitialValue;
        nPlayers_ = nInitialValue;
        nAI_ = nInitialValue;
        nObservers_ = nInitialValue;
        nCurrentPhase_ = nInitialValue;
        nTerritories_ = nInitialValue;
        nCustomSave_ = nInitialValue;
    }
    
    public int getSaveGameHashData()
    {
        return nGameHashData_;
    }
    
    public void setSaveGameHashData(int n)
    {
        nGameHashData_ = n;
    }
    
    public int getSaveGameSubclassData()
    {
        return nGameSubclassData_;
    }
    
    public void setSaveGameSubclassData(int n)
    {
        nGameSubclassData_ = n;
    }
    
    public int getSavePlayers()
    {
        return nPlayers_;
    }
    
    public void setSavePlayers(int n)
    {
        nPlayers_ = n;
    }

    public int getSaveAI()
    {
        return nAI_;
    }

    public void setSaveAI(int n)
    {
        nAI_ = n;
    }

    public int getSaveObservers()
    {
        return nObservers_;
    }

    public void setSaveObservers(int n)
    {
        nObservers_ = n;
    }

    public int getSaveCurrentPhase()
    {
        return nCurrentPhase_;
    }
    
    public void setSaveCurrentPhase(int n)
    {
        nCurrentPhase_ = n;
    }
    
    public int getSaveTerritories()
    {
        return nTerritories_;
    }
    
    public void setSaveTerritories(int n)
    {
        nTerritories_ = n;
    }
    
    public int getTerritoriesDirtyType()
    {
        return nTerritoryDirtyType_;
    }
    
    public void setTerritoriesDirtyType(int n)
    {
        nTerritoryDirtyType_ = n;
    }
    
    public int getTerritoriesUnitOwnerID()
    {
        return nTerritoryUnitOwnerID_;
    }
    
    public void setTerritoriesUnitOwnerID(int n)
    {
        nTerritoryUnitOwnerID_ = n;
    }
    
    public int getSaveCustom()
    {
        return nCustomSave_;
    }
    
    public void setSaveCustom(int n)
    {
        nCustomSave_ = n;
    }
    
    public DataMarshal getCustomInfo()
    {
        return dCustomInfo_;
    }
    
    public void setCustomInfo(DataMarshal d)
    {
        dCustomInfo_ = d;
    }
    
    public void demarshal(MsgState state, String sData)
    {   
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        nGameHashData_ = list.removeIntToken();
    	nGameSubclassData_ = list.removeIntToken();
    	nPlayers_ = list.removeIntToken();
        nAI_ = list.removeIntToken();
        nObservers_ = list.removeIntToken();
    	nCurrentPhase_ = list.removeIntToken();
    	nTerritories_ = list.removeIntToken();
        nTerritoryDirtyType_ = list.removeIntToken();
        nTerritoryUnitOwnerID_ = list.removeIntToken();
    	dCustomInfo_ = list.removeToken();
    }
   
    public String marshal(MsgState state)
    {
        TokenizedList list = new TokenizedList();
        list.addToken(nGameHashData_);
    	list.addToken(nGameSubclassData_);
    	list.addToken(nPlayers_);
        list.addToken(nAI_);
        list.addToken(nObservers_);
    	list.addToken(nCurrentPhase_);
    	list.addToken(nTerritories_);
        list.addToken(nTerritoryDirtyType_);
        list.addToken(nTerritoryUnitOwnerID_);
    	list.addToken(dCustomInfo_);
        return list.marshal(state);
    }
}
