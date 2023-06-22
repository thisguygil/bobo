package bobo.command.commands.admin;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class SayCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.getChannel().sendMessage(Objects.requireNonNull(event.getOption("content")).getAsString()).queue();
        event.reply("Message sent").setEphemeral(true).queue();
    }

    @Override
    public String getName() {
        return "say";
    }
}
