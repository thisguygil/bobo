# Bobo

The greatest Discord bot on the planet (don't fact-check that).

## Features
* Playing music in audio channels from supported sources (see below)
* Clipping audio from audio channels
* Text-to-speech with [Flowery TTS](https://flowery.pw/) in audio channels
* [OpenAI API](https://openai.com/api/) integration with the [Java API](https://github.com/openai/openai-java)
  * Chat with AI ([GPT-4.1 mini](https://openai.com/index/gpt-4-1/))
  * AI-generate images ([DALL-E 3](https://openai.com/index/dall-e-3/))
* Last.fm integration with the [Last.fm API](https://www.last.fm/api)
  * Get currently playing Spotify track information
  * Search for track, album, and artist information
* Search Google (and Google Images) with the [Google API](https://developers.google.com/custom-search/v1/overview)
* Get Fortnite stats, daily shop updates, and map information with the [Fortnite API](https://fortnite-api.com/)

## Supported formats for audio playback
Bobo supports all formats supported by [Lavaplayer](https://github.com/lavalink-devs/lavaplayer?tab=readme-ov-file#supported-formats), and Spotify supported by [LavaSrc](https://github.com/topi314/LavaSrc), with extra functionality for many sources from API integrations.

## Inviting to your server
Bobo isn't publicly available to be invited to your server, but you can host your own instance (see below).

## Hosting
You can run it on any platform with [Docker](https://www.docker.com/) installed by copying the [docker-compose.yml](docker-compose.yml) file, filling in the environment variables in the `.env` file, and using the commands from the tip below.

> [!TIP]  
> To start the bot, run: `docker-compose up -d`. To update, run: `docker-compose pull` to pull the latest image and `docker-compose up -d` to restart. If the bot is in audio channels, it's recommended to first run `docker-compose down` to stop it, and wait for it to go offline before restarting the container.

The Docker configuration also includes a [MySQL](https://www.mysql.com/) database for storing user/server data, so you don't need to set up a database separately (though you will need to add credentials in the `.env` file).

The Docker image for the bot used in the configuration can be found [here](https://hub.docker.com/repository/docker/thisguygil/bobo) on Docker Hub.

> [!IMPORTANT]  
> [.env.example](.env.example) is an example file including all the required environment variables. You can rename it to `.env` and fill in the values. It includes some default (non-sensitive) values, such as `!` as the message command prefix or `0` as the UTC offset (for time-related features), which you can change to your liking, but you must fill in the sensitive values such as the owner's [Discord user ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID), the database credentials, or API keys, for their respective features to work. It's recommended to fill in all variables to ensure all features work as intended.

## Commands
Most commands can be used as both slash commands and message commands, with some exceptions (noted where applicable).
> [!NOTE]  
> The prefix of `!` is shown here for message commands, but can be changed in the `.env` file.  
> Additionally, command aliases are available for some commands (when used as message commands).

### Privileged Commands

#### Bot Owner Commands
Can be used by the bot owner only (as message commands)
* `!restart` - Restarts Bobo
* `!set-activity` - Sets Bobo's activity/status
  * Subcommands:
    * `custom <status>` - Sets Bobo's status to `<status>`
    * `playing <activity>` - Sets Bobo's activity to `playing <activity>`
    * `streaming <activity> <url>` - Sets Bobo's activity to `streaming <activity>` with the stream URL `<url>`
    * `listening <activity>` - Sets Bobo's activity to `listening to <activity>`
    * `watching <activity>` - Sets Bobo's activity to `watching <activity>`
    * `competing <activity>` - Sets Bobo's activity to `competing in <activity>`
* `!dm <user-id> <message>` - Sends a direct message to a user specified by their Discord user ID
* `!sql <statement>` - Executes an SQL statement in the linked database
> [!CAUTION]  
> The `!sql` command is dangerous and should be used with caution, hence the elevated owner-only privilege. It can be used to modify the database directly, which can cause data loss, bugs, or corruption if used incorrectly. It can be invaluable for debugging or fixing issues, and most uses are likely to run read-only queries, which are safe. Regardless, only use this command if you know what you're doing.

#### Server Admin Commands
Can be used by server admins only (as slash commands)
> [!NOTE]  
> These commands may seem less privileged than Bot Owner Commands, but are not available to the bot owner (unless the bot owner is a server admin).
* `/say <message>` - Makes Bobo say `<message>` in the current channel
* `/config <setting> <channel>` - Configures the server
  * Settings:
    * `clips channel` - Sets the channel where audio clips will be sent to (when using `/clip`) and retrieved from (when using `/random clip`)
    * `quotes channel` - Sets the channel where quotes will be retrieved from (when using `/random quote`)
    * `Fortnite Shop channel` - Sets the channel where Fortnite Shop updates will be sent daily at 0:01 UTC
  * No channel input clears the setting

### Non-Privileged Commands
Can be used by anyone with proper permissions

#### General Commands
* `/help <command>` - Gets help for a specific command. No input defaults to a list of all commands
* `/random` - Gets a random quote/clip from the respective configured channel
  * Subcommands:
    * `quote` - Gets a random quote
    * `clip` - Gets a random clip
* `/google <subcommand> <query>` - Searches Google for `<query>`
  * Subcommands:
    * `search` - Searches given query on Google.
    * `images` - Searches given query on Google Images.
* `/fortnite` - Get info about Fortnite
  * Subcommands:
    * `stats <username>` - Gets the stats of `<username>` in Fortnite
    * `info` - Get info about Fortnite
      * Choices:
        * `shop` - Gets the current Fortnite shop
        * `news` - Gets the current Fortnite (Battle Royale) news
        * `map` - Gets the current Fortnite map
* `/fm` - Get your Last.fm info
  * Subcommands:
    * `auth` - Log in or out of Last.fm.
      * Choices:
        * `login` - Log in
        * `logout` - Log out
    * `info` - Get information about a track, album, or artist (no input defaults to last played).
      * Choices:
        * `track` - Get information about a track.
        * `album` - Get information about an album.
        * `artist` - Get information about an artist.
> [!NOTE]  
> Non-auth subcommands of `/fm` require users to be logged in to Last.fm (using `/fm auth login`).

#### AI Commands
* `/tldr <minutes>` - Summarizes the recent conversation in the channel. `<minutes>` is the number of minutes to look back in the channel. If not provided, searches until a 5-minute gap is found
* `/chat` - Opens a thread to chat with ChatGPT
* `/image` - Generates an image with DALL-E 3

#### Voice Commands
Can be used only while in an audio channel
* `/join` - Joins the audio channel you are in
* `/leave` - Leaves the audio channel
* `/mute` - Mutes/unmutes the bot in the audio channel
* `/deafen` - Deafens/undeafens the bot in the audio channel (it will not be able to clip you)
* `/clip` - Clips the last 30 seconds of audio from the audio channel you are in. The clip will also be sent to the configured clips channel, if one is set

#### Music Commands
Subset of voice commands (and so can only be used while in an audio channel)
* `/play` - Plays a track in the audio channel you are in (alias: `!p`)
  * Subcommands:
    * `track <url/query>` - Plays given YouTube `<url>` or the first track result from `<query>`
    * `file <file>` - Plays attached file
* `/tts <message>` - Plays `<message>` as text-to-speech for the audio channel.
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
* `/queue` - Shows/manipulates the current queue (alias: `!q`)
  * Subcommands:
    * `show` - Shows the current queue
    * `clear` - Clears the current queue (alias: `!clear` (rather than `!queue clear`))
    * `remove <index>` - Removes the track at `<index>` from the queue
    * `shuffle` - Shuffles the queue

## Development Plans
For a detailed list of planned features, improvements, and bug fixes, please refer to the [TODO.md](TODO.md) file.