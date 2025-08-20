package bobo.commands.ai;

import bobo.commands.CommandResponse;
import com.openai.errors.OpenAIException;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class ChatCommand extends AAICommand {
    private static final Logger logger = LoggerFactory.getLogger(ChatCommand.class);
    private static final Map<ThreadChannel, String> CONVERSATIONS = new HashMap<>();
    private static final Map<String, String> PING_CONVERSATIONS = new HashMap<>();

    /**
     * Creates a new chat command.
     */
    public ChatCommand() {
        super(Commands.slash("chat", "Starts an OpenAI chat conversation."));
    }

    @Override
    protected CommandResponse handleAICommand() {
        Member member = getMember();
        String memberName = getUser().getGlobalName();

        ThreadChannel threadChannel = ((TextChannel) getChannel())
                .createThreadChannel(memberName + "'s conversation", true)
                .complete();
        threadChannel.addThreadMember(member).queue();

        startConversation(threadChannel);
        return CommandResponse.text("Started a conversation with %s in %s", memberName, threadChannel.getAsMention());
    }

    /**
     * Starts a conversation with the given thread channel by adding it to the conversation map.
     *
     * @param threadChannel the thread channel to start a conversation with
     */
    public static void startConversation(ThreadChannel threadChannel) {
        CONVERSATIONS.put(threadChannel, null);
    }

    /**
     * Handles a message received in a thread.
     * Designed for an OpenAI model that has vision capabilities.
     *
     * @param event the message received to handle
     */
    public static void handleThreadMessage(@Nonnull MessageReceivedEvent event) {
        ThreadChannel threadChannel = event.getChannel().asThreadChannel();
        if (!CONVERSATIONS.containsKey(threadChannel) || event.getAuthor().isSystem() || event.getAuthor().isBot()) {
            return;
        }

        String previousResponseId = CONVERSATIONS.get(threadChannel);
        handleMessageInternal((MessageChannelUnion) threadChannel, event.getMessage(), previousResponseId, null,
                (responseId, sentMessage) -> CONVERSATIONS.put(threadChannel, responseId));
    }

    /**
     * Handles when a user pings the bot in a message.
     * Separate from the thread handling, this is for when the bot is mentioned in a regular message.
     *
     * @param event the message to reply to
     */
    public static void pingResponse(MessageReceivedEvent event) {
        MessageChannelUnion channel = event.getChannel();
        Message message = event.getMessage();

        Message referenced = message.getReferencedMessage();
        String previousResponseId = referenced != null ? PING_CONVERSATIONS.get(referenced.getId()) : null;

        handleMessageInternal(channel, message, previousResponseId, message,
                (responseId, sentMessage) -> {
                    if (referenced != null) {
                        PING_CONVERSATIONS.remove(referenced.getId());
                    }
                    PING_CONVERSATIONS.put(sentMessage.getId(), responseId);
                });
    }

    /**
     * Handles a message in a thread or channel, sending a response using OpenAI's API.
     *
     * @param channel the channel to send the response to
     * @param message the message to process
     * @param previousResponseId the ID of the previous response, if any
     * @param replyTo an optional message to reply to
     * @param onResponseSent an optional callback to execute when the response is sent
     */
    private static void handleMessageInternal(@Nonnull MessageChannelUnion channel, Message message, @Nullable String previousResponseId, @Nullable Message replyTo, @Nullable BiConsumer<String, Message> onResponseSent) {
        channel.sendTyping().queue();

        try {
            List<ResponseInputItem> inputItems = new ArrayList<>();
            ResponseCreateParams.Builder createParams = ResponseCreateParams.builder()
                    .model(CHAT_MODEL)
                    .reasoning(Reasoning.builder()
                            .effort(ReasoningEffort.LOW)
                            .build())
                    .addTool(WebSearchTool.builder()
                            .type(WebSearchTool.Type.WEB_SEARCH_PREVIEW)
                            .searchContextSize(WebSearchTool.SearchContextSize.LOW)
                            .build());

            if (previousResponseId != null) {
                createParams.previousResponseId(previousResponseId);
            } else {
                inputItems.add(createSystemMessage());
            }

            inputItems.add(createUserMessage(message));
            createParams.inputOfResponse(inputItems);

            Response response = openAI.responses().create(createParams.build());

            List<ResponseOutputItem> responseOutput = response.output();
            ResponseOutputItem outputItem;
            if (responseOutput.get(1).isWebSearchCall()) {
                outputItem = responseOutput.getLast();
            } else {
                outputItem = responseOutput.get(1);
            }

            String responseContent = outputItem.message().orElseThrow()
                    .content().getFirst()
                    .outputText().orElseThrow()
                    .text();

            List<String> chunks = splitMessage(responseContent);
            if (replyTo != null && !chunks.isEmpty()) {
                String firstChunk = chunks.removeFirst();
                replyTo.reply(firstChunk).setSuppressEmbeds(true).queue(sentMsg -> {
                    if (onResponseSent != null) {
                        onResponseSent.accept(response.id(), sentMsg);
                    }
                });
            }
            sendChunksSequentially(channel, chunks, 0);
        } catch (OpenAIException | NoSuchElementException e) {
            channel.sendMessage("Error generating response: " + e.getMessage()).queue();
            logger.error("Error generating response: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates a user message for the OpenAI API given a Discord message.
     *
     * @param message the Discord message to convert
     * @return a ResponseInputItem representing the user message
     */
    private static ResponseInputItem createUserMessage(Message message) {
        ResponseInputItem.Message.Builder messageBuilder = ResponseInputItem.Message.builder()
                .role(ResponseInputItem.Message.Role.USER);

        String messageContent = message.getContentDisplay();
        if (!messageContent.isEmpty()) {
            messageBuilder.addInputTextContent(messageContent).build();
        }

        message.getAttachments().stream()
                .filter(Message.Attachment::isImage)
                .forEach(attachment -> messageBuilder.addContent(ResponseInputImage.builder()
                        .imageUrl(attachment.getUrl())
                        .detail(ResponseInputImage.Detail.AUTO)
                        .build()
                ));

        return ResponseInputItem.ofMessage(messageBuilder.build());
    }

    /**
     * Creates a preset system message for the OpenAI API.
     * @return a ResponseInputItem representing the system message
     */
    private static ResponseInputItem createSystemMessage() {
        return ResponseInputItem.ofMessage(ResponseInputItem.Message.builder()
                .role(ResponseInputItem.Message.Role.SYSTEM)
                .addInputTextContent("""
                        You are Bobo, a Discord bot created by Gil. Interact with users through both slash commands and prefix commands, offering features including clipping, music playback, image generation, Last.fm data, Fortnite stats, and more.
                        Direct users to the 'help' command for assistance.
                        Maintain a witty, dry sense of humor—playful sarcasm and gentle teasing are encouraged, especially for repetitive or obvious queries.
                        Responses must remain sharp, entertaining, and clever, but never hostile or dismissive.
                        Always ensure that you provide meaningful answers for every user request.
                        Prefer short natural sentences or paragraphs over lists or bullet points, but use them if they genuinely make the response clearer.
                        """)
                .build());
    }

    /**
     * Handles a thread delete event.
     *
     * @param event the thread delete event to handle
     */
    public static void handleThreadDelete(@Nonnull ChannelDeleteEvent event) {
        CONVERSATIONS.remove(event.getChannel().asThreadChannel());
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                Starts an OpenAI chat conversation in a new thread.
                Usage: `/chat`""";
    }

    @Override
    protected List<Permission> getAICommandPermissions() {
        return new ArrayList<>(List.of(Permission.CREATE_PUBLIC_THREADS, Permission.MESSAGE_SEND_IN_THREADS));
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}