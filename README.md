# Bobo

The greatest Discord bot on the planet.

## Features
* Playing music in voice channels from all Lavaplayer-supported sources (see below)
* Clipping audio from voice channels (can configure channel to receive all clips)
* Chat with AI (ChatGPT)
* AI-generate Images (DALL-E)
* Search Google for Images

## Supported sources and formats for music
Bobo supports the following web sources and file formats:
### Sources
* YouTube
* Spotify (through YouTube)
### Formats
* MP3
* FLAC
* WAV
* Matroska/WebM (AAC, Opus or Vorbis codecs)
* MP4/M4A (AAC codec)
* OGG streams (Opus, Vorbis and FLAC codecs)
* AAC streams
* Stream playlists (M3U and PLS)

## Commands
### Bot Owner commands
* `/set-activity` - Sets Bobo's activity/status
    * Subcommands:
    * `/set-activity custom <status>` - Sets Bobo's status to `<status>`
    * `/set-activity playing <activity>` - Sets Bobo's activity to `playing <activity>`
    * `/set-activity streaming <activity> <url>` - Sets Bobo's activity to `streaming <activity>` with the stream URL `<url>`
    * `/set-activity listening <activity>` - Sets Bobo's activity to `listening to <activity>`
    * `/set-activity watching <activity>` - Sets Bobo's activity to `watching <activity>`
    * `/set-activity competing <activity>` - Sets Bobo's activity to `competing in <activity>`

### Server Admin commands
* `/config` - Configures the server
  * Subcommands
  * `/config clips <channel-id>` - Sets the channel to send clips to (defaults to current channel)
* `/say <message>` - Makes Bobo say `<message>` in the current channel

### General commands
* `/help` - Shows the list of commands or gets info on a specific command
* `/search <query>` - Searches Google for `<query>` and returns the first 10 results
* `/get-quote` - Gets a random quote from the quotes channel

### AI commands
* `/chat` - Opens a thread to chat with Bobo (ChatGPT)
* `/ai-image` - Generates an image (DALL-E)

### Voice commands
* `/join` - Joins the voice channel you are in
* `/leave` - Leaves the voice channel
* `/deafen` - Deafens/undeafens Bobo in the voice channel (Bobo will not be able to clip you)
* `/clip` - Clips the last 30 seconds of audio from the voice channel you are in

### Music commands
* `/play` - Plays a track in the voice channel you are in
    * Subcommands:
    * `/play youtube track <url/query>` - Plays given YouTube `<url>` or the first track result from `<query>`
    * `/play youtube playlist <url/query>` - Plays given YouTube `<url>` or the first playlist result from `<query>`
    * `/play spotify track <url/query>` - Plays given Spotify `<url>` or the first track result from `<query>`
    * `/play spotify playlist <url/query>` - Plays given Spotify `<url>` or the first playlist result from `<query>`
    * `/play spotify album <url/query>` - Plays given Spotify `<url>` or the first album result from `<query>
    * `/play file <file>` - Plays the given `<file>` (must be a valid audio file as detailed above)
* `/pause` - Pauses the current song
* `/resume` - Resumes the current song
* `/skip` - Skips the current song
* `/loop` - Loops the current song
* `/now-playing` - Shows the current song
* `/queue` - Shows/manipulates the current queue
  * Subcommands:
  * `/queue show` - Shows the current queue
  * `/queue clear` - Clears the current queue
  * `/queue remove <index>` - Removes the song at `<index>` from the queue
  * `/queue shuffle` - Shuffles the queue