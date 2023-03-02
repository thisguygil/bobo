package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

public class RemoveCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final TrackScheduler scheduler = musicManager.scheduler;
        final BlockingQueue<AudioTrack> queue = scheduler.queue;
        if (player.getPlayingTrack() == null) {
            event.reply("The queue is currently empty").queue();
            return;
        }
        int position = event.getOption("position").getAsInt();
        if (position < 1 || position > queue.size() + 1) {
            event.reply("Please enter an integer corresponding to a track's position in the queue").queue();
        } else {
            event.reply("Removed track at position **" + position + "**").queue();
            if (position == 1) {
                scheduler.nextTrack();
            } else {
                int count = 1;
                Iterator<AudioTrack> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    if (count == position) {
                        iterator.remove();
                    }
                    count++;
                    iterator.next();
                }
                if (count == position) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getHelp() {
        return "`/remove`\n" +
                "Removes track at given position in the queue\n" +
                "Usage: `/remove <position in queue to remove>`";
    }
}
