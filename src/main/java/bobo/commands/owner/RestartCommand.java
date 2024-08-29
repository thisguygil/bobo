package bobo.commands.owner;

import bobo.Bobo;

public class RestartCommand extends AbstractOwner {
    /**
     * Creates a new restart command.
     */
    public RestartCommand() {}

    @Override
    public String getName() {
        return "restart";
    }

    @Override
    protected void handleOwnerCommand() {
        // Uses callback to ensure that the message is sent before the bot shuts down.
        event.getChannel().sendMessage("Restarting...").queue(success -> Bobo.restart(), failure -> Bobo.restart());
    }

    @Override
    public String getHelp() {
        return """
                Restarts the bot.
                Usage: `""" + PREFIX + "restart`";
    }
}