package bobo.commands.voice.music;

import bobo.commands.voice.JoinCommand;
import bobo.utils.TrackType;
import bobo.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// NOTE: This classifies as a music command only due to the fact that it must use lavaplayer to play tts.
// It should be a voice command, but it is not possible to play the tts without lavaplayer.
public class TTSCommand extends AbstractMusic {
    private static final Map<AudioTrack, String> trackMessageMap = new HashMap<>(); // For retrieving the message associated with a TTS file.

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
    protected void handleMusicCommand() {
        event.deferReply().queue();

        Guild guild = event.getGuildChannel().getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            if (!JoinCommand.join(event)) {
                return;
            }
        } else {
            AudioChannelUnion memberChannel = Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
            if (memberChannel == null) {
                event.getHook().editOriginal("You must be connected to a voice channel to use this command.").queue();
                return;
            } else if (memberChannel != audioManager.getConnectedChannel()) {
                if (!JoinCommand.join(event)) {
                    return;
                }
            }
        }

        String message = Objects.requireNonNull(event.getOption("message")).getAsString();
        message = message.replaceAll(" ", "%20");

        PlayerManager.getInstance().loadAndPlay(event, "ftts://" + message, TrackType.TTS);
    }

    /**
     * Gets the message associated with the given audio track.
     *
     * @param track The audio track.
     */
    public static String getTTSMessage(AudioTrack track) {
        return trackMessageMap.get(track);
    }

    /**
     * Adds a message to the map with the given audio track.
     *
     * @param track The audio track.
     */
    public static void addTTSMessage(AudioTrack track, String message) {
        trackMessageMap.put(track, message);
    }

    /**
     * Removes the message associated with the given audio track.
     *
     * @param track The audio track.
     */
    public static void removeTTSMessage(AudioTrack track) {
        trackMessageMap.remove(track);
    }
}