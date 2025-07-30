package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.utils.StringUtils;
import bobo.lavaplayer.TrackType;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.*;

// NOTE: This classifies as a music command only because it must use lavaplayer to play tts.
// It should be a voice command, but it is not possible to play the tts without lavaplayer.
public class TTSCommand extends AMusicCommand {
    private static final Map<Guild, Map<AudioTrack, String>> trackMessageMap = new HashMap<>();
    private static final Map<Guild, Map<AudioTrack, String>> previousTrackMessage = new HashMap<>();

    /**
     * Creates a new TTS command.
     */
    public TTSCommand() {
        super(Commands.slash("tts", "Generates a message for the voice channel.")
                .addOption(OptionType.STRING, "message", "Message to be said.", true));
    }

    @Override
    public String getName() {
        return "tts";
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (!ensureConnected(getMember())) {
            return CommandResponse.text("You must be connected to a voice channel to use this command.");
        }

        String message;
        try {
            message = getMultiwordOptionValue("message", 0);
        } catch (Exception e) {
            return CommandResponse.text("Please provide a message to say.");
        }
        message = StringUtils.encodeUrl(message);

        return playerManager.loadAndPlay((MessageChannel) getChannel(), getMember(), "ftts://" + message, TrackType.TTS);
    }

    /**
     * Adds the guild to the track message map.
     *
     * @param guild The guild.
     */
    public static void addGuild(Guild guild) {
        if (trackMessageMap.containsKey(guild)) {
            return;
        }

        trackMessageMap.put(guild, new HashMap<>());
        previousTrackMessage.put(guild, new HashMap<>());
    }

    /**
     * Removes the guild from the track message map.
     *
     * @param guild The guild.
     */
    public static void removeGuild(Guild guild) {
        trackMessageMap.remove(guild);
        previousTrackMessage.remove(guild);
    }

    /**
     * Gets the message associated with the given guild and audio track.
     *
     * @param guild The guild.
     * @param track The audio track.
     */
    public static String getTTSMessage(Guild guild, AudioTrack track) {
        return trackMessageMap.get(guild).get(track);
    }

    /**
     * Gets the message associated with the given guild and audio track.
     *
     * @param guild The guild.
     * @param track The audio track.
     */
    public static String getPreviousTTSMessage(Guild guild, AudioTrack track) {
        return previousTrackMessage.get(guild).get(track);
    }

    /**
     * Adds a message to the map with the given audio track.
     *
     * @param guild The guild.
     * @param track The audio track.
     * @param message The message.
     */
    public static void addTTSMessage(Guild guild, AudioTrack track, String message) {
        trackMessageMap.get(guild).put(track, message);
    }

    /**
     * Moves the message associated with the given audio track to the previous track map.
     * Removes the message from the current track map.
     *
     * @param guild The guild.
     * @param track The audio track.
     */
    public static void nextTTSMessage(Guild guild, AudioTrack track) {
        Map<AudioTrack, String> prevMap = previousTrackMessage.get(guild);
        prevMap.clear();
        prevMap.put(track, getTTSMessage(guild, track));
        trackMessageMap.get(guild).remove(track);
    }

    @Override
    public String getHelp() {
        return """
                Generates a message for the voice channel.
                Usage: `/tts <message>`""";
    }

    @Override
    public Boolean isHidden() {
        return false;
    }
}