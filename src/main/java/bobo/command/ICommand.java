package bobo.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface ICommand {
    /**
     * Handles the slash command specified by the event
     *
     * @param event the slash command to be handled
     */
    void handle(SlashCommandInteractionEvent event);

    /**
     * @return the name of the command
     */
    String getName();

    /**
     * @return the help message for the command
     */
    String getHelp();
}