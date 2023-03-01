package bobo.command.commands.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class LoopCommand implements ICommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        if (audioPlayer.getPlayingTrack() == null) {
            event.reply("There is nothing currently playing").queue();
            return;
        }
        musicManager.scheduler.looping = !musicManager.scheduler.looping;
        if (musicManager.scheduler.looping) {
            event.reply("The player has been set to **looping**").queue();
        } else {
            event.reply("The player has been set to **not looping**").queue();
        }
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
