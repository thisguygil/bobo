package bobo.commands.owner;

import bobo.Config;
import bobo.commands.AMessageCommand;
import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;

import java.util.ArrayList;
import java.util.List;

public abstract class AOwnerCommand extends AMessageCommand {
    /**
     * Creates a new owner command.
     */
    public AOwnerCommand() {}

    @Override
    public CommandResponse handleCommand() {
        if (!event.getAuthor().getId().equals(Config.get("OWNER_ID"))) {
            return CommandResponse.EMPTY;
        }

        return handleOwnerCommand();
    }

    /**
     * Handles the owner command.
     */
    protected abstract CommandResponse handleOwnerCommand();

    @Override
    public List<Permission> getCommandPermissions() {
        return new ArrayList<>(); // Empty because the owner should be able to use these commands anywhere
    }
}