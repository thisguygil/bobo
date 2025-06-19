package bobo.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class CommandResponse {
    private final String content;
    private final Boolean invisible;
    private final Collection<? extends FileUpload>  attachments;
    private final Collection<? extends MessageEmbed> embeds;

    // Post-execution actions. If one of these two is non-null, the other should be null.
    private final Consumer<? super Message> postExecutionAsMessage; // Usually this one will be used, as it's the one for message commands, and for slash commands that have their replies deferred (which we do for most of them).
    private final Consumer<? super InteractionHook> postExecutionAsHook;

    // Handler for exceptions during command execution.
    private final Consumer<? super Throwable> failureHandler;

    /**
     * Constructs a CommandResponse with all parameters.
     *
     * @param content                The textual content of the response.
     * @param invisible              Whether the reply should be ephemeral or the bot should show typing before replying.
     * @param attachments            Files to be attached to the message.
     * @param embeds                 Embeds to be sent with the message.
     * @param postExecutionAsMessage Post-processing action after a message is sent.
     * @param postExecutionAsHook    Post-processing action after a deferred interaction is completed.
     * @param failureHandler         Handler for exceptions during command execution.
     */
    public CommandResponse(String content, Boolean invisible, Collection<? extends FileUpload>  attachments, Collection<? extends MessageEmbed> embeds, Consumer<? super Message> postExecutionAsMessage, Consumer<? super InteractionHook> postExecutionAsHook, Consumer<? super Throwable> failureHandler) {
        this.content = content;
        this.invisible = invisible;
        this.attachments = attachments;
        this.embeds = embeds;
        this.postExecutionAsMessage = postExecutionAsMessage;
        this.postExecutionAsHook = postExecutionAsHook;
        this.failureHandler = failureHandler;
    }

    /**
     * Constructs a CommandResponse with only text content.
     *
     * @param content The textual content of the response.
     */
    public CommandResponse(String content) {
        this(content, null, null, null, null, null, null);
    }

    /**
     * Constructs a CommandResponse with content and visibility.
     *
     * @param content   The textual content of the response.
     * @param invisible Whether the reply should be invisible/ephemeral.
     */
    public CommandResponse(String content, Boolean invisible) {
        this(content, invisible, null, null, null, null, null);
    }

    /**
     * Constructs a CommandResponse with a collection of embeds.
     * We choose this constructor over attachments as embeds are more common, and we can't use both because they have the same parameter type (collection).
     * Sadly, we can't use a vararg here, as it would conflict when the first arguments are null.
     *
     * @param embeds Embeds to be sent with the response.
     */
    public CommandResponse(Collection<? extends MessageEmbed> embeds) {
        this(null, null, null, embeds, null, null, null);
    }

    /**
     * Constructs a CommandResponse with a single embed.
     *
     * @param embed The embed to send.
     */
    public CommandResponse(MessageEmbed embed) { // So that we don't have to wrap a single embed in a list.
        this(null, null, null, List.of(embed), null, null, null);
    }

    /**
     * Constructs a default error CommandResponse.
     */
    public CommandResponse() {
        this("Command Error.");
    }

    /**
     * Converts the CommandResponse to {@link MessageCreateData} for sending as a new message.
     *
     * @return The MessageCreateData object.
     */
    public MessageCreateData asMessageCreateData() {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (content != null) builder.setContent(content);
        if (attachments != null) builder.setFiles(attachments);
        if (embeds != null) builder.setEmbeds(embeds);
        return builder.build();
    }

    /**
     * Converts the CommandResponse to {@link MessageEditData} for editing an existing message.
     *
     * @return The MessageEditData object.
     */
    public MessageEditData asMessageEditData() {
        MessageEditBuilder builder = new MessageEditBuilder();
        if (content != null) builder.setContent(content);
        if (attachments != null) builder.setFiles(attachments);
        if (embeds != null) builder.setEmbeds(embeds);
        return builder.build();
    }

    /**
     * Gets the content of the response.
     *
     * @return The textual content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Indicates whether the response should be invisible/ephemeral.
     *
     * @return True if invisible, otherwise false.
     */
    public Boolean isInvisible() {
        return invisible;
    }

    /**
     * Gets the attachments to be sent.
     *
     * @return Collection of attachments.
     */
    public Collection<? extends FileUpload> getAttachments() {
        return attachments;
    }

    /**
     * Gets the embeds to be sent.
     *
     * @return Collection of embeds.
     */
    public Collection<? extends MessageEmbed> getEmbeds() {
        return embeds;
    }

    /**
     * Gets the post-execution action as a message.
     *
     * @return Consumer for message post-processing.
     */
    public Consumer<? super Message> getPostExecutionAsMessage() {
        return postExecutionAsMessage;
    }

    /**
     * Gets the post-execution action as a hook.
     *
     * @return Consumer for interaction hook post-processing.
     */
    public Consumer<? super InteractionHook> getPostExecutionAsHook() {
        return postExecutionAsHook;
    }

    /**
     * Gets the handler for failures/exceptions.
     *
     * @return Consumer for handling exceptions.
     */
    public Consumer<? super Throwable> getFailureHandler() {
        return failureHandler;
    }

    /**
     * Creates a new {@link CommandResponseBuilder} instance for constructing a CommandResponse.
     *
     * @return A new Builder.
     */
    public static CommandResponseBuilder builder() {
        return new CommandResponseBuilder();
    }

    /**
     * Builder class for constructing {@link CommandResponse} instances.
     */
    public static class CommandResponseBuilder {
        private String content;
        private Boolean invisible;
        private final Collection<FileUpload> attachments = new ArrayList<>();
        private final Collection<MessageEmbed> embeds = new ArrayList<>();
        private Consumer<? super Message> postExecutionAsMessage;
        private Consumer<? super InteractionHook> postExecutionAsHook;
        private Consumer<? super Throwable> failureHandler;

        /**
         * Sets the textual content for the response.
         *
         * @param content The response content.
         * @return This builder.
         */
        public CommandResponseBuilder setContent(String content) {
            this.content = content;
            return this;
        }

        /**
         * Sets the visibility (ephemeral) of the response.
         *
         * @param invisible Whether the reply should be invisible/ephemeral.
         * @return This builder.
         */
        public CommandResponseBuilder setInvisible(Boolean invisible) {
            this.invisible = invisible;
            return this;
        }

        /**
         * Adds attachments to the response.
         *
         * @param attachments Files to attach.
         * @return This builder.
         */
        public CommandResponseBuilder addAttachments(Collection<FileUpload> attachments) {
            this.attachments.addAll(attachments);
            return this;
        }

        /**
         * Adds attachments to the response.
         *
         * @param attachments Files to attach.
         * @return This builder.
         */
        public CommandResponseBuilder addAttachments(FileUpload... attachments) {
            this.attachments.addAll(Arrays.asList(attachments));
            return this;
        }

        /**
         * Adds embeds to the response.
         *
         * @param embeds Embeds to add.
         * @return This builder.
         */
        public CommandResponseBuilder addEmbeds(Collection<MessageEmbed> embeds) {
            this.embeds.addAll(embeds);
            return this;
        }

        /**
         * Adds embeds to the response.
         *
         * @param embeds Embeds to add.
         * @return This builder.
         */
        public CommandResponseBuilder addEmbeds(MessageEmbed... embeds) {
            this.embeds.addAll(Arrays.asList(embeds));
            return this;
        }

        /**
         * Sets a post-processing action to run when the response is sent as a message.
         *
         * @param consumer Action to run after sending a message.
         * @return This builder.
         */
        public CommandResponseBuilder setPostExecutionAsMessage(Consumer<? super Message> consumer) {
            this.postExecutionAsMessage = consumer;
            return this;
        }

        /**
         * Sets a post-processing action to run when the response is sent as an interaction hook.
         *
         * @param consumer Action to run after completing the interaction.
         * @return This builder.
         */
        public CommandResponseBuilder setPostExecutionAsHook(Consumer<? super InteractionHook> consumer) {
            this.postExecutionAsHook = consumer;
            return this;
        }

        /**
         * Sets a handler to deal with exceptions during command execution.
         *
         * @param consumer Exception handler.
         * @return This builder.
         */
        public CommandResponseBuilder setFailureHandler(Consumer<? super Throwable> consumer) {
            this.failureHandler = consumer;
            return this;
        }

        /**
         * Builds the {@link CommandResponse} instance with the current builder state.
         *
         * @return The constructed CommandResponse.
         */
        public CommandResponse build() {
            return new CommandResponse(content, invisible, attachments, embeds, postExecutionAsMessage, postExecutionAsHook, failureHandler);
        }
    }

}