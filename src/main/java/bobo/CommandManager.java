package bobo;

import bobo.commands.*;
import bobo.commands.admin.*;
import bobo.commands.lastfm.*;
import bobo.commands.owner.*;
import bobo.commands.ai.*;
import bobo.commands.general.*;
import bobo.commands.voice.*;
import bobo.commands.voice.music.*;
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

    private final List<AbstractSlashCommand> slashCommands = new ArrayList<>();
    private final List<AbstractMessageCommand> messageCommands = new ArrayList<>();

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
        slashCommands.add(new HelpCommand());
        slashCommands.add(new GoogleCommand());
        slashCommands.add(new RandomCommand());
        slashCommands.add(new FortniteCommand());

        // Last.fm commands
        slashCommands.add(new FMLoginCommand());
        slashCommands.add(new FMLogoutCommand());
        slashCommands.add(new TrackCommand());
        slashCommands.add(new AlbumCommand());
        slashCommands.add(new ArtistCommand());

        // AI commands
        slashCommands.add(new TLDRCommand());
        slashCommands.add(new ChatCommand());
        slashCommands.add(new ImageCommand());
        slashCommands.add(new TTSCommand());

        // Voice commands
        slashCommands.add(new JoinCommand());
        slashCommands.add(new LeaveCommand());
        slashCommands.add(new ClipCommand());
        slashCommands.add(new DeafenCommand());
        slashCommands.add(new MuteCommand());
        // Music commands
        slashCommands.add(new PlayCommand());
        slashCommands.add(new SearchCommand());
        slashCommands.add(new LyricsCommand());
        slashCommands.add(new PauseCommand());
        slashCommands.add(new ResumeCommand());
        slashCommands.add(new NowPlayingCommand());
        slashCommands.add(new QueueCommand());
        slashCommands.add(new LoopCommand());
        slashCommands.add(new RepeatCommand());
        slashCommands.add(new SkipCommand());
        slashCommands.add(new SeekCommand());
    }

    /**
     * Gets a slash command by name.
     *
     * @param search The name of the slash command.
     * @return The command.
     */
    @Nullable
    public AbstractSlashCommand getSlashCommand(String search) {
        for (AbstractSlashCommand command : this.slashCommands) {
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
    public AbstractMessageCommand getMessageCommand(String search) {
        for (AbstractMessageCommand command : this.messageCommands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Gets the list of slash commands.
     * @return The list of slash commands.
     */
    public List<AbstractSlashCommand> getSlashCommands() {
        return this.slashCommands;
    }

    /**
     * Gets the list of message commands.
     * @return The list of message commands.
     */
    public List<AbstractMessageCommand> getMessageCommands() {
        return this.messageCommands;
    }

    /**
     * Handles a slash command.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        AbstractSlashCommand command = getSlashCommand(commandName);

        if (command != null) {
            logger.info("Slash command '{}' executed by '{}'.", commandName, event.getUser().getName());
            Boolean ephemeral = command.shouldBeEphemeral();
            if (ephemeral != null) {
                event.deferReply().setEphemeral(ephemeral).queue();
            } // else the command should reply (or defer) in its handle method and decide whether it should be ephemeral there

            command.handle(event);
        } else {
            logger.error("Slash command '{}' not found.", commandName);
            event.reply("Error retrieving command.").queue();
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
        AbstractMessageCommand command = getMessageCommand(baseCommand);

        if (command != null) {
            logger.info("Message command '{}' executed by '{}'.", baseCommand, event.getAuthor().getName());
            command.handle(event, baseCommand, args);
        } // Do nothing if command is not found, as it may be a regular message or a different bot's command
    }
}
