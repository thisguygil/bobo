package bobo.commands.owner;

import bobo.Bobo;
import bobo.commands.CommandResponse;

public class RestartCommand extends AOwnerCommand {
    /**
     * Creates a new restart command.
     */
    public RestartCommand() {}

    @Override
    public String getName() {
        return "restart";
    }

    @Override
    protected CommandResponse handleOwnerCommand() {
        // Uses callback to ensure that the message is sent before the bot shuts down.
        return CommandResponse.builder()
                .setContent("Restarting...")
                .setPostExecutionAsMessage(success -> Bobo.restart())
                .setFailureHandler(failure -> Bobo.restart())
                .build();
    }

    @Override
    public String getHelp() {
        return """
                Restarts the bot.
                Usage: `""" + PREFIX + "restart`";
    }

    @Override
    public Boolean shouldNotShowTyping() {
        return false;
    }
}