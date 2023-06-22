package bobo.commands.voice.music;

public class ClearCommand extends AbstractMusic {
    @Override
    protected void handleMusicCommand() {
        if (player.getPlayingTrack() == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        scheduler.queue.clear();
        scheduler.looping = false;
        player.stopTrack();
        player.setPaused(false);
        hook.editOriginal("Queue cleared.").queue();
    }

    @Override
    public String getName() {
        return "clear";
    }
}
