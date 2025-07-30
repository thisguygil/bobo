package bobo.commands;

import bobo.Bobo;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;

public abstract class ASlashCommand implements ISlashCommand {
    protected SlashCommandInteractionEvent event;

    /**
     * Creates a new slash command.
     *
     * @param commandData The command data.
     */
    public ASlashCommand(@Nonnull CommandData commandData) {
        Bobo.getJDA()
                .upsertCommand(
                        commandData.setContexts(InteractionContextType.GUILD)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(getPermissions()))
                ).queue();
    }

    /**
     * Sets event, then returns the command response from {@link #handleCommand}.
     *
     * @param event The event that triggered this action.
     * @return The command response.
     */
    public CommandResponse handle(@Nonnull SlashCommandInteractionEvent event) {
        this.event = event;

        return handleCommand();
    }

    /**
     * Helper method to get the value of an option.
     *
     * @param optionName The name of the option.
     * @param defaultValue The default value of the option.
     * @return The value of the option.
     */
    protected String getOptionValue(String optionName, String defaultValue) {
        OptionMapping option = event.getOption(optionName);
        return option != null ? option.getAsString() : defaultValue;
    }

    /**
     * Helper method to get the value of an option.
     *
     * @param optionName The name of the option.
     * @return The value of the option.
     */
    protected String getOptionValue(String optionName) {
        return getOptionValue(optionName, null);
    }
}