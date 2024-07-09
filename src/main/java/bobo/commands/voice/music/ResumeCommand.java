package bobo.commands.voice.music;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

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
}
