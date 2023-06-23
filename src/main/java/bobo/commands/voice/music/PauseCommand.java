package bobo.commands.voice.music;

import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class PauseCommand extends AbstractMusic {
    /**
     * Creates a new pause command.
     */
    public PauseCommand() {
        super(Commands.slash("pause", "Pauses the currently playing track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        if (!player.isPaused()) {
            player.setPaused(true);
            hook.editOriginal("Paused.").queue();
        } else {
            hook.editOriginal("The player is already paused. Use `/resume` to resume.").queue();
        }
    }

    @Override
    public String getName() {
        return "pause";
    }
}
