package bobo.lavaplayer;

import bobo.commands.ai.TTSCommand;
import bobo.utils.TimeFormat;
import bobo.utils.TrackChannelTypeRecord;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
                TrackChannelTypeRecord record = scheduler.currentTrack;

                AudioTrack track = startEvent.track;
                if (record.track().equals(track)) {
                    MessageChannel channel = record.channel();
                    AudioTrackInfo trackInfo = track.getInfo();
                    String title = trackInfo.title;
                    String uri = trackInfo.uri;
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor(scheduler.looping ? "Now Looping" : "Now Playing")
                            .setColor(Color.red)
                            .setFooter(TimeFormat.formatTime((track.getDuration())));

                    switch (record.trackType()) {
                        case TRACK -> {
                            embed.setTitle(title, uri)
                                    .setImage("attachment://thumbnail.jpg");

                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            try {
                                ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(uri)), "jpg", outputStream);
                                channel.sendFiles(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
                            } catch (IOException e) {
                                channel.sendMessageEmbeds(embed.build()).queue();
                                e.printStackTrace();
                            }
                        }
                        case FILE -> {
                            embed.setTitle(title, uri);
                            channel.sendMessageEmbeds(embed.build()).queue();
                        }
                        case TTS -> {
                            embed.setTitle("TTS Message")
                                    .setDescription(TTSCommand.getTTSMessage(Paths.get(uri).getFileName().toString()));
                            channel.sendMessageEmbeds(embed.build()).queue();
                        }
                    }
                }
            }
        });
    }
}