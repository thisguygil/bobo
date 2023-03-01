package bobo.command.commands.music;

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
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NowPlayingCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        final AudioTrack track = audioPlayer.getPlayingTrack();
        if (track == null) {
            event.reply("There is no track playing currently").queue();
            return;
        }
        final AudioTrackInfo info = track.getInfo();

        // Creates embedded message with track info
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                .setTitle(info.title, info.uri)
                .setImage("attachment://thumbnail.jpg")
                .setColor(Color.red)
                .setFooter(TimeFormat.formatTime(track.getDuration() - track.getPosition()) + " left");

        // Sets image in embed to proper aspect ratio
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(YouTubeUtil.getThumbnailImage(info.uri), "jpg", outputStream);
        } catch (IOException e) {
            embed.setImage("https://img.youtube.com/vi/" + YouTubeUtil.getYouTubeID(info.uri) + "/hqdefault.jpg");
            event.replyEmbeds(embed.build()).queue();
            return;
        }
        event.replyFiles(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
    }

    @Override
    public String getName() {
        return "nowplaying";
    }

    @Override
    public String getHelp() {
        return "`/nowplaying`\n" +
                "Shows the currently playing track";
    }
}
