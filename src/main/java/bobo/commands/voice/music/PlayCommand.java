package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.utils.Spotify;
import bobo.utils.YouTubeUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.validator.routines.UrlValidator;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.net.URI;
import java.util.Objects;

public class PlayCommand extends AbstractMusic {
    /**
     * Creates a new play command.
     */
    public PlayCommand() {
        super(Commands.slash("play", "Joins the voice channel and plays given track.")
                .addSubcommands(new SubcommandData("track", "Joins the voice channel and plays given YouTube/Spotify link or query.")
                        .addOption(OptionType.STRING, "track", "URL to play or query to search", true))
                .addSubcommands(new SubcommandData("file", "Joins the voice channel and plays audio from attached audio/video file.")
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

        String trackURL = null;
        switch (Objects.requireNonNull(event.getSubcommandName())) {
            case "track" -> {
                String track = Objects.requireNonNull(event.getOption("track")).getAsString();
                if ((new UrlValidator()).isValid(track)) {
                    String youtubeRegex = "^(https?://)?(www\\.)?(m\\.)?youtube\\.com/watch\\?v=.*|^(https?://)?youtu.be/.*";
                    String spotifyRegex = "^(https?://)?open.spotify.com/(track|playlist|album)/.*";

                    if (track.matches(youtubeRegex)) {
                        trackURL = track;
                    } else if (track.matches(spotifyRegex)) {
                        try {
                            SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                            URI uri = URI.create(track);
                            String path = uri.getPath();
                            String[] pathComponents = path.split("/");

                            Track spotifyTrack = spotifyApi.getTrack(pathComponents[pathComponents.length - 1]).build().execute();

                            trackURL = YouTubeUtil.searchForVideo(spotifyTrack.getName() + " " + spotifyTrack.getArtists()[0].getName());
                        } catch (Exception e) {
                            hook.editOriginal("Error: " + e.getMessage()).queue();
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        hook.editOriginal("Please enter a valid YouTube or Spotify link.").queue();
                        return;
                    }
                } else {
                    try {
                        trackURL = YouTubeUtil.searchForVideo(track);
                    } catch (Exception e) {
                        hook.editOriginal("Nothing found by **" + track + "**.").queue();
                        e.printStackTrace();
                        return;
                    }
                }
            } case "file" -> {
                Message.Attachment attachment = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
                if (!isAudioFile(attachment.getFileName())) {
                    hook.editOriginal("Please attach a valid audio file.").queue();
                    return;
                }

                trackURL = attachment.getUrl();
            }
        }

        playerManager.loadAndPlay(event, trackURL);
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