package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;

public class ClearCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final TrackScheduler scheduler = musicManager.scheduler;

        if (player.getPlayingTrack() == null) {
            hook.editOriginal("There is nothing currently playing").queue();
            return;
        }

        scheduler.queue.clear();
        scheduler.looping = false;
        player.stopTrack();
        player.setPaused(false);
        hook.editOriginal("Queue cleared").queue();
    }

    @Override
    public String getName() {
        return "clear";
    }
}
