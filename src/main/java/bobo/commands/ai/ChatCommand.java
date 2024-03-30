package bobo.commands.ai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;
import java.util.*;

public class ChatCommand extends AbstractAI {
    private static final Map<ThreadChannel, List<ChatMessage>> CHANNEL_MESSAGE_MAP = new HashMap<>();

    /**
     * Creates a new chat command.
     */
    public ChatCommand() {
        super(Commands.slash("chat", "Starts an OpenAI chat conversation."));
    }

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();

        Member member = event.getMember();
        assert member != null;
        String memberName = member.getUser().getGlobalName();
        assert memberName != null;

        ThreadChannel threadChannel = event.getChannel()
                .asTextChannel()
                .createThreadChannel(memberName + "'s conversation", true)
                .complete();
        threadChannel.addThreadMember(member).queue();

        startConversation(threadChannel);
        hook.editOriginal("Started a conversation with " + memberName + " in " + threadChannel.getAsMention()).queue();
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

        String prompt = event.getMessage().getContentDisplay();
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

        threadChannel.sendMessage(assistantMessage.getContent()).queue(success -> CHANNEL_MESSAGE_MAP.replace(threadChannel, messages));
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
        final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are Bobo, " +
                "a Discord bot created by Gil. You use slash commands and provide clipping, music, chat, image " +
                "creation, Last.fm info, Fortnite info, and other features. Don't refer to yourself as an AI language " +
                "model. When users talk to you, engage with them. For help, direct users to the 'help' command.");
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
}