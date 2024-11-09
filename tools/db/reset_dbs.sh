#!/bin/bash
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
# DD Poker - Source Code
# Copyright (c) 2003-2024 Doug Donohoe
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

set -euo pipefail

# Run from this dir
SCRIPTDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPTDIR"

DATABASE=$1
export MYSQL_PWD='d@t@b@s3'

if [ -z "$DATABASE" ];
then
    echo "Must supply a database name"
    exit 1
fi

if [[ "$DATABASE" != "poker" && "$DATABASE" != "pokertest" ]];
then
    echo "Database name must be 'poker' or 'pokertest'"
    exit 1
fi

echo "Dropping poker database named '$DATABASE'..."
exists=$(mysql -h 127.0.0.1 -u root --force -N << __END__
SELECT COUNT(*) as count
    FROM INFORMATION_SCHEMA.SCHEMATA
    WHERE SCHEMA_NAME = '$DATABASE'
__END__
)
if [[ "$exists" == "1" ]]; then
  sed -e "s/DBNAME/$DATABASE/g" drop_dbs.sql | mysql -h 127.0.0.1 -u root --force
fi
echo

echo "Creating poker database named '$DATABASE'..."
sed -e "s/DBNAME/$DATABASE/g" create_dbs.sql | mysql -h 127.0.0.1 -u root --force
echo

echo "Creating tables on poker database named '$DATABASE'..."
sed -e "s/DBNAME/$DATABASE/g" create_tables.sql | mysql -h 127.0.0.1 -u root --force
echo

echo "Done."
