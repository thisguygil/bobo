package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ClearCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final TrackScheduler scheduler = musicManager.scheduler;
        if (player.getPlayingTrack() == null) {
            event.reply("There is nothing currently playing").queue();
            return;
        }
        scheduler.queue.clear();
        scheduler.looping = false;
        player.stopTrack();
        player.setPaused(false);
        event.reply("Queue cleared").queue();
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getHelp() {
        return "`/clear`\n" +
                "Clears queue and stops current track";
    }
}
