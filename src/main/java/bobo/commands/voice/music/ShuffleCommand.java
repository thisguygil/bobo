package bobo.commands.voice.music;

import bobo.lavaplayer.TrackScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShuffleCommand extends AbstractMusic {
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
