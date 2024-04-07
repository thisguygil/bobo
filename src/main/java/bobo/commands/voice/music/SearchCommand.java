package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.managers.AudioManager;
import org.json.JSONArray;
import org.json.JSONObject;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.annotation.Nonnull;
import java.util.*;

public class SearchCommand extends AbstractMusic {
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

        if (event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
            if (Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel() == null) {
                event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
                return;
            }
        }

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
                    case "track" -> searchSoundcloudTrack(query);
                    case "playlist" -> searchSoundcloudPlaylist(query);
                    case "album" -> searchSoundcloudAlbum(query);
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
            String[] videoLinks = YouTubeUtil.searchForVideos(query);
            if (videoLinks == null) {
                hook.editOriginal("No videos found.").queue();
                return;
            }

            EVENT_LINKS_MAP.put(event, videoLinks);

            StringBuilder message = new StringBuilder("Found videos from search: **" + query + "**\n");
            for (int i = 0; i < videoLinks.length; i++) {
                String videoLink = videoLinks[i];
                message.append("**")
                        .append(i + 1)
                        .append(":** [")
                        .append(YouTubeUtil.getVideoTitle(videoLink))
                        .append("](<")
                        .append(videoLink)
                        .append(">)\n");
            }

            message.append("\nPlease select a track to play by selecting the proper reaction, or the :x: reaction to cancel.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, videoLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
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
            String[] playlistLinks = YouTubeUtil.searchForPlaylists(query);
            if (playlistLinks == null) {
                hook.editOriginal("No playlists found.").queue();
                return;
            }

            EVENT_LINKS_MAP.put(event, playlistLinks);

            StringBuilder message = new StringBuilder("Found playlists from search: **" + query + "**\n");
            for (int i = 0; i < playlistLinks.length; i++) {
                String playlistLink = playlistLinks[i];
                message.append("**")
                        .append(i + 1)
                        .append(":** [")
                        .append(YouTubeUtil.getPlaylistTitle(playlistLink))
                        .append("](<")
                        .append(playlistLink)
                        .append(">)\n");
            }

            message.append("\nPlease select a playlist to play by selecting the proper reaction, or the :x: reaction to cancel.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, playlistLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for playlists.").queue();
            e.printStackTrace();
        }
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
            StringBuilder message = new StringBuilder("Found tracks from search: **" + query + "**\n");
            for (int i = 0; i < tracks.length; i++) {
                Track track = tracks[i];
                String trackUrl = track.getExternalUrls().get("spotify");
                trackLinks[i] = trackUrl;

                // Build a string for the artists
                StringBuilder artistString = new StringBuilder();
                for (int j = 0; j < artists[i].length; j++) {
                    artistString.append(artists[i][j].getName());
                    if (j < artists.length - 1) {
                        artistString.append(", ");
                    }
                }

                message.append("**")
                        .append(i + 1)
                        .append(":** [")
                        .append(track.getName())
                        .append("](<")
                        .append(trackUrl)
                        .append(">) by ")
                        .append(artistString)
                        .append("\n");
            }

            EVENT_LINKS_MAP.put(event, trackLinks);

            message.append("\nPlease select a track to play by selecting the proper reaction, or the :x: reaction to cancel.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, tracks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
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

            PlaylistSimplified[] playlists = spotifyApi.searchPlaylists(query).build().execute().getItems();
            if (playlists.length == 0) {
                hook.editOriginal("No playlists found.").queue();
                return;
            }

            String[] playlistLinks = new String[playlists.length];
            StringBuilder message = new StringBuilder("Found playlists from search: **" + query + "**\n");
            for (int i = 0; i < playlistLinks.length; i++) {
                PlaylistSimplified playlist = playlists[i];
                String playlistUrl = playlist.getExternalUrls().get("spotify");
                playlistLinks[i] = playlistUrl;
                message.append("**")
                        .append(i + 1)
                        .append(":** [")
                        .append(playlist.getName())
                        .append("](<")
                        .append(playlistUrl)
                        .append(">)\n");
            }

            EVENT_LINKS_MAP.put(event, playlistLinks);

            message.append("\nPlease select a playlist to play by selecting the proper reaction, or the :x: reaction to cancel.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, playlistLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
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

            AlbumSimplified[] albums = spotifyApi.searchAlbums(query).build().execute().getItems();
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
            StringBuilder message = new StringBuilder("Found albums from search: **" + query + "**\n");
            for (int i = 0; i < albumLinks.length; i++) {
                AlbumSimplified album = albums[i];
                String albumUrl = album.getExternalUrls().get("spotify");
                albumLinks[i] = albumUrl;

                StringBuilder artistString = new StringBuilder();
                for (int j = 0; j < artists[i].length; j++) {
                    artistString.append(artists[i][j].getName());
                    if (j < artists.length - 1) {
                        artistString.append(", ");
                    }
                }

                message.append("**")
                        .append(i + 1)
                        .append(":** [")
                        .append(album.getName())
                        .append("](<")
                        .append(albumUrl)
                        .append(">) by ")
                        .append(artistString)
                        .append(">)\n");
            }

            EVENT_LINKS_MAP.put(event, albumLinks);

            message.append("\nPlease select an album to play by selecting the proper reaction, or the :x: reaction to cancel.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, albumLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for albums.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches SoundCloud for tracks.
     *
     * @param query The query to search for.
     */
    private void searchSoundcloudTrack(String query) {
        String apiResponse = SoundCloudAPI.search(query, "tracks", 5);
        if (apiResponse == null) {
            hook.editOriginal("An error occurred while searching for tracks.").queue();
            return;
        }

        JSONObject response = new JSONObject(apiResponse);
        if (response.getInt("total_results") == 0) {
            hook.editOriginal("No tracks found.").queue();
            return;
        }

        JSONArray collection = response.getJSONArray("collection");
        int collectionLength = collection.length();
        String[] titles = new String[collectionLength];
        String[] links = new String[collectionLength];
        for (int i = 0; i < collectionLength; i++) {
            titles[i] = (collection.getJSONObject(i).getString("title"));
            links[i] = (collection.getJSONObject(i).getString("permalink_url"));
        }

        EVENT_LINKS_MAP.put(event, links);

        StringBuilder message = new StringBuilder("Found tracks from search: **" + query + "**\n");
        for (int i = 0; i < links.length; i++) {
            message.append("**")
                    .append(i + 1)
                    .append(":** [")
                    .append(titles[i])
                    .append("](<")
                    .append(links[i])
                    .append(">)\n");
        }

        message.append("\nPlease select a track to play by selecting the proper reaction, or the :x: reaction to cancel.");
        hook.editOriginal(message.toString()).queue(success -> addReactions(success, links.length));
        MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
    }

    /**
     * Searches SoundCloud for playlists.
     *
     * @param query The query to search for.
     */
    private void searchSoundcloudPlaylist(String query) {
        String apiResponse = SoundCloudAPI.search(query, "playlists", 5);
        if (apiResponse == null) {
            hook.editOriginal("An error occurred while searching for playlists.").queue();
            return;
        }

        JSONObject response = new JSONObject(apiResponse);
        if (response.getInt("total_results") == 0) {
            hook.editOriginal("No playlists found.").queue();
            return;
        }

        JSONArray collection = response.getJSONArray("collection");
        int collectionLength = collection.length();
        String[] titles = new String[collectionLength];
        String[] links = new String[collectionLength];
        for (int i = 0; i < collectionLength; i++) {
            titles[i] = (collection.getJSONObject(i).getString("title"));
            links[i] = (collection.getJSONObject(i).getString("permalink_url"));
        }

        EVENT_LINKS_MAP.put(event, links);

        StringBuilder message = new StringBuilder("Found playlists from search: **" + query + "**\n");
        for (int i = 0; i < links.length; i++) {
            message.append("**")
                    .append(i + 1)
                    .append(":** [")
                    .append(titles[i])
                    .append("](<")
                    .append(links[i])
                    .append(">)\n");
        }

        message.append("\nPlease select a playlist to play by selecting the proper reaction, or the :x: reaction to cancel.");
        hook.editOriginal(message.toString()).queue(success -> addReactions(success, links.length));
        MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
    }

    /**
     * Searches SoundCloud for albums.
     *
     * @param query The query to search for.
     */
    private void searchSoundcloudAlbum(String query) {
        String apiResponse = SoundCloudAPI.search(query, "albums", 5);
        if (apiResponse == null) {
            hook.editOriginal("An error occurred while searching for albums.").queue();
            return;
        }

        JSONObject response = new JSONObject(apiResponse);
        if (response.getInt("total_results") == 0) {
            hook.editOriginal("No albums found.").queue();
            return;
        }

        JSONArray collection = response.getJSONArray("collection");
        int collectionLength = collection.length();
        String[] titles = new String[collectionLength];
        String[] links = new String[collectionLength];
        for (int i = 0; i < collectionLength; i++) {
            titles[i] = (collection.getJSONObject(i).getString("title"));
            links[i] = (collection.getJSONObject(i).getString("permalink_url"));
        }

        EVENT_LINKS_MAP.put(event, links);

        StringBuilder message = new StringBuilder("Found albums from search: **" + query + "**\n");
        for (int i = 0; i < links.length; i++) {
            message.append("**")
                    .append(i + 1)
                    .append(":** [")
                    .append(titles[i])
                    .append("](<")
                    .append(links[i])
                    .append(">)\n");
        }

        message.append("\nPlease select an album to play by selecting the proper reaction, or the :x: reaction to cancel.");
        hook.editOriginal(message.toString()).queue(success -> addReactions(success, links.length));
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

        AudioManager manager = commandEvent.getGuildChannel().getGuild().getAudioManager();
        if (!manager.isConnected()) {
            if (!JoinCommand.join(commandEvent)) {
                return;
            }
        } else {
            if (Objects.requireNonNull(Objects.requireNonNull(commandEvent.getMember()).getVoiceState()).getChannel() != manager.getConnectedChannel()) {
                if (!JoinCommand.join(commandEvent)) {
                    return;
                }
            }
        }

        PlayerManager.getInstance().loadAndPlay(commandEvent, links[index], TrackType.TRACK);
        MESSAGE_EVENT_MAP.remove(reactionEvent.getMessageIdLong());
        EVENT_LINKS_MAP.remove(commandEvent);

        removeReactions(commandEvent, reactionEvent, numLinks);
    }

    /**
     * Helper method to add reactions to the message.
     *
     * @param message The message to add reactions to.
     * @param numLinks The number of links to add reactions for.
     */
    private static void addReactions(@Nonnull Message message, int numLinks) {
        message.addReaction(EmojiType.X.asEmoji()).queue();
        List<Emoji> emojis = List.of(EmojiType.ONE.asEmoji(), EmojiType.TWO.asEmoji(), EmojiType.THREE.asEmoji(), EmojiType.FOUR.asEmoji(), EmojiType.FIVE.asEmoji());
        emojis.stream().limit(numLinks).forEach(emoji -> message.addReaction(emoji).queue());
    }

    /**
     * Helper method to remove reactions from the message.
     * To be used after a track has been selected or the search has been cancelled.
     *
     * @param commandEvent The command event.
     * @param reactionEvent The reaction event.
     * @param numLinks The number of links to remove reactions for.
     */
    private static void removeReactions(@Nonnull SlashCommandInteractionEvent commandEvent, @Nonnull MessageReactionAddEvent reactionEvent, int numLinks) {
        Message message = commandEvent.getHook().retrieveOriginal().complete();
        message.removeReaction(reactionEvent.getReaction().getEmoji(), reactionEvent.retrieveUser().complete()).queue();
        message.removeReaction(EmojiType.X.asEmoji()).queue();
        List<Emoji> emojis = List.of(EmojiType.ONE.asEmoji(), EmojiType.TWO.asEmoji(), EmojiType.THREE.asEmoji(), EmojiType.FOUR.asEmoji(), EmojiType.FIVE.asEmoji());
        emojis.stream().limit(numLinks).forEach(emoji -> message.removeReaction(emoji).queue());
    }

    @Override
    public String getHelp() {
        return """
                Searches YouTube/Spotify/SoundCloud, and plays the requested result.
                Usage: `/search <subcommand>`
                Subcommands:
                * `youtube track <query>`: Search YouTube for a track with <query>
                * `youtube playlist <query>`: Search YouTube for a playlist with <query>
                * `spotify track <query>`: Search Spotify for a track with <query>
                * `spotify playlist <query>`: Search Spotify for a playlist with <query>
                * `spotify album <query>`: Search Spotify for an album with <query>
                * `soundcloud track <query>`: Search SoundCloud for a track with <query>
                * `soundcloud playlist <query>`: Search SoundCloud for a playlist with <query>
                * `soundcloud album <query>`: Search SoundCloud for an album with <query>""";
    }
}