package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Steelix implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.reply("https://archives.bulbagarden.net/media/upload/thumb/2/2a/0208Steelix.png/600px-0208Steelix.png").queue();
    }

    @Override
    public String getName() {
        return "steelix";
    }

    @Override
    public String getHelp() {
        return "`/steelix`\n" +
                "steelix";
    }
}
