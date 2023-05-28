package bobo.command.commands;

import bobo.command.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class SayCommand implements CommandInterface {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.getChannel().sendMessage(Objects.requireNonNull(event.getOption("content")).getAsString()).queue();
        event.reply("Message sent").setEphemeral(true).queue();
    }

    @Override
    public String getName() {
        return "say";
    }

    @Override
    public String getHelp() {
        return """
                `/say`
                Make bobo say what you tell it to
                Usage: `/say <message>`""";
    }
}
