package bobo.commands.admin;

import bobo.commands.AbstractCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAdmin extends AbstractCommand {
    /**
     * Creates a new admin command.
     *
     * @param commandData The command data.
     */
    public AbstractAdmin(CommandData commandData) {
        super(commandData);
    }

    @Override
    public void handleCommand() {
        handleAdminCommand();
    }

    /**
     * Handles the admin command.
     */
    protected abstract void handleAdminCommand();

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
