package bobo.commands.owner;

import bobo.Bobo;
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
        // Uses callback to ensure that the message is sent before the bot shuts down.
        event.reply("Restarting...").queue(success -> Bobo.restart(), failure -> Bobo.restart());
    }
}