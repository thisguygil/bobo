package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.lavaplayer.TrackType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SkipCommand extends AMusicCommand {
    /**
     * Creates a new skip command.
     */
    public SkipCommand() {
        super(Commands.slash("skip", "Skips the current track."));
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return new CommandResponse("There is nothing currently playing.");
        } else {
            boolean wasLoopingTrack = scheduler.looping == LoopCommand.looping.TRACK;
            if (wasLoopingTrack) {
                scheduler.looping = LoopCommand.looping.NONE;
            }
            if (currentTrack.trackType() == TrackType.TTS) {
                TTSCommand.nextTTSMessage(getGuild(), currentTrack.track());
            }
            scheduler.nextTrack();
            return new CommandResponse("Skipped." + (wasLoopingTrack ? " Looping has been turned off." : ""));
        }
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getHelp() {
        return """
                Skips the current track.
                Usage: `/skip`""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}