package bobo.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    /**
     * A pair of an audio track and the channel it was queued in
     */
    public record TrackChannelPair(AudioTrack track, MessageChannel channel, boolean tts) {}

    public final AudioPlayer player;
    public BlockingQueue<TrackChannelPair> queue;
    public TrackChannelPair currentTrack;
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
     * @param tts Whether the track is a tts message
     */
    public void queue(AudioTrack track, MessageChannel channel, boolean tts) {
        TrackChannelPair oldTrack = this.currentTrack;
        if (tts) {
            this.currentTrack = new TrackChannelPair(track, channel, true);
            if (!this.player.startTrack(track, false)) {
                this.queue.add(new TrackChannelPair(track, channel, true));
                this.currentTrack = oldTrack;
            }
        } else {
            this.currentTrack = new TrackChannelPair(track, channel, false);
            if (!this.player.startTrack(track, true)) {
                this.queue.add(new TrackChannelPair(track, channel, false));
                this.currentTrack = oldTrack;
            }
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
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
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            if (this.looping) {
                this.player.startTrack(track.makeClone(), false);
            } else {
                nextTrack();
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