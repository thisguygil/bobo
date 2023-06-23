package bobo.commands.voice;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JoinCommand extends AbstractVoice {
    /**
     * Creates a new join command.
     */
    public JoinCommand() {
        super(Commands.slash("join", "Joins the voice channel."));
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

        if (event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
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
        GuildVoiceState voiceState = Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState());
        AudioChannelUnion voiceChannel = voiceState.getChannel();
        if (voiceChannel == null) {
            event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
            return;
        }

        event.getGuildChannel().getGuild().getAudioManager().openAudioConnection(voiceChannel);
    }

    @Override
    public String getName() {
        return "join";
    }
}
