# List of Goals for Bobo
## New Features
* Add normal search to Google command
  * Currently only supports image search
* Add Apple Music source and [Apple Music API](https://developer.apple.com/documentation/applemusicapi/) integration
  * Not currently feasible as the API requires a paid ($99 per year) Apple Developer account to access
## Rewrites
* Add [vision capabilities](https://platform.openai.com/docs/guides/vision) to the chat command
* Enable the bot to be able to be added as a [user app](https://discord.com/developers/docs/tutorials/developing-a-user-installable-app)
  * Currently a normal app, but only usable in servers (as opposed to servers and the DM with the bot) as all commands are set to guild-only
  * Allows users to use the bot in any server or DM, even those the bot is not a member of
* Switch Last.fm commands to subcommands/command choices of one Last.fm-specific command, such as `/fm`, since the current command names aren't enough to convey that Last.fm is needed
* Add a FileAppender to the logback configuration
  * Currently only logs to the console which is fine, but it would be useful to have logs saved to files for later reference
* Migration to [Lavalink](https://lavalink.dev/index.html) for music playback
  * More efficient and scalable than [Lavaplayer](https://github.com/lavalink-devs/lavaplayer) (currently using) and has more features, but is more complex to set up
  * The current music commands are tightly coupled with Lavaplayer
* Add more asynchronous command processing
  * Can currently only process one command at a time, a problem if multiple commands are being used at the same time
    * Commands are dropped if they aren't acknowledged in time (3 seconds)
  * Moving the reply deferral (which extends the 3-second timer) outside the command's handle method would mitigate the issue of commands being dropped, but wouldn't help with the bigger issue - still single-threaded (with some exceptions) and needs to wait for the previous command to finish execution to continue
* Merge command types (slash, message) into one command system
  * Would allow for more flexibility in command handling and able to use the same command's code for both types