package bobo.commands.voice.music;

import bobo.utils.TrackType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SkipCommand extends AbstractMusic {
    /**
     * Creates a new skip command.
     */
    public SkipCommand() {
        super(Commands.slash("skip", "Skips the current track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
        } else {
            hook.editOriginal("Skipped." + (scheduler.looping == LoopCommand.looping.TRACK ? " Looping has been turned off." : "")).queue();
            scheduler.nextTrack();
            if (scheduler.looping == LoopCommand.looping.TRACK) {
                scheduler.looping = LoopCommand.looping.NONE;
            }
            if (currentTrack.trackType() == TrackType.TTS) {
                TTSCommand.removeTTSMessage(currentTrack.track());
            }
        }
    }

    @Override
    public String getName() {
        return "skip";
    }
}