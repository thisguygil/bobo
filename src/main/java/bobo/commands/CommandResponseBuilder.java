package bobo.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public class CommandResponseBuilder {
    private String content;
    private Boolean invisible;
    private final Collection<FileUpload> attachments = new ArrayList<>();
    private final Collection<MessageEmbed> embeds = new ArrayList<>();
    private Consumer<? super Message> postExecutionAsMessage;
    private Consumer<? super InteractionHook> postExecutionAsHook;
    private Consumer<? super Throwable> failureHandler;

    public CommandResponseBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public CommandResponseBuilder setInvisible(Boolean invisible) {
        this.invisible = invisible;
        return this;
    }

    public CommandResponseBuilder addAttachments(Collection<FileUpload> attachments) {
        this.attachments.addAll(attachments);
        return this;
    }

    public CommandResponseBuilder addAttachments(FileUpload... attachments) {
        this.attachments.addAll(Arrays.asList(attachments));
        return this;
    }

    public CommandResponseBuilder addEmbeds(Collection<MessageEmbed> embeds) {
        this.embeds.addAll(embeds);
        return this;
    }

    public CommandResponseBuilder addEmbeds(MessageEmbed... embeds) {
        this.embeds.addAll(Arrays.asList(embeds));
        return this;
    }

    public CommandResponseBuilder setPostExecutionAsMessage(Consumer<? super Message> consumer) {
        this.postExecutionAsMessage = consumer;
        return this;
    }

    public CommandResponseBuilder setPostExecutionAsHook(Consumer<? super InteractionHook> consumer) {
        this.postExecutionAsHook = consumer;
        return this;
    }

    public CommandResponseBuilder setFailureHandler(Consumer<? super Throwable> consumer) {
        this.failureHandler = consumer;
        return this;
    }

    public CommandResponse build() {
        return new CommandResponse(content, invisible, attachments, embeds, postExecutionAsMessage, postExecutionAsHook, failureHandler);
    }
}
