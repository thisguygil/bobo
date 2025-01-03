package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

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
            return new CommandResponse("There is nothing currently paused.");
        }

        if (player.isPaused()) {
            player.setPaused(false);
            return new CommandResponse("Resumed.");
        } else {
            return new CommandResponse("The player is already playing. Use `/pause` to pause.");
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
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}
