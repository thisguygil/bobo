package bobo.commands;

import bobo.Config;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AMessageCommand implements IMessageCommand {
    protected static final String PREFIX = Config.get("PREFIX");
    protected MessageReceivedEvent event;
    protected String command;
    protected List<String> args;

    /**
     * Creates a new message command.
     */
    public AMessageCommand() {}

    /**
     * Sets event, command, and args, then returns the command response from {@link #handleCommand}.
     *
     * @param event The event that triggered this action.
     * @param command The command that was triggered.
     * @param args The arguments of the command.
     * @return The command response.
     */
    public CommandResponse handle(@Nonnull MessageReceivedEvent event, String command, List<String> args) {
        this.event = event;
        this.command = command;
        this.args = args;

        return handleCommand();
    }

    /**
     * Helper method to get the value of an option.
     *
     * @param argPosition The position of the option in the arguments list.
     * @param defaultValue The default value of the option.
     * @return The value of the option.
     * @throws RuntimeException If the option is not found and the default value is null.
     */
    protected String getOptionValue(Integer argPosition, String defaultValue) throws RuntimeException {
        if (args == null || argPosition >= args.size() || argPosition < 0) {
            if (defaultValue == null) {
                throw new RuntimeException("Not enough arguments.");
            }
            return defaultValue;
        }
        return args.get(argPosition);
    }

    /**
     * Helper method to get the value of an option.
     *
     * @param argPosition The position of the option in the arguments list.
     * @return The value of the option.
     * @throws RuntimeException If the option is not found and the default value is null.
     */
    protected String getOptionValue(Integer argPosition) throws RuntimeException {
        return getOptionValue(argPosition, null);
    }

    /**
     * Helper method to get the value of a multiword option.
     *
     * @param argStartPosition The position of the option in the arguments list.
     * @param defaultValue The default value of the option.
     * @return The value of the option.
     * @throws RuntimeException If the option is not found and the default value is null.
     */
    protected String getMultiwordOptionValue(Integer argStartPosition, String defaultValue) throws RuntimeException {
        if (args == null || argStartPosition >= args.size() || argStartPosition < 0) {
            if (defaultValue == null) {
                throw new RuntimeException("Not enough arguments.");
            }
            return defaultValue;
        }
        return String.join(" ", args.subList(argStartPosition, args.size()));
    }

    /**
     * Helper method to get the value of a multiword option.
     *
     * @param argStartPosition The position of the option in the arguments list.
     * @return The value of the option.
     * @throws RuntimeException If the option is not found and the default value is null.
     */
    protected String getMultiwordOptionValue(Integer argStartPosition) throws RuntimeException {
        return getMultiwordOptionValue(argStartPosition, null);
    }
}