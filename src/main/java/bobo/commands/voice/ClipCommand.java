package bobo.commands.voice;

import bobo.lavaplayer.AudioReceiveListener;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.Objects;

public class ClipCommand extends AbstractVoice {
    /**
     * Creates a new clip command.
     */
    public ClipCommand() {
        super(Commands.slash("clip", "Clips the most recent 30 seconds of the voice channel."));
    }

    @Override
    public String getName() {
        return "clip";
    }

    @Override
    protected void handleVoiceCommand() {
        event.deferReply().queue();

        Guild guild = event.getGuildChannel().getGuild();
        AudioReceiveHandler receiveHandler = guild.getAudioManager().getReceivingHandler();

        if (receiveHandler == null) {
            hook.editOriginal("I am not connected to a voice channel.").queue();
            return;
        }
        if (Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isDeafened()) {
            hook.editOriginal("I can't be deafened when using this command.").queue();
            return;
        }

        File file = ((AudioReceiveListener) receiveHandler).createFile(30);
        if (file != null) {
            hook.editOriginalAttachments(FileUpload.fromData(file)).queue(success -> {
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file.getName());
                }
            });
        } else {
            hook.editOriginal("Clip creation failed.").queue();
        }
    }
}