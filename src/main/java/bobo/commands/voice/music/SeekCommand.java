package bobo.commands.voice.music;

import bobo.utils.TimeFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static bobo.utils.StringUtils.markdownBold;

public class SeekCommand extends AbstractMusic {
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
    protected void handleMusicCommand() {
        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        assert subcommand != null;
        switch (subcommand) {
            case "forward" -> seekForward();
            case "backward" -> seekBackward();
            case "position" -> seekPosition();
        }
    }

    /**
     * Seeks forward by specified number of seconds.
     */
    private void seekForward() {
        int seconds = Objects.requireNonNull(event.getOption("seconds")).getAsInt();
        if (seconds <= 0) {
            hook.editOriginal("Number of seconds must be positive.").queue();
            return;
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        currentAudioTrack.setPosition(currentAudioTrack.getPosition() + seconds * 1000L);
        hook.editOriginal("Seeked forward by " + markdownBold(seconds) + " seconds.").queue();
    }

    /**
     * Seeks backward by specified number of seconds.
     */
    private void seekBackward() {
        int seconds = Objects.requireNonNull(event.getOption("seconds")).getAsInt();
        if (seconds <= 0) {
            hook.editOriginal("Number of seconds must be positive.").queue();
            return;
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        currentAudioTrack.setPosition(currentAudioTrack.getPosition() - seconds * 1000L);
        hook.editOriginal("Seeked backward by " + markdownBold(seconds) + " seconds.").queue();
    }

    /**
     * Seeks to specified position in the current track.
     */
    private void seekPosition() {
        String position = Objects.requireNonNull(event.getOption("position")).getAsString();
        long time = TimeFormat.parseTime(position);
        if (time == -1) {
            hook.editOriginal("Invalid time format. Format: **HH:MM:SS**").queue();
            return;
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        currentAudioTrack.setPosition(time);
        hook.editOriginal("Seeked to " + markdownBold(position)).queue();
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
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}