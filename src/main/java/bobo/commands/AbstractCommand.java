package bobo.commands;

import bobo.Bobo;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;

public abstract class AbstractCommand {
    protected SlashCommandInteractionEvent event;
    protected InteractionHook hook;

    /**
     * Creates a new command.
     *
     * @param commandData The command data.
     */
    public AbstractCommand(CommandData commandData) {
        Bobo.getJDA().upsertCommand(commandData).queue();
    }

    /**
     * Sets event and hook, then calls {@link #handleCommand()}.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        this.event = event;
        this.hook = event.getHook();

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
}
