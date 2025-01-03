package bobo.commands;

import net.dv8tion.jda.api.Permission;

import java.util.List;

public interface ICommand {
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
     * Gets the permissions of the command.
     *
     * @return The permissions of the command.
     */
    List<Permission> getPermissions();
}