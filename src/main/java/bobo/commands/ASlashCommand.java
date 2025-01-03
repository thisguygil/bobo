package bobo.commands;

import bobo.Bobo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ASlashCommand implements ICommand {
    protected SlashCommandInteractionEvent event;
    protected InteractionHook hook;

    /**
     * Creates a new slash command.
     *
     * @param commandData The command data.
     */
    public ASlashCommand(@Nonnull CommandData commandData) {
        Bobo.getJDA()
                .upsertCommand(
                        commandData.setGuildOnly(true)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(getPermissions()))
                ).queue();
    }

    /**
     * Sets event and hook, then calls {@link #handleCommand()}.
     *
     * @param event The event that triggered this action.
     */
    public CommandResponse handle(@Nonnull SlashCommandInteractionEvent event) {
        this.event = event;
        this.hook = event.getHook();

        return handleCommand();
    }

    /**
     * Handles the command.
     */
    protected abstract CommandResponse handleCommand();

    /**
     * Gets whether the reply should be ephemeral, or null if it could be either.
     *
     * @return Whether the reply should be ephemeral, or null if it could be either.
     */
    @Nullable
    public abstract Boolean shouldBeEphemeral();

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