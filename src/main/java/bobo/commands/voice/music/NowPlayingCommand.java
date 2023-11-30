package bobo.commands.voice.music;

import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class NowPlayingCommand extends AbstractMusic {
    /**
     * Creates a new now-playing command.
     */
    public NowPlayingCommand() {
        super(Commands.slash("now-playing", "Shows the currently playing track."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        AudioTrack currentAudioTrack = currentTrack.track();

        if (currentAudioTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        final AudioTrackInfo info = currentAudioTrack.getInfo();
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                .setTitle(info.title, info.uri)
                .setImage("attachment://thumbnail.jpg")
                .setColor(Color.red)
                .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(info.uri)), "jpg", outputStream);
        } catch (Exception e) {
            embed.setImage("https://img.youtube.com/vi/" + YouTubeUtil.getYouTubeID(info.uri) + "/hqdefault.jpg");
            hook.editOriginalEmbeds(embed.build()).queue();
            e.printStackTrace();
            return;
        }
        hook.editOriginalAttachments(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
    }

    @Override
    public String getName() {
        return "now-playing";
    }
}
