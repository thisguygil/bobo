package bobo.commands.owner;

import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class RestartCommand extends AbstractOwner {
    /**
     * Creates a new restart command.
     */
    public RestartCommand() {
        super(Commands.slash("restart", "Restarts the bot"));
    }

    @Override
    public String getName() {
        return "restart";
    }

    @Override
    protected void handleOwnerCommand() {
        event.reply("Restarting...").queue(success -> System.exit(1), failure -> System.exit(1));
        // Start script should handle the actual restarting.
    }
}