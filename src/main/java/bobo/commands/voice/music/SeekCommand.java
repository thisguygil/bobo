package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.utils.TimeFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import static bobo.utils.StringUtils.markdownBold;

public class SeekCommand extends AMusicCommand {
    /**
     * Creates a new seek command.
     */
    public SeekCommand() {
        super(Commands.slash("seek", "Seeks to specified position in the current track.")
                .addSubcommands(
                        new SubcommandData("forward", "Seeks forward by specified number of seconds.")
                                .addOption(OptionType.INTEGER, "seconds", "Number of seconds to seek forward.", true),
                        new SubcommandData("backward", "Seeks backward by specified number of seconds.")
                                .addOption(OptionType.INTEGER, "seconds", "Number of seconds to seek backward.", true),
                        new SubcommandData("position", "Seeks to specified position.")
                                .addOption(OptionType.STRING, "position", "Position to seek to. Format: HH:MM:SS", true)
                )
        );
    }

    @Override
    public String getName() {
        return "seek";
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return CommandResponse.text("There is nothing currently playing.");
        }

        String subcommand = getSubcommandName(0);
        assert subcommand != null;
        return switch (subcommand) {
            case "forward" -> seekForward(source);
            case "backward" -> seekBackward(source);
            case "position" -> seekPosition(source);
            default -> CommandResponse.text("Invalid usage. Use `/help seek` for more information.");
        };
    }

    /**
     * Seeks forward by specified number of seconds.
     */
    private CommandResponse seekForward(CommandSource source) {
        int seconds;
        try {
            seconds = Integer.parseInt(getOptionValue("seconds", 1));
        } catch (Exception e) {
            return CommandResponse.text("Invalid usage. Use `/help seek` for more information.");
        }
        if (seconds <= 0) {
            return CommandResponse.text("Number of seconds must be positive.");
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        currentAudioTrack.setPosition(currentAudioTrack.getPosition() + seconds * 1000L);
        return CommandResponse.text("Seeked forward by %s seconds.", markdownBold(seconds));
    }

    /**
     * Seeks backward by specified number of seconds.
     */
    private CommandResponse seekBackward(CommandSource source) {
        int seconds;
        try {
            seconds = Integer.parseInt(getOptionValue("seconds", 1));
        } catch (Exception e) {
            return CommandResponse.text("Invalid usage. Use `/help seek` for more information.");
        }
        if (seconds <= 0) {
            return CommandResponse.text("Number of seconds must be positive.");
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        currentAudioTrack.setPosition(currentAudioTrack.getPosition() - seconds * 1000L);
        return CommandResponse.text("Seeked backward by %s seconds.", markdownBold(seconds));
    }

    /**
     * Seeks to specified position in the current track.
     */
    private CommandResponse seekPosition(CommandSource source) {
        String position;
        try {
            position = getOptionValue("position", 1);
        } catch (Exception e) {
            return CommandResponse.text("Invalid usage. Use `/help seek` for more information.");
        }
        long time = TimeFormat.parseTime(position);
        if (time == -1) {
            return CommandResponse.text("Invalid time format. Format: **HH:MM:SS**");
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        currentAudioTrack.setPosition(time);
        return CommandResponse.text("Seeked to " + markdownBold(position));
    }

    @Override
    public String getHelp() {
        return """
                Seeks to specified position in the current track.
                Usage: `/seek <subcommand>`
                Subcommands:
                * `forward <seconds>` - Seeks forward by <seconds> seconds.
                * `backward <seconds>` - Seeks backward by <seconds> seconds.
                * `position <position>` - Seeks to <position>. Format: HH:MM:SS""";
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}