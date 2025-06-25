package bobo.commands.voice;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class DeafenCommand extends AVoiceCommand {
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
    protected CommandResponse handleVoiceCommand() {
        Guild guild = getGuild();
        if (!guild.getAudioManager().isConnected()) {
            return CommandResponse.text("I am not connected to a voice channel.");
        }

        boolean isDeafened = guild.getSelfMember().getVoiceState().isDeafened();
        guild.getAudioManager().setSelfDeafened(!isDeafened);
        return CommandResponse.text((isDeafened ? "Und" : "D") + "eafened.");
    }

    @Override
    public String getHelp() {
        return """
                Toggles the deafen state of the bot.
                Usage: `/deafen`""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}