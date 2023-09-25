package bobo.commands.voice;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Objects;

public class DeafenCommand extends AbstractVoice {
    /**
     * Creates a new deafen command.
     */
    public DeafenCommand() {
        super(Commands.slash("deafen", "Deafens/undeafens the bot."));
    }

    @Override
    public String getName() {
        return "deafen";
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

        if (!event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
            hook.editOriginal("I am not connected to a voice channel.").queue();
            return;
        }

        Guild guild = event.getGuildChannel().getGuild();
        boolean isDeafened = Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isDeafened();
        guild.getAudioManager().setSelfDeafened(!isDeafened);
        hook.editOriginal((isDeafened ? "Und" : "D") + "eafened.").queue();
    }
}
