package bobo.commands.voice.music;

import bobo.commands.ai.TTSCommand;
import bobo.utils.TrackType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.io.File;

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
            hook.editOriginal("Skipped." + (scheduler.looping ? " Looping has been turned off." : "")).queue();
            scheduler.looping = false;
            scheduler.nextTrack();
            if (currentTrack.trackType() == TrackType.TTS) {
                File file = new File(currentTrack.track().getInfo().uri);
                if (file.exists() && !file.delete()) {
                    System.err.println("Failed to delete TTS file: " + file.getName());
                }
                TTSCommand.removeTTSMessage(file.getName());
            }
        }
    }

    @Override
    public String getName() {
        return "skip";
    }
}