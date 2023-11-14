package bobo.lavaplayer;

import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * This class contains instances of AudioPlayer, TrackScheduler and AudioPlayerSendHandler, to manage them all in one place
 */
public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;

    /**
     * Creates a new GuildMusicManager, which contains instances of AudioPlayer, TrackScheduler and AudioPlayerSendHandler.
     * It also contains a listener for the AudioPlayer, which sends a message with info about the track when it starts.
     *
     * @param manager Audio player manager to use for creating the player.
     */
    public GuildMusicManager(@Nonnull AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.player);
        this.player.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.player);

        this.player.addListener(event -> {
            if (event instanceof TrackStartEvent startEvent) {
                TrackScheduler scheduler = this.scheduler;
                TrackScheduler.TrackChannelPair pair = scheduler.currentTrack;

                AudioTrack track = startEvent.track;
                if (pair.track().equals(track)) {
                    MessageChannel channel = pair.channel();
                    AudioTrackInfo trackInfo = track.getInfo();

                    // Creates embedded message with track info
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor(scheduler.looping ? "Now Looping" : "Now Playing")
                            .setTitle(trackInfo.title, trackInfo.uri)
                            .setImage("attachment://thumbnail.jpg")
                            .setColor(Color.red)
                            .setFooter(TimeFormat.formatTime((track.getDuration())));

                    // Sets image in embed to proper aspect ratio
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(trackInfo.uri)), "jpg", outputStream);
                        channel.sendFiles(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
                    } catch (IOException e) {
                        channel.sendMessageEmbeds(embed.build()).queue();
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
