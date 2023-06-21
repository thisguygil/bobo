package bobo.command.commands.voice;

import bobo.command.ICommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JoinCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        if (Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
            event.reply("I must not be connected to a voice channel to use this command.").queue();
            return;
        }
        join(event);
        event.reply("Joined.").queue();
    }

    public static void join(@Nonnull SlashCommandInteractionEvent event) {
        // Check if joining is valid
        if (Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel() == null) {
            event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
            return;
        }

        // Actually joins
        Guild guild = event.getGuild();
        Objects.requireNonNull(guild).getAudioManager().openAudioConnection(Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel());
    }

    @Override
    public String getName() {
        return "join";
    }

}
