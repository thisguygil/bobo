package bobo.commands.voice.music;

import bobo.lavaplayer.PlayerManager;
import bobo.utils.*;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static bobo.utils.StringUtils.*;

public class SearchCommand extends AbstractMusic {
    private static final ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);
    private static final Map<Long, SlashCommandInteractionEvent> MESSAGE_EVENT_MAP = new HashMap<>();
    private static final Map<SlashCommandInteractionEvent, String[]> EVENT_LINKS_MAP = new HashMap<>();

    /**
     * Creates a new search command.
     */
    public SearchCommand() {
        super(Commands.slash("search", "Searches YouTube/Spotify/SoundCloud, and plays the requested result.")
                .addSubcommandGroups(
                        new SubcommandGroupData("youtube", "Search YouTube.")
                                .addSubcommands(
                                        new SubcommandData("track", "Searches YouTube for a track.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("playlist", "Searches YouTube for a playlist.")
                                                .addOption(OptionType.STRING, "query", "What to search", true)
                                ),
                        new SubcommandGroupData("spotify", "Search Spotify.")
                                .addSubcommands(
                                        new SubcommandData("track", "Search Spotify for a track.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("playlist", "Search Spotify for a playlist.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("album", "Search Spotify for an album.")
                                                .addOption(OptionType.STRING, "query", "What to search", true)
                                ),
                        new SubcommandGroupData("soundcloud", "Search SoundCloud.")
                                .addSubcommands(
                                        new SubcommandData("track", "Search SoundCloud for a track.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("playlist", "Search SoundCloud for a playlist.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("album", "Search SoundCloud for an album.")
                                                .addOption(OptionType.STRING, "query", "What to search", true)
                                )
                )
        );
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        String subcommandGroupName = event.getSubcommandGroup();
        String subcommandName = event.getSubcommandName();
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();
        assert subcommandGroupName != null;
        assert subcommandName != null;

        switch (subcommandGroupName) {
            case "youtube" -> {
                switch (subcommandName) {
                    case "track" -> searchYoutubeTrack(query);
                    case "playlist" -> searchYoutubePlaylist(query);
                    default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
                }
            }
            case "spotify" -> {
                switch (subcommandName) {
                    case "track" -> searchSpotifyTrack(query);
                    case "playlist" -> searchSpotifyPlaylist(query);
                    case "album" -> searchSpotifyAlbum(query);
                    default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
                }
            }
            case "soundcloud" -> {
                switch (subcommandName) {
                    case "track" -> searchSoundcloud(query, "track");
                    case "playlist" -> searchSoundcloud(query, "playlist");
                    case "album" -> searchSoundcloud(query, "album");
                    default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + subcommandGroupName);
        }
    }

    /**
     * Searches YouTube for tracks.
     *
     * @param query The query to search for.
     */
    private void searchYoutubeTrack(String query) {
        try {
            List<SearchResult> videoSearch = YouTubeUtil.searchForVideos(query);
            if (videoSearch == null) {
                hook.editOriginal("No videos found.").queue();
                return;
            }

            String[] videoLinks = new String[videoSearch.size()];
            for (int i = 0; i < videoSearch.size(); i++) {
                SearchResult result = videoSearch.get(i);
                String videoId = result.getId().getVideoId();
                videoLinks[i] = "https://www.youtube.com/watch?v=" + videoId;
            }

            handleYoutubeSearch(videoSearch, videoLinks, query, "video");
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for videos.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches YouTube for playlists.
     *
     * @param query The query to search for.
     */
    private void searchYoutubePlaylist(String query) {
        try {
            List<SearchResult> playlistSearch = YouTubeUtil.searchForPlaylists(query);
            if (playlistSearch == null) {
                hook.editOriginal("No playlists found.").queue();
                return;
            }

            String[] playlistLinks = new String[playlistSearch.size()];
            for (int i = 0; i < playlistSearch.size(); i++) {
                SearchResult result = playlistSearch.get(i);
                String playlistId = result.getId().getPlaylistId();
                playlistLinks[i] = "https://www.youtube.com/playlist?list=" + playlistId;
            }

            handleYoutubeSearch(playlistSearch, playlistLinks, query, "playlist");
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for playlists.").queue();
            e.printStackTrace();
        }
    }

    private void handleYoutubeSearch(List<SearchResult> searchResults, String[] links, String query, String type) {
        EVENT_LINKS_MAP.put(event, links);

        StringBuilder message = new StringBuilder("Found " + type + "s from search: " + markdownBold(query) + "\n");
        for (int i = 0; i < links.length; i++) {
            SearchResultSnippet snippet = searchResults.get(i).getSnippet();
            message.append(markdownBold((i + 1) + ":"))
                    .append(" ")
                    .append(markdownLinkNoEmbed(snippet.getTitle(), links[i]))
                    .append(" by ")
                    .append(markdownLinkNoEmbed(snippet.getChannelTitle(), "https://www.youtube.com/channel/" + snippet.getChannelId()))
                    .append("\n");
        }

        handleResult(message.toString(), type, links.length);
    }

    /**
     * Searches Spotify for tracks.
     *
     * @param query The query to search for.
     */
    private void searchSpotifyTrack(String query) {
        try {
            SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();

            // Get the tracks from the Spotify API
            Track[] tracks = spotifyApi.searchTracks(query).limit(5).build().execute().getItems();
            if (tracks.length == 0) {
                hook.editOriginal("No tracks found.").queue();
                return;
            }

            // Store each track's artist(s) in a 2D array
            ArtistSimplified[][] artists = new ArtistSimplified[tracks.length][];
            for (int i = 0; i < tracks.length; i++) {
                ArtistSimplified[] trackArtists = tracks[i].getArtists();
                artists[i] = new ArtistSimplified[trackArtists.length];
                System.arraycopy(trackArtists, 0, artists[i], 0, trackArtists.length);
            }

            // Build the message
            String[] trackLinks = new String[tracks.length];
            StringBuilder message = new StringBuilder("Found tracks from search: " + markdownBold(query) + "\n");
            for (int i = 0; i < tracks.length; i++) {
                Track track = tracks[i];
                String trackUrl = track.getExternalUrls().get("spotify");
                trackLinks[i] = trackUrl;

                // Build a string for the artists
                StringBuilder artistString = new StringBuilder();
                for (int j = 0; j < artists[i].length; j++) {
                    artistString.append(markdownLinkNoEmbed(artists[i][j].getName(), artists[i][j].getExternalUrls().get("spotify")));
                    if (j < artists[i].length - 1) {
                        artistString.append(", ");
                    }
                }

                message.append(markdownBold((i + 1) + ":"))
                        .append(" ")
                        .append(markdownLinkNoEmbed(track.getName(), trackUrl))
                        .append(" by ")
                        .append(artistString)
                        .append("\n");
            }

            EVENT_LINKS_MAP.put(event, trackLinks);

            handleResult(message.toString(), "track", trackLinks.length);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for tracks.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches Spotify for playlists.
     *
     * @param query The query to search for.
     */
    private void searchSpotifyPlaylist(String query) {
        try {
            SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();

            PlaylistSimplified[] playlists = spotifyApi.searchPlaylists(query).limit(5).build().execute().getItems();
            if (playlists.length == 0) {
                hook.editOriginal("No playlists found.").queue();
                return;
            }

            String[] playlistLinks = new String[playlists.length];
            StringBuilder message = new StringBuilder("Found playlists from search: " + markdownBold(query) + "\n");
            for (int i = 0; i < playlistLinks.length; i++) {
                PlaylistSimplified playlist = playlists[i];
                String playlistUrl = playlist.getExternalUrls().get("spotify");
                User owner = playlist.getOwner();
                playlistLinks[i] = playlistUrl;
                message.append(markdownBold((i + 1) + ":"))
                        .append(" ")
                        .append(markdownLinkNoEmbed(playlist.getName(), playlistUrl))
                        .append(" by ")
                        .append(markdownLinkNoEmbed(owner.getDisplayName(), owner.getExternalUrls().get("spotify")))
                        .append("\n");
            }

            EVENT_LINKS_MAP.put(event, playlistLinks);

            handleResult(message.toString(), "playlist", playlistLinks.length);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for playlists.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches Spotify for albums.
     *
     * @param query The query to search for.
     */
    private void searchSpotifyAlbum(String query) {
        try {
            SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();

            AlbumSimplified[] albums = spotifyApi.searchAlbums(query).limit(5).build().execute().getItems();
            if (albums.length == 0) {
                hook.editOriginal("No tracks found.").queue();
                return;
            }

            ArtistSimplified[][] artists = new ArtistSimplified[albums.length][];
            for (int i = 0; i < albums.length; i++) {
                ArtistSimplified[] trackArtists = albums[i].getArtists();
                artists[i] = new ArtistSimplified[trackArtists.length];
                System.arraycopy(trackArtists, 0, artists[i], 0, trackArtists.length);
            }

            String[] albumLinks = new String[albums.length];
            StringBuilder message = new StringBuilder("Found albums from search: " + markdownBold(query) + "\n");
            for (int i = 0; i < albumLinks.length; i++) {
                AlbumSimplified album = albums[i];
                String albumUrl = album.getExternalUrls().get("spotify");
                albumLinks[i] = albumUrl;

                StringBuilder artistString = new StringBuilder();
                for (int j = 0; j < artists[i].length; j++) {
                    artistString.append(markdownLinkNoEmbed(artists[i][j].getName(), artists[i][j].getExternalUrls().get("spotify")));
                    if (j < artists[i].length - 1) {
                        artistString.append(", ");
                    }
                }

                message.append(markdownBold((i + 1) + ":"))
                        .append(" ")
                        .append(markdownLinkNoEmbed(album.getName(), albumUrl))
                        .append(" by ")
                        .append(artistString)
                        .append("\n");
            }

            EVENT_LINKS_MAP.put(event, albumLinks);

            handleResult(message.toString(), "album", albumLinks.length);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for albums.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches SoundCloud for tracks, playlists, or albums.
     *
     * @param query The query to search for.
     * @param type The type of search.
     */
    private void searchSoundcloud(String query, String type) {
        String apiResponse = SoundCloudAPI.search(query, type + "s", 5);
        if (apiResponse == null) {
            hook.editOriginal("An error occurred while searching for " + type + "s.").queue();
            return;
        }

        JSONObject response = new JSONObject(apiResponse);
        if (response.getInt("total_results") == 0) {
            hook.editOriginal("No " + type + "s found.").queue();
            return;
        }

        JSONArray collection = response.getJSONArray("collection");
        int collectionLength = collection.length();
        String[] titles = new String[collectionLength];
        String[] links = new String[collectionLength];
        String[] artists = new String[collectionLength];
        for (int i = 0; i < collectionLength; i++) {
            titles[i] = (collection.getJSONObject(i).getString("title"));
            links[i] = (collection.getJSONObject(i).getString("permalink_url"));
            artists[i] = collection.getJSONObject(i).getJSONObject("user").getString("username");
        }

        EVENT_LINKS_MAP.put(event, links);

        StringBuilder message = new StringBuilder("Found " + type + "s from search: " + markdownBold(query) + "\n");
        for (int i = 0; i < links.length; i++) {
            message.append(markdownBold((i + 1) + ":"))
                    .append(" ")
                    .append(markdownLinkNoEmbed(titles[i], links[i]))
                    .append(" by ")
                    .append(artists[i])
                    .append("\n");
        }

        handleResult(message.toString(), type, links.length);
    }

    /**
     * Handles the result of the search.
     *
     * @param message The message to send.
     * @param type The type of search.
     * @param numLinks The number of links.
     */
    private void handleResult(String message, String type, int numLinks) {
        message += "\nPlease select a " + type + " to play by selecting the proper reaction, or the " + EmojiType.X + " reaction to cancel.";
        hook.editOriginal(StringEscapeUtils.unescapeHtml4(message)).queue(success -> addReactions(success, numLinks));
        MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
    }

    /**
     * Handles reaction to search message.
     *
     * @param reactionEvent the reaction event
     */
    public static void handleReaction(@Nonnull MessageReactionAddEvent reactionEvent) {
        SlashCommandInteractionEvent commandEvent = MESSAGE_EVENT_MAP.get(reactionEvent.getMessageIdLong());
        if (commandEvent == null) {
            return;
        }

        if (!reactionEvent.retrieveUser().complete().equals(commandEvent.getUser())) {
            return;
        }

        Emoji reaction = reactionEvent.getReaction().getEmoji();
        int index;
        if (reaction.equals(EmojiType.X.asEmoji())) {
            long messageId = reactionEvent.getMessageIdLong();
            MESSAGE_EVENT_MAP.remove(messageId);
            EVENT_LINKS_MAP.remove(commandEvent);
            reactionEvent.getChannel().deleteMessageById(messageId).queue();
            return;
        } else if (reaction.equals(EmojiType.ONE.asEmoji())) {
            index = 0;
        } else if (reaction.equals(EmojiType.TWO.asEmoji())) {
            index = 1;
        } else if (reaction.equals(EmojiType.THREE.asEmoji())) {
            index = 2;
        } else if (reaction.equals(EmojiType.FOUR.asEmoji())) {
            index = 3;
        } else if (reaction.equals(EmojiType.FIVE.asEmoji())) {
            index = 4;
        } else {
            return;
        }

        String[] links = EVENT_LINKS_MAP.get(commandEvent);
        if (links == null) {
            return;
        }

        int numLinks = links.length;
        if (index >= numLinks) {
            return;
        }

        if (!ensureConnected(commandEvent)) {
            return;
        }

        PlayerManager.getInstance().loadAndPlay(commandEvent, links[index], TrackType.TRACK);
        cleanupResources(reactionEvent.getMessageIdLong(), commandEvent);
    }

    /**
     * Adds the reactions to the message.
     *
     * @param message The message to add reactions to.
     * @param numLinks The number of links to add reactions for.
     */
    private void addReactions(@Nonnull Message message, int numLinks) {
        message.addReaction(EmojiType.X.asEmoji()).queue();
        List<Emoji> emojis = List.of(EmojiType.ONE.asEmoji(), EmojiType.TWO.asEmoji(), EmojiType.THREE.asEmoji(), EmojiType.FOUR.asEmoji(), EmojiType.FIVE.asEmoji());
        emojis.stream().limit(numLinks).forEach(emoji -> message.addReaction(emoji).queue());

        // Schedule a task to clean up resources after 1 minute
        scheduledService.schedule(() -> cleanupResources(message.getIdLong(), event), 1, TimeUnit.MINUTES);
    }

    /**
     * Cleans up resources.
     *
     * @param messageId The ID of the message.
     * @param event The event.
     */
    private static void cleanupResources(long messageId, SlashCommandInteractionEvent event) {
        event.getHook().retrieveOriginal().queue(message -> message.clearReactions().queue());
        MESSAGE_EVENT_MAP.remove(messageId);
        EVENT_LINKS_MAP.remove(event);
    }

    @Override
    public String getHelp() {
        return """
                Searches YouTube, Spotify, or SoundCloud, and plays the requested result.
                Usage: `/search <subcommand>`
                Subcommands:
                * `youtube track/playlist <query>`: Search YouTube for a track/playlist with <query>
                * `spotify track/playlist/album <query>`: Search Spotify for a track/playlist/album with <query>
                * `soundcloud track/playlist/album <query>`: Search SoundCloud for a track/playlist/album with <query>""";
    }

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS));
    }
}