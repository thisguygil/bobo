package bobo.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public interface ISlashCommand extends ICommand {
    /**
     * Handles the command with the given event.
     *
     * @param event The event that triggered this action.
     * @return The command response.
     */
    CommandResponse handle(@Nonnull SlashCommandInteractionEvent event);
}