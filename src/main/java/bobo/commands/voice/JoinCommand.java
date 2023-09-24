package bobo.commands.voice;

import bobo.lavaplayer.AudioReceiveListener;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

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

        AudioManager audioManager = event.getGuildChannel().getGuild().getAudioManager();
        if (audioManager.isConnected()) {
            hook.editOriginal("I must not be connected to a voice channel to use this command.").queue();
            return;
        }

        if (join(event)) {
            hook.editOriginal("Joined " + Objects.requireNonNull(audioManager.getConnectedChannel()).getAsMention()).queue();
        }
    }

    /**
     * Joins the voice channel of the user who sent the command.
     *
     * @param event The event that triggered this action.
     * @return Whether the bot successfully joined the voice channel.
     */
    public static boolean join(@Nonnull SlashCommandInteractionEvent event) {
        GuildVoiceState voiceState = Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState());
        AudioChannelUnion voiceChannel = voiceState.getChannel();
        if (voiceChannel == null) {
            event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
            return false;
        }

        try {
            AudioManager audioManager = event.getGuildChannel().getGuild().getAudioManager();
            audioManager.openAudioConnection(voiceChannel);
            audioManager.setReceivingHandler(new AudioReceiveListener(1));
        } catch (Exception e) {
            event.getHook().editOriginal("Failed to join voice channel.").queue();
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "join";
    }
}
