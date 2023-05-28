package bobo.command.commands.voice.music;

import bobo.command.Command;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.annotation.Nonnull;

public class ResumeCommand implements Command {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;
        if (player.getPlayingTrack() == null) {
            event.reply("There is nothing currently paused").queue();
            return;
        }
        if (player.isPaused()) {
            player.setPaused(false);
            event.reply("Resumed").queue();
        } else {
            event.reply("The player is already playing. Use `/pause` to pause").queue();
        }
    }

    @Override
    public String getName() {
        return "resume";
    }

    @Override
    public String getHelp() {
        return "`/resume`\n" +
                "Resumes the currently paused track";
    }
}
