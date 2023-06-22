package bobo.commands.ai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatCommand extends AbstractAI {
    private static final List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();
        String prompt = Objects.requireNonNull(event.getOption("prompt")).getAsString();

        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .build();

        ChatMessage assistantMessage = service.createChatCompletion(chatCompletionRequest)
                .getChoices()
                .get(0)
                .getMessage();
        messages.add(assistantMessage);

        String response = "**" + prompt + "**\n" + assistantMessage.getContent();
        hook.editOriginal(response).queue();
    }

    /**
     * Clears the messages list and adds a system message to it.
     */
    public static void initializeMessages() {
        messages.clear();
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are Bobo, " +
                "a Discord bot. You use slash commands and provide music, chat, image creation, and other features. " +
                "Don't refer to yourself as an AI language model. When users call you with the 'chat' command, " +
                "engage with them. For help, direct users to the 'help' command.");
        messages.add(systemMessage);
    }

    @Override
    public String getName() {
        return "chat";
    }
}
