package bobo.commands.voice.music;

import bobo.lavaplayer.PlayerManager;
import bobo.utils.*;
import bobo.utils.api_clients.SoundCloudAPI;
import bobo.utils.api_clients.SpotifyLink;
import bobo.utils.api_clients.YouTubeUtil;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.*;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static bobo.utils.StringUtils.*;

public class SearchCommand extends AbstractMusic {
    private static final Logger logger = LoggerFactory.getLogger(SearchCommand.class);

    private static final ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);
    private static final List<Emoji> EMOJIS = List.of(
            Emoji.fromUnicode("❌"),
            Emoji.fromUnicode("1️⃣"),
            Emoji.fromUnicode("2️⃣"),
            Emoji.fromUnicode("3️⃣"),
            Emoji.fromUnicode("4️⃣"),
            Emoji.fromUnicode("5️⃣")
    );

    /**
     * Creates a new search command.
     */
    public SearchCommand() {
        super(Commands.slash("search", "Searches a platform, and plays the requested result.")
                .addOptions(
                        new OptionData(OptionType.STRING, "platform", "The platform to search on", true)
                                .addChoices(
                                        new Command.Choice("YouTube", "youtube"),
                                        new Command.Choice("Spotify", "spotify"),
                                        new Command.Choice("SoundCloud", "soundcloud")
                                ),
                        new OptionData(OptionType.STRING, "type", "The type of search", true)
                                .addChoices(
                                        new Command.Choice("track", "track"),
                                        new Command.Choice("playlist", "playlist"),
                                        new Command.Choice("album", "album")
                                ),
                        new OptionData(OptionType.STRING, "query", "What to search", true)
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

        String platform = event.getOption("platform").getAsString();
        String searchType = event.getOption("type").getAsString();
        String query = event.getOption("query").getAsString();

        switch (platform) {
            case "youtube" -> {
                switch (searchType) {
                    case "track" -> searchYoutubeTrack(query);
                    case "playlist" -> searchYoutubePlaylist(query);
                    case "album" -> event.reply("Searching albums is not supported on YouTube.").queue();
                    default -> throw new IllegalStateException("Unexpected value: " + searchType);
                }
            }
            case "spotify" -> {
                switch (searchType) {
                    case "track" -> searchSpotifyTrack(query);
                    case "playlist" -> searchSpotifyPlaylist(query);
                    case "album" -> searchSpotifyAlbum(query);
                    default -> throw new IllegalStateException("Unexpected value: " + searchType);
                }
            }
            case "soundcloud" -> {
                switch (searchType) {
                    case "track" -> searchSoundcloud(query, "track");
                    case "playlist" -> searchSoundcloud(query, "playlist");
                    case "album" -> searchSoundcloud(query, "album");
                    default -> throw new IllegalStateException("Unexpected value: " + searchType);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + platform);
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
            logger.error("An error occurred while searching for YouTube videos.");
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
            logger.error("An error occurred while searching for YouTube playlists.");
        }
    }

    private void handleYoutubeSearch(List<SearchResult> searchResults, String[] links, String query, String type) {
        StringBuilder message = new StringBuilder("Found " + type + "s from search: " + markdownBold(query) + "\n");
        for (int i = 0; i < links.length; i++) {
            SearchResultSnippet snippet = searchResults.get(i).getSnippet();
            message.append(i + 1)
                    .append(". ")
                    .append(markdownLinkNoEmbed(snippet.getTitle(), links[i]))
                    .append(" by ")
                    .append(markdownLinkNoEmbed(snippet.getChannelTitle(), "https://www.youtube.com/channel/" + snippet.getChannelId()))
                    .append("\n");
        }

        handleResult(message.toString(), type, links);
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

                message.append(i + 1)
                        .append(". ")
                        .append(markdownLinkNoEmbed(track.getName(), trackUrl))
                        .append(" by ")
                        .append(artistString)
                        .append("\n");
            }

            handleResult(message.toString(), "track", trackLinks);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for tracks.").queue();
            logger.error("An error occurred while searching for Spotify tracks.");
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
                message.append(i + 1)
                        .append(". ")
                        .append(markdownLinkNoEmbed(playlist.getName(), playlistUrl))
                        .append(" by ")
                        .append(markdownLinkNoEmbed(owner.getDisplayName(), owner.getExternalUrls().get("spotify")))
                        .append("\n");
            }

            handleResult(message.toString(), "playlist", playlistLinks);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for playlists.").queue();
            logger.error("An error occurred while searching for Spotify playlists.");
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

                message.append(i + 1)
                        .append(". ")
                        .append(markdownLinkNoEmbed(album.getName(), albumUrl))
                        .append(" by ")
                        .append(artistString)
                        .append("\n");
            }

            handleResult(message.toString(), "album", albumLinks);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for albums.").queue();
            logger.error("An error occurred while searching for Spotify albums.");
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

        StringBuilder message = new StringBuilder("Found " + type + "s from search: " + markdownBold(query) + "\n");
        for (int i = 0; i < links.length; i++) {
            message.append(i + 1)
                    .append(". ")
                    .append(markdownLinkNoEmbed(titles[i], links[i]))
                    .append(" by ")
                    .append(artists[i])
                    .append("\n");
        }

        handleResult(message.toString(), type, links);
    }

    /**
     * Handles the result of the search.
     *
     * @param message The message to send.
     * @param type The type of search.
     * @param links The links to the search results.
     */
    private void handleResult(String message, String type, String[] links) {
        message += String.format("Which %s would you like to play? Press the corresponding button, or %s to cancel.", type, EMOJIS.getFirst());

        EmojiMapping<ThrowingConsumer<ButtonWrapper>> buttons = getButtons(links);

        hook.editOriginal(StringEscapeUtils.unescapeHtml4(message)).queue(success -> {
            Pages.buttonize(success, buttons, true, false);
            scheduledService.schedule(() -> Pages.clearButtons(success), 1, TimeUnit.MINUTES); // Clear buttons after 1 minute
        });
        event.getHook().retrieveOriginal().queue();
    }

    /**
     * Gets the buttons for the search results.
     *
     * @param links The links to the search results.
     * @return The buttons.
     */
    private static EmojiMapping<ThrowingConsumer<ButtonWrapper>> getButtons(String[] links) {
        ThrowingConsumer<ButtonWrapper> handleButton = (wrapper) -> {
            Emoji emoji = wrapper.getButton().getEmoji();
            if (emoji == null) {
                System.out.println("emoji is null");
                return; // Should never happen
            }

            int index;
            if (emoji.equals(EMOJIS.getFirst())) {
                wrapper.getMessage().delete().queue();
                return;
            } else if (emoji.equals(EMOJIS.get(1))) {
                index = 0;
            } else if (emoji.equals(EMOJIS.get(2))) {
                index = 1;
            } else if (emoji.equals(EMOJIS.get(3))) {
                index = 2;
            } else if (emoji.equals(EMOJIS.get(4))) {
                index = 3;
            } else if (emoji.equals(EMOJIS.get(5))) {
                index = 4;
            } else {
                return; // Should never happen
            }

            if (!ensureConnected(wrapper.getMember())) {
                return;
            }

            PlayerManager.getInstance().loadAndPlay(wrapper.getChannel(), wrapper.getMember(), links[index], TrackType.TRACK);
            Pages.clearButtons(wrapper.getMessage());
        };

        return switch (links.length) {
            case 1 -> new EmojiMapping<>(Map.of(new EmojiId(EMOJIS.getFirst()), handleButton, new EmojiId(EMOJIS.get(1)), handleButton));
            case 2 -> new EmojiMapping<>(Map.of(new EmojiId(EMOJIS.getFirst()), handleButton, new EmojiId(EMOJIS.get(1)), handleButton, new EmojiId(EMOJIS.get(2)), handleButton));
            case 3 -> new EmojiMapping<>(Map.of(new EmojiId(EMOJIS.getFirst()), handleButton, new EmojiId(EMOJIS.get(1)), handleButton, new EmojiId(EMOJIS.get(2)), handleButton, new EmojiId(EMOJIS.get(3)), handleButton));
            case 4 -> new EmojiMapping<>(Map.of(new EmojiId(EMOJIS.getFirst()), handleButton, new EmojiId(EMOJIS.get(1)), handleButton, new EmojiId(EMOJIS.get(2)), handleButton, new EmojiId(EMOJIS.get(3)), handleButton, new EmojiId(EMOJIS.get(4)), handleButton));
            case 5 -> new EmojiMapping<>(Map.of(new EmojiId(EMOJIS.getFirst()), handleButton, new EmojiId(EMOJIS.get(1)), handleButton, new EmojiId(EMOJIS.get(2)), handleButton, new EmojiId(EMOJIS.get(3)), handleButton, new EmojiId(EMOJIS.get(4)), handleButton, new EmojiId(EMOJIS.get(5)), handleButton));
            default -> throw new IllegalStateException("Unexpected value: " + links.length);
        };
    }

    @Override
    public String getHelp() {
        return """
                Searches YouTube, Spotify, or SoundCloud, and plays the requested result.
                Usage: `/search <option>`
                Options:
                * `youtube track/playlist <query>`: Search YouTube for a track/playlist with <query>
                * `spotify track/playlist/album <query>`: Search Spotify for a track/playlist/album with <query>
                * `soundcloud track/playlist/album <query>`: Search SoundCloud for a track/playlist/album with <query>""";
    }

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS));
    }
}