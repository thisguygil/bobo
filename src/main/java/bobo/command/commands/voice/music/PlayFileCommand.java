package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.command.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.Objects;

public class PlayFileCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        JoinCommand.join(event);
        event.deferReply().queue();

        Message.Attachment attachment = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
        if (isAudioFile(attachment.getFileName())) {
            String url = attachment.getUrl();
            PlayerManager.getInstance().loadAndPlay(event, url);
        } else {
            event.getHook().editOriginal("Please attach a valid audio file.").queue();
        }
    }

    /**
     * Checks whether given file name is a valid audio file name
     *
     * @param fileName the file name
     * @return true if the given file is a valid audio file name, false otherwise
     */
    private boolean isAudioFile(String fileName) {
        String[] audioExtensions = {".mp3", ".mp4", ".wav", ".ogg", ".flac", ".m4a", ".aac"};
        String fileExtension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        for (String audioExtension : audioExtensions) {
            if (fileExtension.equals(audioExtension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "play-file";
    }

    @Override
    public String getHelp() {
        return """
                `/play-file`
                Joins the voice channel and plays attached audio/video file
                Usage: `/playfile <file (as attachment)>`""";
    }
}
