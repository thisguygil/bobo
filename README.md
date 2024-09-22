# Bobo

The greatest Discord bot on the planet (don't fact-check that).

## Features
* Playing music in voice channels from supported sources (see below)
* Clipping audio from voice channels
* Text-to-speech with [Flowery TTS](https://flowery.pw/) in voice channels
* OpenAI integration with the [OpenAI API](https://github.com/sashirestela/simple-openai)
  * Chat with AI ([ChatGPT-4o](https://chatgpt.com/) with [Vision](https://platform.openai.com/docs/guides/vision))
  * AI-generate images with [DALL-E 3](https://openai.com/index/dall-e-3/)
* Last.fm integration with the [Last.fm API](https://www.last.fm/api)
  * Get currently playing Spotify track information
* Search Google for images with [Google Images](https://developers.google.com/custom-search/v1/overview)
* Get Fortnite stats, daily shop updates, and map information with the [Fortnite API](https://fortnite-api.com/)

## Supported audio sources and formats
Bobo supports all the web sources and file formats supported by [Lavaplayer](https://github.com/lavalink-devs/lavaplayer), as well as Spotify and Deezer supported by [LavaSrc](https://github.com/topi314/LavaSrc), with extra functionality from API integrations.

## Hosting
Bobo isn't currently publicly available to be invited to your server, but you can host your own instance by building the bot with Gradle and running it with your own environment variables.
<br>
An example file including all the required variables can be found in the repository's `.env.example` file.
<br>
I recommend using an SQL database such as MySQL to store the bot's data. It is not required for the bot to function, but it is used to store [Last.fm](https://www.last.fm/home) login information, server configurations, etc. Note that all queries are written for MySQL, so you may need to adjust them for other SQL databases.

## Commands
### Bot Owner commands
Can be used by the bot owner only (as message commands) - configured with the owner's [Discord user ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID) in the `.env` file
* `!restart` - Restarts Bobo
* `!set-activity` - Sets Bobo's activity/status
    * Subcommands:
    * `custom <status>` - Sets Bobo's status to `<status>`
    * `playing <activity>` - Sets Bobo's activity to `playing <activity>`
    * `streaming <activity> <url>` - Sets Bobo's activity to `streaming <activity>` with the stream URL `<url>`
    * `listening <activity>` - Sets Bobo's activity to `listening to <activity>`
    * `watching <activity>` - Sets Bobo's activity to `watching <activity>`
    * `competing <activity>` - Sets Bobo's activity to `competing in <activity>`

### Server Admin commands
Can be used by server admins only
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
* `/random` - Gets a random quote/clip from the respective configured channel
  * Subcommands:
  * `quote` - Gets a random quote
  * `clip` - Gets a random clip
* `/fortnite` - Get info about Fortnite
  * Subcommands:
  * `stats <username>` - Gets the stats of `<username>` in Fortnite
  * `info` - Get info about Fortnite
    * Choices:
    * `shop` - Gets the current Fortnite shop
    * `news` - Gets the current Fortnite (Battle Royale) news
    * `map` - Gets the current Fortnite map

### Last.fm commands
Requires the user to be logged into Last.fm
* `/fmlogin` - Logs into Last.fm
* `/fmlogout` - Logs out of Last.fm - to be used normally, or if the login becomes invalid/outdated (e.g. the user changes their username)
* `/track <track>` - Gets information about a given track on Last.fm. No input defaults to last played track
* `/album <album>` - Gets information about a given album on Last.fm. No input defaults to last played album
* `/artist <artist>` - Gets information about a given artist on Last.fm. No input defaults to last played artist

### AI commands
* `/tldr <minutes>` - Summarizes the recent conversation in the channel. `<minutes>` is the number of minutes to look back in the channel, if not provided, searches until a 5-minute gap is found
* `/chat` - Opens a thread to chat with ChatGPT
* `/image` - Generates an image with DALL-E 3

### Voice commands
Can be used in voice channels only
* `/join` - Joins the voice channel you are in
* `/leave` - Leaves the voice channel
* `/mute` - Mutes/unmutes Bobo in the voice channel
* `/deafen` - Deafens/undeafens Bobo in the voice channel (Bobo will not be able to clip you)
* `/clip` - Clips the last 30 seconds of audio from the voice channel you are in. The clip will also be sent to the configured clips channel, if one is set

### Music commands
Subset of voice commands
* `/play` - Plays a track in the voice channel you are in
    * Subcommands:
    * `track <url/query>` - Plays given YouTube `<url>` or the first track result from `<query>`
    * `file <file>` - Plays the given `<file>` (must be a valid audio file as detailed above)
* `/tts <message>` - Plays `<message>` as text-to-speech for the voice channel.
* `/search` - Searches a platform, and plays the requested result
    * Choices:
    * `youtube/spotify/soundcloud` - Search YouTube/Spotify/SoundCloud
    * `track/playlist/album` - Search for a track/playlist/album
* `/lyrics` - Get the lyrics of the currently playing track
* `/pause` - Pauses the current track
* `/resume` - Resumes the current track
* `/skip` - Skips the current track
* `/loop` - Loops the currently playing track or queue
  * Subcommands:
  * `track` - Loops the currently playing track
  * `queue` - Loops the entire queue
  * `off` - Turns looping off
* `/repeat` - Repeats the current/last-played track
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

## Development Plans
For a detailed list of planned features, improvements, and bug fixes, please refer to the [TODO.md](TODO.md) file.

## Terms of Service and Privacy Policy
Please refer to the [terms-of-service.md](terms-of-service.md) and [privacy-policy.md](privacy-policy.md) files for Bobo's Terms of Service and Privacy Policy, respectively.