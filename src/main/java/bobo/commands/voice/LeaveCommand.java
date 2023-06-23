package bobo.commands.voice;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;

public class LeaveCommand extends AbstractVoice {
    /**
     * Creates a new leave command.
     */
    public LeaveCommand() {
        super(Commands.slash("leave", "Leaves the voice channel."));
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

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
}
