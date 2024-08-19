# List of Goals for Bobo
## New Features
* Add normal search to Google command
  * Currently only supports image search
* Add Apple Music source and [Apple Music API](https://developer.apple.com/documentation/applemusicapi/) integration
  * Not currently feasible as the API requires a paid ($99 per year) Apple Developer account to access
## Rewrites
* Add [GPT-4o](https://platform.openai.com/docs/models/gpt-4o) to the chat command (it can be used with no rewrite, but won't have vision capabilities, the main reason to switch)
  * Not available in the [OpenAI Java API Wrapper](https://github.com/TheoKanning/openai-java/), and since it's now archived, it will never be added, so I'll have to switch to another wrapper or directly use the API with the HTTP client
* Redo search command to not have so many subcommands, and possibly rename command
  * With YouTube, Spotify, SoundCloud all having subcommands for tracks, playlists, and albums, it's gotten to be too much
  * Adding more music sources would make it even worse, so it's bad for extensibility
  * Command name doesn't convey that it's a music command
* Enable the bot to be able to be added as a [user app](https://discord.com/developers/docs/tutorials/developing-a-user-installable-app)
  * Currently a normal app, but only usable in servers (as opposed to servers and the DM with the bot) as all commands are set to guild-only
  * Allows users to use the bot in any server or DM, even those the bot is not a member of
  * Needs permission checks to be implemented first
* Switch Last.fm commands to subcommands of one Last.fm-specific command, such as `/fm`, since the current command names aren't enough to convey that Last.fm is needed
* Add proper logging to command usage and errors
  * Currently only prints the stack trace for errors, and has no logging for command uses
  * Every class will need to have its own log handling
* Migration to [Lavalink](https://lavalink.dev/index.html) for music playback
  * More efficient and scalable than [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) (currently using) and has more features, but is more complex to set up
  * The current music commands are tightly coupled with Lavaplayer
* Add more asynchronous command processing
  * Can currently only process one command at a time, a problem if multiple commands are being used at the same time
    * Commands are dropped if they aren't acknowledged in time (3 seconds)
  * Moving the reply deferral (which extends the 3-second timer) outside the command's handle method would mitigate the issue of commands being dropped, but wouldn't help with the bigger issue - still single-threaded (with some exceptions) and needs to wait for the previous command to finish execution to continue
* Uncouple commands from slash commands
  * Would allow for more flexible code, message commands and aliases, etc.