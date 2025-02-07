package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.utils.StringUtils;
import bobo.lavaplayer.TrackType;
import bobo.utils.api_clients.YouTubeUtil;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.validator.routines.UrlValidator;

import javax.annotation.Nonnull;
import java.util.List;

public class PlayCommand extends AMusicCommand {
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
    protected CommandResponse handleMusicCommand() {
        if (!ensureConnected(getMember())) {
            return new CommandResponse("You must be connected to a voice channel to use this command.");
        }

        return switch (source) {
            case SLASH_COMMAND -> {
                String subcommandName = getSubcommandName(0);
                yield switch (subcommandName) {
                    case "track" -> playTrack(0);
                    case "file" -> playFile();
                    default -> new CommandResponse("Invalid usage. Use `/help play` for more information.");
                };
            }
            case MESSAGE_COMMAND -> {
                if (args.isEmpty()) {
                    yield new CommandResponse("Invalid usage. Use `/help play` for more information.");
                }

                String firstArg = args.getFirst();
                if (firstArg.equals("file")) {
                    yield playFile();
                } else if (firstArg.equals("track")) {
                    yield playTrack(1);
                } else {
                    yield playTrack(0);
                }
            }
        };
    }

    /**
     * Plays a track.
     *
     * @param argIndex the index of the track argument
     * @return the command response
     */
    private CommandResponse playTrack(int argIndex) {
        String track;
        try {
            track = getMultiwordOptionValue("track", argIndex);
        } catch (Exception e) {
            return new CommandResponse("Please provide a track to play.");
        }

        if ((new UrlValidator()).isValid(track)) {
            return playerManager.loadAndPlay((MessageChannel) getChannel(), getMember(), track, TrackType.TRACK);
        } else {
            try {
                List<SearchResult> videoSearch = YouTubeUtil.searchForVideos(track);
                if (videoSearch == null) {
                    return new CommandResponse("Nothing found by " + StringUtils.markdownBold(track) + ".");
                }

                return playerManager.loadAndPlay((MessageChannel) getChannel(), getMember(), "https://www.youtube.com/watch?v=" + videoSearch.getFirst().getId().getVideoId(), TrackType.TRACK);
            } catch (Exception e) {
                return new CommandResponse("Nothing found by " + StringUtils.markdownBold(track) + ".");
            }
        }
    }

    /**
     * Plays an audio/video file.
     *
     * @return the command response
     */
    private CommandResponse playFile() {
        Message.Attachment attachment;
        try {
            attachment = getAttachment("file");
        } catch (Exception e) {
            return new CommandResponse("Please provide a track to play.");
        }

        if (!isAudioFile(attachment.getFileName())) {
            return new CommandResponse("Please attach a valid audio file.");
        }

        return playerManager.loadAndPlay((MessageChannel) getChannel(), getMember(), attachment.getUrl(), TrackType.FILE);
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
    public Boolean shouldBeInvisible() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("p");
    }
}