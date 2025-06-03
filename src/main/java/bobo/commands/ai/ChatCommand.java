package bobo.commands.ai;

import bobo.commands.CommandResponse;
import com.openai.errors.OpenAIException;
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
        return new CommandResponse(String.format("Started a conversation with %s in %s", memberName, threadChannel.getAsMention()));
    }

    /**
     * Starts a conversation with the given thread channel by adding it to the conversations map.
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

        threadChannel.sendTyping().queue();

        try {
            List<ResponseInputItem> inputItems = new ArrayList<>();
            ResponseCreateParams.Builder createParams = ResponseCreateParams.builder()
                    .model(CHAT_MODEL);

            String previousResponseId = CONVERSATIONS.get(threadChannel);
            if (previousResponseId != null) {
                createParams.previousResponseId(previousResponseId);
            } else {
                inputItems.add(createSystemMessage());
            }

            inputItems.add(createUserMessage(event.getMessage()));
            createParams.inputOfResponse(inputItems);

            Response response = openAI.responses()
                    .create(createParams.build());

            String responseContent = response.output().getFirst()
                    .message().orElseThrow()
                    .content().getFirst()
                    .outputText().orElseThrow()
                    .text();

            splitAndSendMessage((MessageChannelUnion) threadChannel, responseContent, null, null);
            CONVERSATIONS.replace(threadChannel, response.id());
        } catch (OpenAIException | NoSuchElementException e) {
            threadChannel.sendMessage("Error generating response: " + e.getMessage()).queue();
            logger.error("Error generating response: {}", e.getMessage(), e);
        }
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
        channel.sendTyping().queue();

        try {
            List<ResponseInputItem> inputItems = new ArrayList<>();
            ResponseCreateParams.Builder createParams = ResponseCreateParams.builder()
                    .model(CHAT_MODEL);

            Message referenced = message.getReferencedMessage();
            String previousResponseId = null;
            if (referenced != null) {
                previousResponseId = PING_CONVERSATIONS.get(referenced.getId());
            }

            if (previousResponseId != null) {
                createParams.previousResponseId(previousResponseId);
            } else {
                inputItems.add(createSystemMessage());
            }

            inputItems.add(createUserMessage(event.getMessage()));
            createParams.inputOfResponse(inputItems);

            Response response = openAI.responses()
                    .create(createParams.build());

            String responseContent = response.output().getFirst()
                    .message().orElseThrow()
                    .content().getFirst()
                    .outputText().orElseThrow()
                    .text();

            splitAndSendMessage(channel, responseContent, message, response.id());
            if (referenced != null) {
                PING_CONVERSATIONS.remove(referenced.getId());
            }
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
                .addInputTextContent("You are Bobo, a Discord bot created by Gil. You use slash commands and provide " +
                        "clipping, music, chat, image creation, Last.fm info, Fortnite info, and other features. " +
                        "Don't refer to yourself as an AI language model. When users talk to you, engage with them. " +
                        "For help, direct users to the 'help' command.")
                .build());
    }

    /**
     * Splits a message into chunks and sends them to a message channel.
     *
     * @param channel the channel to send the message to
     * @param message the message to split
     * @param asReply a message to reply to, or null if no reply is needed
     * @param conversationId the ID of the conversation, used for tracking replies
     */
    private static void splitAndSendMessage(MessageChannelUnion channel, String message, @Nullable Message asReply, String conversationId) {
        int maxMessageLength = 2000;

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(message.length(), start + 2000);
            chunks.add(message.substring(start, end));
            start = end;
        }

        if (asReply != null && !chunks.isEmpty()) {
            asReply.reply(chunks.getFirst()).queue(success -> PING_CONVERSATIONS.put(success.getId(), conversationId));
            chunks.removeFirst();
        }
        chunks.forEach(chunk -> channel.sendMessage(chunk).queue());
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
    public Boolean shouldBeInvisible() {
        return false;
    }
}