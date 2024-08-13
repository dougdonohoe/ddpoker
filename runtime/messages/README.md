# DD Poker Server Messages

The `poker.html` and `poker-upgrade.html` files are read in `EngineServlet.java:afterConfigInit`.  The
client asks any messages on startup and the server picks one of these to send back (see `EngineServlet:checkDDmsg`
for logic).  It is essentially a way for the folks running the server to send a message to the client that
is displayed on startup.  A message is only sent back to the client if the timestamp is newer on the file
than the last time the client viewed it.

The naming convention is `[appname].html` and `[appname]-upgrade.html` reflecting the fact that the 
game engine can be used for multiple games.