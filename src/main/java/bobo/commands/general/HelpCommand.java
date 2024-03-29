package bobo.commands.general;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.List;

public class HelpCommand extends AbstractGeneral {
    /**
     * Creates a new help command.
     */
    public HelpCommand() {
        super(Commands.slash("help", "Shows the list of commands or gets info on a specific command.")
                .addOption(OptionType.STRING, "command", "Command to explain", false));
    }
    @Override
    protected void handleGeneralCommand() {
        event.deferReply().queue();
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
