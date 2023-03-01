package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SayCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.getChannel().sendMessage(event.getOption("content").getAsString()).queue();
    }

    @Override
    public String getName() {
        return "say";
    }

    @Override
    public String getHelp() {
        return "`/say`\n" +
                "Make bobo say what you tell it to\n" +
                "Usage: `/say <message>`";
    }
}
