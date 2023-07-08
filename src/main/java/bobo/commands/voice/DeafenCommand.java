package bobo.commands.voice;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.Objects;

public class DeafenCommand extends AbstractVoice {
    /**
     * Creates a new deafen command.
     */
    public DeafenCommand() {
        super(Commands.slash("deafen", "Deafens the bot.")
                .addSubcommands(new SubcommandData("on", "Deafens the bot."))
                .addSubcommands(new SubcommandData("off", "Undeafens the bot."))
        );
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

        String subcommandName = Objects.requireNonNull(event.getSubcommandName());
        Guild guild = event.getGuildChannel().getGuild();
        AudioManager audioManager = guild.getAudioManager();
        boolean isDeafened = Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isDeafened();

        if (subcommandName.equals("on")) {
            if (!isDeafened) {
                audioManager.setSelfDeafened(true);
                hook.editOriginal("Deafened.").queue();
            } else {
                hook.editOriginal("I am already deafened. Use /deafen **off** to undeafen me.").queue();
            }
        } else if (subcommandName.equals("off")) {
            if (isDeafened) {
                audioManager.setSelfDeafened(false);
                hook.editOriginal("Undeafened.").queue();
            } else {
                hook.editOriginal("I am already undeafened. Use /deafen **on** to deafen me.").queue();
            }
        }
    }
}
