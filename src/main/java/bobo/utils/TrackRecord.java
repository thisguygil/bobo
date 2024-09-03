package bobo.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

/**
 * A record containing an {@link AudioTrack} object, the {@link Member} that requested it, the {@link MessageChannel} that it was queued in, and the {@link TrackType} it is.
 */
public record TrackRecord(AudioTrack track, Member member, MessageChannel channel, TrackType trackType) {}