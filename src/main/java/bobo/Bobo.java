package bobo;

import bobo.command.commands.ChatCommand;
import bobo.command.commands.SetActivityCommand;
import com.github.ygimenez.model.PaginatorBuilder;
import com.theokanning.openai.service.OpenAiService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Bobo {
    private static JDA jda;
    private static final OpenAiService service = new OpenAiService(Config.get("OPENAI_API_KEY"));

    public static void main(String[] args) {
        jda = JDABuilder.createDefault(Config.get("TOKEN"))
                .addEventListeners(new Listener())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.updateCommands()
                .addCommands(
                        // Message commands
                        Commands.slash("help", "Shows the list of commands or gets info on a specific command.")
                                .addOption(STRING, "command", "Command to explain", false),
                        Commands.slash("set-activity", "Sets bobo's activity with specified type (playing, streaming, listening, watching, competing).")
                                .addOption(STRING, "type", "Activity type to set (playing, streaming, listening, watching, competing)", true)
                                .addOption(STRING, "activity", "Activity to set", true),
                        Commands.slash("search", "Searches given query on Google.")
                                .addOption(STRING, "query", "What to search", true),
                        Commands.slash("chat", "Uses OpenAI to generate a response to the given prompt.")
                                .addOption(STRING, "prompt", "Prompt to respond to", true),
                        Commands.slash("chat-reset", "Resets the current OpenAI chat conversation."),
                        Commands.slash("ai-image", "Uses OpenAI to generate an image of the given prompt.")
                                .addOption(STRING, "prompt", "Image to generate", true),
                        Commands.slash("say", "Make bobo say what you tell it to.")
                                .addOption(STRING, "content", "What bobo should say", true),
                        Commands.slash("get-quote", "Gets a random quote from #boquafiquotes."),
                        Commands.slash("steelix", "steelix"),

                        // Voice commands
                        Commands.slash("join", "Joins the voice channel."),
                        Commands.slash("leave", "Leaves the voice channel."),

                        // Music commands
                        Commands.slash("play", "Joins the voice channel and plays given/searched YouTube link/query.")
                                .addOption(STRING, "track", "YouTube link/query to play/search", true),
                        Commands.slash("play-file", "Joins the voice channel and plays audio from attached audio/video file.")
                                .addOption(ATTACHMENT, "file", "Audio file to play", true),
                        Commands.slash("pause", "Pauses the currently playing track."),
                        Commands.slash("resume", "Resumes the currently paused track."),
                        Commands.slash("now-playing", "Shows the currently playing track."),
                        Commands.slash("queue", "Shows the currently queued tracks."),
                        Commands.slash("loop", "Loop the currently playing track."),
                        Commands.slash("shuffle", "Shuffles the current queue (except for the currently playing track)."),
                        Commands.slash("skip", "Skips the current track."),
                        Commands.slash("remove", "Removes track at given position in the queue.")
                                .addOption(INTEGER, "position", "What position in the queue to remove the track from", true),
                        Commands.slash("clear", "Clears queue and stops current track.")
                )
                .queue();

        ChatCommand.initializeMessages();

        try {
            SetActivityCommand.setActivity();
            PaginatorBuilder.createPaginator(jda)
                    .shouldRemoveOnReact(false)
                    .shouldEventLock(false)
                    .setDeleteOnCancel(true)
                    .activate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the JDA instance.
     *
     * @return JDA instance
     */
    public static JDA getJDA() {
        return jda;
    }

    /**
     * Gets the OpenAI service instance.
     *
     * @return OpenAI service instance
     */
    public static OpenAiService getService() {
        return service;
    }
}