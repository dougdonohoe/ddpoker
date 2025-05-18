#!/usr/bin/env bash
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
# DD Poker - Source Code
# Copyright (c) 2003-2025 Doug Donohoe
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
#
# Script to set dmg icon and volume icon on Install4j generated .dmg
# since we can't do it through the tool.
#

set -e

# Verify we have a version
VERSION=$1
if [[ -z "$VERSION" ]]; then
  echo "mac-set-icons-notarize.sh [version]"
  exit 1
fi

SCRIPTDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPTDIR"
DDHOME="$(git rev-parse --show-toplevel)"

SRC="$DDHOME/installer/builds/ddpoker${VERSION}.dmg"
BAK="/tmp/ddpoker${VERSION}.bak.dmg"
DST_RW="$DDHOME/installer/builds/ddpoker${VERSION}_rw.dmg"
DST_ALT="$DDHOME/installer/builds/ddpoker${VERSION}_alt.dmg"
DST_MNT="/Volumes/dd_poker_dst"

# make read/write copy of installer
rm -f "$DST_RW"
hdiutil convert "$SRC" -format UDRW -o "$DST_RW"

# Add 20000 to the size
input=$(hdiutil resize "$DST_RW" | tail -1)
size=$(echo "$input" | awk '{print $2}')
newsize=$((size + 50000))
echo "Increasing size from $size to $newsize..."
hdiutil resize -sectors $newsize "$DST_RW"

# Mount rw, unmounting if still mounted
if [[ -d "$DST_MNT" ]]; then
  hdiutil detach "$DST_MNT"
fi
hdiutil attach "$DST_RW" -mountpoint "$DST_MNT"

# Copy icon and set finder data
cp -p "${DDHOME}/installer/install4j/custom/ddpokerinstaller.icns" "${DST_MNT}/.VolumeIcon.icns"
SetFile -c icnC "${DST_MNT}/.VolumeIcon.icns"
SetFile -a C "${DST_MNT}"

# Output directory structure
echo
echo "Volume contents:"
ls -la "$DST_MNT"
echo

# Unmount
hdiutil detach "$DST_MNT"

# convert back to ro and remove rw
rm -f "$DST_ALT"
hdiutil convert "$DST_RW" -format UDBZ -o "$DST_ALT"
rm -rf "$DST_RW"

# attach icon to new dmg  (this apparently only works on local mac; doesn't stick after download,
# but keeping around since I like it locally and it was a pain to figure out)
TMP_ICN=/tmp/icons_copy.icns
TMP_RSRC=/tmp/icons_copy.rsrc
cp "${DDHOME}/installer/install4j/custom/ddpokerinstaller.icns" "$TMP_ICN"
sips -i "$TMP_ICN"
DeRez -only icns "$TMP_ICN" > "$TMP_RSRC"
Rez -append "$TMP_RSRC" -o "$DST_ALT"
SetFile -a C "$DST_ALT"

# Copy new one back over original, backing up original to /tmp
mv -v "$SRC" "$BAK"
mv -v "$DST_ALT" "$SRC"

# Sign and notarize new one
~/work/donohoe/ddpoker/mac-sign-notarize.sh "$SRC"
