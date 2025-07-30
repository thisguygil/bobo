package bobo.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.intellij.lang.annotations.PrintFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a response to a command execution, encapsulating the content, visibility, attachments, embeds, and post-execution actions.
 * If one of the post-execution actions is non-null, the other must be null.
 *
 * @param content                  The textual content of the response.
 * @param hidden                Whether the reply should be ephemeral or the bot should show typing before replying.
 * @param attachments              Files to be attached to the message.
 * @param embeds                   Embeds to be sent with the message.
 * @param postExecutionFlatMap     Post-execution action to be queued in succession after this message, returning a {@link RestAction}.
 * @param postExecutionHookFlatMap Post-execution action to be queued in succession after this non-deferred interaction, returning a {@link RestAction}.
 * @param postExecutionFromMessage Post-execution action to be queued after this message is sent.
 * @param postExecutionFromHook    Post-execution action to be queued after this non-deferred interaction is completed.
 * @param failureHandler           Handler for exceptions during command execution.
 */
public record CommandResponse(
        String content,
        Boolean hidden,
        Collection<? extends FileUpload> attachments,
        Collection<? extends MessageEmbed> embeds,
        Function<? super Message, ? extends RestAction<Message>> postExecutionFlatMap,
        Function<? super InteractionHook, ? extends RestAction<InteractionHook>> postExecutionHookFlatMap,
        Consumer<? super Message> postExecutionFromMessage,
        Consumer<? super InteractionHook> postExecutionFromHook,
        Consumer<? super Throwable> failureHandler
) {
    /**
     * Not recommended to use this constructor directly; use the {@link CommandResponseBuilder} or static methods instead for better readability and maintainability.
     * However, we must expose this constructor because it is a record class.
     * <br>
     * Constructs a {@link CommandResponse} with the provided parameters.
     *
     * @param content                  The textual content of the response.
     * @param hidden                Whether the reply should be ephemeral or the bot should show typing before replying.
     * @param attachments              Files to be attached to the message.
     * @param embeds                   Embeds to be sent with the message.
     * @param postExecutionFlatMap     Post-execution action to be queued in succession after this message, returning a {@link RestAction}.
     * @param postExecutionHookFlatMap Post-execution action to be queued in succession after this non-deferred interaction, returning a {@link RestAction}.
     * @param postExecutionFromMessage Post-execution action to be queued after this message is sent.
     * @param postExecutionFromHook    Post-execution action to be queued after this non-deferred interaction is completed.
     * @param failureHandler           Handler for exceptions during command execution.
     */
    public CommandResponse(
            String content,
            Boolean hidden,
            Collection<? extends FileUpload> attachments,
            Collection<? extends MessageEmbed> embeds,
            Function<? super Message, ? extends RestAction<Message>> postExecutionFlatMap,
            Function<? super InteractionHook, ? extends RestAction<InteractionHook>> postExecutionHookFlatMap,
            Consumer<? super Message> postExecutionFromMessage,
            Consumer<? super InteractionHook> postExecutionFromHook,
            Consumer<? super Throwable> failureHandler
    ) {
        if (postExecutionFromMessage != null && postExecutionFromHook != null) {
            throw new IllegalArgumentException("Only one of postExecutionFromMessage or postExecutionFromHook can be non-null.");
        }

        this.content = content;
        this.hidden = hidden;
        this.attachments = (attachments == null || attachments.isEmpty()) ? null : attachments;
        this.embeds = (embeds == null || embeds.isEmpty()) ? null : embeds;
        this.postExecutionFlatMap = postExecutionFlatMap;
        this.postExecutionHookFlatMap = postExecutionHookFlatMap;
        this.postExecutionFromMessage = postExecutionFromMessage;
        this.postExecutionFromHook = postExecutionFromHook;
        this.failureHandler = failureHandler;
    }

    /**
     * Converts the {@link CommandResponse} to {@link MessageCreateData} for sending as a new message.
     *
     * @return The {@link MessageCreateData} object.
     */
    public MessageCreateData asMessageCreateData() {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (content != null && !content.isEmpty()) builder.setContent(content);
        if (attachments != null && !attachments.isEmpty()) builder.setFiles(attachments);
        if (embeds != null && !embeds.isEmpty()) builder.setEmbeds(embeds);
        return builder.build();
    }

    /**
     * Converts the {@link CommandResponse} to {@link MessageEditData} for editing an existing message.
     *
     * @return The {@link MessageEditData} object.
     */
    public MessageEditData asMessageEditData() {
        MessageEditBuilder builder = new MessageEditBuilder();
        if (content != null && !content.isEmpty()) builder.setContent(content);
        if (attachments != null && !attachments.isEmpty()) builder.setFiles(attachments);
        if (embeds != null && !embeds.isEmpty()) builder.setEmbeds(embeds);
        return builder.build();
    }

    /**
     * Applies the response to a {@link SlashCommandInteractionEvent} or {@link MessageReceivedEvent}.
     *
     * @param action The action to apply the response to.
     */
    public void applyToMessage(RestAction<Message> action) {
        if (postExecutionFlatMap() != null) {
            action.flatMap(postExecutionFlatMap()).queue(postExecutionFromMessage(), failureHandler());
        } else {
            action.queue(postExecutionFromMessage(), failureHandler());
        }
    }

    /**
     * Applies the response to an {@link InteractionHook}.
     *
     * @param action The action to apply the response to.
     */
    public void applyToHook(RestAction<InteractionHook> action) {
        if (postExecutionHookFlatMap() != null) {
            action.flatMap(postExecutionHookFlatMap()).queue(postExecutionFromHook(), failureHandler());
        } else {
            action.queue(postExecutionFromHook(), failureHandler());
        }
    }

    /**
     * Builder class for constructing {@link CommandResponse} instances.
     */
    public static class CommandResponseBuilder {
        private String content;
        private Boolean invisible;
        private final Collection<FileUpload> attachments = new ArrayList<>();
        private final Collection<MessageEmbed> embeds = new ArrayList<>();
        private Function<? super Message, ? extends RestAction<Message>> postExecutionFlatMap;
        private Function<? super InteractionHook, ? extends RestAction<InteractionHook>> postExecutionHookFlatMap;
        private Consumer<? super Message> postExecutionFromMessage;
        private Consumer<? super InteractionHook> postExecutionFromHook;
        private Consumer<? super Throwable> failureHandler;

        /**
         * Sets the textual content for the response.
         *
         * @param content The response content.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setContent(String content) {
            this.content = content;
            return this;
        }

        /**
         * Sets the textual content for the response, formatted with the provided arguments.
         *
         * @param content The response content with format specifiers.
         * @param args    Arguments to format the content with.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setContent(@PrintFormat String content, Object... args) {
            this.content = String.format(content, args);
            return this;
        }

        /**
         * Sets the visibility of the response.
         *
         * @param invisible Whether the reply should be hidden or not.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setInvisible(Boolean invisible) {
            this.invisible = invisible;
            return this;
        }

        /**
         * Adds {@link FileUpload} attachments to the response.
         *
         * @param attachments Files to attach.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder addAttachments(Collection<FileUpload> attachments) {
            this.attachments.addAll(attachments);
            return this;
        }

        /**
         * Adds {@link FileUpload} attachments to the response.
         *
         * @param attachments Files to attach.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder addAttachments(FileUpload... attachments) {
            Collections.addAll(this.attachments, attachments);
            return this;
        }

        /**
         * Sets {@link FileUpload} attachments for the response, replacing any existing ones.
         *
         * @param attachments Files to attach.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setAttachments(Collection<FileUpload> attachments) {
            this.attachments.clear();
            return attachments != null ? this.addAttachments(attachments) : this;
        }

        /**
         * Sets {@link FileUpload} attachments for the response, replacing any existing ones.
         *
         * @param attachments Files to attach.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setAttachments(FileUpload... attachments) {
            this.attachments.clear();
            return attachments != null ? this.addAttachments(attachments) : this;
        }

        /**
         * Adds {@link MessageEmbed} embeds to the response.
         *
         * @param embeds Embeds to add.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder addEmbeds(Collection<MessageEmbed> embeds) {
            this.embeds.addAll(embeds);
            return this;
        }

        /**
         * Adds {@link MessageEmbed} embeds to the response.
         *
         * @param embeds Embeds to add.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder addEmbeds(MessageEmbed... embeds) {
            Collections.addAll(this.embeds, embeds);
            return this;
        }

        /**
         * Sets {@link MessageEmbed} embeds for the response, replacing any existing ones.
         *
         * @param embeds Embeds to set.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setEmbeds(Collection<MessageEmbed> embeds) {
            this.embeds.clear();
            return embeds != null ? this.addEmbeds(embeds) : this;
        }

        /**
         * Sets {@link MessageEmbed} embeds for the response, replacing any existing ones.
         *
         * @param embeds Embeds to set.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setEmbeds(MessageEmbed... embeds) {
            this.embeds.clear();
            return embeds != null ? this.addEmbeds(embeds) : this;
        }

        /**
         * Sets a post-processing action to run when the response is sent, returning a {@link RestAction}.
         *
         * @param consumer Action to run after sending the message.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setPostExecutionFlatMap(Function<? super Message, ? extends RestAction<Message>> consumer) {
            this.postExecutionFlatMap = consumer;
            return this;
        }

        /**
         * Sets a post-processing action to run when the response is sent, returning a {@link RestAction}.
         *
         * @param consumer Action to run after completing the interaction.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setPostExecutionHookFlatMap(Function<? super InteractionHook, ? extends RestAction<InteractionHook>> consumer) {
            this.postExecutionHookFlatMap = consumer;
            return this;
        }

        /**
         * Sets a post-processing action to run when the response is sent.
         *
         * @param consumer Action to run after sending the message.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setPostExecutionFromMessage(Consumer<? super Message> consumer) {
            this.postExecutionFromMessage = consumer;
            return this;
        }

        /**
         * Sets a post-processing action to run when the response.
         *
         * @param consumer Action to run after completing the interaction.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setPostExecutionFromHook(Consumer<? super InteractionHook> consumer) {
            this.postExecutionFromHook = consumer;
            return this;
        }

        /**
         * Sets a handler to deal with exceptions during command execution.
         *
         * @param consumer Exception handler.
         * @return This builder for chaining.
         */
        public CommandResponseBuilder setFailureHandler(Consumer<? super Throwable> consumer) {
            this.failureHandler = consumer;
            return this;
        }

        /**
         * Builds the {@link CommandResponse} instance with the current builder state.
         *
         * @return The constructed {@link CommandResponse}.
         */
        public CommandResponse build() {
            return new CommandResponse(
                    content,
                    invisible,
                    attachments.isEmpty() ? null : attachments,
                    embeds.isEmpty() ? null : embeds,
                    postExecutionFlatMap,
                    postExecutionHookFlatMap,
                    postExecutionFromMessage,
                    postExecutionFromHook,
                    failureHandler
            );
        }
    }

    /**
     * Creates a new {@link CommandResponseBuilder} instance for constructing a {@link CommandResponse}.
     *
     * @return The new builder.
     */
    public static CommandResponseBuilder builder() {
        return new CommandResponseBuilder();
    }

    /**
     * Creates a {@link CommandResponse} with just the text content.
     *
     * @param content The textual content of the response.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse text(String content) {
        return builder().setContent(content).build();
    }

    /**
     * Creates a {@link CommandResponse} with formatted text content.
     *
     * @param content The textual content of the response, with format specifiers.
     * @param args    Arguments to format the content with.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse text(@PrintFormat String content, Object... args) {
        return builder().setContent(content, args).build();
    }

    /**
     * Creates a {@link CommandResponse} with just the content, and makes it hidden.
     *
     * @param content The textual content of the response.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse invisible(String content) {
        return builder().setContent(content).setInvisible(true).build();
    }

    /**
     * Creates a {@link CommandResponse} with just a {@link FileUpload} attachment.
     *
     * @param attachment The attachment to send.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse attachment(FileUpload attachment) {
        return builder().addAttachments(attachment).build();
    }

    /**
     * Creates a {@link CommandResponse} with multiple {@link FileUpload} attachments.
     *
     * @param attachments The attachments to send.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse attachments(Collection<FileUpload> attachments) {
        return builder().addAttachments(attachments).build();
    }

    /**
     * Creates a {@link CommandResponse} with multiple {@link FileUpload} attachments.
     *
     * @param attachments The attachments to send.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse attachments(FileUpload... attachments) {
        return builder().addAttachments(attachments).build();
    }

    /**
     * Creates a {@link CommandResponse} with just a {@link MessageEmbed} embed.
     *
     * @param embed The embed to send.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse embed(MessageEmbed embed) {
        return builder().addEmbeds(embed).build();
    }

    /**
     * Creates a {@link CommandResponse} with multiple {@link MessageEmbed} embeds.
     *
     * @param embeds The embeds to send.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse embeds(Collection<MessageEmbed> embeds) {
        return builder().addEmbeds(embeds).build();
    }

    /**
     * Creates a {@link CommandResponse} with multiple {@link MessageEmbed} embeds.
     *
     * @param embeds The embeds to send.
     * @return The new {@link CommandResponse}.
     */
    public static CommandResponse embeds(MessageEmbed... embeds) {
        return builder().addEmbeds(embeds).build();
    }

    /**
     * An empty {@link CommandResponse} with no content.
     * Meant to signify that no response is needed.
     */
    public static final CommandResponse EMPTY = new CommandResponse(null, null, null, null, null, null, null, null, null);
}