package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SkipCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        final TrackScheduler scheduler = musicManager.scheduler;
        if (audioPlayer.getPlayingTrack() == null) {
            event.reply("There is nothing currently playing").queue();
        } else {
            event.reply(scheduler.looping ? "Skipped. Looping has been turned off" : "Skipped").queue();
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
