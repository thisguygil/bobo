package bobo.commands;

import bobo.Bobo;
import bobo.Config;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ADualCommand implements ICommand {
    // Slash command fields
    protected SlashCommandInteractionEvent slashEvent;

    // Message command fields
    protected static final String PREFIX = Config.get("PREFIX");
    protected MessageReceivedEvent messageEvent;
    protected String command;
    protected List<String> args;

    public enum CommandSource {
        SLASH_COMMAND,
        MESSAGE_COMMAND
    }

    protected CommandSource source;

    /**
     * Creates a new dual command.
     *
     * @param commandData The command data.
     */
    public ADualCommand(@Nonnull CommandData commandData) {
        Bobo.getJDA()
                .upsertCommand(
                        commandData.setContexts(InteractionContextType.GUILD)
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(getPermissions()))
                ).queue();
    }

    /**
     * Sets event and command source, then returns the command response from {@link #handleCommand}.
     *
     * @param event The event that triggered this action.
     * @return The command response.
     */
    public CommandResponse handleSlashCommand(@Nonnull SlashCommandInteractionEvent event) {
        this.slashEvent = event;
        this.source = CommandSource.SLASH_COMMAND;

        return handleCommand();
    }

    /**
     * Sets event, command, args, and command source, then returns the command response from {@link #handleCommand}.
     *
     * @param event The event that triggered this action.
     * @param command The command that was triggered.
     * @param args The arguments of the command.
     * @return The command response.
     */
    public CommandResponse handleMessageCommand(@Nonnull MessageReceivedEvent event, String command, List<String> args) {
        this.messageEvent = event;
        this.command = command;
        this.args = args;
        this.source = CommandSource.MESSAGE_COMMAND;

        return handleCommand();
    }

    /**
     * Handles the command as the specified command type.
     *
     * @return The command response.
     */
    protected abstract CommandResponse handleCommand();


    /**
     * For a slash command, gets whether the reply should be ephemeral, or null if it could be either.
     * For a message command, gets whether the bot should show typing before replying.
     *
     * @return Whether the reply should be ephemeral or whether the bot should show typing before replying, or null if it could be either.
     */
    @Nullable
    public abstract Boolean shouldBeInvisible();

    /**
     * Gets aliases of the command, if any exist (for message commands).
     *
     * @return The aliases of the command.
     */
    public List<String> getAliases() {
        return new ArrayList<>();
    }

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
     * Helper method to get the guild of the current command.
     *
     * @return The guild of the command source.
     */
    protected Guild getGuild() {
        return switch (source) {
            case SLASH_COMMAND -> slashEvent.getGuild();
            case MESSAGE_COMMAND -> messageEvent.getGuild();
        };
    }

    /**
     * Helper method to get the channel of the current command.
     *
     * @return The channel of the command source.
     */
    protected Channel getChannel() {
        return switch (source) {
            case SLASH_COMMAND -> slashEvent.getChannel();
            case MESSAGE_COMMAND -> messageEvent.getChannel();
        };
    }

    /**
     * Helper method to get the user that executed the current command.
     *
     * @return The user of the command source.
     */
    protected User getUser() {
        return switch (source) {
            case SLASH_COMMAND -> slashEvent.getUser();
            case MESSAGE_COMMAND -> messageEvent.getAuthor();
        };
    }

    /**
     * Helper method to get the member that executed the current command.
     *
     * @return The member of the command source.
     */
    protected Member getMember() {
        return switch (source) {
            case SLASH_COMMAND -> slashEvent.getMember();
            case MESSAGE_COMMAND -> messageEvent.getMember();
        };
    }

    /**
     * Helper method to get the value of an option.
     *
     * @param optionName The name of the option.
     * @param argPosition The position of the option in the arguments list (for message commands).
     * @param defaultValue The default value of the option.
     * @return The value of the option.
     * @throws RuntimeException If the option is not found and the default value is null.
     */
    protected String getOptionValue(String optionName, Integer argPosition, String defaultValue) throws RuntimeException {
        return switch (source) {
            case SLASH_COMMAND -> {
                OptionMapping option = slashEvent.getOption(optionName);
                yield option != null ? option.getAsString() : defaultValue;
            }
            case MESSAGE_COMMAND -> {
                if (args == null || argPosition >= args.size() || argPosition < 0) {
                    if (defaultValue == null) {
                        throw new RuntimeException("Not enough arguments.");
                    }
                    yield defaultValue;
                }
                yield args.get(argPosition);
            }
        };
    }

    /**
     * Helper method to get the value of an option.
     *
     * @param optionName The name of the option.
     * @param argPosition The position of the option in the arguments list (for message commands).
     * @return The value of the option.
     */
    protected String getOptionValue(String optionName, Integer argPosition) {
        return getOptionValue(optionName, argPosition, null);
    }

    /**
     * Helper method to get the value of a multiword option.
     *
     * @param optionName The name of the option.
     * @param argStartPosition The position of the option in the arguments list (for message commands).
     * @param defaultValue The default value of the option.
     * @return The value of the option.
     * @throws RuntimeException If the option is not found and the default value is null.
     */
    protected String getMultiwordOptionValue(String optionName, Integer argStartPosition, String defaultValue) throws RuntimeException {
        return switch (source) {
            case SLASH_COMMAND -> getOptionValue(optionName, null, defaultValue);
            case MESSAGE_COMMAND -> {
                if (args == null || argStartPosition >= args.size() || argStartPosition < 0) {
                    if (defaultValue == null) {
                        throw new RuntimeException("Not enough arguments.");
                    }
                    yield defaultValue;
                }
                yield String.join(" ", args.subList(argStartPosition, args.size()));
            }
        };
    }

    /**
     * Helper method to get the multiword option value.
     *
     * @param optionName The name of the option.
     * @param argStartPosition The position of the option in the arguments list (for message commands).
     * @return The value of the option.
     */
    protected String getMultiwordOptionValue(String optionName, Integer argStartPosition) {
        return getMultiwordOptionValue(optionName, argStartPosition, null);
    }

    /**
     * Helper method to get the attachment.
     *
     * @param optionName The name of the option.
     * @return The attachment.
     */
    protected Message.Attachment getAttachment(String optionName) {
        return switch (source) {
            case SLASH_COMMAND -> slashEvent.getOption(optionName).getAsAttachment();
            case MESSAGE_COMMAND -> messageEvent.getMessage().getAttachments().getFirst();
        };
    }

    /**
     * Helper method to get the subcommand name.
     *
     * @param argPosition The position of the subcommand in the arguments list (for message commands).
     * @return The subcommand name.
     */
    protected String getSubcommandName(Integer argPosition) {
        return switch (source) {
            case SLASH_COMMAND -> slashEvent.getSubcommandName();
            case MESSAGE_COMMAND -> getOptionValue(null, argPosition, null);
        };
    }
}