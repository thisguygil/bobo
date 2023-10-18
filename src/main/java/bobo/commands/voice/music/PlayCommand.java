package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.utils.Spotify;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.apache.commons.validator.routines.UrlValidator;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.*;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Objects;

public class PlayCommand extends AbstractMusic {
    /**
     * Creates a new play command.
     */
    public PlayCommand() {
        super(Commands.slash("play", "Joins the voice channel and plays given track.")
                .addSubcommandGroups(new SubcommandGroupData("youtube", "Plays/searches a YouTube track/playlist.")
                        .addSubcommands(
                                new SubcommandData("track", "Plays a YouTube track.")
                                        .addOption(OptionType.STRING, "track", "URL to play or query to search", true),
                                new SubcommandData("playlist", "Plays a YouTube playlist.")
                                        .addOption(OptionType.STRING, "playlist", "URL to play or query to search", true)
                        )
                )
                .addSubcommandGroups(new SubcommandGroupData("spotify", "Plays/searches a Spotify track/playlist/album.")
                        .addSubcommands(
                                new SubcommandData("track", "Plays a Spotify track.")
                                        .addOption(OptionType.STRING, "track", "URL to play or query to search", true),
                                new SubcommandData("playlist", "Plays a Spotify playlist.")
                                        .addOption(OptionType.STRING, "playlist", "URL to play or query to search", true),
                                new SubcommandData("album", "Plays a Spotify album.")
                                        .addOption(OptionType.STRING, "album", "URL to play or query to search", true)
                        )
                )
                .addSubcommands(new SubcommandData("file", "Plays audio from attached audio/video file.")
                        .addOption(OptionType.ATTACHMENT, "file", "Audio/video file to play", true))
        );
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();
        if (!event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
            if (!JoinCommand.join(event)) {
                return;
            }
        } else {
            if (Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel() == null) {
                event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
                return;
            }
        }

        String subcommandGroupName = event.getSubcommandGroup();
        String subcommandName = event.getSubcommandName();
        assert subcommandName != null;

        String trackURL;
        if (subcommandGroupName == null) {
            trackURL = playFile();
        } else {
            trackURL = switch (subcommandGroupName) {
                case "youtube" -> switch (subcommandName) {
                    case "track" -> playYoutubeTrack();
                    case "playlist" -> playYoutubePlaylist();
                    default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
                };
                case "spotify" -> switch (subcommandName) {
                    case "track" -> playSpotifyTrack();
                    case "playlist" -> playSpotifyPlaylist();
                    case "album" -> playSpotifyAlbum();
                    default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
                };
                default -> throw new IllegalStateException("Unexpected value: " + subcommandGroupName);
            };
        }

        if (trackURL != null) {
            playerManager.loadAndPlay(event, trackURL);
        }
    }

    /**
     * Plays a YouTube track.
     */
    @Nullable
    private String playYoutubeTrack() {
        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        if ((new UrlValidator()).isValid(track)) {
            String youtubeTrackRegex = "^(https?://)?(www\\.)?(m\\.)?youtube\\.com/watch\\?v=.*|^(https?://)?youtu.be/.*";
            if (track.matches(youtubeTrackRegex)) {
                return track;
            } else {
                hook.editOriginal("Please enter a valid YouTube link.").queue();
                return null;
            }
        } else {
            try {
                return YouTubeUtil.searchForVideo(track);
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + track + "**.").queue();
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Plays a YouTube playlist.
     */
    @Nullable
    private String playYoutubePlaylist() {
        String playlist = Objects.requireNonNull(event.getOption("playlist")).getAsString();
        if ((new UrlValidator()).isValid(playlist)) {
            String youtubePlaylistRegex = "^(https?://)?(www\\.)?(m\\.)?youtube\\.com/playlist\\?list=.*";

            if (playlist.matches(youtubePlaylistRegex)) {
                return playlist;
            } else {
                hook.editOriginal("Please enter a valid YouTube playlist link.").queue();
                return null;
            }
        } else {
            try {
                return YouTubeUtil.searchForPlaylist(playlist);
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + playlist + "**.").queue();
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Plays a Spotify track.
     */
    @Nullable
    private String playSpotifyTrack() {
        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        if ((new UrlValidator()).isValid(track)) {
            String spotifyTrackRegex = "^(https?://)?open.spotify.com/track/.*";
            if (track.matches(spotifyTrackRegex)) {
                try {
                    SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                    URI uri = URI.create(track);
                    String path = uri.getPath();
                    String[] pathComponents = path.split("/");

                    Track spotifyTrack = spotifyApi.getTrack(pathComponents[pathComponents.length - 1]).build().execute();
                    ArtistSimplified[] artists = spotifyTrack.getArtists();

                    StringBuilder query = new StringBuilder(spotifyTrack.getName() + " ");
                    for (int i = 0; i < artists.length; i++) {
                        query.append(artists[i].getName());
                        if (i != artists.length - 1) {
                            query.append(" ");
                        }
                    }

                    return YouTubeUtil.searchForVideo(query.toString());
                } catch (Exception e) {
                    hook.editOriginal("Error: " + e.getMessage()).queue();
                    e.printStackTrace();
                    return null;
                }
            } else {
                hook.editOriginal("Please enter a valid Spotify track link.").queue();
                return null;
            }
        } else {
            try {
                SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                Track spotifyTrack = spotifyApi.searchTracks(track).build().execute().getItems()[0];
                ArtistSimplified[] artists = spotifyTrack.getArtists();

                StringBuilder query = new StringBuilder(spotifyTrack.getName() + " ");
                for (int i = 0; i < artists.length; i++) {
                    query.append(artists[i].getName());
                    if (i != artists.length - 1) {
                        query.append(" ");
                    }
                }

                return YouTubeUtil.searchForVideo(query.toString());
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + track + "**.").queue();
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Plays a Spotify playlist.
     */
    @Nullable
    private String playSpotifyPlaylist() {
        String playlist = Objects.requireNonNull(event.getOption("playlist")).getAsString();
        if ((new UrlValidator()).isValid(playlist)) {
            String spotifyPlaylistRegex = "^(https?://)?open.spotify.com/playlist/.*";
            if (playlist.matches(spotifyPlaylistRegex)) {
                try {
                    SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                    URI uri = URI.create(playlist);
                    String path = uri.getPath();
                    String[] pathComponents = path.split("/");

                    Playlist spotifyPlaylist = spotifyApi.getPlaylist(pathComponents[pathComponents.length - 1]).build().execute();

                    return YouTubeUtil.searchForPlaylist(spotifyPlaylist.getName());
                } catch (Exception e) {
                    hook.editOriginal("Error: " + e.getMessage()).queue();
                    e.printStackTrace();
                    return null;
                }
            } else {
                hook.editOriginal("Please enter a valid Spotify playlist link.").queue();
                return null;
            }
        } else {
            try {
                SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                PlaylistSimplified spotifyPlaylist = spotifyApi.searchPlaylists(playlist).build().execute().getItems()[0];

                return YouTubeUtil.searchForPlaylist(spotifyPlaylist.getName());
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + playlist + "**.").queue();
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Plays a Spotify album.
     */
    @Nullable
    private String playSpotifyAlbum() {
        String album = Objects.requireNonNull(event.getOption("album")).getAsString();
        if ((new UrlValidator()).isValid(album)) {
            String spotifyAlbumRegex = "^(https?://)?open.spotify.com/album/.*";
            if (album.matches(spotifyAlbumRegex)) {
                try {
                    SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                    URI uri = URI.create(album);
                    String path = uri.getPath();
                    String[] pathComponents = path.split("/");

                    Album spotifyAlbum = spotifyApi.getAlbum(pathComponents[pathComponents.length - 1]).build().execute();
                    ArtistSimplified[] artists = spotifyAlbum.getArtists();

                    StringBuilder query = new StringBuilder(spotifyAlbum.getName() + " ");
                    for (int i = 0; i < artists.length; i++) {
                        query.append(artists[i].getName());
                        if (i != artists.length - 1) {
                            query.append(" ");
                        }
                    }

                    return YouTubeUtil.searchForPlaylist(query.toString());
                } catch (Exception e) {
                    hook.editOriginal("Error: " + e.getMessage()).queue();
                    e.printStackTrace();
                    return null;
                }
            } else {
                hook.editOriginal("Please enter a valid Spotify album link.").queue();
                return null;
            }
        } else {
            try {
                SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                AlbumSimplified spotifyAlbum = spotifyApi.searchAlbums(album).build().execute().getItems()[0];
                ArtistSimplified[] artists = spotifyAlbum.getArtists();

                StringBuilder query = new StringBuilder(spotifyAlbum.getName() + " ");
                for (int i = 0; i < artists.length; i++) {
                    query.append(artists[i].getName());
                    if (i != artists.length - 1) {
                        query.append(" ");
                    }
                }

                return YouTubeUtil.searchForPlaylist(query.toString());
            } catch (Exception e) {
                hook.editOriginal("Nothing found by **" + album + "**.").queue();
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Plays an audio/video file.
     */
    @Nullable
    private String playFile() {
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
        if (!isAudioFile(attachment.getFileName())) {
            hook.editOriginal("Please attach a valid audio file.").queue();
            return null;
        }

        return attachment.getUrl();
    }

    /**
     * Checks whether given file name is a valid audio file name
     *
     * @param fileName the file name
     * @return true if the given file is a valid audio file name, false otherwise
     */
    private boolean isAudioFile(String fileName) {
        String[] audioExtensions = {".mp3", ".mp4", ".wav", ".ogg", ".flac", ".m4a", ".aac"};
        String fileExtension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        for (String audioExtension : audioExtensions) {
            if (fileExtension.equals(audioExtension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "play";
    }
}