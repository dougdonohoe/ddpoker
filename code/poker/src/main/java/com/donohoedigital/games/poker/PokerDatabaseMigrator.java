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
package com.donohoedigital.games.poker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.*;

/**
 * Upgrades hand-history databases written by HSQLDB 1.8 so the current HSQLDB can open them.
 * <p>
 * HSQLDB 2.7 can't open 1.8 databases at all, so each one is copied row by row: read with 1.8 (bundled
 * as a resource and loaded in its own class loader, since its classes clash with the current HSQLDB),
 * written to a new database with the current schema, then validated by comparing row counts and max IDs.
 * <p>
 * The original files are kept, untouched, in {@value #BACKUP_DIRECTORY}.  The work is done in a scratch
 * directory (on a copy of the originals), so the database directory only ever holds originals or
 * fully migrated databases.
 */
public class PokerDatabaseMigrator
{
    private static final Logger logger = LogManager.getLogger(PokerDatabaseMigrator.class);

    public static final String BACKUP_DIRECTORY = "v1";
    private static final String WORK_DIRECTORY = "upgrade-work";
    private static final String HSQLDB18_RESOURCE = "/hsqldb18/hsqldb-1.8.0.10.jar.bin";
    private static final String HSQLDB18_DRIVER = "org.hsqldb.jdbcDriver";
    private static final String URL_PREFIX = "jdbc:hsqldb:file:";
    private static final long LOCK_STALE_MILLIS = 30000;

    // so a failure part way doesn't leave a database open (and its files locked) in this JVM
    private static final String SHUTDOWN_ON_CLOSE = ";shutdown=true";

    // table -> identity column (null if none), used to validate the migration
    private static final Map<String, String> TABLES = new LinkedHashMap<>();

    static
    {
        TABLES.put("TOURNAMENT", "TRN_ID");
        TABLES.put("TOURNAMENT_FINISH", "TRF_ID");
        TABLES.put("HAND", "HND_ID");
        TABLES.put("TOURNAMENT_PLAYER", "TPL_ID");
        TABLES.put("PLAYER_HAND", null);
        TABLES.put("PLAYER_ACTION", null);
    }

    /**
     * Told as each database is started, for showing progress
     */
    public interface Progress
    {
        /**
         * @param name  database name (poker-[profile file number]-[key hash])
         * @param index 0-based
         * @param count number of databases
         */
        void migrating(String name, int index, int count);
    }

    /**
     * Outcome of {@link #migrate(File, Progress)}
     */
    public static class Result
    {
        private final File backupDir_;
        private final List<String> migrated_ = new ArrayList<>();
        private final Map<String, String> failed_ = new LinkedHashMap<>();

        Result(File backupDir)
        {
            backupDir_ = backupDir;
        }

        public File getBackupDir()
        {
            return backupDir_;
        }

        public List<String> getMigrated()
        {
            return migrated_;
        }

        /**
         * database name -> reason
         */
        public Map<String, String> getFailed()
        {
            return failed_;
        }
    }

    /**
     * Are there any HSQLDB 1.8 databases in the given directory?
     */
    public static boolean needsMigration(File dbDir)
    {
        return !getOldDatabaseNames(dbDir).isEmpty();
    }

    /**
     * Is another copy of the game using one of the old databases?  HSQLDB 1.8 rewrites its
     * .lck file every 10 seconds while the database is open.
     */
    public static boolean isInUse(File dbDir)
    {
        File[] locks = dbDir.listFiles((_, name) -> name.endsWith(".lck"));
        if (locks == null) return false;

        long now = System.currentTimeMillis();
        for (File lock : locks)
        {
            if (now - lock.lastModified() < LOCK_STALE_MILLIS) return true;
        }
        return false;
    }

    /**
     * Migrate all HSQLDB 1.8 databases in the given directory.  A database that fails is moved to
     * the backup directory like the rest, so the game starts that profile with an empty database.
     */
    public static Result migrate(File dbDir) throws IOException
    {
        return migrate(dbDir, null);
    }

    /**
     * Migrate, telling progress (if not null) as each database starts
     */
    public static Result migrate(File dbDir, Progress progress) throws IOException
    {
        List<String> names = getOldDatabaseNames(dbDir);
        File backupDir = new File(dbDir, BACKUP_DIRECTORY);
        File workDir = new File(dbDir, WORK_DIRECTORY);
        Result result = new Result(backupDir);

        Files.createDirectories(backupDir.toPath());
        deleteDir(workDir);
        Files.createDirectories(workDir.toPath());

        File oldDir = new File(workDir, "old");
        File newDir = new File(workDir, "new");
        Files.createDirectories(oldDir.toPath());
        Files.createDirectories(newDir.toPath());

        try (Hsqldb18 hsqldb18 = new Hsqldb18())
        {
            for (int i = 0; i < names.size(); i++)
            {
                String name = names.get(i);
                if (progress != null) progress.migrating(name, i, names.size());
                logger.info("Migrating database {}", name);
                try
                {
                    copyFiles(name, dbDir, oldDir);
                    migrate(hsqldb18, new File(oldDir, name), new File(newDir, name));
                    moveFiles(name, dbDir, backupDir);
                    moveFiles(name, newDir, dbDir);
                    result.migrated_.add(name);
                    logger.info("Migrated database {}", name);
                }
                catch (Exception e)
                {
                    logger.error("Unable to migrate database {}", name, e);
                    result.failed_.put(name, e.toString());
                    moveFiles(name, dbDir, backupDir);
                    deleteFiles(name, newDir);
                }
            }
        }
        finally
        {
            deleteDir(workDir);
        }

        return result;
    }

    /**
     * Copy one database (a copy of the original) into a new database with the current schema
     */
    private static void migrate(Hsqldb18 hsqldb18, File oldDb, File newDb) throws SQLException
    {
        Map<String, Long> before;
        Map<String, Long> after;

        try (Connection from = hsqldb18.connect(oldDb);
             Connection to = DriverManager.getConnection(URL_PREFIX + newDb.getAbsolutePath() + SHUTDOWN_ON_CLOSE, "sa", ""))
        {
            before = getStats(from);

            PokerDatabase.createTables(to);
            to.setAutoCommit(false);
            for (Map.Entry<String, String> table : TABLES.entrySet())
            {
                if (!hasTable(from, table.getKey())) continue;
                copyTable(from, to, table.getKey());
                if (table.getValue() != null) restartIdentity(to, table.getKey(), table.getValue());
            }
            to.commit();
            to.setAutoCommit(true);

            after = getStats(to);

            try (Statement stmt = from.createStatement())
            {
                stmt.execute("SHUTDOWN");
            }
            try (Statement stmt = to.createStatement())
            {
                stmt.execute("SHUTDOWN");
            }
        }

        if (!before.equals(after))
        {
            throw new SQLException("Validation failed: before " + before + ", after " + after);
        }
        logger.info("Validated {}: {}", newDb.getName(), after);
    }

    /**
     * Copy all rows of a table, by column name (older databases could lack newer columns)
     */
    private static void copyTable(Connection from, Connection to, String table) throws SQLException
    {
        try (Statement select = from.createStatement();
             ResultSet rs = select.executeQuery("SELECT * FROM " + table))
        {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            StringBuilder cols = new StringBuilder();
            StringBuilder params = new StringBuilder();
            for (int i = 1; i <= count; i++)
            {
                if (i > 1)
                {
                    cols.append(',');
                    params.append(',');
                }
                cols.append(meta.getColumnName(i));
                params.append('?');
            }

            try (PreparedStatement insert = to.prepareStatement("INSERT INTO " + table + " (" + cols + ") VALUES (" + params + ")"))
            {
                int rows = 0;
                while (rs.next())
                {
                    for (int i = 1; i <= count; i++)
                    {
                        insert.setObject(i, rs.getObject(i));
                    }
                    insert.addBatch();
                    if (++rows % 1000 == 0) insert.executeBatch();
                }
                if (rows % 1000 != 0) insert.executeBatch(); // HSQLDB rejects an empty batch
            }
        }
    }

    /**
     * Continue the identity after the highest copied ID
     */
    private static void restartIdentity(Connection to, String table, String column) throws SQLException
    {
        try (Statement stmt = to.createStatement())
        {
            long next;
            try (ResultSet rs = stmt.executeQuery("SELECT MAX(" + column + ") FROM " + table))
            {
                rs.next();
                next = rs.getLong(1) + 1;
            }
            stmt.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " RESTART WITH " + next);
        }
    }

    private static boolean hasTable(Connection conn, String table) throws SQLException
    {
        try (ResultSet tables = conn.getMetaData().getTables(null, null, table, null))
        {
            return tables.next();
        }
    }

    /**
     * Row count and max ID of each table
     */
    static Map<String, Long> getStats(Connection conn) throws SQLException
    {
        Map<String, Long> stats = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement())
        {
            for (Map.Entry<String, String> table : TABLES.entrySet())
            {
                if (!hasTable(conn, table.getKey())) continue;

                String id = table.getValue();
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*)" + (id == null ? "" : ", MAX(" + id + ")") +
                                                      " FROM " + table.getKey()))
                {
                    rs.next();
                    stats.put(table.getKey() + ".count", rs.getLong(1));
                    if (id != null) stats.put(table.getKey() + ".max", rs.getLong(2));
                }
            }
        }
        return stats;
    }

    /**
     * Names of databases whose .properties says HSQLDB 1.8
     */
    private static List<String> getOldDatabaseNames(File dbDir)
    {
        List<String> names = new ArrayList<>();
        File[] files = dbDir.listFiles((_, name) -> name.startsWith("poker") && name.endsWith(".properties"));
        if (files == null) return names;

        for (File file : files)
        {
            Properties props = new Properties();
            try (InputStream in = new FileInputStream(file))
            {
                props.load(in);
            }
            catch (IOException e)
            {
                logger.warn("Unable to read {}", file, e);
                continue;
            }

            if (props.getProperty("version", "").startsWith("1.8"))
            {
                String name = file.getName();
                names.add(name.substring(0, name.length() - ".properties".length()));
            }
        }
        Collections.sort(names);
        return names;
    }

    private static File[] getFiles(String name, File dir)
    {
        File[] files = dir.listFiles((d, n) -> n.startsWith(name + ".") && new File(d, n).isFile());
        return files == null ? new File[0] : files;
    }

    private static void copyFiles(String name, File from, File to) throws IOException
    {
        for (File file : getFiles(name, from))
        {
            // a copied lock file would make the copy look in use
            if (file.getName().endsWith(".lck")) continue;
            Files.copy(file.toPath(), new File(to, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void moveFiles(String name, File from, File to) throws IOException
    {
        for (File file : getFiles(name, from))
        {
            Files.move(file.toPath(), new File(to, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteFiles(String name, File dir)
    {
        for (File file : getFiles(name, dir))
        {
            if (!file.delete()) logger.warn("Unable to delete {}", file);
        }
    }

    private static void deleteDir(File dir)
    {
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isDirectory()) deleteDir(file);
                else if (!file.delete()) logger.warn("Unable to delete {}", file);
            }
        }
        if (dir.exists() && !dir.delete()) logger.warn("Unable to delete {}", dir);
    }

    /**
     * HSQLDB 1.8, loaded from the bundled jar in its own class loader.  The parent is the platform
     * class loader, so it can't see (or be seen by) the current HSQLDB.  The driver is called directly
     * rather than through DriverManager, which won't hand it to code in the application class loader.
     */
    static class Hsqldb18 implements Closeable
    {
        private final File jar_;
        private final URLClassLoader loader_;
        private final Driver driver_;

        Hsqldb18() throws IOException
        {
            jar_ = File.createTempFile("hsqldb-1.8.0.10-", ".jar");
            jar_.deleteOnExit();
            try (InputStream in = PokerDatabaseMigrator.class.getResourceAsStream(HSQLDB18_RESOURCE))
            {
                if (in == null) throw new FileNotFoundException("Missing resource " + HSQLDB18_RESOURCE);
                Files.copy(in, jar_.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            loader_ = new URLClassLoader(new URL[]{jar_.toURI().toURL()}, ClassLoader.getPlatformClassLoader());
            try
            {
                driver_ = (Driver) Class.forName(HSQLDB18_DRIVER, true, loader_).getDeclaredConstructor().newInstance();
            }
            catch (ReflectiveOperationException e)
            {
                close();
                throw new IOException("Unable to load HSQLDB 1.8", e);
            }
        }

        /**
         * Connect to an existing database
         */
        Connection connect(File db) throws SQLException
        {
            return connect(db, true);
        }

        /**
         * Connect, optionally creating the database (tests)
         */
        Connection connect(File db, boolean ifExists) throws SQLException
        {
            Properties props = new Properties();
            props.setProperty("user", "sa");
            props.setProperty("password", "");
            Connection conn = driver_.connect(URL_PREFIX + db.getAbsolutePath() + SHUTDOWN_ON_CLOSE +
                                              (ifExists ? ";ifexists=true" : ""), props);
            if (conn == null) throw new SQLException("HSQLDB 1.8 driver refused " + db);
            return conn;
        }

        public void close() throws IOException
        {
            try
            {
                loader_.close();
            }
            finally
            {
                if (!jar_.delete()) logger.debug("Unable to delete {}", jar_);
            }
        }
    }
}
