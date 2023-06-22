package bobo.commands.ai;

public class ChatResetCommand extends AbstractAI {
    @Override
    protected void handleAICommand() {
        event.deferReply().queue();

        ChatCommand.initializeMessages();
        hook.editOriginal("Chat reset").queue();
    }

    @Override
    public String getName() {
        return "chat-reset";
    }
}
