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
package com.donohoedigital.installer;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.DefaultRuntimeDirectory;
import com.donohoedigital.config.Prefs;
import com.install4j.api.actions.UninstallAction;
import com.install4j.api.context.Context;
import com.install4j.api.context.ProgressInterface;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;

import java.io.File;

/**
 * Install4j cleanup action to remove .ddpoker-3 files and prefs
 */
@SuppressWarnings({"CallToPrintStackTrace", "UseOfSystemOutOrSystemErr", "unused"})
public class FileCleanup implements UninstallAction {

    // app and version need to match PokerConstants (copied here to avoid pulling in entire tree in Install4j)
    private static final String APP = "poker";
    private static final String VER = "3";
    private static final boolean DEBUG = false;

    public void init(Context context) {
    }

    @SuppressWarnings("RedundantThrows")
    public boolean uninstall(UninstallerContext uninstallerContext) throws UserCanceledException {
        ProgressInterface progressInterface = uninstallerContext.getProgressInterface();
        Utils.setVersionString(VER);
        Prefs.setRootNodeName(APP + VER);

        try {
            if (uninstallerContext.getBooleanVariable("deleteFiles")) {
                progressInterface.setStatusMessage("Deleting user-specific files...");
                deleteDir(new DefaultRuntimeDirectory().getClientHome(APP), progressInterface);
                Utils.sleepSeconds(1);
            }

            progressInterface.setPercentCompleted(50);

            if (uninstallerContext.getBooleanVariable("deletePrefs")) {
                progressInterface.setStatusMessage("Removing preferences...");
                Prefs.clearAll();
                Utils.sleepSeconds(1);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        progressInterface.setPercentCompleted(100);

        return true;
    }

    /**
     * Delete a directory and all of its contents
     */
    private void deleteDir(File dir, ProgressInterface progressInterface) {
        File[] files = dir.listFiles();
        File file;

        progressInterface.setDetailMessage(dir.getAbsolutePath());
        for (int i = 0; files != null && i < files.length; i++) {
            file = files[i];
            if (file.isDirectory()) {
                // skip jre dir - handled by install4j
                if (file.getName().equals("jre") || file.getName().equalsIgnoreCase(".install4j")) continue;

                // recurse through subdirectory
                deleteDir(file, progressInterface);
            } else {
                // delete a file
                if (DEBUG) System.err.println("Delete file " + file.getAbsolutePath());
                progressInterface.setDetailMessage(file.getName());
                if (!file.delete()) {
                    if (DEBUG) System.err.println("Unable to delete file: " + file.getAbsolutePath());
                }
            }
        }

        // delete the directory
        if (DEBUG) System.err.println("Delete directory " + dir.getAbsolutePath());
        if (!dir.delete()) {
            if (DEBUG) System.err.println("Unable to delete directory: " + dir.getAbsolutePath());
        }
    }
}
