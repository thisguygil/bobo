package bobo.lavaplayer;

import bobo.Config;
import bobo.commands.voice.music.TTSCommand;
import bobo.utils.SpotifyLink;
import bobo.utils.TrackType;
import com.github.topi314.lavalyrics.LyricsManager;
import com.github.topi314.lavasrc.deezer.DeezerAudioSourceManager;
import com.github.topi314.lavasrc.flowerytts.FloweryTTSSourceManager;
import com.github.topi314.lavasrc.mirror.DefaultMirroringAudioTrackResolver;
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

import static bobo.utils.StringUtils.*;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;
    private final LyricsManager lyricsManager;

    /**
     * Creates a new player manager.
     */
    @SuppressWarnings("deprecation")
    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager(
                new MusicWithThumbnail(),
                new WebWithThumbnail(),
                new WebEmbeddedWithThumbnail(),
                new AndroidTestsuiteWithThumbnail(),
                new AndroidLiteWithThumbnail(),
                new AndroidMusicWithThumbnail(),
                new MediaConnectWithThumbnail(),
                new TvHtml5EmbeddedWithThumbnail()
        );
        Web.setPoTokenAndVisitorData(Config.get("PO_TOKEN"), Config.get("PO_VISITOR_DATA"));

        SpotifySourceManager spotifySourceManager = new SpotifySourceManager(
                Config.get("SPOTIFY_CLIENT_ID"),
                Config.get("SPOTIFY_CLIENT_SECRET"),
                Config.get("SP_DC"),
                "US",
                (v) -> audioPlayerManager,
                new DefaultMirroringAudioTrackResolver(null)
        );

        DeezerAudioSourceManager deezerAudioSourceManager = new DeezerAudioSourceManager(Config.get("DEEZER_MASTER_DECRYPTION_KEY"));
        FloweryTTSSourceManager floweryTTSSourceManager = new FloweryTTSSourceManager("Eric");

        this.audioPlayerManager.registerSourceManagers(
                youtubeAudioSourceManager,
                spotifySourceManager,
                deezerAudioSourceManager,
                floweryTTSSourceManager
        );

        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
        AudioSourceManagers.registerRemoteSources(
                this.audioPlayerManager,
                com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class // Exclude deprecated YoutubeAudioSourceManager
        );

        this.lyricsManager = new LyricsManager();
        this.lyricsManager.registerLyricsManager(spotifySourceManager);
    }

    /**
     * Gets the guild music manager.
     *
     * @param guild The guild to get the music manager for.
     * @return The guild music manager.
     */
    public GuildMusicManager getMusicManager(@Nonnull Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager, guild);
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
                        message.append(markdownLinkNoEmbed(info.title, info.uri))
                                .append(" by ")
                                .append(markdownBold(info.author));
                    }
                    case TTS -> {
                        message.append(markdownBold("TTS Message"));
                        TTSCommand.addGuild(guild);
                        TTSCommand.addTTSMessage(event.getGuild(), track, decodeUrl(trackURL.replace("ftts://", "")));
                    }
                }

                // Uses the success callback to ensure that the message is edited before the track is queued.
                // Otherwise, the message that the track was added to the queue may be sent after the message that the track is now playing.
                // Also ensures that if there's an error, the track is not queued.
                hook.editOriginal(message.toString()).queue(success -> scheduler.queue(track, member, channel, trackType));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // If the playlist is a search result, play the first track in the search results
                if (playlist.isSearchResult()) {
                    trackLoaded(playlist.getTracks().get(0));
                    return;
                }

                final List<AudioTrack> tracks = playlist.getTracks();
                boolean isAlbum = SpotifyLink.URL_PATTERN
                        .matcher(trackURL)
                        .group("type")
                        .equals("album");
                hook.editOriginal("Adding to queue " + markdownBold(tracks.size()) + " tracks from " + (isAlbum ? "album" : "playlist") + " " + markdownLinkNoEmbed(playlist.getName(), trackURL)).queue(success -> {
                    for (final AudioTrack track : tracks) {
                        scheduler.queue(track, member, channel, TrackType.TRACK);
                    }
                });
            }

            @Override
            public void noMatches() {
                hook.editOriginal(trackType == TrackType.TTS ? "No speakable text found" : "Nothing found by " + markdownBold(trackURL)).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                hook.editOriginal("Could not load: " + markdownBold(e.getMessage())).queue();
            }
        });
    }

    public LyricsManager getLyricsManager() {
        return this.lyricsManager;
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