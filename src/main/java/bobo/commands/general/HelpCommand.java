package bobo.commands.general;

import bobo.Bobo;
import bobo.Config;
import bobo.Listener;
import bobo.commands.AbstractMessageCommand;
import bobo.commands.AbstractSlashCommand;
import bobo.commands.owner.AbstractOwner;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

import static bobo.commands.AbstractMessageCommand.PREFIX;

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
        boolean owner = event.getUser().getId().equals(Config.get("OWNER_ID"));
        StringBuilder message = new StringBuilder();
        List<AbstractSlashCommand> slashCommands = ((Listener) Bobo.getJDA().getRegisteredListeners().get(0)).getManager().getSlashCommands();
        List<AbstractMessageCommand> messageCommands = ((Listener) Bobo.getJDA().getRegisteredListeners().get(0)).getManager().getMessageCommands();
        assert slashCommands != null;
        OptionMapping input = event.getOption("command");

        if (input == null) {
            message.append("## List of commands\n")
                    .append("To get info on a specific command: `/help <command name>`\n");
            for (AbstractSlashCommand command : slashCommands) {
                if (!(command instanceof HelpCommand)) {
                    message.append("`/")
                            .append(command.getName())
                            .append("`\n");
                }
            }

            for (AbstractMessageCommand command : messageCommands) {
                if (owner || (!(command instanceof AbstractOwner))) {
                    message.append("`")
                            .append(PREFIX)
                            .append(command.getName())
                            .append("`\n");
                }
            }
        } else  {
            String search = input.getAsString();
            AbstractSlashCommand slashCommand = slashCommands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            AbstractMessageCommand messageCommand = messageCommands.stream()
                    .filter(c -> c.getName().equals(search))
                    .findFirst()
                    .orElse(null);
            if (slashCommand == null && messageCommand == null) {
                message.append("Nothing found for ")
                        .append(search);
            } else {
                if (slashCommand != null) {
                    if (!(slashCommand instanceof HelpCommand)) {
                        message.append("## Command: ")
                                .append(slashCommand.getName())
                                .append("\n")
                                .append(slashCommand.getHelp());
                    }
                } else {
                    if (owner || (!(messageCommand instanceof AbstractOwner))) {
                        message.append("## Command: ")
                                .append(messageCommand.getName())
                                .append("\n")
                                .append(messageCommand.getHelp());
                    }
                }
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

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}
