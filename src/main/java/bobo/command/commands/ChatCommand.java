package bobo.command.commands;

import bobo.Bobo;
import bobo.command.ICommand;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatCommand implements ICommand {
    private static final List<ChatMessage> messages = new ArrayList<>();

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        OpenAiService service = Bobo.getService();
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
        event.getHook().editOriginal(response).queue();
    }

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
