package bobo.lavaplayer;

import bobo.utils.TrackType;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    /**
     * A pair of an audio track and the channel it was queued in
     */
    public record TrackChannelTypeRecord(AudioTrack track, MessageChannel channel, TrackType trackType) {}

    public final AudioPlayer player;
    public BlockingQueue<TrackChannelTypeRecord> queue;
    public TrackChannelTypeRecord currentTrack;
    public boolean looping;

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.looping = false;
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     * @param channel The channel to send messages to
     * @param trackType The type of track to queue
     */
    public void queue(AudioTrack track, MessageChannel channel, TrackType trackType) {
        TrackChannelTypeRecord oldTrack = this.currentTrack;
        this.currentTrack = new TrackChannelTypeRecord(track, channel, trackType);
        if (!this.player.startTrack(track, true)) {
            this.queue.add(new TrackChannelTypeRecord(track, channel, trackType));
            this.currentTrack = oldTrack;
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        this.currentTrack = this.queue.poll();
        this.player.startTrack(currentTrack == null ? null : currentTrack.track(), false);
    }

    /**
     * Starts the next track (or loops current track) upon track completion
     *
     * @param player Audio player
     * @param track Audio track that ended
     * @param endReason The reason why the track stopped playing
     */
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, @Nonnull AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (this.looping) {
                this.player.startTrack(track.makeClone(), false);
            } else {
                nextTrack();
                String uri = track.getInfo().uri;
                if (uri.startsWith("tts-")) {
                    File file = new File(uri);
                    if (file.exists() && !file.delete()) {
                        System.err.println("Failed to delete TTS file: " + file.getName());
                    }
                }
            }
        }
    }

    /**
     * Sends a message to the channel if the track fails to start
     *
     * @param player Audio player
     * @param track Audio track that threw the exception
     * @param exception The exception thrown
     */
    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, @Nonnull FriendlyException exception) {
        currentTrack.channel().sendMessage("Failed to start track: **" + exception.getMessage() + "**").queue();
    }
}