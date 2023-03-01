package bobo.command.commands;

import bobo.CommandManager;
import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class HelpCommand implements ICommand {
    private final CommandManager manager;

    public HelpCommand(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        StringBuilder message = new StringBuilder();
        if (event.getOption("command") == null) {
            message.append("List of commands\n");
            for (ICommand command : manager.getCommands()) {
                message.append("`/")
                        .append(command.getName())
                        .append("`\n");
                if (command.getName().equals("help")) {
                    message.append("To get info on a specific command: `/help <command (without /)>`\n");
                }
            }
            event.reply(message.toString()).queue();
        } else  {
            String commandSearch = event.getOption("command").getAsString();
            ICommand command = manager.getCommand(commandSearch);
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
