package bobo.commands.voice;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Objects;

public class UndeafenCommand extends AbstractVoice {
    /**
     * Creates a new Undeafen command.
     */
    public UndeafenCommand() {
        super(Commands.slash("undeafen", "Undeafens the bot."));
    }

    @Override
    public String getName() {
        return "undeafen";
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
        if (Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isDeafened()) {
            audioManager.setSelfDeafened(false);
            hook.editOriginal("Undeafened.").queue();
        } else {
            hook.editOriginal("I am already undeafened. Use /deafen to deafen me.").queue();
        }
    }
}
