package bobo.utils;

import bobo.commands.voice.ClipCommand;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
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
import java.util.ArrayList;
import java.util.List;

public class AudioReceiveListener implements AudioReceiveHandler {
    private static final Logger logger = LoggerFactory.getLogger(AudioReceiveListener.class);

    private final List<byte[]> receivedBytes = new ArrayList<>();
    private final double volume;
    private static final int MAX_TIME = 30 * 1000 / 20;

    public AudioReceiveListener(double volume) {
        this.volume = volume;
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public void handleCombinedAudio(@Nonnull CombinedAudio combinedAudio) {
        try {
            if (receivedBytes.size() > MAX_TIME)
            {
                receivedBytes.removeFirst();
            }
            receivedBytes.add(combinedAudio.getAudioData(volume));
        } catch (OutOfMemoryError e) {
            logger.error("Failed to handle combined audio");
        }
    }

    /**
     * Creates a {@link ClipCommand.ClipResult} containing the file, waveform, and duration of the clip.
     *
     * @param seconds the number of seconds to record
     * @param name    the name of the file
     * @return the {@link ClipCommand.ClipResult}, or null if the clip could not be created
     */
    @Nullable
    public ClipCommand.ClipResult createClip(int seconds, String name) {
        int packetCount = (seconds * 1000) / 20;
        File file;
        try {
            int size = 0;
            List<byte[]> packets = new ArrayList<>();
            int lastPacket = Math.max(receivedBytes.size() - packetCount, 0);

            for (int x = receivedBytes.size(); x > lastPacket; x--) {
                packets.addFirst(receivedBytes.get(x - 1));
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

            byte[] waveform = generateWaveform(decodedData);
            int duration = calculateClipDuration(decodedData);

            file = new File(name + ".wav");
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(decodedData), AudioReceiveHandler.OUTPUT_FORMAT, decodedData.length), AudioFileFormat.Type.WAVE, file);
            return new ClipCommand.ClipResult(file, waveform, duration);
        } catch (IOException | OutOfMemoryError e) {
            logger.error("Failed to create clip");
        }

        return null;
    }

    /**
     * Generates a waveform from the decoded audio data.
     *
     * @param decodedData the decoded audio data
     * @return the waveform byte array
     */
    public static byte[] generateWaveform(byte[] decodedData) {
        byte[] waveform = new byte[256];

        int step = Math.max(decodedData.length / 256, 1);
        for (int j = 0; j < waveform.length; j++) {
            int index = j * step;
            waveform[j] = decodedData[index];
        }

        return waveform;
    }

    /**
     * Calculates the duration of the clip from the audio data.
     *
     * @param audioData the audio data
     * @return the duration of the clip
     */
    @Contract(pure = true)
    public static int calculateClipDuration(@Nonnull byte[] audioData) {
        int bytesPerSample = 2;
        int sampleRate = 48000;
        int numChannels = 2;

        return audioData.length / (sampleRate * bytesPerSample * numChannels);
    }
}