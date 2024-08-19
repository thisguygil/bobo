package bobo.commands.voice.music;

import bobo.utils.StringUtils;
import bobo.utils.TrackType;
import bobo.utils.YouTubeUtil;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.validator.routines.UrlValidator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayCommand extends AbstractMusic {
    /**
     * Creates a new play command.
     */
    public PlayCommand() {
        super(Commands.slash("play", "Joins the voice channel and plays given track.")
                .addSubcommands(
                        new SubcommandData("track", "Plays given track url or searches YouTube and plays first result.")
                                .addOption(OptionType.STRING, "track", "Url to play or query to search", true),
                        new SubcommandData("file", "Plays audio from attached audio/video file.")
                                .addOption(OptionType.ATTACHMENT, "file", "Audio/video file to play", true)
                )
        );
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (!ensureConnected(event)) {
            return;
        }

        String subcommandName = event.getSubcommandName();
        assert subcommandName != null;
        switch (subcommandName) {
                case "track" -> playTrack();
                case "file" -> playFile();
                default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
        }
    }

    /**
     * Plays a track.
     */
    private void playTrack() {
        String track = Objects.requireNonNull(event.getOption("track")).getAsString();
        if ((new UrlValidator()).isValid(track)) {
            playerManager.loadAndPlay(event, track, TrackType.TRACK);
        } else {
            try {
                List<SearchResult> videoSearch = YouTubeUtil.searchForVideos(track);
                if (videoSearch == null) {
                    hook.editOriginal("Nothing found by " + StringUtils.markdownBold(track) + ".").queue();
                    return;
                }

                playerManager.loadAndPlay(event, "https://www.youtube.com/watch?v=" + videoSearch.get(0).getId().getVideoId(), TrackType.TRACK);
            } catch (Exception e) {
                hook.editOriginal("Nothing found by " + StringUtils.markdownBold(track) + ".").queue();
                e.printStackTrace();
            }
        }
    }

    /**
     * Plays an audio/video file.
     */
    private void playFile() {
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
        if (!isAudioFile(attachment.getFileName())) {
            hook.editOriginal("Please attach a valid audio file.").queue();
            return;
        }

        playerManager.loadAndPlay(event, attachment.getUrl(), TrackType.FILE);
    }

    /**
     * Checks whether given file name is a valid audio file name
     *
     * @param fileName the file name
     * @return true if the given file is a valid audio file name, false otherwise
     */
    private boolean isAudioFile(@Nonnull String fileName) {
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

    @Override
    public String getHelp() {
        return """
                Joins the voice channel and plays given track.
                Usage: `/play <subcommand>`
                Subcommands:
                * `track <track>`: Plays <track> (or searches YouTube for it and plays the first result, use /search otherwise).
                * `file <file>`: Plays audio from attached audio/video file.""";
    }

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }
}