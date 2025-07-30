package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackType;
import bobo.utils.*;
import bobo.utils.api_clients.SoundCloudAPI;
import bobo.utils.api_clients.SpotifyLink;
import bobo.utils.api_clients.YouTubeUtil;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.*;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

public class SearchCommand extends AMusicCommand {
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
    protected CommandResponse handleMusicCommand() {
        String platform, searchType, query;
        try {
            platform = getOptionValue("platform", 0);
            searchType = getOptionValue("type", 1);
            query = getMultiwordOptionValue("query", 2);
        } catch (Exception e) {
            return CommandResponse.text("Invalid usage. Use `/help search` for more information.");
        }

        return switch (platform) {
            case "youtube" -> switch (searchType) {
                case "track" -> searchYoutubeTrack(query);
                case "playlist" -> searchYoutubePlaylist(query);
                case "album" -> CommandResponse.text("Searching albums is not supported on YouTube.");
                default -> CommandResponse.text("Invalid usage. Use `/help search` for more information.");
            };
            case "spotify" -> switch (searchType) {
                case "track" -> searchSpotifyTrack(query);
                case "playlist" -> searchSpotifyPlaylist(query);
                case "album" -> searchSpotifyAlbum(query);
                default -> CommandResponse.text("Invalid usage. Use `/help search` for more information.");
            };
            case "soundcloud" -> switch (searchType) {
                case "track" -> searchSoundcloud(query, "track");
                case "playlist" -> searchSoundcloud(query, "playlist");
                case "album" -> searchSoundcloud(query, "album");
                default -> CommandResponse.text("Invalid usage. Use `/help search` for more information.");
            };
            default -> CommandResponse.text("Invalid usage. Use `/help search` for more information.");
        };
    }

    /**
     * Searches YouTube for tracks.
     *
     * @param query The query to search for.
     * @return The command response.
     */
    private CommandResponse searchYoutubeTrack(String query) {
        try {
            List<SearchResult> videoSearch = YouTubeUtil.searchForVideos(query);
            if (videoSearch == null) {
                return CommandResponse.text("No videos found.");
            }

            String[] videoLinks = new String[videoSearch.size()];
            for (int i = 0; i < videoSearch.size(); i++) {
                SearchResult result = videoSearch.get(i);
                String videoId = result.getId().getVideoId();
                videoLinks[i] = "https://www.youtube.com/watch?v=" + videoId;
            }

            return handleYoutubeSearch(videoSearch, videoLinks, query, "video");
        } catch (Exception e) {
            logger.error("An error occurred while searching for YouTube videos.");
            return CommandResponse.text("An error occurred while searching for videos.");
        }
    }


    /**
     * Searches YouTube for playlists.
     *
     * @param query The query to search for.
     * @return The command response.
     */
    private CommandResponse searchYoutubePlaylist(String query) {
        try {
            List<SearchResult> playlistSearch = YouTubeUtil.searchForPlaylists(query);
            if (playlistSearch == null) {
                return CommandResponse.text("No playlists found.");
            }

            String[] playlistLinks = new String[playlistSearch.size()];
            for (int i = 0; i < playlistSearch.size(); i++) {
                SearchResult result = playlistSearch.get(i);
                String playlistId = result.getId().getPlaylistId();
                playlistLinks[i] = "https://www.youtube.com/playlist?list=" + playlistId;
            }

            return handleYoutubeSearch(playlistSearch, playlistLinks, query, "playlist");
        } catch (Exception e) {
            logger.error("An error occurred while searching for YouTube playlists.");
            return CommandResponse.text("An error occurred while searching for playlists.");
        }
    }

    /**
     * Handles the search results from YouTube.
     *
     * @param searchResults The search results.
     * @param links The links to the search results.
     * @param query The query.
     * @param type The type of search.
     * @return The command response.
     */
    private CommandResponse handleYoutubeSearch(List<SearchResult> searchResults, String[] links, String query, String type) {
        StringBuilder message = new StringBuilder(String.format("Found %ss from search: **%s**\n", type, query));
        for (int i = 0; i < links.length; i++) {
            SearchResultSnippet snippet = searchResults.get(i).getSnippet();
            message.append(String.format("%d. %s by %s\n",
                    i + 1,
                    markdownLinkNoEmbed(snippet.getTitle(), links[i]),
                    markdownLinkNoEmbed(snippet.getChannelTitle(), "https://www.youtube.com/channel/" + snippet.getChannelId())
            ));
        }

        return handleResult(message.toString(), type, links);
    }

    /**
     * Searches Spotify for tracks.
     *
     * @param query The query to search for.
     * @return The command response.
     */
    private CommandResponse searchSpotifyTrack(String query) {
        try {
            SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();

            // Get the tracks from the Spotify API
            Track[] tracks = spotifyApi.searchTracks(query).limit(5).build().execute().getItems();
            if (tracks.length == 0) {
                return CommandResponse.text("No tracks found.");
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

                StringBuilder artistString = new StringBuilder();
                for (int j = 0; j < artists[i].length; j++) {
                    artistString.append(markdownLinkNoEmbed(artists[i][j].getName(), artists[i][j].getExternalUrls().get("spotify")));
                    if (j < artists[i].length - 1) {
                        artistString.append(", ");
                    }
                }

                message.append(String.format("%d. %s (%s) by %s\n",
                        i + 1,
                        markdownLinkNoEmbed(track.getName(), trackUrl),
                        markdownCode(TimeFormat.formatTime(track.getDurationMs())),
                        artistString
                ));
            }

            return handleResult(message.toString(), "track", trackLinks);
        } catch (Exception e) {
            logger.error("An error occurred while searching for Spotify tracks.");
            return CommandResponse.text("An error occurred while searching for tracks.");
        }
    }

    /**
     * Searches Spotify for playlists.
     *
     * @param query The query to search for.
     * @return The command response.
     */
    private CommandResponse searchSpotifyPlaylist(String query) {
        try {
            SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();

            PlaylistSimplified[] playlists = spotifyApi.searchPlaylists(query).limit(5).build().execute().getItems();
            if (playlists.length == 0) {
                return CommandResponse.text("No playlists found.");
            }

            String[] playlistLinks = new String[playlists.length];
            StringBuilder message = new StringBuilder("Found playlists from search: " + markdownBold(query) + "\n");
            for (int i = 0; i < playlistLinks.length; i++) {
                PlaylistSimplified playlist = playlists[i];
                String playlistUrl = playlist.getExternalUrls().get("spotify");
                User owner = playlist.getOwner();
                playlistLinks[i] = playlistUrl;
                message.append(String.format("%d. %s by %s\n",
                        i + 1,
                        markdownLinkNoEmbed(playlist.getName(), playlistUrl),
                        markdownLinkNoEmbed(owner.getDisplayName(), owner.getExternalUrls().get("spotify"))
                ));
            }

            return handleResult(message.toString(), "playlist", playlistLinks);
        } catch (Exception e) {
            logger.error("An error occurred while searching for Spotify playlists.");
            return CommandResponse.text("An error occurred while searching for playlists.");
        }
    }

    /**
     * Searches Spotify for albums.
     *
     * @param query The query to search for.
     * @return The command response.
     */
    private CommandResponse searchSpotifyAlbum(String query) {
        try {
            SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();

            AlbumSimplified[] albums = spotifyApi.searchAlbums(query).limit(5).build().execute().getItems();
            if (albums.length == 0) {
                return CommandResponse.text("No tracks found.");
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

                message.append(String.format("%d. %s by %s\n",
                        i + 1,
                        markdownLinkNoEmbed(album.getName(), albumUrl),
                        artistString
                ));
            }

            return handleResult(message.toString(), "album", albumLinks);
        } catch (Exception e) {
            logger.error("An error occurred while searching for Spotify albums.");
            return CommandResponse.text("An error occurred while searching for albums.");
        }
    }

    /**
     * Searches SoundCloud for tracks, playlists, or albums.
     *
     * @param query The query to search for.
     * @param type The type of search.
     * @return The command response.
     */
    private CommandResponse searchSoundcloud(String query, String type) {
        String apiResponse = SoundCloudAPI.search(query, type + "s", 5);
        if (apiResponse == null) {
            return CommandResponse.text("An error occurred while searching for " + type + "s.");
        }

        JSONObject response = new JSONObject(apiResponse);
        if (response.getInt("total_results") == 0) {
            return CommandResponse.text("No " + type + "s found.");
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

        StringBuilder message = new StringBuilder(String.format("Found %ss from search: %s\n", type, markdownBold(query)));
        for (int i = 0; i < links.length; i++) {
            message.append(String.format("%d. %s by %s\n",
                    i + 1,
                    markdownLinkNoEmbed(titles[i], links[i]),
                    artists[i]
            ));
        }

        return handleResult(message.toString(), type, links);
    }

    /**
     * Handles the result of the search.
     *
     * @param message The message to send.
     * @param type The type of search.
     * @param links The links to the search results.
     * @return The command response.
     */
    private CommandResponse handleResult(String message, String type, String[] links) {
        message += String.format("Which %s would you like to play? Press the corresponding button, or %s to cancel.", type, EMOJIS.getFirst());

        EmojiMapping<ThrowingConsumer<ButtonWrapper>> buttons = doResults(links);

        return CommandResponse.builder()
                .setContent(StringEscapeUtils.unescapeHtml4(message))
                .setPostExecutionFromMessage(success -> {
                    Pages.buttonize(success, buttons, true, false);
                    scheduledService.schedule(() -> Pages.clearButtons(success), 1, TimeUnit.MINUTES); // Clear buttons after 1 minute
                })
                .build();
    }

    /**
     * Sends the selected result to the music manager.
     *
     * @param links The links to the search results.
     * @return The buttons to deal with.
     */
    private static EmojiMapping<ThrowingConsumer<ButtonWrapper>> doResults(String[] links) {
        ThrowingConsumer<ButtonWrapper> handleButton = (wrapper) -> {
            Emoji emoji = wrapper.getButton().getEmoji();
            if (emoji == null) {
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

            MessageChannel channel = wrapper.getChannel();
            CommandResponse response = PlayerManager.getInstance().loadAndPlay(wrapper.getChannel(), wrapper.getMember(), links[index], TrackType.TRACK);
            channel.sendMessage(response.asMessageCreateData()).queue(response.postExecutionFromMessage());
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

    @Override
    public Boolean isHidden() {
        return false;
    }
}