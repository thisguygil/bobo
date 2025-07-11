package bobo.commands.ai;

import bobo.commands.CommandResponse;
import com.openai.errors.OpenAIException;
import com.openai.models.responses.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class TLDRCommand extends AAICommand {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Creates a new tldr command.
     */
    public TLDRCommand() {
        super(Commands.slash("tldr", "Summarizes the recent conversation in the channel.")
                .addOption(OptionType.INTEGER, "minutes", "How many minutes to summarize. No input searches until a 5-minute gap is found.", false));
    }

    @Override
    protected CommandResponse handleAICommand() {
        Integer minutesOption;
        try {
            minutesOption = Integer.parseInt(getOptionValue("minutes", 0));
        } catch (NumberFormatException e) {
            return CommandResponse.text("The number of minutes must be an integer.");
        } catch (RuntimeException ignored) {
            minutesOption = null;
        }

        Integer minutes = null;
        if (minutesOption != null) {
            minutes = minutesOption;
            if (minutes <= 0) {
                return CommandResponse.text("The number of minutes must be greater than zero.");
            }
        }

        List<Message> messages = fetchMessages((MessageChannelUnion) getChannel(), minutes);
        if (messages.size() < 10) {
            return CommandResponse.text("Not enough messages to summarize.");
        }

        String summary = summarizeMessages(messages);
        List<String> chunks = splitMessage(summary);
        MessageChannelUnion channel = (MessageChannelUnion) getChannel();

        return CommandResponse.builder()
                .setContent(chunks.getFirst())
                .setPostExecutionFromMessage(message -> sendChunksSequentially(channel, chunks, 1))
                .build();
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

            if (!messages.isEmpty() && shouldBreakLoop(messages.getFirst(), message, minutes)) {
                break;
            }

            messages.addFirst(message);
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
        return callOpenAI(prompt);
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
     */
    private String callOpenAI(String prompt) {
        List<ResponseInputItem> inputItems = new ArrayList<>();

        // System message
        inputItems.add(ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                .role(EasyInputMessage.Role.SYSTEM)
                .content("You are an assistant that summarizes Discord conversations. You will be given a conversation and are to provide a concise summary, highlighting key points and main topics discussed.")
                .build()));

        // User message
        inputItems.add(ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                .role(EasyInputMessage.Role.USER)
                .content(prompt)
                .build()));

        // Create the response request
        ResponseCreateParams createParams = ResponseCreateParams.builder()
                .model(CHAT_MODEL)
                .inputOfResponse(inputItems)
                .build();

        try {
            return openAI.responses()
                    .create(createParams)
                    .output().getFirst()
                    .message().orElseThrow()
                    .content().getFirst()
                    .outputText().orElseThrow()
                    .text();
        } catch (OpenAIException | NoSuchElementException e) {
            return "Unable to summarize conversation.";
        }
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

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}