package bobo.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * This is a wrapper around AudioPlayer which makes it behave as an AudioSendHandler for JDA. As JDA calls canProvide
 * before every call to provide20MsAudio(), we pull the frame in canProvide() and use the frame we already pulled in
 * provide20MsAudio().
 */
public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer player;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;

    /**
     * @param audioPlayer Audio player to wrap.
     */
    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.player = audioPlayer;
        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
    }

    /**
     * @return true if audio can provide
     */
    @Override
    public boolean canProvide() {
        return this.player.provide(this.frame);
    }

    /**
     * @return 20ms of audio data
     */
    @Override
    public ByteBuffer provide20MsAudio() {
        ((Buffer) buffer).flip();
        return buffer;
    }

    /**
     * @return true as the audio is opus
     */
    @Override
    public boolean isOpus() {
        return true;
    }
}