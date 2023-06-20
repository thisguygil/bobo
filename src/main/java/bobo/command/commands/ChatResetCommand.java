package bobo.command.commands;

import bobo.command.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class ChatResetCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        ChatCommand.initializeMessages();
        event.reply("Chat reset").queue();
    }

    @Override
    public String getName() {
        return "chat-reset";
    }
}
