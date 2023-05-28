package bobo.command.commands.voice;

import bobo.command.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class LeaveCommand implements CommandInterface {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
            event.getHook().editOriginal("I must already be connected to a voice channel to use this command.").queue();
            return;
        }
        leave(event);
        event.getHook().editOriginal("Left.").queue();
    }

    public static void leave(@Nonnull SlashCommandInteractionEvent event) {
        Objects.requireNonNull(event.getGuild()).getAudioManager().closeAudioConnection();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp() {
        return "`/leave`\n" +
                "Leaves the voice channel";
    }
}
