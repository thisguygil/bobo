package bobo.commands.voice.music;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

public class PauseCommand extends AbstractMusic {
    /**
     * Creates a new pause command.
     */
    public PauseCommand() {
        super(Commands.slash("pause", "Pauses the currently playing track."));
    }

    @Override
    protected void handleMusicCommand() {
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

    @Override
    public String getHelp() {
        return """
                Pauses the currently playing track. If the player is already paused, it will not do anything. Use `/resume` to resume.
                Usage: `/pause`""";
    }

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}
