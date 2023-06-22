package bobo.commands.admin;

import bobo.commands.AbstractCommand;

public abstract class AbstractAdmin extends AbstractCommand {
    @Override
    public void handleCommand() {
        handleAdminCommand();
    }

    /**
     * Handles the admin command.
     */
    protected abstract void handleAdminCommand();
}
