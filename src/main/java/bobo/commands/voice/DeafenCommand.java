package bobo.commands.voice;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Objects;

public class DeafenCommand extends AbstractVoice {
    /**
     * Creates a new deafen command.
     */
    public DeafenCommand() {
        super(Commands.slash("deafen", "Deafens the bot."));
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
        AudioManager audioManager = guild.getAudioManager();
        if (!Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isDeafened()) {
            audioManager.setSelfDeafened(true);
            hook.editOriginal("Deafened.").queue();
        } else {
            hook.editOriginal("I am already deafened. Use /undeafen to undeafen me.").queue();
        }
    }
}
