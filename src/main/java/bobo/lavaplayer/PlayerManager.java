package bobo.lavaplayer;

import bobo.Config;
import bobo.commands.voice.music.TTSCommand;
import bobo.utils.api_clients.SpotifyLink;
import bobo.utils.TrackType;
import bobo.utils.api_clients.YouTubeUtil;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

        String poToken = Config.get("PO_TOKEN");
        String poVisitorData = Config.get("PO_VISITOR_DATA");
        Web.setPoTokenAndVisitorData(poToken, poVisitorData);
        WebEmbedded.setPoTokenAndVisitorData(poToken, poVisitorData);

        SpotifySourceManager spotifySourceManager = new SpotifySourceManager(
                Config.get("SPOTIFY_CLIENT_ID"),
                Config.get("SPOTIFY_CLIENT_SECRET"),
                Config.get("SP_DC"),
                "US",
                (_) -> audioPlayerManager,
                new DefaultMirroringAudioTrackResolver(null)
        );

        DeezerAudioSourceManager deezerAudioSourceManager = new DeezerAudioSourceManager(Config.get("DEEZER_MASTER_DECRYPTION_KEY"));
        FloweryTTSSourceManager floweryTTSSourceManager = new FloweryTTSSourceManager(Config.get("FLOWERY_TTS_VOICE"));

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
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (_) -> {
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
                int position = scheduler.queue.size() + 1 + (scheduler.currentTrack != null ? 1 : 0);
                StringBuilder message = new StringBuilder("Adding ");
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
                message.append(" to queue position ")
                        .append(markdownBold(position))
                        .append(".");

                hook.editOriginal(message.toString()).queue(_ -> scheduler.queue(track, member, channel, trackType));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                if (tracks.isEmpty()) {
                    noMatches();
                    return;
                }

                // If the playlist is a search result, play the first track in the search results
                if (playlist.isSearchResult()) {
                    trackLoaded(playlist.getTracks().getFirst());
                    return;
                }

                String message = getMessage(playlist, tracks);

                hook.editOriginal(message).queue(_ -> {
                    for (final AudioTrack track : tracks) {
                        scheduler.queue(track, member, channel, TrackType.TRACK);
                    }
                });
            }

            @Nonnull
            private String getMessage(AudioPlaylist playlist, List<AudioTrack> tracks) {
                int trackCount = tracks.size();
                int position = scheduler.queue.size() + 1 + (scheduler.currentTrack != null ? 1 : 0);

                AtomicBoolean isAlbum = new AtomicBoolean();
                SpotifyLink.URL_PATTERN
                        .matcher(trackURL)
                        .results()
                        .findFirst()
                        .ifPresent(matchResult -> isAlbum.set(matchResult.group("type").equals("album")));

                return String.format("Adding **%s** tracks from %s %s to queue positions **%d-%d**.",
                        trackCount,
                        isAlbum.get() ? "album" : "playlist",
                        markdownLinkNoEmbed(playlist.getName(), trackURL),
                        position,
                        position + trackCount - 1
                );
            }

            @Override
            public void noMatches() {
                String query;
                if (trackURL.startsWith("scsearch:")) {
                    query = trackURL.replace("scsearch:", "");
                } else {
                    query = trackURL;
                }
                hook.editOriginal(trackType == TrackType.TTS ? "No speakable text found" : "Nothing found by " + markdownBold(query)).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                String errorMessage = e.getMessage();
                if (errorMessage.equals("Sign in to confirm you’re not a bot")) {
                    String youtubeTitle = YouTubeUtil.getTitle(trackURL);
                    if (youtubeTitle == null) {
                        hook.editOriginal("Could not load: " + markdownBold(errorMessage)).queue();
                        return;
                    }

                    loadAndPlay(event, "scsearch:" + youtubeTitle, trackType);
                } else {
                    hook.editOriginal("Could not load: " + markdownBold(errorMessage)).queue();
                }
            }
        });
    }

    /**
     * Gets the lyrics manager.
     *
     * @return The lyrics manager.
     */
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