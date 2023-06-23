package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Objects;

import static net.dv8tion.jda.api.interactions.commands.OptionType.ATTACHMENT;

public class PlayFileCommand extends AbstractMusic {
    /**
     * Creates a new play-file command.
     */
    public PlayFileCommand() {
        super(Commands.slash("play-file", "Joins the voice channel and plays audio from attached audio/video file.")
                .addOption(ATTACHMENT, "file", "Audio file to play", true));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();
        JoinCommand.join(event);

        Attachment attachment = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
        if (!isAudioFile(attachment.getFileName())) {
            hook.editOriginal("Please attach a valid audio file.").queue();
            return;
        }

        playerManager.loadAndPlay(event, attachment.getUrl());
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
}
