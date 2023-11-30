package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.Spotify;
import bobo.utils.TrackType;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.managers.AudioManager;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SearchCommand extends AbstractMusic {
    private static final Map<Long, SlashCommandInteractionEvent> MESSAGE_EVENT_MAP = new HashMap<>();
    private static final Map<SlashCommandInteractionEvent, String[]> EVENT_LINKS_MAP = new HashMap<>();
    private static final Emoji xEmoji = Emoji.fromUnicode("U+274C");
    private static final Emoji oneEmoji = Emoji.fromUnicode("U+0031 U+20E3");
    private static final Emoji twoEmoji = Emoji.fromUnicode("U+0032 U+20E3");
    private static final Emoji threeEmoji = Emoji.fromUnicode("U+0033 U+20E3");
    private static final Emoji fourEmoji = Emoji.fromUnicode("U+0034 U+20E3");
    private static final Emoji fiveEmoji = Emoji.fromUnicode("U+0035 U+20E3");

    /**
     * Creates a new search command.
     */
    public SearchCommand() {
        super(Commands.slash("search", "Searches YouTube/Spotify for a track/playlist/album, and plays requested result.")
                .addSubcommandGroups(
                        new SubcommandGroupData("youtube", "Searches YouTube for a track/playlist.")
                                .addSubcommands(
                                        new SubcommandData("track", "Searches YouTube for a track.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("playlist", "Searches YouTube for a playlist.")
                                                .addOption(OptionType.STRING, "query", "What to search", true)
                                ),
                        new SubcommandGroupData("spotify", "Searches Spotify for a track/playlist/album (NOTE: PLAYS THROUGH YOUTUBE).")
                                .addSubcommands(
                                        new SubcommandData("track", "Searches Spotify for a track.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("playlist", "Searches Spotify for a playlist.")
                                                .addOption(OptionType.STRING, "query", "What to search", true),
                                        new SubcommandData("album", "Searches Spotify for an album.")
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
            default -> throw new IllegalStateException("Unexpected value: " + subcommandGroupName);
        }
    }

    /**
     * Searches YouTube for tracks.
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
     */
    private void searchSpotifyTrack(String query) {
        try {
            SpotifyApi spotifyApi = Spotify.getSpotifyApi();

            Track spotifyTrack = spotifyApi.searchTracks(query).build().execute().getItems()[0];
            ArtistSimplified[] artists = spotifyTrack.getArtists();

            StringBuilder spotifyQuery = new StringBuilder(spotifyTrack.getName() + " ");
            for (int i = 0; i < artists.length; i++) {
                spotifyQuery.append(artists[i].getName());
                if (i != artists.length - 1) {
                    spotifyQuery.append(" ");
                }
            }

            String[] videoLinks = YouTubeUtil.searchForVideos(spotifyQuery.toString());
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

            message.append("\nPlease select a track to play by selecting the proper reaction, or the :x: reaction to cancel.")
                    .append("\n\n**NOTE:** This plays the track through YouTube, not Spotify.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, videoLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for videos.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches Spotify for playlists.
     */
    private void searchSpotifyPlaylist(String query) {
        try {
            SpotifyApi spotifyApi = Spotify.getSpotifyApi();

            PlaylistSimplified spotifyPlaylist = spotifyApi.searchPlaylists(query).build().execute().getItems()[0];

            String[] playlistLinks = YouTubeUtil.searchForPlaylists(spotifyPlaylist.getName());
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

            message.append("\nPlease select a playlist to play by selecting the proper reaction, or the :x: reaction to cancel.")
                    .append("\n\n**NOTE:** This plays the playlist through YouTube, not Spotify.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, playlistLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for playlists.").queue();
            e.printStackTrace();
        }
    }

    /**
     * Searches Spotify for albums.
     */
    private void searchSpotifyAlbum(String query) {
        try {
            SpotifyApi spotifyApi = Spotify.getSpotifyApi();

            AlbumSimplified spotifyAlbum = spotifyApi.searchAlbums(query).build().execute().getItems()[0];
            ArtistSimplified[] artists = spotifyAlbum.getArtists();

            StringBuilder spotifyQuery = new StringBuilder(spotifyAlbum.getName() + " ");
            for (int i = 0; i < artists.length; i++) {
                spotifyQuery.append(artists[i].getName());
                if (i != artists.length - 1) {
                    spotifyQuery.append(" ");
                }
            }

            String[] playlistLinks = YouTubeUtil.searchForPlaylists(spotifyQuery.toString());
            if (playlistLinks == null) {
                hook.editOriginal("No albums found.").queue();
                return;
            }

            EVENT_LINKS_MAP.put(event, playlistLinks);

            StringBuilder message = new StringBuilder("Found albums from search: **" + query + "**\n");
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

            message.append("\nPlease select an album to play by selecting the proper reaction, or the :x: reaction to cancel.")
                    .append("\n\n**NOTE:** This plays the album through a YouTube playlist, not Spotify.");
            hook.editOriginal(message.toString()).queue(success -> addReactions(success, playlistLinks.length));
            MESSAGE_EVENT_MAP.put(event.getHook().retrieveOriginal().complete().getIdLong(), event);
        } catch (Exception e) {
            hook.editOriginal("An error occurred while searching for albums.").queue();
            e.printStackTrace();
        }
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
        if (reaction.equals(xEmoji)) {
            long messageId = reactionEvent.getMessageIdLong();
            MESSAGE_EVENT_MAP.remove(messageId);
            EVENT_LINKS_MAP.remove(commandEvent);
            reactionEvent.getChannel().deleteMessageById(messageId).queue();
            return;
        } else if (reaction.equals(oneEmoji)) {
            index = 0;
        } else if (reaction.equals(twoEmoji)) {
            index = 1;
        } else if (reaction.equals(threeEmoji)) {
            index = 2;
        } else if (reaction.equals(fourEmoji)) {
            index = 3;
        } else if (reaction.equals(fiveEmoji)) {
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
        message.addReaction(xEmoji).queue();
        switch (numLinks) {
            case 5 -> {
                message.addReaction(oneEmoji).queue();
                message.addReaction(twoEmoji).queue();
                message.addReaction(threeEmoji).queue();
                message.addReaction(fourEmoji).queue();
                message.addReaction(fiveEmoji).queue();
            }
            case 4 -> {
                message.addReaction(oneEmoji).queue();
                message.addReaction(twoEmoji).queue();
                message.addReaction(threeEmoji).queue();
                message.addReaction(fourEmoji).queue();
            }
            case 3 -> {
                message.addReaction(oneEmoji).queue();
                message.addReaction(twoEmoji).queue();
                message.addReaction(threeEmoji).queue();
            }
            case 2 -> {
                message.addReaction(oneEmoji).queue();
                message.addReaction(twoEmoji).queue();
            }
            case 1 -> message.addReaction(oneEmoji).queue();
        }
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
        message.removeReaction(xEmoji).queue();
        switch (numLinks) {
            case 5 -> {
                message.removeReaction(oneEmoji).queue();
                message.removeReaction(twoEmoji).queue();
                message.removeReaction(threeEmoji).queue();
                message.removeReaction(fourEmoji).queue();
                message.removeReaction(fiveEmoji).queue();
            }
            case 4 -> {
                message.removeReaction(oneEmoji).queue();
                message.removeReaction(twoEmoji).queue();
                message.removeReaction(threeEmoji).queue();
                message.removeReaction(fourEmoji).queue();
            }
            case 3 -> {
                message.removeReaction(oneEmoji).queue();
                message.removeReaction(twoEmoji).queue();
                message.removeReaction(threeEmoji).queue();
            }
            case 2 -> {
                message.removeReaction(oneEmoji).queue();
                message.removeReaction(twoEmoji).queue();
            }
            case 1 -> message.removeReaction(oneEmoji).queue();
        }
    }
}