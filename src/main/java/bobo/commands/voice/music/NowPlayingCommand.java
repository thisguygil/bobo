package bobo.commands.voice.music;

import bobo.utils.Spotify;
import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import se.michaelthelin.spotify.SpotifyApi;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

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
                .setAuthor(musicManager.scheduler.looping == LoopCommand.looping.TRACK ? "Now Looping" : "Now Playing")
                .setColor(Color.red);

        switch (currentTrack.trackType()) {
            case TRACK -> {
                embed.setTitle(title, uri)
                        .addField("Author", info.author, true)
                        .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");

                // Get the YouTube thumbnail or Spotify album cover
                try {
                    String spotifyRegex = "^(https?://)?open.spotify.com/.*";
                    if (YouTubeUtil.isYouTubeUrl(uri)) {
                        // Get the thumbnail
                        BufferedImage image = YouTubeUtil.getThumbnailImage(uri);
                        if (image == null) {
                            hook.editOriginalEmbeds(embed.build()).queue();
                            return;
                        }
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", outputStream);
                        embed.setThumbnail("attachment://thumbnail.jpg");
                        hook.editOriginalAttachments(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg"))
                                .setEmbeds(embed.build()).queue();
                    } else if (spotifyRegex.matches(uri)) {
                        // Get the album cover
                        SpotifyApi spotifyApi = Spotify.getSpotifyApi();

                        String id = uri.split("/")[uri.split("/").length - 1];
                        String imageUrl = spotifyApi.getTrack(id).build().execute().getAlbum().getImages()[0].getUrl();
                        embed.setThumbnail(imageUrl);
                    } else {
                        hook.editOriginalEmbeds(embed.build()).queue();
                    }
                } catch (Exception e) {
                    hook.editOriginalEmbeds(embed.build()).queue();
                    e.printStackTrace();
                }
            }
            case FILE -> {
                embed.setTitle(title, uri)
                        .addField("Author", info.author, true)
                        .setFooter(TimeFormat.formatTime(currentAudioTrack.getDuration() - currentAudioTrack.getPosition()) + " left");
                hook.editOriginalEmbeds(embed.build()).queue();
            }
            case TTS -> {
                embed.setTitle("TTS Message")
                        .setDescription(TTSCommand.getTTSMessage(currentAudioTrack));
                hook.editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @Override
    public String getName() {
        return "now-playing";
    }

    @Override
    public String getHelp() {
        return """
                Shows the currently playing track.
                Usage: `/now-playing`""";
    }
}