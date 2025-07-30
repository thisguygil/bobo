package bobo;

import bobo.commands.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.Modifier;
import java.util.*;

public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    private static final String PREFIX = Config.get("PREFIX");
    private final Map<String, ICommand> commands = new HashMap<>();

    /**
     * Adds all commands and their aliases to the command map.
     */
    public CommandManager() {
        Reflections reflections = new Reflections("bobo.commands");
        reflections.getSubTypesOf(ICommand.class).forEach(command -> {
            try {
                if (Modifier.isAbstract(command.getModifiers())) return;

                ICommand iCommand = command.getDeclaredConstructor().newInstance();
                commands.put(iCommand.getName(), iCommand);

                if (iCommand instanceof IMessageCommand messageCommand) {
                    for (String alias : messageCommand.getAliases()) {
                        commands.put(alias, iCommand);
                    }
                }
            } catch (Exception e) {
                logger.error("Error loading command '{}'.", command.getName());
            }
        });
    }

    /**
     * Gets the command map.
     *
     * @return The command map.
     */
    public Map<String, ICommand> getCommands() {
        return commands;
    }

    /**
     * Handles a {@link SlashCommandInteractionEvent} for {@link ASlashCommand} and {@link ADualCommand} commands.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        ICommand command = commands.get(commandName);

        CommandResponse response;
        if (command instanceof ISlashCommand slashCommand) {
            logger.info("Slash command '{}' executed by '{}'.", commandName, event.getUser().getName());
            Boolean isHidden = command.isHidden();
            if (isHidden != null) event.deferReply().setEphemeral(isHidden).queue();
            response = slashCommand.handle(event);
        } else { // Should never happen, but just in case.
            logger.error("Slash command '{}' not found.", commandName);
            response = CommandResponse.invisible("Error retrieving command.");
        }

        if (response == null || response == CommandResponse.EMPTY) return;

        Boolean hidden = response.hidden();
        if (hidden != null) {
            response.applyToHook(event.reply(response.asMessageCreateData()).setEphemeral(hidden));
        } else {
            response.applyToMessage(event.getHook().editOriginal(response.asMessageEditData()));
        }
    }

    /**
     * Handles a {@link MessageReceivedEvent} for {@link AMessageCommand} and {@link ADualCommand} commands.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull MessageReceivedEvent event) {
        String[] split = event.getMessage().getContentRaw().split("\\s+");
        List<String> args = Arrays.asList(split).subList(1, split.length);
        String baseCommand = split[0].substring(PREFIX.length());
        ICommand command = commands.get(baseCommand);
        MessageChannelUnion channel = event.getChannel();

        CommandResponse response;
        if (hasPermission(event, command)) { // We must handle permissions for message commands manually, as they are not handled by the JDA.
            if (command instanceof IMessageCommand messageCommand) {
                logger.info("Message command '{}' executed by '{}'.", baseCommand, event.getAuthor().getName());
                checkSendTyping(command.isHidden(), channel);
                response = messageCommand.handle(event, baseCommand, args);
            } else return; // No way to distinguish between a mistakenly invalid usage and an unrelated message, so we do nothing.
        } else response = CommandResponse.text("You do not have the required permissions to execute this command.");

        if (response == null || response == CommandResponse.EMPTY) return;

        checkSendTyping(response.hidden(), channel);
        channel.retrieveMessageById(event.getMessageId()).queue(
                success -> response.applyToMessage(success.reply(response.asMessageCreateData())),
                failure -> response.applyToMessage(channel.sendMessage(response.asMessageCreateData()))
        );
    }

    /**
     * Checks if the message command should send a typing indicator.
     *
     * @param isHidden If false, the command will send a typing indicator.
     * @param channel  The channel to send the typing indicator in.
     */
    private static void checkSendTyping(Boolean isHidden, MessageChannelUnion channel) {
        if (Boolean.FALSE.equals(isHidden)) channel.sendTyping().queue();
    }

    /**
     * Checks if the user has the required permissions to execute the command.
     *
     * @param event   The event that triggered this action.
     * @param command The command to check permissions for.
     * @return True if the user has the required permissions, false otherwise.
     */
    private static boolean hasPermission(MessageReceivedEvent event, ICommand command) {
        Member member = event.getMember();
        if (member == null) {
            logger.warn("Member is null for event: {}", event.getMessageId());
            return false; // No member, no permissions.
        }
        EnumSet<Permission> permissions = member.getPermissions();
        return permissions.contains(Permission.ADMINISTRATOR) || permissions.containsAll(command.getPermissions());
    }
}