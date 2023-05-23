package bobo.command.commands.voice.music;

import bobo.command.CommandInterface;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class SkipCommand implements CommandInterface {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final TrackScheduler scheduler = musicManager.scheduler;
        if (player.getPlayingTrack() == null) {
            event.getHook().editOriginal("There is nothing currently playing").queue();
        } else {
            event.getHook().editOriginal(scheduler.looping ? "Skipped. Looping has been turned off." : "Skipped").queue();
            scheduler.looping = false;
            scheduler.nextTrack();
        }
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getHelp() {
        return "`/skip`\n" +
                "Skips the current track";
    }
}
