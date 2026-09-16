# DD Poker Testing Notes

This page covers manual testing of DD Poker and the tools that make it easier.  For
building, running and setting up the servers and database, see [README-DEV.md](README-DEV.md).

**NOTE**: all commands below assume you have sourced `ddpoker.rc` and are in the root of the
`ddpoker` repository.

## Debug Settings

There are lots of `settings.debug.*` entries in the code which are used to make
development easier.  Typically, you put these in your
`config/poker/override/[username].properties` file, so they only are used by you
(see *Properties Files* in [README-DEV.md](README-DEV.md#properties-files)).

The defaults live in `code/pokerengine/src/main/resources/config/poker/common.properties`.
Flags are read through `DebugConfig.TESTING(...)`; the constants are defined in
`EngineConstants` (engine) and `PokerConstants` (poker), with a handful declared
inline at the point of use.

Several flags enable a keyboard shortcut - those are described in
[README-DEV.md - Keyboard Shortcuts](README-DEV.md#keyboard-shortcuts).

### Master Switch

```properties
# Master switch.  No other settings.debug.* flag has any effect unless this is
# on, because DebugConfig.TESTING() returns false whenever it is off.
#
# It also enables "Use UDP When Hosting (beta testing)" under
# Options -> Online -> Miscellaneous.  That checkbox is always shown, but without
# this flag it is forced off and disabled with a "reserved for DD Poker personnel"
# tooltip.  Hosting only picks the UDP game prefix when this flag is on as well,
# so both the flag and the checkbox are needed to host over UDP
settings.debug.enabled=true
```

### Engine Settings

```properties
# Add "Start"/"Stop" chat flood-test buttons to the chat panel, show the chat
# send controls even where chat is normally read-only, and enable the 'g' key
# on the base panel to force a garbage collection
settings.debug.performance=true

# Enable the Edit/Copy/Delete buttons in profile lists even for profiles that
# normally don't allow it
settings.debug.editprofile=true

# Force the main window to start at 800x600 (EngineConstants.TESTING_CHANGE_SIZE_*)
# and stop saving its size to preferences.  Defaults to true
settings.debug.changesize=true

# Log the begin and end of every servlet call (doGet, processMessage,
# returnMessage) with a sequence number
settings.debug.servlet=true

# Server skips sending profile emails and game invites.  The generated password
# is written to the log instead, so you can activate without a mail server
settings.debug.skipemail=true

# Log every peer-to-peer online message sent, received and replied to
settings.debug.p2p=true

# Time every JDBC query and log the elapsed time along with a stack trace
# trimmed to DD Poker frames
settings.debug.dbperf=true

# In game, log each repaint and draw a colored border around the area Swing is
# repainting
settings.debug.repaint=true

# Additional per-territory and per-game-piece repaint logging, on top of the above
settings.debug.repaint.details=true

# AI debugging: log a line at the start of each hand, plus each AI action and
# the AI's "improve" value for the current table (see V1Player and
# HoldemHand.addHistory).  Also enables the 'x' key, which swaps each seat's name
# for that player's hand strength.
#
# Note the hand strength and hand potential log sites this flag also guards are
# unreachable: HandStrength has a hard-coded DEBUG=false, and HandPotential's is
# only reached from PokerPlayer.getHandPotential(), which has no live callers
settings.debug.ai=true

# Log low-level UDP traffic (incoming, outgoing, resends, acks, timeouts, MTU).
# Ctrl-F12 (Cmd-F12 on Mac) toggles this at runtime
settings.debug.udp=true

# Log application-level UDP traffic, including lobby chat.  Ctrl-F11 (Cmd-F11 on
# Mac) toggles this at runtime
settings.debug.udp.app=true

# Open secondary windows as internal dialogs inside the main window instead of
# as separate top-level windows
settings.debug.no.external=true

# Let the -key command line argument replace the stored activation key.
# Defaults to true.  Without it, -key is ignored with a warning in the log
settings.debug.override.key=true

# Skip the startup check that exits when another client on the LAN has the same
# IP or activation key.  Required to run two clients on one machine
settings.debug.skip.dup.key.check=true

```

### Poker Settings

```properties
# Arm autopilot.  While it is running, isHumanControlled() is false for every
# player, so the AI plays the human's hand, and the pauses at new levels, color
# ups and community card deals are skipped.  F5 or F9 pauses and resumes it
settings.debug.autopilot=true

# Autopilot's running state - this is what F5/F9 toggles.  Set it here to start
# already running; otherwise autopilot starts armed but paused, and the game
# says so on startup
settings.debug.autopilot.on=true

# Human player makes decisions for AI players in game (useful for creating
# various scenarios, like all players go all-in)
settings.debug.dougcontrolsai=true

# Pause before each AI decision and wait for the 'n' key to step it forward
settings.debug.pauseai=true

# Auto-save once at the first pre-flop betting round, before any AI decisions
# are made, and enable the 's' key to save at any time without going through the
# save menu
settings.debug.fastsave=true

# AI calls every bet, never folds or raises
settings.debug.aialwayscalls=true

# Add "Players" and "Debug" tabs to the Advisor dialog and log the V2 AI's
# reasoning behind each recommendation
settings.debug.advisordebug=true

# Verbose output in the Advisor dashboard item
settings.debug.advisorverbose=true

# Log the rule engine's results for every AI decision
settings.debug.logai=true

# Add a hand weight grid to the Player Info dashboard item
settings.debug.handweightgrid=true

# Print info about each pot, including the pots at the end of a hand
settings.debug.pots=true

# Raise the tournament profile limits (max chips, max rebuy chips, max buy-in)
# from their normal values to 10,000,000
settings.debug.levels=true

# Add a "Test Case" button to the table, which writes out the current hand as a
# test case
settings.debug.testcase=true

# Allow "Change Blinds" in the table right-click menu (only at showdown, and
# known to have issues - testing only).  Defaults to true
settings.debug.changelevel=true

# Show the cheat popup items (peek at cards, etc.) in online games, where they
# are normally suppressed.  Defaults to false
settings.debug.cheatonline=true

# Let an online game start with a single human plus computer players.
# Defaults to true
settings.debug.singleplayeronline=true

# Turn off auto-deal in online games, so each hand has to be dealt manually
settings.debug.onlineautodealoff=true

# Remove the AI's artificial pause in online games so hands play out at full speed
settings.debug.onlineainowait=true

# Seat humans at different tables when testing with 2 humans and >10 players
# (or 3 humans and >20).  Defaults to true
settings.debug.onlinesplithumans=true

# Process all-computer tables the same as tables with humans, rather than
# fast-forwarding them
settings.debug.processallaitables=true

# Chat performance testing - adds the flood-test buttons and attaches padding
# data to each chat message
settings.debug.chat.perf=true
```

### Server Settings

```properties
# On server, when sending online profile email, always send to this address,
# which is useful for testing registrations with other emails
settings.debug.profile.email.override=true
settings.debug.profile.email.override.to=my-email@my-domain.com
```

### Website Settings

```properties
# Documentation mode - drops the interactive portal and admin pieces so
# generate-website can extract a static ddpoker.com (see README-DEV.md
# Appendix G).  Must be on before starting PokerJetty or pokerweb
settings.debug.docmode=true
```

### `ddmailer` Settings

These live in `config/ddmailer/override/[username].properties`, not the `poker`
config, and are read directly with `PropertyConfig` rather than through
`DebugConfig.TESTING()`.

```properties
# Master switch for the two settings below
settings.debug.enabled=true

# Stop after sending this many messages
settings.debug.limit=3

# Send every message to this address instead of the real recipient
settings.debug.testto=my-email@my-domain.com
```

## Release Checklist

When testing major changes, here's a checklist of things to manually
verify:

* Start MySQL (either in Docker or locally), then connect via `mysql`
  * `mysql -h 127.0.0.1 -D poker -u poker -pp0k3rdb!`
  * `mysql -h 127.0.0.1 -D pokertest -u pokertest -pp0k3rdb!`
* `mvn-package`
* Start server via `PokerServerMain` and `pokerserver`
* Start website via `PokerJetty` and `pokerweb`
* Build website Docker image and run via Docker
* Start game via `PokerMain` and `poker`
* With the server running
  * verify game can start an online game (adjust online settings using server's IP)
  * verify global *Online Lobby*
* Start game from Ubuntu Docker
* Build and start the game natively on Windows via `.\mvn` (see
  [README-DEV.md Appendix I](README-DEV.md#appendix-i-native-windows-and-powershell))
* Build `act` docker image and running `act-ddpoker` (remember to stop MySQL)

## Online Tests

Online testing on one machine means running two game clients side by side, each with its own
player profile and activation key, against a local `pokerserver`.

**1. Turn on these debug settings** in your `config/poker/override/[username].properties` file
(see *Properties Files* in [README-DEV.md](README-DEV.md#properties-files)):

```properties
settings.debug.enabled=                true
settings.debug.skip.dup.key.check=     true
settings.debug.override.key=           true
```

* `settings.debug.enabled` - master switch; no other `settings.debug.*` flag takes effect without it.
* `settings.debug.skip.dup.key.check` - on startup, a client that finds another client on the
  LAN with the same IP or key exits.  Two clients on one machine always share an IP, so this
  skips that check.
* `settings.debug.override.key` - lets `-key` replace the stored activation key.  Each client
  needs a different key, because joining a game rejects a key already at the table, and no
  debug flag gets around that.  Without this flag, `-key` is ignored (with a warning in the log).

**2. Create two activated test profiles** (once; see [`activateprofile`](#activateprofile--online-activation-without-email)
below).  This needs MySQL running:

```shell
activateprofile -name "Test Profile 1" -email test1@ddpoker.com -password password -create
activateprofile -name "Test Profile 2" -email test2@ddpoker.com -password password -create
```

**3. Start the server and both clients**, each in its own terminal:

```shell
pokerserver
player1
player2
```

`player1` and `player2` are aliases defined in `ddpoker.rc`.  Each runs `poker` with one of the
test profiles, a different activation key, and a window position, so the two clients sit side
by side instead of on top of each other:

```shell
alias player1='poker -key KEY-23-6569DDEF-258B-470E-8081-9CF251941638-25-4647 -profile "Test Profile 1" -x 25 -y 40'
alias player2='poker -key KEY-23-AEAC9471-EAB6-4FE8-8EB5-500707990851-84-0631 -profile "Test Profile 2" -x 900 -y 40'
```

If the clients aren't already pointed at your local server, set that under
_Options → Online → Public Online Servers_.  Both clients share the same preferences, so doing
it in one is enough.

A spare key, for a third client:

```text
KEY-23-6B38FBB4-340C-4ABC-9F18-AA6C77B5A1C4-50-9311
```

If you need more, mint one from a random GUID with `jshell` (after `mvn-package-notests`):

```shell
jshell --class-path "code/common/target/classes:$(cat code/common/target/classpath.txt)" /dev/stdin <<'JSH'
System.out.println(com.donohoedigital.config.Activation.createKeyFromGuid(23, java.util.UUID.randomUUID().toString().toUpperCase(), null))
/exit
JSH
```

`23` is the key prefix for version 3.  Leave the locale `null`: it is part of the key's hash,
and the game validates with no locale unless started with `-locale`.

## Testing Tools

### `activateprofile` — online activation without email

Joining the *Online Lobby* requires an activated online profile.  Normally that means creating
the profile in the game, waiting for the server to email a generated password, and entering it.
`activateprofile` skips all of that and creates activated profiles directly.  To set up two
players for online testing:

```shell
activateprofile -name "Test Profile 1" -email test1@ddpoker.com -password password -create
activateprofile -name "Test Profile 2" -email test2@ddpoker.com -password password -create
```

With `-create`, a profile that doesn't exist is created (using the next free
`profile.NN.dat` file, just as the game does), and one that already exists is skipped, so
running these again is harmless.  Names are limited to 15 characters, the same as in the game.

Without `-create`, the profile must already exist and its email and password are updated.
The name must match exactly; if it doesn't, the tool lists the profiles it found and changes
nothing.

```shell
activateprofile -name "Test Profile 2" -email other@ddpoker.com -password newpassword
```

Activation lives in two places, the lobby checks both, and the tool always updates both:

* **The local profile** in `~/.dd-poker3/save/profiles/profile.NN.dat`, handled by
  `ActivateProfile` (`poker` module).  Line 2 of the file holds the activated flag, email and
  password (`b+:s<email>:s<password>`).  The password is encrypted with a key derived from the
  profile name, so the file can't be edited by hand.
* **The `wan_profile` row** in the local `poker` database, handled by `ActivateOnlineProfile`
  (`pokerserver` module).  `ChatServer` rejects the lobby login unless this row exists, is
  activated and has the same password.  Rows the tool inserts get the license key
  `KEY-TEST-ACTIVATEPROFILE`; the lobby authenticates using the client's own key, so this
  doesn't need to match.

These are two Java programs because `PlayerProfile` lives in the Swing client and the server
code can't depend on it.  The script runs the local step first, so a mistyped name fails before
anything touches the database.  `-create` applies to each side on its own, so after a database
reset (`reset_dbs.sh poker`), rerunning the `-create` commands restores just the missing rows.

Since the `pokerserver` database is hardcoded to `poker` (see `app-context-gameserver.xml`),
the tool always writes to the local development database.  It needs MySQL running (see
[README-DEV.md Appendix A](README-DEV.md#appendix-a--database-via-docker)), but not the server.
