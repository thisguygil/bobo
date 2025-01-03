package bobo;

import bobo.commands.*;
import bobo.commands.admin.*;
import bobo.commands.lastfm.*;
import bobo.commands.owner.*;
import bobo.commands.ai.*;
import bobo.commands.general.*;
import bobo.commands.voice.*;
import bobo.commands.voice.music.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final List<ADualCommand> dualCommands = new ArrayList<>();
    private final List<ASlashCommand> slashCommands = new ArrayList<>();
    private final List<AMessageCommand> messageCommands = new ArrayList<>();

    /**
     * Adds all commands to the lists.
     */
    public CommandManager() {
        // Bot Owner commands
        messageCommands.add(new RestartCommand());
        messageCommands.add(new SetActivityCommand());
        messageCommands.add(new SQLCommand());
        messageCommands.add(new DMCommand());

        // Server Admin commands
        slashCommands.add(new ConfigCommand());
        slashCommands.add(new SayCommand());

        // General commands
        dualCommands.add(new HelpCommand());
        dualCommands.add(new GoogleCommand());
        dualCommands.add(new RandomCommand());
        dualCommands.add(new FortniteCommand());

        // Last.fm commands
        dualCommands.add(new FMLoginCommand());
        dualCommands.add(new FMLogoutCommand());
        dualCommands.add(new TrackCommand());
        dualCommands.add(new AlbumCommand());
        dualCommands.add(new ArtistCommand());

        // AI commands
        dualCommands.add(new TLDRCommand());
        dualCommands.add(new ChatCommand());
        dualCommands.add(new ImageCommand());

        // Voice commands
        dualCommands.add(new JoinCommand());
        dualCommands.add(new LeaveCommand());
        dualCommands.add(new ClipCommand());
        dualCommands.add(new DeafenCommand());
        dualCommands.add(new MuteCommand());
        // Music commands
        dualCommands.add(new PlayCommand());
        dualCommands.add(new TTSCommand());
        dualCommands.add(new SearchCommand());
        dualCommands.add(new LyricsCommand());
        dualCommands.add(new PauseCommand());
        dualCommands.add(new ResumeCommand());
        dualCommands.add(new NowPlayingCommand());
        dualCommands.add(new QueueCommand());
        dualCommands.add(new LoopCommand());
        dualCommands.add(new RepeatCommand());
        dualCommands.add(new SkipCommand());
        dualCommands.add(new SeekCommand());
    }

    /**
     * Gets a dual command by name.
     *
     * @param search The name of the dual command.
     * @return The command.
     */
    @Nullable
    public ADualCommand getDualCommand(String search) {
        for (ADualCommand command : this.dualCommands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Gets a slash command by name.
     *
     * @param search The name of the slash command.
     * @return The command.
     */
    @Nullable
    public ASlashCommand getSlashCommand(String search) {
        for (ASlashCommand command : this.slashCommands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Gets a message command by name.
     *
     * @param search The name of the message command.
     * @return The command.
     */
    @Nullable
    public AMessageCommand getMessageCommand(String search) {
        for (AMessageCommand command : this.messageCommands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Gets the list of dual commands.
     * @return The list of dual commands.
     */
    public List<ADualCommand> getDualCommands() {
        return this.dualCommands;
    }

    /**
     * Gets the list of slash commands.
     * @return The list of slash commands.
     */
    public List<ASlashCommand> getSlashCommands() {
        return this.slashCommands;
    }

    /**
     * Gets the list of message commands.
     * @return The list of message commands.
     */
    public List<AMessageCommand> getMessageCommands() {
        return this.messageCommands;
    }

    /**
     * Handles a slash command.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        ADualCommand dualCommand = getDualCommand(commandName);
        ASlashCommand slashCommand = getSlashCommand(commandName);

        CommandResponse response;
        if (dualCommand != null) {
            logger.info("Dual command '{}' executed as slash command by '{}'.", commandName, event.getUser().getName());
            Boolean shouldBeEphemeral = dualCommand.shouldBeInvisible();
            if (shouldBeEphemeral != null) {
                event.deferReply().setEphemeral(shouldBeEphemeral).queue();
            }
            response = dualCommand.handleSlashCommand(event);
        } else if (slashCommand != null) {
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
        ADualCommand dualCommand = getDualCommand(baseCommand);
        AMessageCommand messageCommand = getMessageCommand(baseCommand);

        CommandResponse response;
        if (dualCommand != null) {
            if (!event.getMember().getPermissions().containsAll(dualCommand.getPermissions())) {
                response = new CommandResponse("You do not have the required permissions to execute this command.");
            } else {
                logger.info("Dual command '{}' executed as message command by '{}'.", baseCommand, event.getAuthor().getName());
                if (Boolean.FALSE.equals(dualCommand.shouldBeInvisible())) {
                    event.getChannel().sendTyping().queue();
                }
                response = dualCommand.handleMessageCommand(event, baseCommand, args);
            }
        } else if (messageCommand != null) {
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
