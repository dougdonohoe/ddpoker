# DD Poker Testing Notes

This page covers manual testing of DD Poker and the tools that make it easier.  For
building, running and setting up the servers and database, see [README-DEV.md](README-DEV.md).

**NOTE**: all commands below assume you have sourced `ddpoker.rc` and are in the root of the
`ddpoker` repository.

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
  debug flag gets around that.  Without this flag, `-key` is silently ignored.

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
