package bobo.command.commands;

import bobo.CommandManager;
import bobo.command.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class HelpCommand implements CommandInterface {
    private final CommandManager manager;

    public HelpCommand(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        StringBuilder message = new StringBuilder();
        if (event.getOption("command") == null) {
            message.append("List of commands\n");
            for (CommandInterface command : manager.getCommands()) {
                message.append("`/")
                        .append(command.getName())
                        .append("`\n");
                if (command.getName().equals("help")) {
                    message.append("To get info on a specific command: `/help <command name>`\n");
                }
            }
            event.reply(message.toString()).queue();
        } else  {
            String commandSearch = Objects.requireNonNull(event.getOption("command")).getAsString();
            CommandInterface command = manager.getCommand(commandSearch);
            if (command == null) {
                event.reply("Nothing found for " + commandSearch).queue();
            } else {
                event.reply(command.getHelp()).queue();
            }
        }
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp() {
        return "`/help`\n" +
                "Shows the list of commands";
    }
}
