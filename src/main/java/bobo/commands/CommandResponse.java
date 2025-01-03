package bobo.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class CommandResponse {
    private final String content;
    private final Boolean invisible;
    private final Collection<? extends FileUpload>  attachments;
    private final Collection<? extends MessageEmbed> embeds;

    // Post-execution actions. If one of these is non-null, the other should be null.
    private final Consumer<? super Message> postExecutionAsMessage; // Usually this one will be used, as it's the one for message commands, and for slash commands that have their replies deferred (which we do for most of them).
    private final Consumer<? super InteractionHook> postExecutionAsHook;
    private final Consumer<? super Throwable> failureHandler;

    /**
     * Creates a new command response.
     *
     * @param content The content.
     * @param invisible Whether the reply should be ephemeral or the bot should show typing before replying.
     * @param attachments The attachments.
     * @param embeds The embeds.
     * @param postExecutionAsMessage The post-execution action as a message.
     * @param postExecutionAsHook The post-execution action as a hook.
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

    public CommandResponse(String content) {
        this(content, null, null, null, null, null, null);
    }

    public CommandResponse(String content, Boolean invisible) {
        this(content, invisible, null, null, null, null, null);
    }

    public CommandResponse(Collection<? extends MessageEmbed> embeds) { // We choose this constructor over attachments as embeds are more common, and we can't use both because they have the same parameter type (collection).
        this(null, null, null, embeds, null, null, null);
    }

    public CommandResponse(MessageEmbed embed) { // So that we don't have to wrap a single embed in a list.
        this(null, null, null, List.of(embed), null, null, null);
    }

    // Sadly we can't use a vararg here, as it would conflict when the first arguments are null. It's not a big deal though, we can just wrap multiple in a List.of() or similar.

    public CommandResponse() {
        this("Command Error.");
    }

    /**
     * Converts the command response to a message create data.
     *
     * @return The message create data.
     */
    public MessageCreateData asMessageCreateData() {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (content != null) {
            builder.setContent(content);
        }
        if (attachments != null) {
            builder.addFiles(attachments);
        }
        if (embeds != null) {
            builder.setEmbeds(embeds);
        }
        return builder.mentionRepliedUser(false)
                .build();
    }

    /**
     * Converts the command response to a message edit data.
     *
     * @return The message edit data.
     */
    public MessageEditData asMessageEditData() {
        MessageEditBuilder builder = new MessageEditBuilder();
        if (content != null) {
            builder.setContent(content);
        }
        if (attachments != null) {
            builder.setFiles(attachments);
        }
        if (embeds != null) {
            builder.setEmbeds(embeds);
        }
        return builder.build();
    }

    public String getContent() {
        return content;
    }

    public Boolean isInvisible() {
        return invisible;
    }

    public Collection<? extends FileUpload> getAttachments() {
        return attachments;
    }

    public Collection<? extends MessageEmbed> getEmbeds() {
        return embeds;
    }

    public Consumer<? super Message> getPostExecutionAsMessage() {
        return postExecutionAsMessage;
    }

    public Consumer<? super InteractionHook> getPostExecutionAsHook() {
        return postExecutionAsHook;
    }

    public Consumer<? super Throwable> getFailureHandler() {
        return failureHandler;
    }
}