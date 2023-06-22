package bobo.command.commands.voice;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;
import java.util.Objects;

public class LeaveCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        if (!Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
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
        Objects.requireNonNull(event.getGuild()).getAudioManager().closeAudioConnection();
    }

    @Override
    public String getName() {
        return "leave";
    }
}
