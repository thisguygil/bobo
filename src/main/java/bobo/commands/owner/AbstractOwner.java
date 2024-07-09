package bobo.commands.owner;

import bobo.Config;
import bobo.commands.AbstractCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOwner extends AbstractCommand {
    /**
     * Creates a new owner command.
     *
     * @param commandData The command data.
     */
    public AbstractOwner(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected void handleCommand() {
        if (event.getUser().getId().equals(Config.get("OWNER_ID"))) {
            handleOwnerCommand();
        } else {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
        }
    }

    /**
     * Handles the owner command.
     */
    protected abstract void handleOwnerCommand();

    @Override
    protected List<Permission> getCommandPermissions() {
        return new ArrayList<>();
    }
}