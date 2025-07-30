package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ResumeCommand extends AMusicCommand {
    /**
     * Creates a new resume command.
     */
    public ResumeCommand() {
        super(Commands.slash("resume", "Resumes the currently paused track."));
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return CommandResponse.text("There is nothing currently paused.");
        }

        if (player.isPaused()) {
            player.setPaused(false);
            return CommandResponse.text("Resumed.");
        } else {
            return CommandResponse.text("The player is already playing. Use `/pause` to pause.");
        }
    }

    @Override
    public String getName() {
        return "resume";
    }

    @Override
    public String getHelp() {
        return """
                Resumes the currently paused track. If the player is already playing, it will not do anything. Use `/pause` to pause.
                Usage: `/resume`""";
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}
