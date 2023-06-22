package bobo.command.commands.general;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;
import java.util.List;

public class HelpCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();
        StringBuilder message = new StringBuilder();
        List<Command> commands = event.getJDA().retrieveCommands().complete();
        OptionMapping input = event.getOption("command");

        if (input == null) {
            message.append("**List of commands**\n")
                    .append("To get info on a specific command: `/help <command name>`\n");
            for (Command command : commands) {
                if (!command.getName().equals("help")) {
                    message.append("`/")
                            .append(command.getName())
                            .append("`\n");
                }
            }
        } else  {
            String search = input.getAsString();
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
        hook.editOriginal(message.toString()).queue();
    }

    @Override
    public String getName() {
        return "help";
    }
}
