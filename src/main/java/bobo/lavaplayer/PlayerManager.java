package bobo.lavaplayer;

import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void loadAndPlay(SlashCommandInteractionEvent event, String trackURL) {
        final GuildMusicManager musicManager = this.getMusicManager(event.getGuildChannel().getGuild());
        // Bot joins only if not already in a vc
        if (!Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
            event.getGuild().getAudioManager().openAudioConnection(Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel());

            // Sets event listener to send message whenever new track starts
            musicManager.setEventListener(audioEvent -> {
                if (audioEvent instanceof TrackStartEvent) {
                    AudioTrackInfo info = ((TrackStartEvent) audioEvent).track.getInfo();

                    // Creates embedded message with track info
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                            .setTitle(info.title, info.uri)
                            .setImage("attachment://thumbnail.jpg")
                            .setColor(Color.red)
                            .setFooter(TimeFormat.formatTime(((TrackStartEvent) audioEvent).track.getDuration()));

                    // Sets image in embed to proper aspect ratio
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(info.uri)), "jpg", outputStream);
                        event.getMessageChannel().sendFiles(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
                    } catch (Exception e) {
                        event.getMessageChannel().sendMessageEmbeds(embed.build()).queue();
                    }

                }
            });
        }
        this.audioPlayerManager.loadItemOrdered(musicManager, trackURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                AudioTrackInfo info = track.getInfo();
                event.getHook().editOriginal("Adding to queue [" + info.title + "](<" + info.uri + ">) by **" + info.author + "**").queue();
                musicManager.scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                final List<AudioTrack> tracks = playlist.getTracks();
                event.getHook().editOriginal("Adding to queue **" + tracks.size() + "** tracks from playlist **" + playlist.getName() + "**").queue();
                for (final AudioTrack track : tracks) {
                    musicManager.scheduler.queue(track);
                }
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal("Nothing found by **" + trackURL + "**").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getMessageChannel().sendMessage("Could not play: **" + e.getMessage() + "**").queue();
            }
        });
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }
}