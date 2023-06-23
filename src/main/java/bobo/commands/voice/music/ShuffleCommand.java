package bobo.commands.voice.music;

import bobo.lavaplayer.TrackScheduler;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShuffleCommand extends AbstractMusic {
    /**
     * Creates a new shuffle command.
     */
    public ShuffleCommand() {
        super(Commands.slash("shuffle", "Shuffles the current queue (except for the currently playing track)."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        List<TrackScheduler.TrackChannelPair> trackList = new ArrayList<>();
        queue.drainTo(trackList);
        Collections.shuffle(trackList);
        queue.clear();
        queue.addAll(trackList);
        hook.editOriginal("Shuffled.").queue();
    }

    @Override
    public String getName() {
        return "shuffle";
    }
}
