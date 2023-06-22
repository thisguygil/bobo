package bobo.command.commands.admin;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;
import java.util.Objects;

public class SayCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();
        InteractionHook hook = event.getHook();

        event.getChannel().sendMessage(Objects.requireNonNull(event.getOption("content")).getAsString()).queue();
        hook.editOriginal("Message sent").queue();
    }

    @Override
    public String getName() {
        return "say";
    }
}
