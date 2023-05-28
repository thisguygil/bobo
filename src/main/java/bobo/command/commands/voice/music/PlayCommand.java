package bobo.command.commands.voice.music;

import bobo.command.CommandInterface;
import bobo.command.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.URLValidator;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class PlayCommand implements CommandInterface {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        JoinCommand.join(event);

        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        String trackURL;
        if (!URLValidator.isValidURL(track)) {
            try {
                trackURL = YouTubeUtil.searchForVideo(track);
            } catch (Exception e) {
                event.getHook().editOriginal("Nothing found by **" + track + "**").queue();
                return;
            }
        } else {
            trackURL = track;
        }
        PlayerManager.getInstance().loadAndPlay(event, trackURL);
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp() {
        return """
                `/play`
                Joins the voice channel and plays given or searched YouTube link
                Usage: `/play <YouTube link/query>`""";
    }
}