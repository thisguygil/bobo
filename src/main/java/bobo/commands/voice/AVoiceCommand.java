package bobo.commands.voice;

import bobo.commands.ADualCommand;
import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public abstract class AVoiceCommand extends ADualCommand {
    /**
     * Creates a new voice command.
     *
     * @param commandData The command data.
     */
    public AVoiceCommand(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected CommandResponse handleCommand() {
        return handleVoiceCommand();
    }

    /**
     * Handles the voice command.
     */
    protected abstract CommandResponse handleVoiceCommand();

    @Override
    protected List<Permission> getCommandPermissions() {
        List<Permission> permissions = getVoiceCommandPermissions();
        permissions.add(Permission.VOICE_CONNECT);
        return permissions;
    }

    /**
     * Gets the permissions required for the voice command.
     *
     * @return The permissions required for the voice command.
     */
    protected abstract List<Permission> getVoiceCommandPermissions();
}
