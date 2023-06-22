package bobo.command.commands.voice.music;

import bobo.command.ICommand;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import javax.annotation.Nonnull;

public class PauseCommand implements ICommand {
    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        InteractionHook hook = event.getHook();
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuildChannel().getGuild());
        final AudioPlayer player = musicManager.player;

        if (player.getPlayingTrack() == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        if (!player.isPaused()) {
            player.setPaused(true);
            hook.editOriginal("Paused.").queue();
        } else {
            hook.editOriginal("The player is already paused. Use `/resume` to resume.").queue();
        }
    }

    @Override
    public String getName() {
        return "pause";
    }
}
