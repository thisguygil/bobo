package bobo.commands.voice.music;

import bobo.commands.ai.TTSCommand;
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
import java.nio.file.Paths;
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

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        AudioTrack currentAudioTrack = currentTrack.track();
        AudioTrackInfo info = currentAudioTrack.getInfo();
        String title = info.title;
        String uri = info.uri;
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                .setColor(Color.red);

        switch (currentTrack.trackType()) {
            case TRACK -> {
                embed.setTitle(title, uri)
                        .setImage("attachment://thumbnail.jpg")
                        .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(uri)), "jpg", outputStream);
                } catch (Exception e) {
                    embed.setImage("https://img.youtube.com/vi/" + YouTubeUtil.getYouTubeID(uri) + "/hqdefault.jpg");
                    hook.editOriginalEmbeds(embed.build()).queue();
                    e.printStackTrace();
                    return;
                }
                hook.editOriginalAttachments(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
            }
            case FILE -> {
                embed.setTitle(title, uri)
                        .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");
                hook.editOriginalEmbeds(embed.build()).queue();
            }
            case TTS -> {
                embed.setTitle("TTS Message")
                        .setDescription(TTSCommand.getTTSMessage(Paths.get(uri).getFileName().toString()));
                hook.editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @Override
    public String getName() {
        return "now-playing";
    }
}