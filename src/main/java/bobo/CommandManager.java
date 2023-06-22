package bobo;

import bobo.command.ICommand;
import bobo.command.commands.admin.*;
import bobo.command.commands.ai.*;
import bobo.command.commands.general.*;
import bobo.command.commands.voice.*;
import bobo.command.commands.voice.music.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final List<ICommand> commands = new ArrayList<>();

    public CommandManager() {
        // Admin commands
        commands.add(new SetActivityCommand());
        commands.add(new SayCommand());

        // General commands
        commands.add(new HelpCommand());
        commands.add(new SearchCommand());
        commands.add(new GetQuoteCommand());
        commands.add(new SteelixCommand());

        // AI commands
        commands.add(new ChatCommand());
        commands.add(new ChatResetCommand());
        commands.add(new AIImageCommand());

        // Voice commands
        commands.add(new JoinCommand());
        commands.add(new LeaveCommand());
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
    public ICommand getCommand(String search) {
        for (ICommand command : this.commands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        ICommand command = getCommand(event.getName());
        if (command != null) {
            command.handle(event);
        } else {
            event.reply("Error retrieving command").queue();
            throw new RuntimeException();
        }
    }

}
