#!/usr/bin/env bash
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
# DD Poker - Source Code
# Copyright (c) 2003-2026 Doug Donohoe
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# For the full License text, please see the LICENSE.txt file
# in the root directory of this project.
#
# The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
# graphics, text, and documentation found in this repository (including but not
# limited to written documentation, website content, and marketing materials)
# are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
# 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
# without explicit written permission for any uses not covered by this License.
# For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
# in the root directory of this project.
#
# For inquiries regarding commercial licensing of this source code or
# the use of names, logos, images, text, or other assets, please contact
# doug [at] donohoe [dot] info.
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

# Tool to run SqlTool so can run simple queries against a hsqldb.
# hsqldb.sh [database] [sql]
#  [sql], if given, is run and the tool exits, instead of prompting
#  [database] is any of the database's files, the path without an extension, or a jdbc url:
#  ~/.dd-poker3/save/db/poker-2-1304257217.script
#  ~/.dd-poker3/save/db/poker-2-1304257217
#  jdbc:hsqldb:file:/var/folders/.../poker-database-test/db/poker-1-1088779314

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $(basename "$0") [database file, path without extension, or jdbc url] [sql to run]"
  exit 1
fi

case "$1" in
  jdbc:*)
    JDBC=$1
    ;;
  *)
    DB=$1
    # strip any of the files' extensions (a test database's .tmp directory too)
    for EXT in data properties script log lck backup tmp; do
      DB=${DB%.$EXT}
    done
    DIR=$(cd "$(dirname "$DB")" 2>/dev/null && pwd) || { echo "No such directory: $(dirname "$DB")"; exit 1; }
    DB=$DIR/$(basename "$DB")
    # hsqldb silently creates an empty database if none exists, so check first
    if [[ ! -f $DB.properties && ! -f $DB.script ]]; then
      echo "No database at $DB (expected $DB.properties or $DB.script)"
      exit 1
    fi
    # shutdown=true closes the database cleanly on exit, so it doesn't leave .log/.tmp behind
    JDBC="jdbc:hsqldb:file:$DB;shutdown=true"
    ;;
esac

echo "Connecting to $JDBC"
SQL=()
# SqlTool needs each statement terminated, and rolls back on exit without autocommit
[[ -n $2 ]] && SQL=(--autoCommit --sql "${2%;};")
VERSION=2.7.4
HSQLDB=~/.m2/repository/org/hsqldb/hsqldb/$VERSION/hsqldb-$VERSION.jar
SQLTOOL=~/.m2/repository/org/hsqldb/sqltool/$VERSION/sqltool-$VERSION.jar
# The databases define SQL functions backed by PokerDatabaseProcs, so it has to be on the
# classpath (and allowed, as PokerDatabase does) for the database to open at all
POKER=${WORK}/ddpoker/code/poker/target/classes
java -Dhsqldb.method_class_names='com.donohoedigital.games.poker.PokerDatabaseProcs.*' \
     -cp "$HSQLDB:$SQLTOOL:$POKER" org.hsqldb.cmdline.SqlTool --inlineRc="url=$JDBC,user=sa,password=" "${SQL[@]}"