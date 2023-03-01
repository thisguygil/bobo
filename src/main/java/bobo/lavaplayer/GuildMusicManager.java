package bobo.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;

/**
 * This class contains instances of AudioPlayer, TrackScheduler and AudioPlayerSendHandler, to manage them all in one place
 */
public class GuildMusicManager {

    public final AudioPlayer audioPlayer;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;
    private static AudioEventListener eventListener;

    /**
     * Creates a player and a track scheduler.
     *
     * @param manager Audio player manager to use for creating the player.
     */
    public GuildMusicManager(AudioPlayerManager manager) {
        this.audioPlayer = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.audioPlayer);
        this.audioPlayer.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
    }

    /**
     * Sets an event listener (in this case, a listener for the start of an audio track)
     *
     * @param listener the event listener
     */
    public void setEventListener(AudioEventListener listener) {
        if (eventListener == null) {
            eventListener = listener;
            audioPlayer.addListener(eventListener);
        }
    }

    /**
     * Removes the event listener
     */
    public void removeEventListener() {
        audioPlayer.removeListener(eventListener);
        eventListener = null;
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }
}
