package bobo.lavaplayer;

import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

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
     * Creates a player and a track scheduler.
     *
     * @param manager Audio player manager to use for creating the player.
     */
    public GuildMusicManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.player);
        this.player.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.player);

        player.addListener(event -> {
            if (event instanceof TrackStartEvent startEvent) {
                TrackScheduler.TrackChannelPair pair = scheduler.currentTrack;

                if (pair.track().equals(startEvent.track)) {
                    MessageChannel channel = pair.channel();
                    AudioTrackInfo info = startEvent.track.getInfo();

                    // Creates embedded message with track info
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor(scheduler.looping ? "Now Looping" : "Now Playing")
                            .setTitle(info.title, info.uri)
                            .setImage("attachment://thumbnail.jpg")
                            .setColor(Color.red)
                            .setFooter(TimeFormat.formatTime((startEvent.track.getDuration())));

                    // Sets image in embed to proper aspect ratio
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(info.uri)), "jpg", outputStream);
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
