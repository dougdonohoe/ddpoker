#!/bin/bash
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

export MYSQL_PWD='d@t@b@s3'
MYSQL="mysql poker -h 127.0.0.1 -u root "

if [ "$1" = "" ]; then
 echo "Usage: load_backup.sh [dir containing backups]"
 exit 1
fi

cd $1

ORDER="banned_key upgraded_key wan_history wan_game wan_profile registration"
#ORDER="banned_key upgraded_key wan_history wan_game wan_profile"

# drop/create tables
for file in $ORDER; do
	echo "Processing $file.sql..."
    cat $file.sql | $MYSQL
done

# import data (assumes docker where /var/lib/mysql-files/ is mounted to dir files are in)
for file in $ORDER; do
	echo "Processing $file.txt..." 
    echo "load data infile '/var/lib/mysql-files/$file.txt' replace into table $file; show warnings;" | $MYSQL
done
