package bobo.commands.voice.music;

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
            hook.editOriginal("Skipped." + (scheduler.looping ? " Looping has been turned off." : "")).queue();
            scheduler.looping = false;
            scheduler.nextTrack();
        }
    }

    @Override
    public String getName() {
        return "skip";
    }
}
