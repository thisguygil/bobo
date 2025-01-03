package bobo.commands.admin;

import bobo.commands.ASlashCommand;
import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.List;

public abstract class AAdminCommand extends ASlashCommand {
    /**
     * Creates a new admin command.
     *
     * @param commandData The command data.
     */
    public AAdminCommand(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected CommandResponse handleCommand() {
        return handleAdminCommand();
    }

    /**
     * Handles the admin command.
     */
    protected abstract CommandResponse handleAdminCommand();

    @Override
    public String getHelp() {
        return "Server admin command.";
    }

    @Override
    protected List<Permission> getCommandPermissions() {
        List<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.ADMINISTRATOR);
        return permissions;
    }
}
