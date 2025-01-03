package bobo.lavaplayer;

import bobo.commands.voice.music.NowPlayingCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import net.dv8tion.jda.api.entities.Guild;

import javax.annotation.Nonnull;

/**
 * This class contains instances of AudioPlayer, TrackScheduler and AudioPlayerSendHandler, to manage them all in one place
 */
public class GuildMusicManager {
    public final Guild guild;
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;

    /**
     * Creates a new GuildMusicManager, which contains instances of AudioPlayer, TrackScheduler and AudioPlayerSendHandler.
     * It also contains a listener for the AudioPlayer, which sends a message with info about the track when it starts.
     *
     * @param manager Audio player manager to use for creating the player.
     * @param guild The guild this manager is for.
     */
    public GuildMusicManager(@Nonnull AudioPlayerManager manager, @Nonnull Guild guild) {
        this.guild = guild;
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.player, guild);
        this.player.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.player);

        // Listener for the AudioPlayer, which sends a message with info about the track when it starts
        this.player.addListener(event -> {
            if (event instanceof TrackStartEvent startEvent) {
                TrackRecord record = this.scheduler.currentTrack;
                if (record.track().equals(startEvent.track)) {
                    record.channel().sendMessageEmbeds(NowPlayingCommand.createEmbed(record, this)).queue();
                }
            }
        });
    }
}