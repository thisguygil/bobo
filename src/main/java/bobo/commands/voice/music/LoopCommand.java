package bobo.commands.voice.music;

import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class LoopCommand extends AbstractMusic {
    /**
     * Creates a new loop command.
     */
    public LoopCommand() {
        super(Commands.slash("loop", "Loop the currently playing track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (player.getPlayingTrack() == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        scheduler.looping = !scheduler.looping;
        hook.editOriginal("The player has been set to **" + (scheduler.looping ? "" : "not ") + "looping**.").queue();
    }

    @Override
    public String getName() {
        return "loop";
    }
}
