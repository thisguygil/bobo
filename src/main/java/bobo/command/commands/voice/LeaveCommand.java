package bobo.command.commands.voice;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class LeaveCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
            event.reply("I must already be connected to a voice channel to use this command.").queue();
            return;
        }
        leave(event);
        event.reply("Left.").queue();
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
