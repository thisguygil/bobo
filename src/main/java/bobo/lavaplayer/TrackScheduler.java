package bobo.lavaplayer;

import bobo.commands.voice.music.LoopCommand;
import bobo.commands.voice.music.TTSCommand;
import bobo.utils.TrackRecord;
import bobo.utils.TrackType;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    public final AudioPlayer player;
    public BlockingDeque<TrackRecord> queue;
    public TrackRecord currentTrack;
    public TrackRecord previousTrack;
    public LoopCommand.looping looping;

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingDeque<>();
        this.looping = LoopCommand.looping.NONE;
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     * @param channel The channel to send messages to
     * @param trackType The type of track to queue
     */
    public void queue(AudioTrack track, Member member, MessageChannel channel, TrackType trackType) {
        TrackRecord oldTrack = this.currentTrack;
        this.currentTrack = new TrackRecord(track, member, channel, trackType);
        if (!this.player.startTrack(track, true)) {
            this.queue.add(new TrackRecord(track, member, channel, trackType));
            this.currentTrack = oldTrack;
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        this.previousTrack = this.currentTrack;
        switch (this.looping) {
            case NONE -> this.currentTrack = this.queue.poll();
            case TRACK -> this.currentTrack = new TrackRecord(currentTrack.track().makeClone(), currentTrack.member(), currentTrack.channel(), currentTrack.trackType());
            case QUEUE -> {
                this.queue.add(new TrackRecord(currentTrack.track().makeClone(), currentTrack.member(), currentTrack.channel(), currentTrack.trackType()));
                this.currentTrack = this.queue.poll();
            }
        }
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
            switch (this.looping) {
                case NONE -> {
                    switch (currentTrack.trackType()) {
                        case TRACK, FILE -> nextTrack();
                        case TTS -> {
                            nextTrack();
                            TTSCommand.removeTTSMessage(track);
                        }
                    }
                }
                case TRACK, QUEUE -> nextTrack();
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
        if (this.looping != LoopCommand.looping.NONE) {
            this.looping = LoopCommand.looping.NONE;
        }
        if (currentTrack.trackType() == TrackType.TTS) {
            TTSCommand.removeTTSMessage(track);
        }
    }
}