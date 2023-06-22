package bobo.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

public abstract class AbstractCommand {
    protected SlashCommandInteractionEvent event;
    protected InteractionHook hook;

    /**
     * Sets event and hook.
     *
     * @param event The event that triggered this action.
     */
    public void handle(SlashCommandInteractionEvent event) {
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
