package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Iterator;

public class RemoveCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        if (audioPlayer.getPlayingTrack() == null) {
            event.reply("There is nothing currently playing").queue();
            return;
        }
        int position = event.getOption("position").getAsInt();
        if (position < 1 || position > musicManager.scheduler.queue.size() + 1) {
            event.reply("Please enter an integer corresponding to a track's position in the queue").queue();
        } else {
            event.reply("Removed track at position **" + position + "**").queue();
            if (position == 1) {
                musicManager.scheduler.nextTrack();
            } else {
                int count = 1;
                Iterator<AudioTrack> iterator = musicManager.scheduler.queue.iterator();
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
