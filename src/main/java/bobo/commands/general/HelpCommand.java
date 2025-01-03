package bobo.commands.general;

import bobo.Bobo;
import bobo.CommandManager;
import bobo.Config;
import bobo.Listener;
import bobo.commands.ADualCommand;
import bobo.commands.AMessageCommand;
import bobo.commands.ASlashCommand;
import bobo.commands.CommandResponse;
import bobo.commands.owner.AOwnerCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

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
        StringBuilder message = new StringBuilder();
        CommandManager manager = ((Listener) Bobo.getJDA().getRegisteredListeners().getFirst()).getManager();
        List<ADualCommand> dualCommands = manager.getDualCommands();
        List<ASlashCommand> slashCommands = manager.getSlashCommands();
        List<AMessageCommand> messageCommands = manager.getMessageCommands();

        String input;
        try {
            input = getOptionValue("command", 0);
        } catch (RuntimeException e) {
            input = null;
        }

        if (input == null) {
            message.append("## List of commands\n")
                    .append("To get info on a specific command: `/help <command name>`\n");
            for (ADualCommand command : dualCommands) {
                if (!(command instanceof HelpCommand)) {
                    message.append("`/")
                            .append(command.getName())
                            .append("`\n");
                }
            }

            for (ASlashCommand command : slashCommands) {
                message.append("`/")
                        .append(command.getName())
                        .append("`\n");
            }

            for (AMessageCommand command : messageCommands) {
                if (owner || (!(command instanceof AOwnerCommand))) {
                    message.append("`")
                            .append(PREFIX)
                            .append(command.getName())
                            .append("`\n");
                }
            }
        } else  {
            String search = input;
            ADualCommand dualCommand = dualCommands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            ASlashCommand slashCommand = slashCommands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            AMessageCommand messageCommand = messageCommands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            if (dualCommand == null && slashCommand == null && messageCommand == null) {
                message.append("Nothing found for ")
                        .append(search);
            } else {
                if (dualCommand != null) {
                    if (!(dualCommand instanceof HelpCommand)) {
                        message.append("## Command: ")
                                .append(dualCommand.getName())
                                .append("\n")
                                .append(dualCommand.getHelp());
                    }
                } else if (slashCommand != null) {
                    message.append("## Command: ")
                            .append(slashCommand.getName())
                            .append("\n")
                            .append(slashCommand.getHelp());
                } else {
                    if (owner || (!(messageCommand instanceof AOwnerCommand))) {
                        message.append("## Command: ")
                                .append(messageCommand.getName())
                                .append("\n")
                                .append(messageCommand.getHelp());
                    }
                }
            }
        }

        return new CommandResponse(message.toString());
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
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}