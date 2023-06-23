package bobo.commands.voice.music;

import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ResumeCommand extends AbstractMusic {
    /**
     * Creates a new resume command.
     */
    public ResumeCommand() {
        super(Commands.slash("resume", "Resumes the currently paused track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently paused.").queue();
            return;
        }

        if (player.isPaused()) {
            player.setPaused(false);
            hook.editOriginal("Resumed").queue();
        } else {
            hook.editOriginal("The player is already playing. Use `/pause` to pause.").queue();
        }
    }

    @Override
    public String getName() {
        return "resume";
    }
}
