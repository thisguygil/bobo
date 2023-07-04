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
            if (receivedBytes.size() > MAX_TIME) // records only past MAX_TIME seconds
            {
                receivedBytes.remove(0);
            }
            receivedBytes.add(combinedAudio.getAudioData(volume));
        } catch (OutOfMemoryError e) {
            // close connection
        }
    }

    /**
     * Creates a wav file from the received bytes.
     *
     * @param outFile the file to write to
     * @param decodedData the audio data
     * @throws IOException if an I/O error occurs
     */
    private void getWavFile(File outFile, byte[] decodedData) throws IOException {
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(decodedData), AudioReceiveHandler.OUTPUT_FORMAT,
                decodedData.length), AudioFileFormat.Type.WAVE, outFile);
    }

    /**
     * Generates a random alphanumeric string of length 16.
     *
     * @return the random alphanumeric string
     */
    private static String getAlphaNumericString() {
        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(16);

        for (int i = 0; i < 16; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index = (int) (AlphaNumericString.length() * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Creates a file with the audio data received in the last x seconds.
     *
     * @param seconds the number of seconds to record
     * @return the file created
     */
    public File createFile(int seconds) {
        int packetCount = (seconds * 1000) / 20; // number of milliseconds you want to record / data sent every 20ms
        File file;
        try {
            int size = 0;
            List<byte[]> packets = new ArrayList<>();
            int lastPacket = Math.max(receivedBytes.size() - packetCount, 0);

            // add audio data (receivedBytes) for specified length of time (packetCount)
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

            file = new File(getAlphaNumericString() + ".wav");
            getWavFile(file, decodedData);
        } catch (IOException | OutOfMemoryError e) {
            file = null;
            e.printStackTrace();
        }

        return file;
    }
}
