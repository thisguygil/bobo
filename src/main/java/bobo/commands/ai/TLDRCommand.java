package bobo.commands.ai;

import bobo.Config;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TLDRCommand extends AbstractAI {
    private static final String TLDR_MODEL = Config.get("TLDR_MODEL");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Creates a new tldr command.
     */
    public TLDRCommand() {
        super(Commands.slash("tldr", "Summarizes the recent conversation in the channel.")
                .addOption(OptionType.INTEGER, "minutes", "How many minutes to summarize. No input searches until a 5-minute gap is found.", false));
    }

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();

        OptionMapping minutesOption = event.getOption("minutes");
        Integer minutes = null;
        if (minutesOption != null) {
            minutes = minutesOption.getAsInt();
            if (minutes <= 0) {
                event.getHook().editOriginal("The number of minutes must be greater than zero.").queue();
                return;
            }
        }

        List<Message> messages = fetchMessages(event.getChannel(), minutes);
        if (messages.size() < 10) {
            hook.editOriginal("Not enough messages to summarize.").queue();
            return;
        }

        String summary = summarizeMessages(messages);
        hook.editOriginal(summary).queue();
    }

    /**
     * Fetches the messages from the given channel.
     *
     * @param channel the channel to fetch messages from
     * @param minutes the number of minutes to fetch messages for
     * @return the fetched messages
     */
    private List<Message> fetchMessages(MessageChannelUnion channel, Integer minutes) {
        List<Message> messages = new ArrayList<>();

        for (Message message : channel.getIterableHistory()) {
            if (isMessageSkippable(message)) {
                continue;
            }

            if (!messages.isEmpty() && shouldBreakLoop(messages.get(0), message, minutes)) {
                break;
            }

            messages.add(0, message);
        }

        int size = messages.size();
        return size > 100 ? messages.subList(size - 100, size) : messages;
    }

    /**
     * Checks if the given message is skippable.
     *
     * @param message the message to check
     * @return true if the message is skippable, false otherwise
     */
    private boolean isMessageSkippable(Message message) {
        User author = message.getAuthor();
        return author.isBot() || author.isSystem() || message.getContentDisplay().isBlank();
    }

    /**
     * Checks if the loop should be broken.
     *
     * @param firstMessage the first message in the conversation
     * @param currentMessage the current message
     * @param minutes the number of minutes to summarize
     * @return true if the loop should be broken, false otherwise
     */
    private boolean shouldBreakLoop(Message firstMessage, Message currentMessage, Integer minutes) {
        if (minutes != null) {
            Duration timeFromNow = Duration.between(currentMessage.getTimeCreated(), OffsetDateTime.now()).abs();
            return timeFromNow.toMinutes() > minutes;
        } else {
            Duration timeGap = Duration.between(currentMessage.getTimeCreated(), firstMessage.getTimeCreated()).abs();
            return timeGap.toMinutes() > 5;
        }
    }

    /**
     * Summarizes the given messages.
     *
     * @param messages the messages to summarize
     * @return the summarized messages
     */
    private String summarizeMessages(List<Message> messages) {
        String prompt = "Conversation:\n" + formatMessages(messages) + "\n";
        try {
            return callOpenAI(prompt);
        } catch (Exception e) {
            return "Failed to summarize the conversation: " + e.getMessage();
        }
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
            String timestamp = message.getTimeCreated().format(FORMATTER);
            formattedConversation.append(message.getAuthor().getEffectiveName())
                    .append(" [")
                    .append(timestamp)
                    .append("]: ")
                    .append(message.getContentDisplay())
                    .append("\n");
        }

        return formattedConversation.toString();
    }

    /**
     * Calls the OpenAI API with the given prompt.
     *
     * @param prompt the prompt to call the API with
     * @return the response from the API
     * @throws Exception if an error occurs
     */
    private String callOpenAI(String prompt) throws Exception {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(TLDR_MODEL)
                .messages(List.of(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are an assistant that summarizes Discord conversations. You will be given a conversation and are to provide a concise summary, highlighting key points and main topics discussed."),
                        new ChatMessage(ChatMessageRole.USER.value(), prompt)
                ))
                .build();

        return service.createChatCompletion(chatCompletionRequest)
                .getChoices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new Exception("No response from OpenAI"))
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
                Summarizes the recent conversation in the channel, up to 100 messages.
                Optionally, you can specify the number of minutes to summarize.
                If not specified, the command will take messages until a 5-minute gap is found.
                Usage: `/tldr <minutes>`""";
    }

    @Override
    protected List<Permission> getAICommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_HISTORY));
    }
}