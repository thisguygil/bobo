package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class LoopCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        final TrackScheduler scheduler = musicManager.scheduler;
        if (player.getPlayingTrack() == null) {
            event.reply("There is nothing currently playing").queue();
            return;
        }
        scheduler.looping = !scheduler.looping;
        event.reply(scheduler.looping ? "The player has been set to **looping**" : "The player has been set to **not looping**").queue();
    }

    @Override
    public String getName() {
        return "loop";
    }

    @Override
    public String getHelp() {
        return "`/loop`\n" +
                "Loop the currently playing track";
    }
}
