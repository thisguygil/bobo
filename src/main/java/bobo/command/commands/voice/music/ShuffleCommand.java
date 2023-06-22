package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;

public class ShuffleCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final TrackScheduler scheduler = musicManager.scheduler;
        final BlockingQueue<AudioTrack> newQueue = scheduler.queue;

        if (player.getPlayingTrack() == null) {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        ArrayList<AudioTrack> list = new ArrayList<>();
        newQueue.drainTo(list);
        Collections.shuffle(list);
        newQueue.clear();
        newQueue.addAll(list);

        scheduler.queue = newQueue;
        hook.editOriginal("Shuffled.").queue();
    }

    @Override
    public String getName() {
        return "shuffle";
    }
}
