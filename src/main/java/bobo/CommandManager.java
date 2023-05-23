package bobo;

import bobo.command.*;
import bobo.command.commands.*;
import bobo.command.commands.voice.*;
import bobo.command.commands.voice.music.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final List<CommandInterface> commands = new ArrayList<>();

    public CommandManager() {
        // Message commands
        commands.add(new HelpCommand(this));
        commands.add(new SearchCommand());
        commands.add(new ChatCommand());
        commands.add(new ChatResetCommand());
        commands.add(new AIImageCommand());
        commands.add(new SayCommand());
        commands.add(new GetQuoteCommand());
        commands.add(new Steelix());

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

    public List<CommandInterface> getCommands() {
        return commands;
    }

    @Nullable
    public CommandInterface getCommand(String search) {
        for (CommandInterface command : this.commands) {
            if (command.getName().equals(search)) {
                return command;
            }
        }
        return null;
    }

    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        CommandInterface command = getCommand(event.getName());
        if (command != null) {
            command.handle(event);
        } else {
            event.getHook().editOriginal("use a real command bozo. `/help` if you don't know them.").queue();
        }
    }

}
