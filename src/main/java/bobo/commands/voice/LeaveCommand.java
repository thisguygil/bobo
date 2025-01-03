package bobo.commands.voice;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class LeaveCommand extends AVoiceCommand {
    /**
     * Creates a new leave command.
     */
    public LeaveCommand() {
        super(Commands.slash("leave", "Leaves the voice channel."));
    }

    @Override
    protected CommandResponse handleVoiceCommand() {
        Guild guild = getGuild();
        if (!guild.getAudioManager().isConnected()) {
            return new CommandResponse("I must already be connected to a voice channel to use this command.");
        }

        leave(guild);
        return new CommandResponse("Left.");
    }

    /**
     * Leaves the voice channel of the specified guild.
     *
     * @param guild The guild's voice channel to leave.
     */
    public static void leave(@Nonnull Guild guild) {
        guild.getAudioManager().closeAudioConnection();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp() {
        return """
                Leaves the voice channel.
                Usage: `/leave`""";
    }

    @Override
    protected List<Permission> getVoiceCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}
