package bobo.commands.general;

import bobo.Bobo;
import bobo.Config;
import bobo.Listener;
import bobo.commands.AbstractCommand;
import bobo.commands.owner.AbstractOwner;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
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

        boolean owner = event.getUser().getId().equals(Config.get("OWNER_ID"));
        StringBuilder message = new StringBuilder();
        List<AbstractCommand> commands = ((Listener) Bobo.getJDA().getRegisteredListeners().get(0)).getManager().getCommands();
        assert commands != null;
        OptionMapping input = event.getOption("command");

        if (input == null) {
            message.append("## List of commands\n")
                    .append("To get info on a specific command: `/help <command name>`\n");
            for (AbstractCommand command : commands) {
                if (!(command instanceof HelpCommand) && (!(command instanceof AbstractOwner) || owner)) {
                    message.append("`/")
                            .append(command.getName())
                            .append("`\n");
                }
            }
        } else  {
            String search = input.getAsString();
            AbstractCommand command = commands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            if (command == null) {
                message.append("Nothing found for ")
                        .append(search);
            } else {
                if (!(command instanceof HelpCommand) && (!(command instanceof AbstractOwner) || owner)) {
                    message.append("## Command: ")
                            .append(command.getName())
                            .append("\n");
                }
                message.append(command.getHelp());
            }
        }
        hook.editOriginal(message.toString()).queue();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp() {
        return "You just think you're so smart, don't you?";
    }

    @Override
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>();
    }
}
