package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import bobo.utils.TimeFormat;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class QueueCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final TrackScheduler scheduler = musicManager.scheduler;
        final BlockingQueue<AudioTrack> queue = scheduler.queue;
        final List<AudioTrack> trackList = new ArrayList<>(queue);
        final List<Page> pages = new ArrayList<>();
        AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        if (currentTrack != null) {
            trackList.add(0, currentTrack);
        } else {
            event.reply("The queue is currently empty").queue();
            return;
        }

        AudioTrack track;
        AudioTrackInfo info;
        int count = 0;
        int numPages = trackList.size();
        Member member = event.getMember();
        assert member != null;
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getAvatarUrl())
                .setTitle("Current Queue")
                .setFooter("Page 1/" + (int) Math.ceil((double) numPages / 10));

        for (int i = 0; i < numPages; i++) {
            track = trackList.get(i);
            info = track.getInfo();
            builder.addField((i + 1) + ":", "[" + info.title + "](" + info.uri + ") by **" + info.author + "** [" + (i == 0 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) : TimeFormat.formatTime(track.getDuration())) + (i == 0 ? (scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n") : "]\n"), false);
            count++;
            if (count == 10) {
                pages.add(InteractPage.of(builder.build()));
                builder = new EmbedBuilder()
                        .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getAvatarUrl())
                        .setTitle("Current Queue")
                        .setFooter("Page " + ((int) Math.ceil((double) (i + 1) / 10) + 1) + "/" + (int) Math.ceil((double) numPages / 10));
                count = 0;
            }
        }
        if (!builder.getFields().isEmpty()) {
            pages.add(InteractPage.of(builder.build()));
        }

        if (pages.size() == 1) {
            event.replyEmbeds((MessageEmbed) pages.get(0).getContent()).queue();
        } else {
            event.getMessageChannel().sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> Pages.paginate(success, pages, true));
            event.reply("Queue sent").setEphemeral(true).queue();
        }
    }

    @Override
    public String getName() {
        return "queue";
    }

}
