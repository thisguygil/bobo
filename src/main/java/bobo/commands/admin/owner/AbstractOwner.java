package bobo.commands.admin.owner;

import bobo.Config;
import bobo.commands.admin.AbstractAdmin;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractOwner extends AbstractAdmin {
    /**
     * Creates a new owner command.
     *
     * @param commandData The command data.
     */
    public AbstractOwner(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected void handleAdminCommand() {
        if (event.getUser().getId().equals(Config.get("OWNER_ID"))) {
            handleOwnerCommand();
        } else {
            event.reply("You must be the owner of the bot to use this command.").setEphemeral(true).queue();
        }
    }

    /**
     * Handles the owner command.
     */
    protected abstract void handleOwnerCommand();
}