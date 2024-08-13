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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import org.apache.log4j.*;
import com.donohoedigital.comms.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.nio.channels.*;

/**
 * Performs auto update tasks.
 */
public class UpdateInstaller
{
    static Logger logger = Logger.getLogger(UpdateInstaller.class);

    private static final String UPDATE_DIR_NAME = "update";
    private static final String WORK_DIR_NAME = "work";
    private static final String BACKUP_DIR_NAME = "backup";
    private static final String CURRENT_DIR_NAME = "current";

    private static final String MANIFEST_FILE_PATH = "META-INF/MANIFEST.MF";
    private static final String ATTR_ACTION = "Action";

    private static final String ACTION_ADDED = "ADDED";
    private static final String ACTION_CHANGED = "CHANGED";
    private static final String ACTION_REMOVED = "REMOVED";

    private static final String PERMISSION_PREFIX = "install-";
    private static final String CLASS_EXT_ENCRYPTED = ".ssalc";
    private static final String CLASS_EXT_STANDARD = ".class";

    private static final File installDir_;
    private static final File updateDir_;
    private static final File workDir_;
    private static File workBackupDir_ = null;
    private static File workCurrentDir_ = null;

    static
    {
        // Initialize the top-level directories used for installation.
        File configDir = null; //ConfigManager.getConfigDir(); // FIX: need to rewrite update installer given new modules/etc
        File userDir = ConfigManager.getUserHome();

        installDir_ = configDir.getParentFile();
        updateDir_ = new File(userDir, UPDATE_DIR_NAME);
        workDir_ = new File(updateDir_, WORK_DIR_NAME);
    }

    private Listener listener_ = null;
    private int installCount_ = 0;
    private int installTotal_ = 0;

    /**
     * Create an instance that reports its progress via the given listener.
     *
     * @param listener listener
     */
    public UpdateInstaller(Listener listener)
    {
        listener_ = listener;
    }

    /**
     * Check that the current user has permissions to perform the install.
     *
     * @return <code>true</code> if the user has permissions, <code>false</code> otherwise
     */
    public boolean checkPermissions()
    {
        // Create a temp file in the installation directory.
        File file = new File(installDir_, (PERMISSION_PREFIX + System.currentTimeMillis()));

        try
        {
            file.createNewFile();
        }
        catch (IOException e)
        {
            return false;
        }
        finally
        {
            // Clean up.
            file.delete();
        }

        return true;
    }

    /**
     * Install the given update release.
     *
     * @param version current version
     * @param release update release
     * @param file update file
     */
    public void installUpdate(Version version, PatchRelease release, File file)
    {
        // Do all the work in an internal method to ensure proper cleanup.
        try
        {
            logger.info("Installing update: " + release.getVersion());
            _installUpdate(version, release, file);
        }
        finally
        {
            cleanUp();
        }
    }

    /**
     * Install the given update release.
     *
     * @param version current version
     * @param release update release
     * @param file update file
     */
    private void _installUpdate(Version version, PatchRelease release, File file)
    {
        // Check that a valid file has been downloaded.
        String hash = ConfigUtils.getMD5Hash(file);

        if (!hash.equals(release.getHash()))
        {
            // Get rid of the file to avoid future errors.
            file.delete();

            throw new ApplicationError(PropertyConfig.getMessage("msg.update.invalidFile", release.getHash(), hash));
        }

        // Create the install work area.
        long tempExt = System.currentTimeMillis();
        workBackupDir_ = new File(workDir_, BACKUP_DIR_NAME + "-" + tempExt);
        workCurrentDir_ = new File(workDir_, CURRENT_DIR_NAME + "-" + tempExt);

        ConfigUtils.verifyNewDirectory(workBackupDir_);
        ConfigUtils.verifyNewDirectory(workCurrentDir_);

        // Unjar the downloaded file and open the manifest file.
        Manifest manifest = null;

        try
        {
            // Set totals to track progress.
            initTotal(file);

            // Unjar everything into the the work directory.
            unjarFile(file, workCurrentDir_);

            File manifestFile = new File(workCurrentDir_, MANIFEST_FILE_PATH);
            FileInputStream manifestStream = new FileInputStream(manifestFile);

            try
            {
                manifest = new Manifest(manifestStream);
            }
            finally
            {
                try { manifestStream.close(); } catch (IOException e) { }
            }
        }
        catch (IOException e)
        {
            throw new ApplicationError(e);
        }

        // Use the manifest to copy all of the files to the proper location.
        logger.info("Backing up install.");
        backupInstall(manifest);

        try
        {
            logger.info("Updating install.");
            updateInstall(manifest);
        }
        catch (Throwable t)
        {
            // Restore to the original state.
            logger.info("Restoring install.");
            restoreInstall(manifest);

            throw new ApplicationError(t);
        }

        // Move the update files to a "permanent" location.
        File updateVersionDir = new File(updateDir_, release.getVersion().toString());
        File updateBackupDir = new File(updateVersionDir, BACKUP_DIR_NAME);
        File updateCurrentDir = new File(updateVersionDir, CURRENT_DIR_NAME);

        ConfigUtils.verifyNewDirectory(updateVersionDir);
        workBackupDir_.renameTo(updateBackupDir);
        workCurrentDir_.renameTo(updateCurrentDir);

        // Remove any patch files for the current version.
        File installVersionDir = new File(updateDir_, version.toString());

        if (installVersionDir.exists())
        {
            ConfigUtils.deleteDir(installVersionDir);
        }

        // Finally remove the update JAR.
        file.delete();
    }


    /**
     * Clean up the area used to process download/install.
     */
    private void cleanUp()
    {
        // Kill the top level work directory.
        logger.info("Cleaning up.");
        if (workDir_.exists())
        {
            ConfigUtils.deleteDir(workDir_);
        }
    }

    /**
     * Initialize the file count using the manifest contained in the given JAR file.
     *
     * @param file JAR file
     */
    private void initTotal(File file) throws IOException
    {
        if (listener_ == null)
        {
            return;
        }

        // Total number of files processed is:
        //
        // Unjarred files (including manifest) +
        // Backed up files (excluding inner classes) +
        // Installed files (excluding inner classes)

        JarFile jarFile = new JarFile(file);
        Enumeration jarEntries = jarFile.entries();

        // Unjarred files.
        while (jarEntries.hasMoreElements())
        {
            jarEntries.nextElement();
            installTotal_++;
        }

        // Backed up and installed files.
        Manifest manifest = jarFile.getManifest();
        Iterator manifestEntries = manifest.getEntries().entrySet().iterator();
        Map.Entry entry = null;
        String name = null;
        Attributes attributes = null;
        String actionValue = null;

        while (manifestEntries.hasNext())
        {
            entry = (Map.Entry) manifestEntries.next();
            name = (String) entry.getKey();
            attributes = (Attributes) entry.getValue();
            actionValue = attributes.getValue(ATTR_ACTION);

            if (isInnerClass(name))
            {
                continue;
            }

            if (ACTION_ADDED.equals(actionValue))
            {
                // Installed file.
                installTotal_++;
            }
            else
            {
                // Backed up and installed file.
                installTotal_ += 2;
            }
        }
    }

    /**
     * Unjar the given file into the given directory.
     *
     * @param file JAR file
     * @param dir target directory
     *
     * @throws IOException if an error occurs writing any of the files
     */
    private void unjarFile(File file, File dir) throws IOException
    {
        JarFile jarFile = new JarFile(file);
        Enumeration jarEntries = jarFile.entries();
        JarEntry jarEntry = null;
        InputStream jarStream = null;
        File outputFile = null;
        FileOutputStream outputStream = null;
        byte[] bytes = new byte[1024];
        int byteCount = 0;

        while (jarEntries.hasMoreElements())
        {
            jarEntry = (JarEntry) jarEntries.nextElement();
            jarStream = jarFile.getInputStream(jarEntry);
            outputFile = new File(dir, jarEntry.getName());

            try
            {
                if (jarEntry.isDirectory())
                {
                    // Create a directory.  Assumes parent directories are read before child directories.
                    outputFile.mkdir();
                }
                else
                {
                    // Create a file.
                    outputStream = new FileOutputStream(outputFile);

                    try
                    {
                        while ((byteCount = jarStream.read(bytes)) != -1)
                        {
                            outputStream.write(bytes, 0, byteCount);
                        }
                    }
                    finally
                    {
                        try { outputStream.close(); } catch (IOException e) { }
                    }
                }

                // Update the progress.
                ++installCount_;
                setPercentDone();
            }
            finally
            {
                try { jarStream.close(); } catch (IOException e) { }
            }
        }
    }

    /**
     * Use the manifest information to create a backup of the install directory.
     *
     * @param manifest update file manifest
     */
    private void backupInstall(Manifest manifest)
    {
        // Keep track of changed and deleted files so that they can be copied back over later.
        copyToDir(manifest, ACTION_CHANGED, installDir_, workBackupDir_, true);
        copyToDir(manifest, ACTION_REMOVED, installDir_, workBackupDir_, true);
    }

    /**
     * Use the manifest information to update the install directory.
     *
     * @param manifest update file manifest
     */
    private void updateInstall(Manifest manifest)
    {
        // Copy over added and changed files.
        copyToDir(manifest, ACTION_ADDED, workCurrentDir_, installDir_, true);
        copyToDir(manifest, ACTION_CHANGED, workCurrentDir_, installDir_, true);

        // Delete removed files.
        removeFromDir(manifest, ACTION_REMOVED, installDir_, true);
    }

    /**
     * Use the manifest information to restor the install directory.
     *
     * @param manifest update file manifest
     */
    private void restoreInstall(Manifest manifest)
    {
        // Restore previously change and removed files.
        copyToDir(manifest, ACTION_CHANGED, workBackupDir_, installDir_, false);
        copyToDir(manifest, ACTION_REMOVED, workBackupDir_, installDir_, false);

        // Remove previously added files.
        removeFromDir(manifest, ACTION_ADDED, installDir_, false);
    }

    /**
     * Is the given name a class?
     *
     * @param name name
     *
     * @return <code>true</code> if a parent class, <code>false</code> otherwise
     */
    private boolean isClass(String name)
    {
        return (isEncryptedClass(name) || isStandardClass(name));
    }

    /**
     * Is the given name an encrypted class?
     *
     * @param name name
     *
     * @return <code>true</code> if a parent class, <code>false</code> otherwise
     */
    private boolean isEncryptedClass(String name)
    {
        return name.endsWith(CLASS_EXT_ENCRYPTED);
    }

    /**
     * Is the given name a standard class?
     *
     * @param name name
     *
     * @return <code>true</code> if a parent class, <code>false</code> otherwise
     */
    private boolean isStandardClass(String name)
    {
        return name.endsWith(CLASS_EXT_STANDARD);
    }

    /**
     * Is the given name an inner class?
     *
     * @param name name
     *
     * @return <code>true</code> if a parent class, <code>false</code> otherwise
     */
    private boolean isInnerClass(String name)
    {
        return isClass(name) && (name.indexOf('$') >= 0);
    }

    /**
     * Is the given name a parent class?
     *
     * @param name name
     *
     * @return <code>true</code> if a parent class, <code>false</code> otherwise
     */
    private boolean isParentClass(String name)
    {
        return isClass(name) && (name.indexOf('$') < 0);
    }

    /**
     * Copy files with the given action from one directory to another.
     *
     * @param manifest update file manifest
     * @param action action
     * @param fromDir from directory
     * @param toDir to directory
     * @param updateProgress where or not to update the installation progress
     */
    private void copyToDir(Manifest manifest, String action, File fromDir, File toDir, boolean updateProgress)
    {
        // Loop through the manifest looking for files with the matching action.
        Map map = manifest.getEntries();
        Iterator entries = map.entrySet().iterator();
        Map.Entry entry = null;
        String name = null;
        Attributes attributes = null;
        String actionValue = null;
        File file = null;

        while (entries.hasNext())
        {
            entry = (Map.Entry) entries.next();
            name = (String) entry.getKey();
            attributes = (Attributes) entry.getValue();
            actionValue = attributes.getValue(ATTR_ACTION);

            if (action.equals(actionValue))
            {
                // Skip inner classes since they are processed along with their respective parent classes.
                if (isInnerClass(name))
                {
                    continue;
                }

                // Copy the associated files.
                file = new File(name);
                copyFiles(file, fromDir, toDir);

                // Update the progress.
                if (updateProgress)
                {
                    ++installCount_;
                    setPercentDone();
                }
            }
        }
    }

    /**
     * Delete files with the given action from the given directory.
     *
     * @param manifest update file manifest
     * @param action action
     * @param dir directory
     * @param updateProgress where or not to update the installation progress
     */
    private void removeFromDir(Manifest manifest, String action, File dir, boolean updateProgress)
    {
        // Loop through the manifest looking for files with the matching action.
        Map map = manifest.getEntries();
        Iterator entries = map.entrySet().iterator();
        Map.Entry entry = null;
        String name = null;
        Attributes attributes = null;
        String actionValue = null;
        File file = null;

        while (entries.hasNext())
        {
            entry = (Map.Entry) entries.next();
            name = (String) entry.getKey();
            attributes = (Attributes) entry.getValue();
            actionValue = attributes.getValue(ATTR_ACTION);

            if (action.equals(actionValue))
            {
                // Skip inner classes since they are processed along with their respective parent classes.
                if (isInnerClass(name))
                {
                    continue;
                }

                // Delete the associated files.
                file = new File(dir, name);
                deleteFiles(file, true);

                // Update the progress.
                if (updateProgress)
                {
                    ++installCount_;
                    setPercentDone();
                }
            }
        }
    }

    /**
     * Copy the given file and any associated files.
     *
     * @param file file to copy
     * @param fromDir source directory
     * @param toDir target directory
     */
    private void copyFiles(File file, File fromDir, File toDir)
    {
        // Make sure the target directory exists.
        File fromFile = new File(fromDir, file.getPath());
        File toFile = new File(toDir, file.getPath());

        File toParentDir = toFile.getParentFile();
        toParentDir.mkdirs();

        // Copy over the source file.
        deleteFiles(toFile, false);
        copyFile(fromFile, toFile);

        // If a parent class, also copy over inner classes.
        String fromFileName = fromFile.getName();

        if (isParentClass(fromFileName))
        {
            File fromParentDir = fromFile.getParentFile();
            File[] fromFiles = fromParentDir.listFiles(new InnerClassFilter(fromFileName));
            int fromFileCount = (fromFiles != null) ? fromFiles.length : 0;

            for (int i = 0; i < fromFileCount; ++i)
            {
                fromFile = new File(fromParentDir, fromFiles[i].getName());
                toFile = new File(toParentDir, fromFile.getName());
                copyFile(fromFile, toFile);
            }
        }
    }

    /**
     * Copy the given file.
     */
    private void copyFile(File fromFile, File toFile)
    {
        try
        {
            logger.info("Copying file: " + fromFile.getAbsolutePath() + " => " + toFile.getAbsolutePath());
            FileChannel src = new FileInputStream(fromFile).getChannel();
            FileChannel dst = new FileOutputStream(toFile).getChannel();
            src.transferTo(0, src.size(), dst);
            src.close();
            dst.close();
        }
        catch (Throwable t)
        {
            //throw new ApplicationError(t);
            logger.error("Copy file failed: " + Utils.formatExceptionText(t));
        }
    }

    /**
     * Delete the given file and any associated files.
     *
     * @param file file to delete
     * @param deleteDir delete empty directories
     *
     * @return <code>true</code> if the file was deleted, <code>false</code> otherwise
     */
    private boolean deleteFiles(File file, boolean deleteDir)
    {
        // If an encrypted class file does not exist on disk, try again with the standard extension.
        String fileName = file.getName();

        if (isEncryptedClass(fileName) && !file.exists())
        {
            String path = file.getAbsolutePath();
            int extIndex = path.lastIndexOf('.');
            path = path.substring(0, extIndex) + CLASS_EXT_STANDARD;
            file = new File(path);
            fileName = file.getName();
        }

        if (!file.exists())
        {
            return false;
        }

        // Delete the given file.
        boolean deleted = deleteFile(file, deleteDir);

        // If a parent class file, also delete any inner classes.
        if (isParentClass(fileName))
        {
            File dir = file.getParentFile().getAbsoluteFile();
            File[] files = dir.listFiles(new InnerClassFilter(fileName));
            int fileCount = (files != null) ? files.length : 0;

            for (int i = 0; i < fileCount; ++i)
            {
                deleteFile(files[i], deleteDir);
            }
        }

        return deleted;
    }

    /**
     * Delete the given file and any associated files.
     *
     * @param file file to delete
     * @param deleteDir delete empty directories
     *
     * @return <code>true</code> if the file was deleted, <code>false</code> otherwise
     */
    private boolean deleteFile(File file, boolean deleteDir)
    {
        // Delete the file.
        logger.info("Deleting file: " + file.getAbsolutePath());
        boolean deleted = file.delete();

        // Optionally delete any empty directories.
        if (deleteDir)
        {
            File dir = file.getParentFile();
            String[] files = dir.list();

            while ((files != null) && (files.length == 0))
            {
                dir.delete();
                dir = file.getParentFile();
                files = (dir != null) ? dir.list() : null;
            }
        }

        return deleted;
    }

    /**
     * Set percent done.
     */
    private void setPercentDone()
    {
        if (listener_ == null)
        {
            return;
        }

        int percent = (int) ((((double) installCount_) / ((double) installTotal_)) * 100);
        listener_.setPercentDone(percent);
    }

    /**
     * Reports installation progress.
     */
    public static interface Listener
    {
        /**
         * Called to report the percentage of update files installed.
         *
         * @param percent percent read
         */
        public void setPercentDone(int percent);
    }

    /**
     * Returns inner classes.
     */
    private static class InnerClassFilter implements FilenameFilter
    {
        private String startFilter_ = null;
        private String endFilter_ = CLASS_EXT_ENCRYPTED;

        /**
         * Create a filter that returns classes located in the given parent class.
         *
         * @param className parent class name
         */
        public InnerClassFilter(String className)
        {
            int extIndex = className.lastIndexOf('.');

            if (extIndex >= 0)
            {
                className = className.substring(0, extIndex);
                endFilter_ = className.substring(extIndex);
            }

            startFilter_ = className + "$";
        }

        /**
         * Detrermine if the given file is an inner class.
         *
         * @param dir directory
         * @param fileName file name
         *
         * @return <code>true</code> if an inner class, <code>false</code> otherwise
         */
        public boolean accept(File dir, String fileName)
        {
            return fileName.startsWith(startFilter_) && fileName.endsWith(endFilter_);
        }
    }
}
