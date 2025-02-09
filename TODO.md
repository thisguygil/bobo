# List of Goals for Bobo
## New Features
* Add Apple Music source and [Apple Music API](https://developer.apple.com/documentation/applemusicapi/) integration
  * Not currently feasible as the API requires a paid ($99 per year) Apple Developer account to access
* Enable the bot to be able to be added as a [user app](https://discord.com/developers/docs/tutorials/developing-a-user-installable-app)
  * Currently a normal app, but only usable in servers (as opposed to servers and the DM with the bot) as all commands are set to guild-only
  * Allows users to use the bot in any server or DM, even those the bot is not a member of
## Rewrites
* Add a FileAppender to the logback configuration
  * Currently only logs to the console which is fine, but it would be useful to have logs saved to files for later reference
* Migration to [Lavalink](https://lavalink.dev/index.html) for music playback
  * More efficient and scalable, with more features than [LavaPlayer](https://github.com/lavalink-devs/lavaplayer) (currently using), but more complex to set up
  * The current music commands are tightly coupled with LavaPlayer
* Add more asynchronous command processing
  * Can currently only process one command at a time, a problem if multiple long-running commands are being used at the same time