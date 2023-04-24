package bobo.command.commands.music;

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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

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
        MessageCreateBuilder message = new MessageCreateBuilder()
                .addContent("**Current Queue:** Page 1/" + (int) Math.ceil((double) trackList.size() / 10) + "\n");
        for (int i = 0; i < trackList.size(); i++) {
            track = trackList.get(i);
            info = track.getInfo();
            message.addContent(String.valueOf(i + 1))
                    .addContent(": ")
                    .addContent(info.title)
                    .addContent(" by **")
                    .addContent(info.author)
                    .addContent("** [")
                    .addContent(i == 0 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) : TimeFormat.formatTime(track.getDuration()))
                    .addContent(i == 0 ? (scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n") : "]\n");
            count++;
            if (count == 10) {
                pages.add(new InteractPage(message.getContent()));
                message = new MessageCreateBuilder()
                        .addContent("**Current Queue:** Page " + ((int) Math.ceil((double) (i + 1) / 10) + 1) + "/" + (int) Math.ceil((double) trackList.size() / 10) + "\n");
                count = 0;
            }
        }
        if (!message.isEmpty()) {
            pages.add(new InteractPage(message.getContent()));
        }

        if (pages.size() == 1) {
            event.reply((String) pages.get(0).getContent()).queue();
        } else {
            event.getMessageChannel().sendMessage((String) pages.get(0).getContent()).queue(success -> Pages.paginate(success, pages, true));
            event.reply("Queue sent").queue();
        }


    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getHelp() {
        return "`/queue`\n" +
                "Shows the currently queued tracks";
    }
}
