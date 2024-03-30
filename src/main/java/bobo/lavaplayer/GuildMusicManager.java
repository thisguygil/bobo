package bobo.lavaplayer;

import bobo.commands.voice.music.LoopCommand;
import bobo.commands.voice.music.TTSCommand;
import bobo.utils.Spotify;
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
import se.michaelthelin.spotify.SpotifyApi;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

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
                            .setAuthor(scheduler.looping == LoopCommand.looping.TRACK ? "Now Looping" : "Now Playing")
                            .setColor(Color.red);

                    switch (record.trackType()) {
                        case TRACK -> {
                            embed.setTitle(title, uri)
                                    .addField("Author", trackInfo.author, true)
                                    .setFooter(TimeFormat.formatTime((track.getDuration())));

                            // Get the YouTube thumbnail or Spotify album cover
                            try {
                                String spotifyRegex = "^(https?://)?open.spotify.com/.*";
                                if (YouTubeUtil.isYouTubeUrl(uri)) {
                                    // Get the thumbnail
                                    BufferedImage image = YouTubeUtil.getThumbnailImage(uri);
                                    if (image == null) {
                                        channel.sendMessageEmbeds(embed.build()).queue();
                                        return;
                                    }
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    ImageIO.write(image, "jpg", outputStream);
                                    embed.setThumbnail("attachment://thumbnail.jpg");
                                    channel.sendFiles(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg"))
                                            .setEmbeds(embed.build()).queue();
                                } else if (uri.matches(spotifyRegex)) {
                                    // Get the album cover
                                    SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                                    String id = uri.split("/")[uri.split("/").length - 1];
                                    String imageUrl = spotifyApi.getTrack(id).build().execute().getAlbum().getImages()[0].getUrl();
                                    embed.setThumbnail(imageUrl);
                                    channel.sendMessageEmbeds(embed.build()).queue();
                                } else {
                                    channel.sendMessageEmbeds(embed.build()).queue();
                                }
                            } catch (Exception e) {
                                channel.sendMessageEmbeds(embed.build()).queue();
                                e.printStackTrace();
                            }
                        }
                        case FILE -> {
                            embed.setTitle(title, uri)
                                    .addField("Author", trackInfo.author, true)
                                    .setFooter(TimeFormat.formatTime((track.getDuration())));
                            channel.sendMessageEmbeds(embed.build()).queue();
                        }
                        case TTS -> {
                            embed.setTitle("TTS Message")
                                    .setDescription(TTSCommand.getTTSMessage(track));
                            channel.sendMessageEmbeds(embed.build()).queue();
                        }
                    }
                }
            }
        });
    }
}