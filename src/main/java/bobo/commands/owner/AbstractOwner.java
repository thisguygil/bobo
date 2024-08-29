package bobo.commands.owner;

import bobo.Config;
import bobo.commands.AbstractMessageCommand;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOwner extends AbstractMessageCommand {
    /**
     * Creates a new owner command.
     */
    public AbstractOwner() {}

    @Override
    protected void handleCommand() {
        if (event.getAuthor().getId().equals(Config.get("OWNER_ID"))) {
            handleOwnerCommand();
        } // Else do nothing
    }

    /**
     * Handles the owner command.
     */
    protected abstract void handleOwnerCommand();

    @Override
    protected List<Permission> getCommandPermissions() {
        return new ArrayList<>(); // Empty because the owner should be able to use these commands anywhere
    }
}