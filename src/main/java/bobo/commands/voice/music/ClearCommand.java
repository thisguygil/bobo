package bobo.commands.voice.music;

import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ClearCommand extends AbstractMusic {
    /**
     * Creates a new clear command.
     */
    public ClearCommand() {
        super(Commands.slash("clear", "Clears queue and stops current track."));
    }

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
