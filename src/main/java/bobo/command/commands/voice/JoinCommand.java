package bobo.command.commands.voice;

import bobo.command.Command;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.TimeFormat;
import bobo.utils.YouTubeUtil;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class JoinCommand implements Command {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        if (Objects.requireNonNull(event.getGuild()).getAudioManager().isConnected()) {
            event.reply("I must not be connected to a voice channel to use this command.").queue();
            return;
        }
        join(event);
        event.reply("Joined.").queue();
    }

    public static void join(@Nonnull SlashCommandInteractionEvent event) {
        // Check if joining is valid
        if (Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel() == null) {
            event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
            return;
        }

        // Actually joins
        Objects.requireNonNull(event.getGuild()).getAudioManager().openAudioConnection(Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel());
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());

        // Set event listener for music queue
        musicManager.setEventListener(audioEvent -> {
            if (audioEvent instanceof TrackStartEvent) {
                AudioTrackInfo info = ((TrackStartEvent) audioEvent).track.getInfo();

                // Creates embedded message with track info
                EmbedBuilder embed = new EmbedBuilder()
                        .setAuthor(musicManager.scheduler.looping ? "Now Looping" : "Now Playing")
                        .setTitle(info.title, info.uri)
                        .setImage("attachment://thumbnail.jpg")
                        .setColor(Color.red)
                        .setFooter(TimeFormat.formatTime(((TrackStartEvent) audioEvent).track.getDuration()));

                // Sets image in embed to proper aspect ratio
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    ImageIO.write(Objects.requireNonNull(YouTubeUtil.getThumbnailImage(info.uri)), "jpg", outputStream);
                    event.getMessageChannel().sendFiles(FileUpload.fromData(outputStream.toByteArray(), "thumbnail.jpg")).setEmbeds(embed.build()).queue();
                } catch (Exception e) {
                    event.getMessageChannel().sendMessageEmbeds(embed.build()).queue();
                }
            }
        });
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getHelp() {
        return "`/join`\n" +
                "Joins the voice channel";
    }
}
