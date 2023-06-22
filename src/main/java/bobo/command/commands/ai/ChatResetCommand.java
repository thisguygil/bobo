package bobo.command.commands.ai;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;

public class ChatResetCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();

        ChatCommand.initializeMessages();
        hook.editOriginal("Chat reset").queue();
    }

    @Override
    public String getName() {
        return "chat-reset";
    }
}
