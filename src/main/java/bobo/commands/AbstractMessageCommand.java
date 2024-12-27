package bobo.commands;

import bobo.Config;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMessageCommand {
    public static final String PREFIX = Config.get("PREFIX");
    protected MessageReceivedEvent event;
    protected String command;
    protected List<String> args;

    /**
     * Creates a new message command.
     */
    public AbstractMessageCommand() {}

    /**
     * Sets event and hook, then calls {@link #handleCommand()}.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull MessageReceivedEvent event, String command, List<String> args) {
        this.event = event;
        this.command = command;
        this.args = args;

        handleCommand();
    }

    /**
     * Handles the command.
     */
    protected abstract void handleCommand();

    /**
     * Gets the name of the command.
     *
     * @return The name of the command.
     */
    public abstract String getName();

    /**
     * Gets the help message of the command.
     *
     * @return The help message of the command.
     */
    public abstract String getHelp();

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
}
