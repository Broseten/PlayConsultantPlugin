# PlayConsultantPlugin

A Paper plugin for collecting player comments with a megaphone item and comment markers.

## Configuration

The plugin creates `config.yml` on first run. The main values currently supported are:

- `comments.min-word-count` - minimum words required for a comment
- `comments.min-comment-distance` - minimum distance from the last comment before a new one is allowed
- `comments.creative-unlock-comment-count` - number of valid comments needed to unlock the creative key
- `commands.removecomment.search-radius` - search radius for the admin remove-comment command

## Commands

- `/megaphone` - gives the player a megaphone
- `/removecomment` - removes the nearest comment marker and its hologram
- `/reloadconfig` - reloads the plugin configuration from disk

## Reloading

After editing `config.yml`, run:

```text
/reloadconfig
```

or restart the server.

