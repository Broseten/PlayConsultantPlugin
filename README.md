# PlayConsultantPlugin

A Paper plugin for collecting player comments with a megaphone item and comment markers.

## Configuration

The plugin creates `config.yml` on first run. The main values currently supported are:

- `comments.min-word-count` - minimum words required for a comment
- `comments.min-comment-distance` - minimum distance from the last comment before a new one is allowed
- `comments.creative-unlock-comment-count` - number of valid comments needed to unlock the creative key
- `commands.removecomment.search-radius` - search radius for the admin remove-comment command

## Commands

All commands are subcommands of `/playconsultant` (alias: `/pc`).

- `/pc megaphone` - Gives the player a megaphone item to start commenting.
- `/pc removecomment [radius]` - Removes the nearest comment marker and its hologram within an optional radius. (Requires `playconsultant.removecomment` permission or OP)
- `/pc reload` - Reloads the plugin configuration from disk. (Requires `playconsultant.reloadconfig` permission or OP)
- `/pc resetplayerdata <player>` - Resets a specific player's data, including comments count and assigned plot. (Requires `playconsultant.resetplayerdata` permission or OP)
- `/pc creativekey` - Gives the player a creative key to travel between worlds.
- `/pc cleanupcomments` - Cleans up orphaned comments and holograms. (OP only)
- `/pc grantreward <player>` - Grants a creative plot to a player and teleports them there, even if they haven't met the comment requirement. (Requires `playconsultant.grantreward` permission or OP)

## Reloading

After editing `config.yml`, run:

```text
/pc reload
```

or restart the server.