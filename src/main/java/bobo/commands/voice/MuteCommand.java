package bobo.commands.voice;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Objects;

public class MuteCommand extends AbstractVoice {
    /**
     * Creates a new mute command.
     */
    public MuteCommand() {
        super(Commands.slash("mute", "Mutes/unmutes the bot."));
    }

    @Override
    public String getName() {
        return "mute";
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

        if (!event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
            hook.editOriginal("I am not connected to a voice channel.").queue();
            return;
        }

        Guild guild = event.getGuildChannel().getGuild();
        boolean isMuted = Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isMuted();
        guild.getAudioManager().setSelfMuted(!isMuted);
        hook.editOriginal((isMuted ? "Unm" : "M") + "uted.").queue();
    }
}