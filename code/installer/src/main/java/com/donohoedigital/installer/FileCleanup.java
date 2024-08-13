package com.donohoedigital.installer;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.install4j.api.actions.*;
import com.install4j.api.context.*;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Jul 22, 2005
 * Time: 11:47:09 AM
 */
@SuppressWarnings({"CallToPrintStackTrace", "UseOfSystemOutOrSystemErr"})
public class FileCleanup implements UninstallAction
{
    private static String APP = "poker";
    private static String VER = "3";

    private static boolean DEBUG = false;

    public void init(Context context)
    {
    }

    public boolean uninstall(UninstallerContext uninstallerContext) throws UserCanceledException
    {
        ProgressInterface progressInterface = uninstallerContext.getProgressInterface();
        Utils.setVersionString(VER);
        Prefs.setRootNodeName(APP + VER);

        //File installDir = context.getInstallationDirectory();
        //progressInterface.setIndeterminateProgress(true);

        try
        {
            if (uninstallerContext.getBooleanVariable("deleteFiles"))
            {
                progressInterface.setStatusMessage("Deleting user-specific files...");
                deleteDir(new DefaultRuntimeDirectory().getClientHome(APP), progressInterface, true);
                Utils.sleepSeconds(1);
            }

            progressInterface.setPercentCompleted(50);

            //progressInterface.setStatusMessage("Deleting auto-update files...");
            //deleteDir(installDir, progressInterface, false);

            if (uninstallerContext.getBooleanVariable("deletePrefs"))
            {
                progressInterface.setStatusMessage("Removing preferences...");
                Prefs.clearAll();
                Utils.sleepSeconds(1);
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }

        //progressInterface.setIndeterminateProgress(false);
        progressInterface.setPercentCompleted(100);

        return true;
    }

    /**
     * Delete a directory and all of its contents
     */
    private void deleteDir(File dir, ProgressInterface progressInterface, boolean bDeleteDir)
    {
        File files[] = dir.listFiles();
        File file;

        progressInterface.setDetailMessage(dir.getAbsolutePath());
        for (int i = 0; files != null && i < files.length; i++)
        {
            file = files[i];
            if (file.isDirectory())
            {
                // skip jre dir - handled by install4j
                if (file.getName().equals("jre") || file.getName().toLowerCase().equals(".install4j")) continue;

                // recurse through subdirectory
                deleteDir(file, progressInterface, true);
            }
            else
            {
                // delete a file
                if (DEBUG) System.err.println("Delete file " + file.getAbsolutePath());
                progressInterface.setDetailMessage(file.getName());
                if (!file.delete())
                {
                    if (DEBUG) System.err.println("Unable to delete file: " + file.getAbsolutePath());
                }
            }
        }

        // delete the directory
        if (DEBUG) System.err.println("Delete directory " + dir.getAbsolutePath());
        if (bDeleteDir)
        {
            if (!dir.delete())
            {
                if (DEBUG) System.err.println("Unable to delete directory: " + dir.getAbsolutePath());
            }
        }
    }
}
