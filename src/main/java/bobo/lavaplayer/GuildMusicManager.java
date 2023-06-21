package bobo.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains instances of AudioPlayer, TrackScheduler and AudioPlayerSendHandler, to manage them all in one place
 */
public class GuildMusicManager {

    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;
    private final Map<SlashCommandInteractionEvent, AudioEventListener> eventListenerMap;

    /**
     * Creates a player and a track scheduler.
     * @param manager Audio player manager to use for creating the player.
     */
    public GuildMusicManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.player);
        this.player.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.player);
        this.eventListenerMap = new HashMap<>();
    }

    /**
     * Adds an audio event listener to the player
     * @param event the command key
     * @param listener The event listener to add
     */
    public void addAudioEventListener(SlashCommandInteractionEvent event, AudioEventListener listener) {
        player.addListener(listener);
        eventListenerMap.put(event, listener);
    }

    /**
     * Removes an audio event listener from the player
     * @param event the command key
     */
    public void removeAudioEventListener(SlashCommandInteractionEvent event) {
        AudioEventListener eventListener = eventListenerMap.get(event);
        player.removeListener(eventListener);
        eventListenerMap.remove(event);
    }
}
