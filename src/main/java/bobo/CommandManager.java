package bobo;

import bobo.commands.*;
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

    private final Map<String, ICommand> commands = new HashMap<>();

    /**
     * Adds all commands and their aliases to the command map.
     */
    public CommandManager() {
        Reflections reflections = new Reflections("bobo.commands");
        reflections.getSubTypesOf(ICommand.class).forEach(command -> {
            try {
                if (Modifier.isAbstract(command.getModifiers())) {
                    return;
                }

                ICommand iCommand = command.getDeclaredConstructor().newInstance();
                commands.put(iCommand.getName(), iCommand);
                if (iCommand instanceof ADualCommand dualCommand) {
                    for (String alias : dualCommand.getAliases()) {
                        commands.put(alias, dualCommand);
                    }
                } else if (iCommand instanceof AMessageCommand messageCommand) {
                    for (String alias : messageCommand.getAliases()) {
                        commands.put(alias, messageCommand);
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
     * Handles a slash command.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        ICommand command = commands.get(commandName);

        CommandResponse response;
        if (command instanceof ADualCommand dualCommand) {
            logger.info("Dual command '{}' executed as slash command by '{}'.", commandName, event.getUser().getName());
            Boolean shouldBeEphemeral = dualCommand.shouldBeInvisible();
            if (shouldBeEphemeral != null) {
                event.deferReply().setEphemeral(shouldBeEphemeral).queue();
            }
            response = dualCommand.handleSlashCommand(event);
        } else if (command instanceof ASlashCommand slashCommand) {
            logger.info("Slash command '{}' executed by '{}'.", commandName, event.getUser().getName());
            Boolean shouldBeEphemeral = slashCommand.shouldBeEphemeral();
            if (shouldBeEphemeral != null) {
                event.deferReply().setEphemeral(shouldBeEphemeral).queue();
            }
            response = slashCommand.handle(event);
        } else {
            logger.error("Slash command '{}' not found.", commandName);
            response = new CommandResponse("Error retrieving command.");
        }

        if (response.isInvisible() != null) { // This means the command did not reply yet, and should
            event.reply(response.asMessageCreateData()).setEphemeral(response.isInvisible()).queue(response.getPostExecutionAsHook(), response.getFailureHandler());
        } else {
            event.getHook().editOriginal(response.asMessageEditData()).queue(response.getPostExecutionAsMessage(), response.getFailureHandler());
        }
    }

    /**
     * Handles a message command.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull MessageReceivedEvent event) {
        String prefix = Config.get("PREFIX");

        String[] split = event.getMessage().getContentRaw().split("\\s+");
        List<String> args = new ArrayList<>(Arrays.asList(split).subList(1, split.length));

        String baseCommand = split[0].substring(prefix.length());
        ICommand command = commands.get(baseCommand);

        CommandResponse response;
        if (command instanceof ADualCommand dualCommand) {
            if (!event.getMember().getPermissions().containsAll(dualCommand.getPermissions())) {
                response = new CommandResponse("You do not have the required permissions to execute this command.");
            } else {
                logger.info("Dual command '{}' executed as message command by '{}'.", baseCommand, event.getAuthor().getName());
                if (Boolean.FALSE.equals(dualCommand.shouldBeInvisible())) {
                    event.getChannel().sendTyping().queue();
                }
                response = dualCommand.handleMessageCommand(event, baseCommand, args);
            }
        } else if (command instanceof AMessageCommand messageCommand) {
            if (!event.getMember().getPermissions().containsAll(messageCommand.getPermissions())) {
                response = new CommandResponse("You do not have the required permissions to execute this command.");
            } else {
                logger.info("Message command '{}' executed by '{}'.", baseCommand, event.getAuthor().getName());
                if (Boolean.FALSE.equals(messageCommand.shouldNotShowTyping())) {
                    event.getChannel().sendTyping().queue();
                }
                response = messageCommand.handle(event, baseCommand, args);
            }
        } else { // It's a regular message or unrelated bot command
            return;
        }

        MessageChannelUnion channel = event.getChannel();
        if (Boolean.FALSE.equals(response.isInvisible())) { // This means the command did not send typing yet, and should
            channel.sendTyping().queue();
        }

        channel.retrieveMessageById(event.getMessageId()).queue(
                success -> success.reply(response.asMessageCreateData()).queue(response.getPostExecutionAsMessage(), response.getFailureHandler()),
                failure -> channel.sendMessage(response.asMessageCreateData()).queue(response.getPostExecutionAsMessage(), response.getFailureHandler())
        );
    }
}