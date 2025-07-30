package bobo.commands.voice;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class MuteCommand extends AVoiceCommand {
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
    protected CommandResponse handleVoiceCommand() {
        Guild guild = getGuild();
        if (!guild.getAudioManager().isConnected()) {
            return CommandResponse.text("I am not connected to a voice channel.");
        }

        boolean isMuted = guild.getSelfMember().getVoiceState().isMuted();
        guild.getAudioManager().setSelfMuted(!isMuted);
        return CommandResponse.text((isMuted ? "Unm" : "M") + "uted.");
    }

    @Override
    public String getHelp() {
        return """
                Toggles the mute state of the bot.
                Usage: `/mute`""";
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}