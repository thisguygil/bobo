package bobo.commands;

import bobo.Config;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AMessageCommand implements ICommand {
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
     * Handles the command.
     */
    protected abstract CommandResponse handleCommand();

    /**
     * Gets whether the bot should not show typing before replying.
     * <br>
     * (Note: it is 'should <b><em>not</em></b> show typing' for consistency with {@link ASlashCommand#shouldBeEphemeral()}, as to be ephemeral and to <b><em>not</em></b> show typing are similar in that they are both invisible to other users.)
     *
     * @return Whether the bot should not show typing before replying, or null if it could be either.
     */
    @Nullable
    public abstract Boolean shouldNotShowTyping();

    /**
     * Gets aliases of the command, if any exist.
     *
     * @return The aliases of the command.
     */
    public List<String> getAliases() {
        return new ArrayList<>();
    }

    /**
     * Gets the permissions of the command.
     *
     * @return The permissions of the command.
     */
    public List<Permission> getPermissions() {
        List<Permission> permissions = new ArrayList<>(List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND));
        permissions.addAll(getCommandPermissions());
        return permissions;
    }

    /**
     * Gets the command permissions.
     *
     * @return The command permissions.
     */
    protected abstract List<Permission> getCommandPermissions();

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