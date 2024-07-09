package bobo.commands.general;

import bobo.commands.AbstractCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public abstract class AbstractGeneral extends AbstractCommand {
    /**
     * Creates a new general command.
     *
     * @param commandData The command data.
     */
    public AbstractGeneral(CommandData commandData) {
        super(commandData);
    }

    @Override
    public void handleCommand() {
        handleGeneralCommand();
    }

    /**
     * Handles the general command.
     */
    protected abstract void handleGeneralCommand();

    @Override
    protected List<Permission> getCommandPermissions() {
        List<Permission> permissions = getGeneralCommandPermissions();
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        return permissions;
    }

    /**
     * Gets the permissions required for the general command.
     *
     * @return The permissions required for the general command.
     */
    protected abstract List<Permission> getGeneralCommandPermissions();
}
