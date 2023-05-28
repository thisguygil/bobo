package bobo.command.commands.voice.music;

import bobo.command.CommandInterface;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class PauseCommand implements CommandInterface {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        if (player.getPlayingTrack() == null) {
            event.reply("There is nothing currently playing").queue();
            return;
        }
        if (!player.isPaused()) {
            player.setPaused(true);
            event.reply("Paused").queue();
        } else {
            event.reply("The player is already paused. Use `/resume` to resume").queue();
        }
    }

    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public String getHelp() {
        return "`/pause`\n" +
                "Pauses the currently playing track";
    }
}
