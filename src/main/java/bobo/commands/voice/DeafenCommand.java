package bobo.commands.voice;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

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
            return new CommandResponse("I am not connected to a voice channel.");
        }

        boolean isDeafened = guild.getSelfMember().getVoiceState().isDeafened();
        guild.getAudioManager().setSelfDeafened(!isDeafened);
        return new CommandResponse((isDeafened ? "Und" : "D") + "eafened.");
    }

    @Override
    public String getHelp() {
        return """
                Toggles the deafen state of the bot.
                Usage: `/deafen`""";
    }

    @Override
    protected List<Permission> getVoiceCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}