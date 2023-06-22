package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class NowPlayingCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final AudioTrack track = player.getPlayingTrack();

        if (track == null) {
            hook.editOriginal("There is nothing currently playing").queue();
            return;
        }

        final AudioTrackInfo info = track.getInfo();
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                .setTitle(info.title, info.uri)
                .setImage("attachment://thumbnail.jpg")
                .setColor(Color.red)
                .setFooter(TimeFormat.formatTime(track.getDuration() - track.getPosition()) + " left");

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
