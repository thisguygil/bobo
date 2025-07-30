package bobo.commands.voice;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;

public class LeaveCommand extends AVoiceCommand {
    /**
     * Creates a new leave command.
     */
    public LeaveCommand() {
        super(Commands.slash("leave", "Leaves the voice channel."));
    }

    @Override
    protected CommandResponse handleVoiceCommand() {
        Guild guild = getGuild();
        if (!guild.getAudioManager().isConnected()) {
            return CommandResponse.text("I must already be connected to a voice channel to use this command.");
        }

        leave(guild);
        return CommandResponse.text("Left.");
    }

    /**
     * Leaves the voice channel of the specified guild.
     *
     * @param guild The guild's voice channel to leave.
     */
    public static void leave(@Nonnull Guild guild) {
        guild.getAudioManager().closeAudioConnection();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp() {
        return """
                Leaves the voice channel.
                Usage: `/leave`""";
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}
