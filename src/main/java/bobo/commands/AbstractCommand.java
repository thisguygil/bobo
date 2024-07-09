package bobo.commands;

import bobo.Bobo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommand {
    protected SlashCommandInteractionEvent event;
    protected InteractionHook hook;

    /**
     * Creates a new command.
     *
     * @param commandData The command data.
     */
    public AbstractCommand(@Nonnull CommandData commandData) {
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

    protected abstract List<Permission> getCommandPermissions();
}
