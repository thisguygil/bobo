package bobo.lavaplayer;

import bobo.commands.voice.music.LoopCommand;
import bobo.commands.voice.music.TTSCommand;
import bobo.utils.*;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import se.michaelthelin.spotify.SpotifyApi;

import javax.annotation.Nonnull;
import java.awt.*;

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

        // Listener for the AudioPlayer, which sends a message with info about the track when it starts
        this.player.addListener(event -> {
            if (event instanceof TrackStartEvent startEvent) {
                TrackScheduler scheduler = this.scheduler;
                TrackRecord record = scheduler.currentTrack;

                AudioTrack track = startEvent.track;
                if (record.track().equals(track)) {
                    MessageChannel channel = record.channel();
                    AudioTrackInfo trackInfo = track.getInfo();
                    String title = trackInfo.title;
                    String uri = trackInfo.uri;
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor("Now " + (trackInfo.isStream ? "Streaming" : (scheduler.looping == LoopCommand.looping.TRACK ? "Looping" : "Playing")))
                            .addField("Requested by", record.member().getAsMention(), true)
                            .setColor(Color.red);

                    switch (record.trackType()) {
                        case TRACK -> {
                            embed.setTitle(title, uri)
                                    .addField("Author", trackInfo.author, true)
                                    .setThumbnail(trackInfo.artworkUrl);

                            if (!trackInfo.isStream) {
                                embed.setFooter(TimeFormat.formatTime(trackInfo.length));
                            }

                            // Add the album name if the track is from Spotify
                            try {
                                String spotifyRegex = "^(https?://)?open.spotify.com/.*";
                                if (uri.matches(spotifyRegex)) {
                                    SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();
                                    String id = uri.split("/")[uri.split("/").length - 1];
                                    String albumName = spotifyApi.getTrack(id)
                                            .build()
                                            .execute()
                                            .getAlbum()
                                            .getName();
                                    embed.addField("Album", albumName, true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        case FILE -> embed.setTitle(title, uri)
                                .addField("Author", trackInfo.author, true)
                                .setFooter(TimeFormat.formatTime((track.getDuration())));
                        case TTS -> embed.setTitle("TTS Message")
                                .setDescription(TTSCommand.getTTSMessage(track));
                    }

                    channel.sendMessageEmbeds(embed.build()).queue();
                }
            }
        });
    }
}