package bobo.commands.admin;

import bobo.commands.AbstractCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractAdmin extends AbstractCommand {
    /**
     * Creates a new admin command.
     *
     * @param commandData The command data.
     */
    public AbstractAdmin(CommandData commandData) {
        super(commandData.setDefaultPermissions(DefaultMemberPermissions.DISABLED));
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
}
