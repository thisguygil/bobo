package bobo.lavaplayer;

import bobo.Config;
import bobo.commands.voice.music.TTSCommand;
import bobo.utils.TrackType;
import com.github.topi314.lavasrc.flowerytts.FloweryTTSSourceManager;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;
import java.util.*;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    /**
     * Creates a new player manager.
     */
    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager(true, new MusicWithThumbnail(), new WebWithThumbnail(), new AndroidWithThumbnail(), new TvHtml5EmbeddedWithThumbnail()));
        this.audioPlayerManager.registerSourceManager(new SpotifySourceManager(null, Config.get("SPOTIFY_CLIENT_ID"), Config.get("SPOTIFY_CLIENT_SECRET"), "US", audioPlayerManager));
        this.audioPlayerManager.registerSourceManager(new FloweryTTSSourceManager("Eric"));
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class); // Need to exclude the deprecated YoutubeAudioSourceManager
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    /**
     * Gets the guild music manager.
     *
     * @param guild The guild to get the music manager for.
     * @return The guild music manager.
     */
    public GuildMusicManager getMusicManager(@Nonnull Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.sendHandler);
            return guildMusicManager;
        });
    }

    /**
     * Loads and plays a track.
     *
     * @param event The event that triggered this action.
     * @param trackURL The URL of the track to play.
     * @param trackType The type of track to play.
     */
    public void loadAndPlay(@Nonnull SlashCommandInteractionEvent event, String trackURL, TrackType trackType) {
        InteractionHook hook = event.getHook();
        MessageChannel channel = event.getMessageChannel();
        Guild guild = event.getGuildChannel().getGuild();
        Member member = event.getMember();
        final GuildMusicManager musicManager = this.getMusicManager(guild);
        TrackScheduler scheduler = musicManager.scheduler;

        if (Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel()).getType() == ChannelType.STAGE) {
            guild.requestToSpeak();
        }

        this.audioPlayerManager.loadItemOrdered(musicManager, trackURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                StringBuilder message = new StringBuilder("Adding to queue ");
                switch (trackType) {
                    case TRACK, FILE -> {
                        AudioTrackInfo info = track.getInfo();
                        message.append(markdownLink(info.title, info.uri))
                                .append(" by **")
                                .append(info.author)
                                .append("**");
                    }
                    case TTS -> {
                        message.append("**TTS**");
                        TTSCommand.addTTSMessage(track, trackURL.replace("ftts://", "").replace("%20", " ").replace("%22", "\""));
                    }
                }

                // Uses the success callback to ensure that the message is edited before the track is queued.
                // Otherwise, the message that the track was added to the queue may be sent after the message that the track is now playing.
                // Also ensures that if there's an error, the track is not queued.
                hook.editOriginal(message.toString()).queue(success -> scheduler.queue(track, member, channel, trackType));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                final List<AudioTrack> tracks = playlist.getTracks();
                String regex = "^(https://open.spotify.com/album/|spotify:album:)([a-zA-Z0-9]+)(.*)";
                hook.editOriginal("Adding to queue **" + tracks.size() + "** tracks from " + (trackURL.matches(regex) ? "album" : "playlist") + " " + markdownLink(playlist.getName(), trackURL)).queue(success -> {
                    for (final AudioTrack track : tracks) {
                        scheduler.queue(track, member, channel, TrackType.TRACK);
                    }
                });
            }

            @Override
            public void noMatches() {
                hook.editOriginal(trackType == TrackType.TTS ? "No speakable text found" : "Nothing found by **" + trackURL + "**").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                hook.editOriginal("Could not load: **" + e.getMessage() + "**").queue();
            }
        });
    }

    private String markdownLink(String text, String url) {
        return "[" + text + "](<" + url + ">)";
    }

    /**
     * Gets the player manager instance.
     *
     * @return The player manager instance.
     */
    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }
}