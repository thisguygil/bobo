package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class HelpCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        StringBuilder message = new StringBuilder();
        List<Command> commands = event.getJDA().retrieveCommands().complete();
        if (event.getOption("command") == null) {
            message.append("List of commands\n")
                    .append("To get info on a specific command: `/help <command name>`\n");
            for (Command command : commands) {
                if (!command.getName().equals("help")) {
                    message.append("`/")
                            .append(command.getName())
                            .append("`\n");
                }
            }
        } else  {
            String search = Objects.requireNonNull(event.getOption("command")).getAsString();
            Command command = commands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            if (command == null) {
                message.append("Nothing found for ")
                        .append(search);
            } else {
                message.append("`/")
                        .append(command.getName())
                        .append("`\n")
                        .append(command.getDescription());
            }
        }
        event.reply(message.toString()).queue();
    }

    @Override
    public String getName() {
        return "help";
    }

}
