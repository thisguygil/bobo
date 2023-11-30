package bobo.commands.voice.music;

import bobo.commands.voice.AbstractVoice;
import bobo.lavaplayer.GuildMusicManager;
import bobo.lavaplayer.PlayerManager;
import bobo.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.concurrent.BlockingQueue;

public abstract class AbstractMusic extends AbstractVoice {
    protected PlayerManager playerManager;
    protected GuildMusicManager musicManager;
    protected AudioPlayer player;
    protected TrackScheduler scheduler;
    protected BlockingQueue<TrackScheduler.TrackChannelTypeRecord> queue;
    protected TrackScheduler.TrackChannelTypeRecord currentTrack;

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
}
