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
package com.donohoedigital.games.engine;

import com.donohoedigital.config.*;
import com.donohoedigital.gui.*;
import org.apache.log4j.*;
import com.donohoedigital.base.*;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 16, 2005
 * Time: 5:18:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableExporter implements DDTable.Exporter
{
    static Logger logger = Logger.getLogger(TableExporter.class);

    private GameContext context_;
    private String sName_;
    
    public TableExporter(GameContext context, String sName)
    {
        context_ = context;
        sName_ = sName;
    }

    public void exportRequested(DDTable table)
    {
        TypedHashMap params = new TypedHashMap();
        params.setString(FileChooserDialog.PARAM_SUGGESTED_NAME, sName_);
        Phase choose = context_.processPhaseNow("ExportTable", params);
        Object oResult = choose.getResult();
        if (oResult != null && oResult instanceof File)
        {
            File file = (File) oResult;
            logger.info("Exporting table to " + file.getAbsolutePath());
            ConfigUtils.writeFile((File)oResult, table.toCSV(true), false);
        }

    }
}
