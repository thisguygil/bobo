package bobo.lavaplayer;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AudioReceiveListener implements AudioReceiveHandler {
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
                receivedBytes.remove(0);
            }
            receivedBytes.add(combinedAudio.getAudioData(volume));
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a file with the audio data received in the last x seconds.
     *
     * @param seconds the number of seconds to record
     * @return the file created
     */
    public File createFile(int seconds, String name) {
        int packetCount = (seconds * 1000) / 20;
        File file;
        try {
            int size = 0;
            List<byte[]> packets = new ArrayList<>();
            int lastPacket = Math.max(receivedBytes.size() - packetCount, 0);

            for (int x = receivedBytes.size(); x > lastPacket; x--) {
                packets.add(0, receivedBytes.get(x - 1));
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

            file = new File(name + ".wav");
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(decodedData), AudioReceiveHandler.OUTPUT_FORMAT, decodedData.length), AudioFileFormat.Type.WAVE, file);
        } catch (IOException | OutOfMemoryError e) {
            file = null;
            e.printStackTrace();
        }

        return file;
    }
}