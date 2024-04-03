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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final List<AbstractCommand> commands = new ArrayList<>();

    /**
     * Adds all commands to the list.
     */
    public CommandManager() {
        // Bot Owner commands
        commands.add(new RestartCommand());
        commands.add(new SetActivityCommand());

        // Server Admin commands
        commands.add(new ConfigCommand());
        commands.add(new SayCommand());

        // General commands
        commands.add(new HelpCommand());
        commands.add(new GoogleCommand());
        commands.add(new GetQuoteCommand());
        commands.add(new FortniteCommand());

        // Last.fm commands
        commands.add(new FMLoginCommand());
        commands.add(new FMLogoutCommand());
        commands.add(new TrackCommand());
        commands.add(new AlbumCommand());
        commands.add(new ArtistCommand());

        // AI commands
        commands.add(new ChatCommand());
        commands.add(new ImageCommand());
        commands.add(new TTSCommand());

        // Voice commands
        commands.add(new JoinCommand());
        commands.add(new LeaveCommand());
        commands.add(new ClipCommand());
        commands.add(new DeafenCommand());
        commands.add(new MuteCommand());
        // Music commands
        commands.add(new PlayCommand());
        commands.add(new SearchCommand());
        commands.add(new PauseCommand());
        commands.add(new ResumeCommand());
        commands.add(new NowPlayingCommand());
        commands.add(new QueueCommand());
        commands.add(new LoopCommand());
        commands.add(new SkipCommand());
        commands.add(new SeekCommand());
    }

    /**
     * Gets a command by name.
     *
     * @param search The name of the command.
     * @return The command.
     */
    @Nullable
    public AbstractCommand getCommand(String search) {
        for (AbstractCommand command : this.commands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Gets the list of commands.
     * @return The list of commands.
     */
    public List<AbstractCommand> getCommands() {
        return this.commands;
    }

    /**
     * Handles a slash command.
     *
     * @param event The event that triggered this action.
     */
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        AbstractCommand command = getCommand(event.getName());
        if (command != null) {
            command.handle(event);
        } else {
            event.reply("Error retrieving command.").queue();
        }
    }
}
