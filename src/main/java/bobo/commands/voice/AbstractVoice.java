package bobo.commands.voice;

import bobo.commands.AbstractSlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public abstract class AbstractVoice extends AbstractSlashCommand {
    /**
     * Creates a new voice command.
     *
     * @param commandData The command data.
     */
    public AbstractVoice(CommandData commandData) {
        super(commandData);
    }

    @Override
    public void handleCommand() {
        handleVoiceCommand();
    }

    /**
     * Handles the voice command.
     */
    protected abstract void handleVoiceCommand();

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
