package bobo.utils;

import bobo.lavaplayer.PlayerManager;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AudioReceiveListener implements AudioReceiveHandler {
    private static final Logger logger = LoggerFactory.getLogger(AudioReceiveListener.class);

    private final List<byte[]> clipBytes = new ArrayList<>();
    private final List<byte[]> listenerBytes = new ArrayList<>();
    private final Guild guild;
    private static final Map<Guild, Guild> listeners = new HashMap<>(); // Guild: Listener
    private static final Map<Guild, Integer> fileIndices = new HashMap<>();
    private final double volume;
    private static final int MAX_TIME = 30 * 1000 / 20;

    public AudioReceiveListener(Guild guild, double volume) {
        this.guild = guild;
        this.volume = volume;
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public void handleCombinedAudio(@Nonnull CombinedAudio combinedAudio) {
        if (clipBytes.size() > MAX_TIME) {
            clipBytes.removeFirst();
        }

        Guild guild = listeners.get(this.guild);
        if (guild != null && listenerBytes.size() > MAX_TIME) {
            handleListening(guild);
        } else if (listenerBytes.size() > MAX_TIME) {
            listenerBytes.removeFirst();
        }

        byte[] audio = combinedAudio.getAudioData(volume);
        clipBytes.add(audio);
        listenerBytes.add(audio);
    }

    /**
     * Creates a file containing the file, waveform, and duration of the clip.
     *
     * @param seconds the number of seconds to record
     * @param name    the name of the file
     * @return the file, or null if the clip could not be created
     */
    @Nullable
    public File createClip(int seconds, String name) {
        try {
            byte[] decodedData = getDecodedData(clipBytes, seconds);
            return createClipFile(decodedData, name);
        } catch (OutOfMemoryError e) {
            logger.error("Failed to create clip");
        }

        return null;
    }

    /**
     * Handles the listening audio data.
     *
     * @param guild the guild to handle the listening for
     */
    private void handleListening(Guild guild) {
        byte[] decodedData = getDecodedData(listenerBytes, 30);
        listenerBytes.clear();

        int currentIndex = fileIndices.getOrDefault(guild, 0) + 1;
        fileIndices.put(guild, currentIndex);
        String name = String.format("listener-%s-%d", guild.getId(), currentIndex);

        File file = createClipFile(decodedData, name);
        PlayerManager.getInstance().listen(guild, name + ".wav");

        new Timer().schedule( // Delete the file after 35 seconds (30 seconds of audio + 5 seconds of buffer)
                new TimerTask() {
                    @Override
                    public void run() {
                        if (!file.delete()) {
                            logger.error("Failed to delete listener file: {}", file.getName());
                        }
                    }
                },
                35 * 1000
        );
    }

    private byte[] getDecodedData(List<byte[]> bytes, int seconds) {
        int packetCount = (seconds * 1000) / 20;
        int size = 0;
        List<byte[]> packets = new ArrayList<>();
        int lastPacket = Math.max(bytes.size() - packetCount, 0);

        for (int x = bytes.size(); x > lastPacket; x--) {
            packets.addFirst(bytes.get(x - 1));
        }

        for (byte[] bs : packets) {
            size += bs.length;
        }
        byte[] decodedData = new byte[size];
        int i = 0;
        for (byte[] bs : packets) {
            for (byte b : bs) {
                decodedData[i++] = b;
            }
        }
        return decodedData;
    }

    private static File createClipFile(byte[] decodedData, String name) {
        try {
            File file = new File(name + ".wav");
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(decodedData), AudioReceiveHandler.OUTPUT_FORMAT, decodedData.length), AudioFileFormat.Type.WAVE, file);
            return file;
        } catch (IOException e) {
            logger.error("Failed to create clip file");
        }

        return null;
    }

    /**
     * Sets the listener for the given guild.
     *
     * @param guild    the guild to set the listener for
     * @param listener the listener
     */
    public void setListener(Guild guild, Guild listener) {
        listeners.put(guild, listener);
    }

    /**
     * Removes the listener from the map.
     *
     * @param guild the guild to remove
     */
    public static void stopListening(Guild guild) {
        listeners.remove(guild);
        listeners.values().remove(guild);
    }

    /**
     * Checks if the bot is listening to audio in the given guild.
     *
     * @param listener the guild to check
     * @return true if the bot is listening to audio in the given guild, false otherwise
     */
    public static boolean isListening(Guild listener) {
        return listeners.containsValue(listener);
    }
}