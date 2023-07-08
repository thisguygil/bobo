package bobo;

import bobo.commands.AbstractCommand;
import bobo.commands.admin.*;
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

    public CommandManager() {
        // Admin commands
        commands.add(new SetActivityCommand());
        commands.add(new SayCommand());

        // General commands
        commands.add(new HelpCommand());
        commands.add(new SearchCommand());
        commands.add(new GetQuoteCommand());

        // AI commands
        commands.add(new ChatCommand());
        commands.add(new AIImageCommand());

        // Voice commands
        commands.add(new JoinCommand());
        commands.add(new LeaveCommand());
        commands.add(new ClipCommand());
        commands.add(new DeafenCommand());
        // Music commands
        commands.add(new PlayCommand());
        commands.add(new PlayFileCommand());
        commands.add(new PauseCommand());
        commands.add(new ResumeCommand());
        commands.add(new NowPlayingCommand());
        commands.add(new QueueCommand());
        commands.add(new LoopCommand());
        commands.add(new ShuffleCommand());
        commands.add(new SkipCommand());
        commands.add(new RemoveCommand());
        commands.add(new ClearCommand());
    }

    @Nullable
    public AbstractCommand getCommand(String search) {
        for (AbstractCommand command : this.commands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        AbstractCommand command = getCommand(event.getName());
        if (command != null) {
            command.handle(event);
        } else {
            event.reply("Error retrieving command").queue();
            throw new RuntimeException();
        }
    }
}
