package bobo.commands.voice.music;

import bobo.commands.voice.AbstractVoice;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.TrackRecord;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractMusic extends AbstractVoice {
    protected PlayerManager playerManager;
    protected GuildMusicManager musicManager;
    protected AudioPlayer player;
    protected TrackScheduler scheduler;
    protected BlockingQueue<TrackRecord> queue;
    protected TrackRecord currentTrack;

    /**
     * Creates a new music command.
     *
     * @param commandData The command data.
     */
    public AbstractMusic(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected void handleVoiceCommand() {
        this.playerManager = PlayerManager.getInstance();
        this.musicManager = playerManager.getMusicManager(event.getGuildChannel().getGuild());
        this.player = musicManager.player;
        this.scheduler = musicManager.scheduler;
        this.queue = scheduler.queue;
        this.currentTrack = scheduler.currentTrack;

        handleMusicCommand();
    }

    /**
     * Handles the music command.
     */
    protected abstract void handleMusicCommand();

    @Override
    protected List<Permission> getVoiceCommandPermissions() {
        return getMusicCommandPermissions();
    }

    /**
     * Gets the permissions required for the music command.
     *
     * @return The permissions required for the music command.
     */
    protected abstract List<Permission> getMusicCommandPermissions();
}
