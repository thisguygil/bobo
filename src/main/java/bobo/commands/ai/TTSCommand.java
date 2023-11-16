package bobo.commands.ai;

import bobo.commands.voice.JoinCommand;
import bobo.lavaplayer.PlayerManager;
import bobo.utils.SQLConnection;
import com.theokanning.openai.audio.CreateSpeechRequest;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/* NOTE: This also classifies as a music command (as it must use lavaplayer to play tts),
   but Java doesn't allow multiple inheritance. However, I decided to make this an AI command
   since the AI is the bulk of the command's logic.
 */
public class TTSCommand extends AbstractAI {
    private static final String selectSQL = "SELECT voice FROM tts_voice WHERE guild_id = ?";

    /**
     * Creates a new TTS command.
     */
    public TTSCommand() {
        super(Commands.slash("tts", "Generates an AI voice message for the voice channel.")
                .addOption(OptionType.STRING, "message", "Message to be said.", true));
    }

    @Override
    public String getName() {
        return "tts";
    }

    @Override
    protected void handleAICommand() {
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

        if (Objects.requireNonNull(guild.getSelfMember().getVoiceState()).isMuted()) {
            hook.editOriginal("I can't be muted when using this command.").queue();
            return;
        }

        PlayerManager playerManager = PlayerManager.getInstance();
        if (playerManager.getMusicManager(guild).player.getPlayingTrack() != null) {
            event.getHook().editOriginal("Can't use TTS while a track is playing.").queue();
            return;
        }

        String voice = "onyx";
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            statement.setString(1, guild.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                voice = resultSet.getString("voice");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String message = Objects.requireNonNull(event.getOption("message")).getAsString();
        CreateSpeechRequest createSpeechRequest = CreateSpeechRequest.builder()
                .model("tts-1")
                .input(message)
                .voice(voice)
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
        hook.editOriginal("**Playing TTS:** " + message).queue(success -> {
            if (!file.delete()) {
                System.err.println("Failed to delete file: " + file.getName());
            }
        });
    }
}
