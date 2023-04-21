package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class PlayFileCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        Message.Attachment attachment = event.getOption("file").getAsAttachment();
        if (isAudioFile(attachment.getFileName())) {
            String url = attachment.getUrl();
            PlayerManager.getInstance().loadAndPlay(event, url);
        } else {
            event.reply("Please attach a valid audio file.").queue();
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
        return "playfile";
    }

    @Override
    public String getHelp() {
        return "`/playfile`\n" +
                "Joins the voice channel and plays attached audio/video file\n" +
                "Usage: `/playfile <file (as attachment)>`";
    }
}
