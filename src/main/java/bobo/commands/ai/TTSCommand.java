package bobo.commands.ai;

import bobo.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import com.theokanning.openai.audio.CreateSpeechRequest;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

/* NOTE: This also classifies as a music command (as it must use lavaplayer to play tts),
   but Java doesn't allow multiple inheritance. However, I decided to make this an AI command
   since the AI is the bulk of the command's logic.
 */
public class TTSCommand extends AbstractAI {
    /**
     * Creates a new TTS command.
     */
    public TTSCommand() {
        super(Commands.slash("tts", "Sends a message to the AI to be said in the voice channel.")
                .addOption(OptionType.STRING, "message", "Message to be said.", true));
    }

    @Override
    public String getName() {
        return "tts";
    }

    @Override
    protected void handleAICommand() {
        event.deferReply().queue();

        AudioManager audioManager = event.getGuildChannel().getGuild().getAudioManager();
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

        PlayerManager playerManager = PlayerManager.getInstance();
        if (playerManager.getMusicManager(Objects.requireNonNull(event.getGuild())).player.getPlayingTrack() != null) {
            event.getHook().editOriginal("Can't use TTS while a track is playing.").queue();
            return;
        }

        String message = Objects.requireNonNull(event.getOption("message")).getAsString();
        CreateSpeechRequest createSpeechRequest = CreateSpeechRequest.builder()
                .model("tts-1")
                .input(message)
                .voice("onyx")
                .build();

        File file;
        try {
            byte[] bytes = service.createSpeech(createSpeechRequest).bytes();

            file = new File("tts.mp3");
            Files.write(file.toPath(), bytes);
        } catch (Exception e) {
            hook.editOriginal(e.getMessage()).queue();
            return;
        }

        playerManager.loadAndPlay(event, "tts.mp3", true);
        hook.editOriginal("Playing TTS: **" + message + "**").queue(success -> {
            if (!file.delete()) {
                System.err.println("Failed to delete file: " + file.getName());
            }
        });
    }
}
