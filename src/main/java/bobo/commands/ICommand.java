package bobo.commands;

import net.dv8tion.jda.api.Permission;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface ICommand {
    /**
     * Handles the command and returns a response.
     *
     * @return The command response.
     */
    CommandResponse handleCommand();

    /**
     * Gets the name of the command.
     *
     * @return The name of the command.
     */
    String getName();

    /**
     * Gets the help message of the command.
     *
     * @return The help message of the command.
     */
    String getHelp();

    /**
     * Gets whether the command should be hidden from others.
     * For slash commands, this means the command will be ephemeral.
     * For message commands, this means the bot will not show typing before replying.
     *
     * @return Whether the command should be hidden, or null if it could be either.
     */
    @Nullable
    Boolean isHidden();

    /**
     * Gets the full list of permissions required for the command.
     *
     * @return The permissions.
     */
    default List<Permission> getPermissions() {
        List<Permission> permissions = new ArrayList<>(List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND));
        permissions.addAll(getCommandPermissions());
        return permissions;
    }

    /**
     * Gets the command-specific permissions.
     *
     * @return The permissions.
     */
    List<Permission> getCommandPermissions();
}