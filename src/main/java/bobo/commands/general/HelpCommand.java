package bobo.commands.general;

import bobo.Bobo;
import bobo.Config;
import bobo.Listener;
import bobo.commands.*;
import bobo.commands.owner.AOwnerCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Map;

public class HelpCommand extends AGeneralCommand {
    private static final String PREFIX = Config.get("PREFIX");

    /**
     * Creates a new help command.
     */
    public HelpCommand() {
        super(Commands.slash("help", "Shows the list of commands or gets info on a specific command.")
                .addOption(OptionType.STRING, "command", "Command to explain", false));
    }

    @Override
    protected CommandResponse handleGeneralCommand() {
        boolean owner = getUser().getId().equals(Config.get("OWNER_ID"));
        Listener listener = (Listener) Bobo.getJDA().getRegisteredListeners().getFirst();
        Map<String, ICommand> commands = listener.getManager().getCommands();

        String input;
        try {
            input = getOptionValue("command", 0);
        } catch (RuntimeException ignored) {
            input = null;
        }

        StringBuilder message = new StringBuilder();
        if (input == null) {
            message.append("## List of commands\n")
                    .append("To get info on a specific command: `/help <command name>`\n");

            commands.values().stream()
                    .distinct()
                    .filter(command -> (owner || (!(command instanceof AOwnerCommand))) && !(command instanceof HelpCommand))
                    .forEach(command -> {
                        if (command instanceof AMessageCommand) {
                            message.append(String.format("`%s%s`\n", PREFIX, command.getName()));
                        } else {
                            message.append(String.format("`/%s`\n", command.getName()));
                        }
                    });
        } else {
            ICommand command = commands.get(input);
            if (command == null) {
                message.append(String.format("Command `%s` not found", input));
            } else if ((command instanceof AOwnerCommand && !owner) || !getMember().getPermissions().containsAll(command.getPermissions())) {
                message.append("You do not have permission to view details for this command");
            } else {
                message.append(String.format("## %s\n%s", command.getName(), command.getHelp()));
            }
        }

        return CommandResponse.text(message.toString());
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp() {
        return "You just think you're so clever, don't you?";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}