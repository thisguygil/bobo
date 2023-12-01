package bobo.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

/**
 * A record of an audio track, the channel it was queued in, and the type of track it is.
 */
public record TrackChannelTypeRecord(AudioTrack track, MessageChannel channel, TrackType trackType) {}