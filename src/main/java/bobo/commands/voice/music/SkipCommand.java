package bobo.commands.voice.music;

public class SkipCommand extends AbstractMusic {
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
