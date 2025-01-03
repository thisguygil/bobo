package bobo.commands.ai;

import bobo.Config;
import bobo.commands.CommandResponse;
import io.github.sashirestela.openai.common.content.ContentPart;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;
import java.util.*;

public class ChatCommand extends AAICommand {
    private static final String CHAT_MODEL = Config.get("CHAT_MODEL");
    private static final Map<ThreadChannel, List<ChatMessage>> CHANNEL_MESSAGE_MAP = new HashMap<>();

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
        List<ChatMessage> messages = new ArrayList<>();
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
        List<ChatMessage> messages = CHANNEL_MESSAGE_MAP.get(threadChannel);

        Message message = event.getMessage();
        String prompt = message.getContentDisplay();
        List<Message.Attachment> attachments = message.getAttachments();

        List<ContentPart> userMessageContent = new ArrayList<>();
        if (prompt.isEmpty() && attachments.isEmpty()) {
            return;
        }

        if (!prompt.isEmpty()) {
            userMessageContent.add(ContentPart.ContentPartText.of(prompt));
        }

        attachments.stream()
                .filter(Message.Attachment::isImage)
                .map(attachment -> ContentPart.ContentPartImageUrl.of(
                        ContentPart.ContentPartImageUrl.ImageUrl.of(attachment.getUrl())))
                .forEach(userMessageContent::add);

        ChatMessage userMessage = ChatMessage.UserMessage.of(userMessageContent);
        messages.add(userMessage);

        ChatRequest chatCompletionRequest = ChatRequest.builder()
                .model(CHAT_MODEL)
                .messages(messages)
                .build();

        Chat futureAssistantMessage = openAI.chatCompletions()
                .create(chatCompletionRequest)
                .join();

        ChatMessage.ResponseMessage assistantMessage = futureAssistantMessage.firstMessage();
        messages.add(assistantMessage);

        String responseContent = futureAssistantMessage.firstContent();

        List<String> responseChunks = splitMessage(responseContent, 2000);

        for (String chunk : responseChunks) {
            threadChannel.sendMessage(chunk).queue();
        }

        CHANNEL_MESSAGE_MAP.replace(threadChannel, messages);
    }

    /**
     * Splits a message into chunks of the specified size.
     *
     * @param message the message to split
     * @param maxLength the maximum length of each chunk
     * @return a list of message chunks
     */
    private static List<String> splitMessage(String message, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(message.length(), start + maxLength);
            chunks.add(message.substring(start, end));
            start = end;
        }
        return chunks;
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
    public static void initializeMessages(@Nonnull List<ChatMessage> messages) {
        messages.clear();
        final ChatMessage systemMessage = ChatMessage.SystemMessage.of(
                "You are Bobo, a Discord bot created by Gil. You use slash commands and provide clipping, " +
                        "music, chat, image creation, Last.fm info, Fortnite info, and other features. Don't refer " +
                        "to yourself as an AI language model. When users talk to you, engage with them. For help, " +
                        "direct users to the 'help' command."
        );
        messages.add(systemMessage);
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