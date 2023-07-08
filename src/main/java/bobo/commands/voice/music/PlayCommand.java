package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.Objects;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class PlayCommand extends AbstractMusic {
    /**
     * Creates a new play command.
     */
    public PlayCommand() {
        super(Commands.slash("play", "Joins the voice channel and plays given/searched YouTube link/query.")
                .addOption(STRING, "track", "YouTube link/query to play/search", true));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();
        if (!event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
            if (!JoinCommand.join(event)) {
                return;
            }
        }

        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        String trackURL;
        if ((new UrlValidator()).isValid(track)) {
            trackURL = track;
        } else {
            try {
                trackURL = YouTubeUtil.searchForVideo(track);
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + track + "**.").queue();
                e.printStackTrace();
                return;
            }
        }

        playerManager.loadAndPlay(event, trackURL);
    }

    @Override
    public String getName() {
        return "play";
    }
}