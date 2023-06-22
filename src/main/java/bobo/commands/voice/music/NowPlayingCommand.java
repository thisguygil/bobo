package bobo.commands.voice.music;

import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class NowPlayingCommand extends AbstractMusic {
    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        final AudioTrackInfo info = currentTrack.getInfo();
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                .setTitle(info.title, info.uri)
                .setImage("attachment://thumbnail.jpg")
                .setColor(Color.red)
                .setFooter(TimeFormat.formatTime(currentTrack.getDuration() - currentTrack.getPosition()) + " left");

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
