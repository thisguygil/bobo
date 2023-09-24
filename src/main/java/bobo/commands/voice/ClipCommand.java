package bobo.commands.voice;

import bobo.Bobo;
import bobo.Config;
import bobo.lavaplayer.AudioReceiveListener;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ClipCommand extends AbstractVoice {
    /**
     * Creates a new clip command.
     */
    public ClipCommand() {
        super(Commands.slash("clip", "Clips the most recent 30 seconds of the voice channel.")
                .addOption(OptionType.STRING, "name", "The name of the clip. No input defaults to current date/time.", false)
        );
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

        OptionMapping nameOption = event.getOption("name");
        String name = nameOption == null ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) : nameOption.getAsString();
        File file = ((AudioReceiveListener) receiveHandler).createFile(30, name);
        if (file != null) {
            TextChannel channel = Bobo.getJDA().getTextChannelById(Long.parseLong(Config.get("CLIPS_CHANNEL_ID")));
            assert channel != null;
            channel.sendFiles(FileUpload.fromData(file)).queue();
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