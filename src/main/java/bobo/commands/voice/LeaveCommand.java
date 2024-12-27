package bobo.commands.voice;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class LeaveCommand extends AbstractVoice {
    /**
     * Creates a new leave command.
     */
    public LeaveCommand() {
        super(Commands.slash("leave", "Leaves the voice channel."));
    }

    @Override
    protected void handleVoiceCommand() {
        if (!event.getGuildChannel().getGuild().getAudioManager().isConnected()) {
            hook.editOriginal("I must already be connected to a voice channel to use this command.").queue();
            return;
        }

        leave(event);
        hook.editOriginal("Left.").queue();
    }

    /**
     * Leaves the voice channel of the user who sent the command.
     *
     * @param event The event that triggered this action.
     */
    public static void leave(@Nonnull SlashCommandInteractionEvent event) {
        event.getGuildChannel().getGuild().getAudioManager().closeAudioConnection();
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
    protected List<Permission> getVoiceCommandPermissions() {
        return new ArrayList<>();
    }

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}
