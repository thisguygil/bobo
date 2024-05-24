package bobo.commands.ai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TLDRCommand extends AbstractAI {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Creates a new tldr command.
     */
    public TLDRCommand() {
        super(Commands.slash("tldr", "Summarizes the recent conversation in the channel, until a 5-minute gap is found."));
    }

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();

        MessageChannelUnion channel = event.getChannel();
        List<Message> messages = new ArrayList<>();

        // Get the last messages in the channel
        for (Message message : channel.getIterableHistory()) {
            // Skip bot and system messages
            User author = message.getAuthor();
            if (author.isBot() || author.isSystem()) {
                continue;
            }

            // Break if the time gap between messages is greater than 5 minutes
            if (!messages.isEmpty()) {
                Message previousMessage = messages.get(0);
                Duration timeGap = Duration.between(previousMessage.getTimeCreated(), message.getTimeCreated()).abs();
                if (timeGap.toMinutes() > 5) {
                    break;
                }
            }

            messages.add(0, message);
        }

        // Only summarize 10 or more messages
        if (messages.size() < 10) {
            hook.editOriginal("Not enough messages to summarize.").queue();
            return;
        }

        // Format the conversation and summarize it
        String formattedConversation = formatMessages(messages);
        String summary;
        try { // An exception can be thrown for various reasons
            summary = summarizeConversation(formattedConversation);
        } catch (Exception e) {
            summary = "Failed to summarize the conversation: " + e.getMessage();
        }

        hook.editOriginal(summary).queue();
    }

    /**
     * Formats the given messages into a readable conversation.
     *
     * @param messages the messages to format
     * @return the formatted conversation
     */
    public String formatMessages(List<Message> messages) {
        StringBuilder formattedConversation = new StringBuilder();
        for (Message message : messages) {
            String content = message.getContentDisplay();
            if (content.isBlank()) {
                continue;
            }

            String timestamp = message.getTimeCreated().format(FORMATTER);
            formattedConversation.append(message.getAuthor().getEffectiveName())
                    .append(" [")
                    .append(timestamp)
                    .append("]: ")
                    .append(content)
                    .append("\n");
        }

        return formattedConversation.toString();
    }

    /**
     * Summarizes the given conversation using OpenAI
     *
     * @param conversation the formatted conversation to summarize
     * @return the summary of the messages
     */
    public String summarizeConversation(String conversation) {
        String prompt = "Summarize the following Discord conversation, highlighting the key points and main topics discussed. Provide a concise summary.\n\n" +
                "Conversation:\n" + conversation + "\n";

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), prompt)))
                .build();

        return service.createChatCompletion(chatCompletionRequest)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }

    @Override
    public String getName() {
        return "tldr";
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                Summarizes the recent conversation in the channel.
                Usage: `/tldr`""";
    }
}
