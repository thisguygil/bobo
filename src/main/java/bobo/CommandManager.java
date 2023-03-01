package bobo;

import bobo.command.ICommand;
import bobo.command.commands.HelpCommand;
import bobo.command.commands.SayCommand;
import bobo.command.commands.Steelix;
import bobo.command.commands.music.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private final List<ICommand> commands = new ArrayList<>();

    public CommandManager() {
        commands.add(new HelpCommand(this));
        commands.add(new SayCommand());
        commands.add(new Steelix());

        // Music commands
        commands.add(new PlayCommand());
        commands.add(new NowPlayingCommand());
        commands.add(new QueueCommand());
        commands.add(new LoopCommand());
        commands.add(new SkipCommand());
        commands.add(new RemoveCommand());
        commands.add(new ClearCommand());
    }

    public List<ICommand> getCommands() {
        return commands;
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
        ICommand command = this.getCommand(event.getName());
        if (command != null) {
            command.handle(event);
        } else {
            event.reply("use a real command bozo. /help if you don't know them.").queue();
        }
    }

}
