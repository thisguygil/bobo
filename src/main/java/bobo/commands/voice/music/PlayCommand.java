package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.utils.YouTubeUtil;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.Objects;

public class PlayCommand extends AbstractMusic {
    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();
        JoinCommand.join(event);

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