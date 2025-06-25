package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class PauseCommand extends AMusicCommand {
    /**
     * Creates a new pause command.
     */
    public PauseCommand() {
        super(Commands.slash("pause", "Pauses the currently playing track."));
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return CommandResponse.text("There is nothing currently playing.");
        }

        if (!player.isPaused()) {
            player.setPaused(true);
            return CommandResponse.text("Paused.");
        } else {
            return CommandResponse.text("The player is already paused. Use `/resume` to resume.");
        }
    }

    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public String getHelp() {
        return """
                Pauses the currently playing track. If the player is already paused, it will not do anything. Use `/resume` to resume.
                Usage: `/pause`""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}
