# Bobo

The greatest Discord bot on the planet (don't fact-check that).

## Features
* Playing music in voice channels from supported sources (see below)
* Clipping audio from voice channels
* Text-to-speech with [Flowery TTS](https://flowery.pw/) in voice channels
* OpenAI integration
  * Chat with AI with [ChatGPT](https://chat.openai.com/)
  * AI-generate images with [DALL-E 3](https://openai.com/dall-e-3)
* Last.fm integration
  * Get currently playing Spotify track information
* Search Google for images
* Get Fortnite stats, daily shop updates, and maps with [Fortnite-API](https://fortnite-api.com/)

## Supported sources and formats for music
Bobo supports all the web sources and file formats supported by [Lavaplayer](https://github.com/lavalink-devs/lavaplayer), as well as Spotify supported by [LavaSrc](https://github.com/topi314/LavaSrc)

## Commands
### Bot Owner commands
* `/restart` - Restarts Bobo
* `/set-activity` - Sets Bobo's activity/status
    * Subcommands:
    * `custom <status>` - Sets Bobo's status to `<status>`
    * `playing <activity>` - Sets Bobo's activity to `playing <activity>`
    * `streaming <activity> <url>` - Sets Bobo's activity to `streaming <activity>` with the stream URL `<url>`
    * `listening <activity>` - Sets Bobo's activity to `listening to <activity>`
    * `watching <activity>` - Sets Bobo's activity to `watching <activity>`
    * `competing <activity>` - Sets Bobo's activity to `competing in <activity>`

### Server Admin commands
* `/say <message>` - Makes Bobo say `<message>` in the current channel
* `/config` - Configures the server
  * Subcommands:
  * `clips <channel-id>` - Sets `<channel-id>` as the channel to send clips to (no input defaults to current channel)
  * `quotes <channel-id>` - Sets `<channel-id>` to send quotes to (no input defaults to current channel)
  * `fortnite-shop <channel-id>` - Sets `<channel-id>` to send Fortnite shop updates to (no input defaults to current channel)
  * Subcommand Groups:
    * `reset`
      * Subcommands: `clips`, `quotes`, `fortnite-shop` (resets the channel to none)

### General commands
* `/help` - Shows the list of commands or gets info on a specific command
* `/google <query>` - Searches Google for `<query>` and returns the first 10 results
* `/get-quote` - Gets a random quote from the configured quotes channel
* `/fortnite` - Get info about Fortnite
  * Subcommands:
  * `shop` - Gets the current Fortnite shop
  * `news` - Gets the current Fortnite (Battle Royale) news
  * `stats <username>` - Gets the stats of `<username>` in Fortnite
  * `map` - Gets the current Fortnite map

### Last.fm commands
* `/fmlogin` - Logs into Last.fm
* `/track <track>` - Gets information about a given track on Last.fm. No input defaults to last played track

### AI commands
* `/chat` - Opens a thread to chat with ChatGPT
* `/image` - Generates an image DALL-E 3

### Voice commands
* `/join` - Joins the voice channel you are in
* `/leave` - Leaves the voice channel
* `/mute` - Mutes/unmutes Bobo in the voice channel
* `/deafen` - Deafens/undeafens Bobo in the voice channel (Bobo will not be able to clip you)
* `/clip` - Clips the last 30 seconds of audio from the voice channel you are in

### Music commands
* `/play` - Plays a track in the voice channel you are in
    * Subcommands:
    * `track <url/query>` - Plays given YouTube `<url>` or the first track result from `<query>`
    * `file <file>` - Plays the given `<file>` (must be a valid audio file as detailed above)
* `/tts <message>` - Plays `<message>` as text-to-speech for the voice channel.
* `/search` - Searches YouTube/Spotify for a track/playlist/album, and plays requested result (this differs from the search of `/play` in that it shows the first several results and allows you to choose one, in addition to allowing you to search YouTube playlists and Spotify tracks/playlists/albums)
    * Subcommand Groups:
    * `youtube` - Searches YouTube for a track/playlist
      * Subcommands:
      * `track <query>` - Searches YouTube for a track
      * `playlist <query>` - Searches YouTube for a playlist
    * `spotify` - Searches Spotify for a track/album/playlist
      * Subcommands:
      * `track <query>` - Searches Spotify for a track
      * `album <query>` - Searches Spotify for an album
      * `playlist <query>` - Searches Spotify for a playlist
* `/pause` - Pauses the current track
* `/resume` - Resumes the current track
* `/skip` - Skips the current track
* `/loop` - Loops the currently playing track or queue
  * Subcommands:
  * `track` - Loops the currently playing track
  * `queue` - Loops the entire queue
  * `off` - Turns looping off
* `/now-playing` - Shows the current track
* `/seek` - Seeks to specified position in the current track
  * Subcommands:
  * `forward <seconds>` - Seeks forward by `<seconds>` seconds
  * `backward <seconds>` - Seeks backward by `<seconds>` seconds
  * `position <position>` - Seeks to `<position>` in the current track
* `/queue` - Shows/manipulates the current queue
  * Subcommands:
  * `show` - Shows the current queue
  * `clear` - Clears the current queue
  * `remove <index>` - Removes the track at `<index>` from the queue
  * `shuffle` - Shuffles the queue