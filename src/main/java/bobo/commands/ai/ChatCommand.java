package bobo.commands.ai;

import bobo.Config;
import bobo.commands.CommandResponse;
import com.openai.errors.OpenAIException;
import com.openai.models.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class ChatCommand extends AAICommand {
    private static final Logger logger = LoggerFactory.getLogger(ChatCommand.class);
    private static final String CHAT_MODEL = Config.get("CHAT_MODEL");
    private static final Map<ThreadChannel, List<ChatCompletionMessageParam>> CHANNEL_MESSAGE_MAP = new HashMap<>();

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
        return new CommandResponse("Started a conversation with " + memberName + " in " + threadChannel.getAsMention());
    }

    /**
     * Starts a conversation with the given thread channel.
     *
     * @param threadChannel the thread channel to start a conversation with
     */
    public static void startConversation(ThreadChannel threadChannel) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        initializeMessages(messages);
        CHANNEL_MESSAGE_MAP.put(threadChannel, messages);
    }

    /**
     * Handles a message received in a thread.
     * Designed for an OpenAI model that has vision capabilities.
     *
     * @param event the message received to handle
     */
    public static void handleThreadMessage(@Nonnull MessageReceivedEvent event) {
        ThreadChannel threadChannel = event.getChannel().asThreadChannel();
        if (!CHANNEL_MESSAGE_MAP.containsKey(threadChannel) || event.getAuthor().isSystem() || event.getAuthor().isBot()) {
            return;
        }

        threadChannel.sendTyping().queue();
        List<ChatCompletionMessageParam> messages = CHANNEL_MESSAGE_MAP.get(threadChannel);

        List<ChatCompletionContentPart> userMessageContent = buildUserMessageContent(event.getMessage());
        if (userMessageContent.isEmpty()) {
            return;
        }

        ChatCompletionMessageParam userMessage = createUserMessage(userMessageContent);
        messages.add(userMessage);

        try {
            ChatCompletionCreateParams chatCompletionRequest = ChatCompletionCreateParams.builder()
                    .model(CHAT_MODEL)
                    .messages(messages)
                    .build();

            String responseContent = openAI.chat()
                    .completions()
                    .create(chatCompletionRequest)
                    .choices()
                    .getFirst()
                    .message()
                    .content()
                    .orElse("No response.");

            ChatCompletionMessageParam assistantMessageParam = createAssistantMessage(responseContent);
            messages.add(assistantMessageParam);

            splitAndSendMessage(threadChannel, responseContent);

            CHANNEL_MESSAGE_MAP.replace(threadChannel, messages);
        } catch (OpenAIException e) {
            threadChannel.sendMessage("Error generating response: " + e.getMessage()).queue();
            logger.error("Error generating response: " + e.getMessage(), e);
        }
    }

    private static List<ChatCompletionContentPart> buildUserMessageContent(Message message) {
        List<ChatCompletionContentPart> contentParts = new ArrayList<>();

        if (!message.getContentDisplay().isEmpty()) {
            contentParts.add(ChatCompletionContentPart.ofChatCompletionContentPartText(ChatCompletionContentPartText.builder()
                    .type(ChatCompletionContentPartText.Type.TEXT)
                    .text(message.getContentDisplay())
                    .build()
            ));
        }

        message.getAttachments().stream()
                .filter(Message.Attachment::isImage)
                .forEach(attachment -> contentParts.add(ChatCompletionContentPart.ofChatCompletionContentPartImage(ChatCompletionContentPartImage.builder()
                        .type(ChatCompletionContentPartImage.Type.IMAGE_URL)
                        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                .url(attachment.getUrl())
                                .build()
                        )
                        .build()
                )));

        return contentParts;
    }

    private static ChatCompletionMessageParam createUserMessage(List<ChatCompletionContentPart> content) {
        return ChatCompletionMessageParam.ofChatCompletionUserMessageParam(ChatCompletionUserMessageParam.builder()
                .role(ChatCompletionUserMessageParam.Role.USER)
                .content(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(content))
                .build()
        );
    }

    private static ChatCompletionMessageParam createAssistantMessage(String responseContent) {
        return ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(ChatCompletionAssistantMessageParam.builder()
                .role(ChatCompletionAssistantMessageParam.Role.ASSISTANT)
                .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(responseContent))
                .build()
        );
    }

    /**
     * Splits a message into chunks and sends them to a thread channel.
     *
     * @param threadChannel the thread channel to send the message to
     * @param message the message to split
     */
    private static void splitAndSendMessage(ThreadChannel threadChannel, String message) {
        int maxMessageLength = 2000;

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(message.length(), start + 2000);
            chunks.add(message.substring(start, end));
            start = end;
        }

        chunks.forEach(chunk -> threadChannel.sendMessage(chunk).queue());
    }

    /**
     * Handles a thread delete event.
     *
     * @param event the thread delete event to handle
     */
    public static void handleThreadDelete(@Nonnull ChannelDeleteEvent event) {
        CHANNEL_MESSAGE_MAP.remove(event.getChannel().asThreadChannel());
    }

    /**
     * Clears the messages list and adds a system message to it.
     *
     * @param messages the messages list to initialize
     */
    public static void initializeMessages(@Nonnull List<ChatCompletionMessageParam> messages) {
        messages.clear();
        final ChatCompletionMessageParam systemMessageParam = ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(ChatCompletionSystemMessageParam.builder()
                .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                .content(ChatCompletionSystemMessageParam.Content.ofTextContent(
                "You are Bobo, a Discord bot created by Gil. You use slash commands and provide clipping, " +
                        "music, chat, image creation, Last.fm info, Fortnite info, and other features. Don't refer " +
                        "to yourself as an AI language model. When users talk to you, engage with them. For help, " +
                        "direct users to the 'help' command.")
                ).build()
        );
        messages.add(systemMessageParam);
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