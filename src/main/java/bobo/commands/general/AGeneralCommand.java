package bobo.commands.general;

import bobo.commands.ADualCommand;
import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.List;

public abstract class AGeneralCommand extends ADualCommand {
    /**
     * Creates a new general command.
     *
     * @param commandData The command data.
     */
    public AGeneralCommand(CommandData commandData) {
        super(commandData);
    }

    @Override
    public CommandResponse handleCommand() {
        return handleGeneralCommand();
    }

    /**
     * Handles the general command.
     */
    protected abstract CommandResponse handleGeneralCommand();

    @Override
    public List<Permission> getCommandPermissions() {
        List<Permission> permissions = getGeneralCommandPermissions();
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        return permissions;
    }

    /**
     * Gets the permissions required for the general command.
     *
     * @return The permissions required for the general command.
     */
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>();
    }
}