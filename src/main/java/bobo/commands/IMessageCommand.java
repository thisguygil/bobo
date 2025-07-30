package bobo.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public interface IMessageCommand extends ICommand {
    /**
     * Handles the command with the given event, name, and arguments.
     *
     * @param event The event that triggered this action.
     * @param command The name of the command that was triggered.
     * @param args The arguments of the command.
     * @return The command response.
     */
    CommandResponse handle(@Nonnull MessageReceivedEvent event, String command, List<String> args);

    /**
     * Gets aliases of the command if any exist.
     *
     * @return The aliases of the command.
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }
}