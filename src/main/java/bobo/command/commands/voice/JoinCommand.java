package bobo.command.commands.voice;

import bobo.command.ICommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JoinCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        if (Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
            hook.editOriginal("I must not be connected to a voice channel to use this command.").queue();
            return;
        }

        join(event);
        hook.editOriginal("Joined.").queue();
    }

    /**
     * Joins the voice channel of the user who sent the command.
     *
     * @param event The event that triggered this action.
     */
    public static void join(@Nonnull SlashCommandInteractionEvent event) {
        GuildVoiceState voiceState = Objects.requireNonNull(event.getMember()).getVoiceState();
        if (Objects.requireNonNull(voiceState).getChannel() == null) {
            event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
            return;
        }

        Guild guild = event.getGuild();
        Objects.requireNonNull(guild).getAudioManager().openAudioConnection(Objects.requireNonNull(voiceState).getChannel());
    }

    @Override
    public String getName() {
        return "join";
    }
}
