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
        AbstractSlashCommand command = getSlashCommand(event.getName());
        if (command != null) {
            command.handle(event);
            logger.info("Slash command '{}' executed by '{}'.", event.getName(), event.getUser().getName());
        } else {
            event.reply("Error retrieving command.").queue();
            logger.error("Slash command '{}' not found.", event.getName());
        }
    }

    /**
     * Handles a message command.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        AbstractMessageCommand command = getMessageCommand(args[0].substring(1));
        if (command != null) {
            command.handle(event);
            logger.info("Message command '{}' executed by '{}'.", args[0], event.getAuthor().getName());
        } // Do nothing if command is not found, as it may be a regular message or a different bot's command
    }
}
