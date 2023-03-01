package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.TimeFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class QueueCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;
        AudioTrack currentTrack = musicManager.audioPlayer.getPlayingTrack();
        if (queue.isEmpty() && currentTrack == null) {
            event.reply("The queue is currently empty.").queue();
            return;
        }
        AudioTrackInfo info = currentTrack.getInfo();
        StringBuilder message = new StringBuilder("**Current Queue:**\n");
        message.append("1: `")
                .append(info.title)
                .append("` by `")
                .append(info.author)
                .append("` [")
                .append(TimeFormat.formatTime(currentTrack.getDuration() - currentTrack.getPosition()))
                .append(musicManager.scheduler.looping ? " left] (currently looping)\n" : " left] (currently playing)\n");
        final List<AudioTrack> trackList = new ArrayList<>(queue);
        int count = 2;
        for (AudioTrack track : trackList) {
            info = track.getInfo();
            message.append(count)
                    .append(": `")
                    .append(info.title)
                    .append("` by `")
                    .append(info.author)
                    .append("` [")
                    .append(TimeFormat.formatTime(track.getDuration()))
                    .append("]\n");
            count++;
        }
        event.reply(message.toString()).queue();
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
