package bobo;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Bobo {
    private Bobo() {
        JDABuilder.createDefault(Config.get("TOKEN"))
                .addEventListeners(new Listener())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.streaming("Splatoon 3", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .build()
                .updateCommands()
                .addCommands(
                        Commands.slash("help", "Shows the list of commands or gets info on a specific command.")
                                .addOption(STRING, "command", "Command to explain", false),
                        Commands.slash("say", "Make bobo say what you tell it to.")
                                .addOption(STRING, "content", "What the bot should say", true),
                        Commands.slash("getquote", "Sends a random quote from #boquafiquotes"),
                        Commands.slash("steelix", "steelix"),

                        // Music commands
                        Commands.slash("play", "Joins the voice channel and plays given track.")
                                .addOption(STRING, "track", "YouTube link/query to play or search", true),
                        Commands.slash("playfile", "Joins the voice channel and plays attached audio/video file.")
                                .addOption(ATTACHMENT, "file", "Audio file to play", true),
                        Commands.slash("pause", "Pauses the currently playing track"),
                        Commands.slash("resume", "Resumes the currently paused track"),
                        Commands.slash("nowplaying", "Shows the currently playing track."),
                        Commands.slash("queue", "Shows the currently queued tracks."),
                        Commands.slash("loop", "Loop the currently playing track."),
                        Commands.slash("skip", "Skips the current track."),
                        Commands.slash("remove", "Removes track at given position in the queue.")
                                .addOption(INTEGER, "position", "What position in the queue to remove the track from", true),
                        Commands.slash("clear", "Clears queue and stops current track.")
                )
                .queue();
    }

    public static void main(String[] args) {
        new Bobo();
    }
}